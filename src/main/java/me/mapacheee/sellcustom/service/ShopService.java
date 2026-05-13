package me.mapacheee.sellcustom.service;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.sellcustom.config.ScConfig;
import me.mapacheee.sellcustom.data.CustomItem;
import me.mapacheee.sellcustom.data.CustomItemStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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

    public int sellItem(Player player, CustomItem shopItem, int amount) {
        if (!shopItem.isCanSell()) return 0;

        int removed = removeMatchingItems(player, shopItem, amount);
        if (removed <= 0) return 0;

        double totalEarn = shopItem.getSellPrice() * removed;

        if (economyService.isEnabled()) {
            economyService.depositMoney(player, totalEarn);
        }

        logger.info("Player {} sold {} x {} for ${}", player.getName(), removed, shopItem.getName(), totalEarn);
        return removed;
    }

    public List<ItemStack> findPlayerItems(Player player, CustomItem shopItem) {
        List<ItemStack> foundItems = new ArrayList<>();
        Material targetMaterial = Material.getMaterial(shopItem.getMaterial().toUpperCase());

        if (targetMaterial == null) return foundItems;

        for (ItemStack item : player.getInventory()) {
            if (item == null) continue;
            if (matchesCustomItem(item, shopItem)) {
                foundItems.add(item);
            }
        }

        return foundItems;
    }

    public int countItemsInInventory(Player player, CustomItem shopItem) {
        int count = 0;

        for (ItemStack item : player.getInventory()) {
            if (item == null) continue;
            if (matchesCustomItem(item, shopItem)) {
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
                meta.displayName(deserializeText(item.getName()));
            }

            if (item.getLore() != null && !item.getLore().isEmpty()) {
                var lore = item.getLore().stream()
                        .map(this::deserializeText)
                        .toList();
                meta.lore(lore);
            }

            if (item.getCustomModelData() > 0) {
                meta.setCustomModelData(item.getCustomModelData());
            }

            if (material == Material.PLAYER_HEAD && item.getHeadTexture() != null && !item.getHeadTexture().isEmpty()) {
                applyHeadTexture(meta, item.getHeadTexture());
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

        String displayName = materialName;
        if (handItem.getItemMeta() != null && handItem.getItemMeta().displayName() != null) {
            displayName = MiniMessage.miniMessage().serialize(handItem.getItemMeta().displayName());
        }

        CustomItem newItem = new CustomItem(
                itemId,
                displayName,
                materialName,
                buyPrice,
                sellPrice
        );

        if (handItem.getItemMeta() != null) {
            ItemMeta meta = handItem.getItemMeta();
            if (meta.lore() != null) {
                newItem.setLore(Objects.requireNonNull(meta.lore()).stream()
                        .map(c -> net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(c))
                        .toList());
            }
            if (meta.hasCustomModelData()) {
                newItem.setCustomModelData(meta.getCustomModelData());
            }
        }

        if (handItem.getType() == Material.PLAYER_HEAD) {
            newItem.setHeadTexture(extractHeadTexture(handItem));
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

    private int removeMatchingItems(Player player, CustomItem shopItem, int amount) {
        int remaining = amount;

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (remaining <= 0) break;
            ItemStack stack = contents[i];
            if (stack == null) continue;
            if (!matchesCustomItem(stack, shopItem)) continue;

            int take = Math.min(stack.getAmount(), remaining);
            int newAmount = stack.getAmount() - take;
            remaining -= take;

            if (newAmount <= 0) {
                contents[i] = null;
            } else {
                stack.setAmount(newAmount);
                contents[i] = stack;
            }
        }

        player.getInventory().setContents(contents);
        return amount - remaining;
    }

    public boolean matchesCustomItem(ItemStack stack, CustomItem shopItem) {
        if (stack == null) return false;

        Material targetMaterial = Material.getMaterial(shopItem.getMaterial().toUpperCase());
        if (targetMaterial == null || stack.getType() != targetMaterial) return false;

        ItemMeta meta = stack.getItemMeta();

        if (shopItem.getCustomModelData() > 0) {
            if (meta == null || !meta.hasCustomModelData()) return false;
            if (meta.getCustomModelData() != shopItem.getCustomModelData()) return false;
        }

        if (targetMaterial == Material.PLAYER_HEAD && shopItem.getHeadTexture() != null && !shopItem.getHeadTexture().isEmpty()) {
            String currentTexture = extractHeadTexture(stack);
            if (currentTexture == null || !currentTexture.equals(shopItem.getHeadTexture())) return false;
        }

        if (shopItem.getLore() != null && !shopItem.getLore().isEmpty()) {
            if (meta == null || meta.lore() == null) return false;
            List<String> targetLore = shopItem.getLore().stream()
                    .map(this::normalizePlain)
                    .toList();
            List<String> currentLore = meta.lore().stream()
                    .map(PlainTextComponentSerializer.plainText()::serialize)
                    .map(String::trim)
                    .toList();
            if (!currentLore.equals(targetLore)) return false;
        }

        if (shopItem.getName() != null && !shopItem.getName().isEmpty()) {
            if (meta != null && meta.displayName() != null) {
                String expectedPlain = normalizePlain(shopItem.getName());
                String currentPlain = PlainTextComponentSerializer.plainText().serialize(meta.displayName()).trim();
                if (!currentPlain.equals(expectedPlain)) return false;
            }
        }

        return true;
    }

    private String normalizePlain(String input) {
        return PlainTextComponentSerializer.plainText().serialize(deserializeText(input)).trim();
    }

    private Component deserializeText(String input) {
        if (input == null) return Component.empty();
        if (input.indexOf('&') >= 0) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(input);
        }
        if (input.indexOf('§') >= 0) {
            return LegacyComponentSerializer.legacySection().deserialize(input);
        }
        return MiniMessage.miniMessage().deserialize(input);
    }

    public String extractHeadTexture(ItemStack stack) {
        if (stack == null || stack.getType() != Material.PLAYER_HEAD) return null;
        ItemMeta meta = stack.getItemMeta();
        if (!(meta instanceof SkullMeta skullMeta)) return null;
        PlayerProfile profile = skullMeta.getPlayerProfile();
        if (profile == null) return null;
        for (ProfileProperty property : profile.getProperties()) {
            if ("textures".equals(property.getName())) {
                return property.getValue();
            }
        }
        return null;
    }

    private void applyHeadTexture(ItemMeta meta, String texture) {
        if (!(meta instanceof SkullMeta skullMeta)) return;
        PlayerProfile profile = skullMeta.getPlayerProfile();
        if (profile == null) {
            profile = Bukkit.createProfile(UUID.randomUUID());
        }
        profile.getProperties().removeIf(property -> "textures".equals(property.getName()));
        profile.getProperties().add(new ProfileProperty("textures", texture));
        skullMeta.setPlayerProfile(profile);
    }
}