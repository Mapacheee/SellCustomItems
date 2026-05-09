package me.mapacheee.sellcustom.gui;

import com.google.inject.Inject;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.sellcustom.config.ScConfig;
import me.mapacheee.sellcustom.config.ScMessages;
import me.mapacheee.sellcustom.data.CustomItem;
import me.mapacheee.sellcustom.service.EconomyService;
import me.mapacheee.sellcustom.service.ShopService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public final class MainShopGui {

    private final ShopService shopService;
    private final EconomyService economyService;
    private final ScConfig config;
    private final ScMessages messages;

    private static final int ITEMS_PER_PAGE = 45;

    @Inject
    public MainShopGui(ShopService shopService, EconomyService economyService, ScConfig config, ScMessages messages) {
        this.shopService = shopService;
        this.economyService = economyService;
        this.config = config;
        this.messages = messages;
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
            inv.setItem(rows * 9 - 9, createNavigationItem("<green>Previous Page", "prev"));
        }
        if (page < totalPages) {
            inv.setItem(rows * 9 - 1, createNavigationItem("<green>Next Page", "next"));
        }

        ItemStack balanceItem = createBalanceItem(player);
        inv.setItem(rows * 9 - 5, balanceItem);

        player.openInventory(inv);
        player.setMetadata("shop_page", new org.bukkit.metadata.FixedMetadataValue(
                Bukkit.getPluginManager().getPlugin("SellCustomItems"), page));
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

            if (item.isCanBuy()) {
                lore.add("<gray>Buy: <green>" + economyService.formatMoney(item.getBuyPrice()));
            } else {
                lore.add("<gray>Buy: <red>Disabled");
            }

            if (item.isCanSell()) {
                lore.add("<gray>Sell: <green>" + economyService.formatMoney(item.getSellPrice()));
            } else {
                lore.add("<gray>Sell: <red>Disabled");
            }

            lore.add("<dark_gray>Click to sell");
            lore.add("<dark_gray>Shift+Click to buy");

            if (item.getAmount() > 1) {
                lore.add("<gray>Amount: " + item.getAmount());
            }

            meta.lore(lore.stream().map(l -> MiniMessage.miniMessage().deserialize(l)).toList());

            if (item.getCustomModelData() > 0) {
                meta.setCustomModelData(item.getCustomModelData());
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
            meta.lore(List.of(MiniMessage.miniMessage().deserialize("<gray>Your current balance")));
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createNavigationItem(String name, String action) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(name));
            item.setItemMeta(meta);
        }

        return item;
    }

    public void handleClick(Player player, ItemStack clicked, boolean shiftClick) {
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.lore() == null) return;

        String displayName = meta.displayName() != null ? Objects.requireNonNull(meta.displayName()).toString() : "";

        if (displayName.contains("Previous Page")) {
            int currentPage = 1;
            if (player.hasMetadata("shop_page")) {
                currentPage = player.getMetadata("shop_page").getFirst().asInt();
            }
            open(player, currentPage - 1);
            return;
        }

        if (displayName.contains("Next Page")) {
            int currentPage = 1;
            if (player.hasMetadata("shop_page")) {
                currentPage = player.getMetadata("shop_page").getFirst().asInt();
            }
            open(player, currentPage + 1);
            return;
        }

        List<Component> loreComponents = meta.lore();
        if (loreComponents == null || loreComponents.isEmpty()) return;

        List<String> lore = loreComponents.stream()
                .map(c -> MiniMessage.miniMessage().serialize(c))
                .toList();

        for (CustomItem item : shopService.getEnabledItems()) {
            String buyLine = "<gray>Buy: ";
            String sellLine = "<gray>Sell: ";

            boolean isShopItem = false;
            for (String line : lore) {
                if (line.contains("Buy:") || line.contains("Sell:")) {
                    isShopItem = true;
                    break;
                }
            }

            if (!isShopItem) continue;

            if (shiftClick && item.isCanBuy() && hasBuyLore(lore)) {
                openSellConfirmGui(player, item, true);
                return;
            } else if (!shiftClick && item.isCanSell() && hasSellLore(lore)) {
                openSellConfirmGui(player, item, false);
                return;
            }
        }
    }

    private boolean hasBuyLore(List<String> lore) {
        return lore.stream().anyMatch(l -> l.contains("Buy:") && !l.contains("Disabled"));
    }

    private boolean hasSellLore(List<String> lore) {
        return lore.stream().anyMatch(l -> l.contains("Sell:") && !l.contains("Disabled"));
    }

    public void openSellConfirmGui(Player player, CustomItem item, boolean buying) {
        Inventory inv = Bukkit.createInventory(null, 9, MiniMessage.miniMessage().deserialize(
                buying ? messages.confirmPurchaseTitle() : messages.confirmSellTitle()
        ));

        ItemStack itemStack = shopService.createItemStack(item, 1);
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null) {
            List<String> lore = new ArrayList<>();
            if (buying) {
                for (String line : messages.confirmPurchaseLore()) {
                    if (line.contains("<price>")) {
                        lore.add(line.replace("<price>", economyService.formatMoney(item.getBuyPrice())));
                    } else {
                        lore.add(line);
                    }
                }
            } else {
                for (String line : messages.confirmSellLore()) {
                    if (line.contains("<price>")) {
                        lore.add(line.replace("<price>", economyService.formatMoney(item.getSellPrice())));
                    } else {
                        lore.add(line);
                    }
                }
            }
            meta.lore(lore.stream().map(l -> MiniMessage.miniMessage().deserialize(l)).toList());
            itemStack.setItemMeta(meta);
        }

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(MiniMessage.miniMessage().deserialize(" "));
        glass.setItemMeta(glassMeta);

        for (int i = 0; i < 9; i++) {
            if (i != 4) {
                inv.setItem(i, glass);
            }
        }

        inv.setItem(4, itemStack);

        player.setMetadata("shop_item", new org.bukkit.metadata.FixedMetadataValue(
                Bukkit.getPluginManager().getPlugin("SellCustomItems"), item.getId()));
        player.setMetadata("shop_buying", new org.bukkit.metadata.FixedMetadataValue(
                Bukkit.getPluginManager().getPlugin("SellCustomItems"), buying));

        player.openInventory(inv);
    }
}