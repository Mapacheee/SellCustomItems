package me.mapacheee.sellcustom.gui;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.sellcustom.SellCustomItemsPlugin;
import me.mapacheee.sellcustom.config.ScConfig;
import me.mapacheee.sellcustom.config.ScMessages;
import me.mapacheee.sellcustom.data.CustomItem;
import me.mapacheee.sellcustom.service.EconomyService;
import me.mapacheee.sellcustom.service.ShopService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

@Service
public final class MainShopGui {

    private final ShopService shopService;
    private final EconomyService economyService;
    private final ScConfig config;
    private final ScMessages messages;

    private static final int ITEMS_PER_PAGE = 45;

    private final NamespacedKey actionKey;
    private final NamespacedKey itemKey;
    @Inject
    public MainShopGui(ShopService shopService, EconomyService economyService, Container<ScConfig> configContainer, Container<ScMessages> messagesContainer) {
        this.shopService = shopService;
        this.economyService = economyService;
        this.config = configContainer.get();
        this.messages = messagesContainer.get();
        this.actionKey = new NamespacedKey(SellCustomItemsPlugin.getInstance(), "sc_action");
        this.itemKey = new NamespacedKey(SellCustomItemsPlugin.getInstance(), "sc_item_id");
    }

    public void open(Player player, int page) {
        List<CustomItem> items = shopService.getEnabledItems();
        int totalPages = (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE);

        if (page < 1) page = 1;
        if (totalPages == 0) page = 1;
        if (page > totalPages && totalPages > 0) page = totalPages;

        int rows = config.guiRows();
        Inventory inv = Bukkit.createInventory(null, rows * 9, MiniMessage.miniMessage().deserialize(
                config.guiTitle(),
                Placeholder.parsed("page", String.valueOf(page)),
                Placeholder.parsed("pages", String.valueOf(Math.max(1, totalPages)))
        ));

        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());

        for (int i = startIndex; i < endIndex; i++) {
            CustomItem item = items.get(i);
            int slot = item.getSlot() >= 0 ? item.getSlot() : i - startIndex;

            if (slot < rows * 9) {
                inv.setItem(slot, createShopItem(item));
            }
        }

        if (page > 1) {
            inv.setItem(rows * 9 - 9, createNavigationItem(messages.shopPrevPage(), "prev"));
        }
        if (page < totalPages) {
            inv.setItem(rows * 9 - 1, createNavigationItem(messages.shopNextPage(), "next"));
        }

        ItemStack balanceItem = createBalanceItem(player);
        inv.setItem(rows * 9 - 5, balanceItem);

        player.openInventory(inv);
        player.setMetadata("shop_page", new org.bukkit.metadata.FixedMetadataValue(
                Bukkit.getPluginManager().getPlugin("SellCustomItems"), page));
        player.setMetadata("sc_gui", new org.bukkit.metadata.FixedMetadataValue(
                Bukkit.getPluginManager().getPlugin("SellCustomItems"), "main_shop"));
    }

    @SuppressWarnings("deprecation")
    private ItemStack createShopItem(CustomItem item) {
        Material material = Material.getMaterial(item.getMaterial().toUpperCase());
        if (material == null) material = Material.STONE;

        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();

        if (meta != null) {
            if (item.getName() != null && !item.getName().isEmpty()) {
                meta.displayName(MiniMessage.miniMessage().deserialize(item.getName()));
            }

            List<String> lore = new ArrayList<>();

            if (item.isCanSell()) {
                lore.add(messages.shopSellLine().replace("<price>", economyService.formatMoney(item.getSellPrice())));
                lore.add(messages.shopClickSell());
                lore.add(messages.shopClickSellAll());
            } else {
                lore.add(messages.shopSellDisabled());
            }


            if (item.getAmount() > 1) {
                lore.add(messages.shopAmountLine().replace("<amount>", String.valueOf(item.getAmount())));
            }

            meta.lore(lore.stream().map(l -> MiniMessage.miniMessage().deserialize(l)).toList());

            if (item.getCustomModelData() > 0) {
                meta.setCustomModelData(item.getCustomModelData());
            }

            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "shop_item");
            if (item.getId() != null && !item.getId().isEmpty()) {
                meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, item.getId());
            }
            stack.setItemMeta(meta);
        }

        return stack;
    }

    private ItemStack createBalanceItem(Player player) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            double balance = economyService.getBalance(player);
            String balanceStr = economyService.formatMoney(balance);
            meta.displayName(MiniMessage.miniMessage().deserialize(messages.balance()
                    .replace("<balance>", balanceStr)));
            meta.lore(List.of(MiniMessage.miniMessage().deserialize(messages.balanceLore())));
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createNavigationItem(String name, String action) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(name));
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }

        return item;
    }

    // Confirm GUI removed: shop now sells directly from the main GUI.
}
