package me.mapacheee.sellcustom.listener;

import com.google.inject.Inject;
import com.thewinterframework.paper.listener.ListenerComponent;
import me.mapacheee.sellcustom.data.CustomItem;
import me.mapacheee.sellcustom.data.CustomItemStorage;
import me.mapacheee.sellcustom.gui.EditorGui;
import me.mapacheee.sellcustom.gui.MainShopGui;
import me.mapacheee.sellcustom.service.EconomyService;
import me.mapacheee.sellcustom.service.ShopService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@ListenerComponent
public final class ShopListener implements Listener {

    private final ShopService shopService;
    private final EconomyService economyService;
    private final MainShopGui mainShopGui;
    private final EditorGui editorGui;
    private final CustomItemStorage itemStorage;

    @Inject
    public ShopListener(ShopService shopService, EconomyService economyService, MainShopGui mainShopGui,
                        EditorGui editorGui, CustomItemStorage itemStorage) {
        this.shopService = shopService;
        this.economyService = economyService;
        this.mainShopGui = mainShopGui;
        this.editorGui = editorGui;
        this.itemStorage = itemStorage;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        boolean shiftClick = event.isShiftClick();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

        event.getView().title();
        String title = event.getView().title() != null ? event.getView().title().toString() : "";

        boolean isEditor = title.contains("Editor") || title.contains("Editing:");
        boolean isConfirmShop = title.contains("Confirm") || title.contains("Purchase");
        boolean isMainShop = title.contains("Shop") || title.contains("Custom Items");

        if (isEditor) {
            handleEditorClick(player, clicked, event.getSlot());
            event.setCancelled(true);
        } else if (isConfirmShop) {
            handleConfirmClick(player, clicked);
            event.setCancelled(true);
        } else if (isMainShop) {
            handleMainShopClick(player, clicked, shiftClick);
            event.setCancelled(true);
        }
    }

    private void handleMainShopClick(Player player, ItemStack clicked, boolean shiftClick) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.lore() == null) return;

        List<Component> loreComponents = meta.lore();
        if (loreComponents == null || loreComponents.isEmpty()) return;

        List<String> lore = loreComponents.stream()
                .map(c -> MiniMessage.miniMessage().serialize(c))
                .toList();

        for (CustomItem item : shopService.getEnabledItems()) {
            String itemName = item.getName() != null ? item.getName() : item.getId();

            boolean hasBuy = lore.stream().anyMatch(l -> l.contains("Buy:") && !l.contains("Disabled"));
            boolean hasSell = lore.stream().anyMatch(l -> l.contains("Sell:") && !l.contains("Disabled"));

            if (shiftClick && hasBuy && item.isCanBuy()) {
                mainShopGui.openSellConfirmGui(player, item, true);
                return;
            } else if (!shiftClick && hasSell && item.isCanSell()) {
                openSellAmountGui(player, item);
                return;
            }
        }
    }

    private void openSellAmountGui(Player player, CustomItem item) {
        int available = shopService.countItemsInInventory(player, item);

        if (available == 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have any items to sell!"));
            return;
        }

        Bukkit.getScheduler().runTask(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("SellCustomItems")), () -> {
            player.closeInventory();
        });

        String formattedPrice = economyService.formatMoney(item.getSellPrice() * available);
        player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<dark_gray>You have <gold>" + available + " <gray>x " + item.getName() +
                        "<dark_gray>. Each sells for <green>" + economyService.formatMoney(item.getSellPrice()) +
                        "<dark_gray>. Total: <green>" + formattedPrice
        ));
        player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<dark_gray>Click an item in your inventory to sell it (or type /sc sell <amount>)"
        ));

        player.setMetadata("sell_item", new org.bukkit.metadata.FixedMetadataValue(
            Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("SellCustomItems")), item.getId()));
    }

    private void handleConfirmClick(Player player, ItemStack clicked) {
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        if (!player.hasMetadata("shop_item") || !player.hasMetadata("shop_buying")) return;

        String itemId = player.getMetadata("shop_item").getFirst().asString();
        boolean buying = player.getMetadata("shop_buying").getFirst().asBoolean();

        CustomItem item = shopService.getItem(itemId);
        if (item == null) return;

        if (buying) {
            if (economyService.hasMoney(player, item.getBuyPrice())) {
                if (shopService.buyItem(player, item, 1)) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<green>You purchased " + item.getName() + " for " + economyService.formatMoney(item.getBuyPrice())
                    ));
                } else {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to purchase item!"));
                }
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Not enough money!"));
            }
        } else {
            int count = shopService.countItemsInInventory(player, item);
            if (count > 0) {
                shopService.sellItem(player, item, player.getInventory().getItem(0), count);
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<green>You sold " + count + " x " + item.getName() + " for " + economyService.formatMoney(item.getSellPrice() * count)
                ));
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have any items to sell!"));
            }
        }

        player.removeMetadata("shop_item", Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("SellCustomItems")));
        player.removeMetadata("shop_buying", Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("SellCustomItems")));
        player.closeInventory();
    }

    private void handleEditorClick(Player player, ItemStack clicked, int slot) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String displayName = meta.displayName() != null ? Objects.requireNonNull(meta.displayName()).toString() : "";
        String plainName = MiniMessage.miniMessage().serialize(MiniMessage.miniMessage().deserialize(displayName));

        if (plainName.contains("Click to add item from hand")) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<dark_gray>Hold an item and run <white>/sc sell <price>"
            ));
            return;
        }

        if (plainName.contains("Click to save all") || plainName.contains("Click to save")) {
            itemStorage.save();
            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>All changes saved!"));
            player.closeInventory();
            return;
        }

        if (plainName.contains("Click to close") || plainName.contains("Cancel")) {
            player.closeInventory();
            return;
        }

        if (plainName.contains("Delete Item")) {
            if (player.hasMetadata("editing_item")) {
                String itemId = player.getMetadata("editing_item").getFirst().asString();
                itemStorage.removeItem(itemId);
                player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Item deleted!"));
                editorGui.openMainEditor(player);
            }
            return;
        }

        Map<String, CustomItem> items = shopService.getAllItems();
        for (CustomItem item : items.values()) {
            String itemName = item.getName() != null ? item.getName() : item.getId();
            if (displayName.contains(itemName) && !displayName.contains("Editing")) {
                editorGui.openItemEditor(player, item);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (player.hasMetadata("editing_item")) {
            player.removeMetadata("editing_item", Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("SellCustomItems")));
        }
        if (player.hasMetadata("shop_item")) {
            player.removeMetadata("shop_item", Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("SellCustomItems")));
        }
        if (player.hasMetadata("shop_buying")) {
            player.removeMetadata("shop_buying", Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("SellCustomItems")));
        }
    }
}