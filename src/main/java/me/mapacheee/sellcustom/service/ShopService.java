package me.mapacheee.sellcustom.service;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.sellcustom.config.ScConfig;
import me.mapacheee.sellcustom.data.CustomItem;
import me.mapacheee.sellcustom.data.CustomItemStorage;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public final class ShopService {

    private final CustomItemStorage itemStorage;
    private final EconomyService economyService;
    private final ScConfig config;
    private final Logger logger;

    @Inject
    public ShopService(CustomItemStorage itemStorage, EconomyService economyService, Container<ScConfig> configContainer, Logger logger) {
        this.itemStorage = itemStorage;
        this.economyService = economyService;
        this.config = configContainer.get();
        this.logger = logger;
    }

    public Map<String, CustomItem> getAllItems() {
        return itemStorage.getAllItems();
    }

    public List<CustomItem> getEnabledItems() {
        return itemStorage.getEnabledItems();
    }

    public CustomItem getItem(String id) {
        return itemStorage.getItem(id);
    }

    public boolean buyItem(Player player, CustomItem item, int amount) {
        if (!item.isEnabled()) return false;
        if (!item.isCanBuy()) return false;

        if (!economyService.isEnabled()) {
            return false;
        }

        double totalPrice = item.getBuyPrice() * amount;

        if (!economyService.hasMoney(player, totalPrice)) {
            return false;
        }

        if (!economyService.withdrawMoney(player, totalPrice)) {
            return false;
        }

        ItemStack itemStack = createItemStack(item, amount);
        var remaining = player.getInventory().addItem(itemStack);

        if (!remaining.isEmpty()) {
            economyService.depositMoney(player, totalPrice - (item.getBuyPrice() * (amount - remaining.values().iterator().next().getAmount())));
            return false;
        }

        logger.info("Player {} bought {} x {} for ${}", player.getName(), amount, item.getName(), totalPrice);
        return true;
    }

    public int sellItem(Player player, CustomItem shopItem, ItemStack playerItem, int amount) {
        if (!shopItem.isCanSell()) return 0;

        ItemStack toRemove = playerItem.clone();
        toRemove.setAmount(amount);

        player.getInventory().removeItem(toRemove);

        double totalEarn = shopItem.getSellPrice() * amount;

        if (economyService.isEnabled()) {
            economyService.depositMoney(player, totalEarn);
        }

        logger.info("Player {} sold {} x {} for ${}", player.getName(), amount, shopItem.getName(), totalEarn);
        return amount;
    }

    public List<ItemStack> findPlayerItems(Player player, CustomItem shopItem) {
        List<ItemStack> foundItems = new ArrayList<>();
        Material targetMaterial = Material.getMaterial(shopItem.getMaterial().toUpperCase());

        if (targetMaterial == null) return foundItems;

        for (ItemStack item : player.getInventory()) {
            if (item == null) continue;
            if (item.getType() == targetMaterial) {
                foundItems.add(item);
            }
        }

        return foundItems;
    }

    public int countItemsInInventory(Player player, CustomItem shopItem) {
        int count = 0;
        Material targetMaterial = Material.getMaterial(shopItem.getMaterial().toUpperCase());

        if (targetMaterial == null) return 0;

        for (ItemStack item : player.getInventory()) {
            if (item == null) continue;
            if (item.getType() == targetMaterial) {
                count += item.getAmount();
            }
        }

        return count;
    }

    @SuppressWarnings("deprecation")
    public ItemStack createItemStack(CustomItem item, int amount) {
        Material material = Material.getMaterial(item.getMaterial().toUpperCase());
        if (material == null) {
            material = Material.STONE;
        }

        ItemStack stack = new ItemStack(material, amount);

        var meta = stack.getItemMeta();
        if (meta != null) {
            if (item.getName() != null && !item.getName().isEmpty()) {
                meta.displayName(MiniMessage.miniMessage().deserialize(item.getName()));
            }

            if (item.getLore() != null && !item.getLore().isEmpty()) {
                var lore = item.getLore().stream()
                        .map(line -> MiniMessage.miniMessage().deserialize(line))
                        .toList();
                meta.lore(lore);
            }

            if (item.getCustomModelData() > 0) {
                meta.setCustomModelData(item.getCustomModelData());
            }

            stack.setItemMeta(meta);
        }

        return stack;
    }

    @SuppressWarnings("deprecation")
    public void createItemFromHand(Player player, double buyPrice, double sellPrice) {
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType() == Material.AIR) {
            return;
        }

        String materialName = handItem.getType().name();
        String itemId = materialName.toLowerCase() + "_" + System.currentTimeMillis();

        CustomItem newItem = new CustomItem(
                itemId,
                handItem.getItemMeta() != null && handItem.getItemMeta().displayName() != null
                        ? handItem.getItemMeta().displayName().toString()
                        : materialName,
                materialName,
                buyPrice,
                sellPrice
        );

        if (handItem.getItemMeta() != null) {
            if (handItem.getItemMeta().lore() != null) {
                newItem.setLore(Objects.requireNonNull(handItem.getItemMeta().lore()).stream()
                        .map(c -> net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(c))
                        .toList());
            }
            if (handItem.getItemMeta().getCustomModelData() != 0) {
                newItem.setCustomModelData(handItem.getItemMeta().getCustomModelData());
            }
        }

        newItem.setAmount(handItem.getAmount());

        int maxSlot = -1;
        for (CustomItem existing : itemStorage.getAllItems().values()) {
            if (existing.getSlot() > maxSlot) {
                maxSlot = existing.getSlot();
            }
        }
        newItem.setSlot(maxSlot + 1);

        itemStorage.addItem(newItem);
    }
}