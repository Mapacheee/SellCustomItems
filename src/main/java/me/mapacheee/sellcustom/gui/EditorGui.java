package me.mapacheee.sellcustom.gui;

import com.google.inject.Inject;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.sellcustom.config.ScMessages;
import me.mapacheee.sellcustom.data.CustomItem;
import me.mapacheee.sellcustom.service.ShopService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public final class EditorGui {

    private final ShopService shopService;
    private final ScMessages messages;

    @Inject
    public EditorGui(ShopService shopService, ScMessages messages) {
        this.shopService = shopService;
        this.messages = messages;
    }

    public void openMainEditor(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MiniMessage.miniMessage().deserialize(messages.editorTitle()));

        Map<String, CustomItem> items = shopService.getAllItems();

        int slot = 0;
        for (CustomItem item : items.values()) {
            if (slot >= 45) break;
            inv.setItem(slot, createEditorItem(item));
            slot++;
        }

        ItemStack addButton = createButton(Material.LIME_WOOL, messages.editorAddItem(), "<green>Click to add item from hand");
        inv.setItem(48, addButton);

        ItemStack saveButton = createButton(Material.GREEN_CONCRETE, messages.editorSave(), "<green>Click to save all");
        inv.setItem(50, saveButton);

        ItemStack closeButton = createButton(Material.RED_CONCRETE, messages.editorCancel(), "<red>Click to close");
        inv.setItem(53, closeButton);

        for (int i = 45; i < 54; i++) {
            if (inv.getItem(i) == null) {
                ItemStack border = createBorder();
                inv.setItem(i, border);
            }
        }

        player.openInventory(inv);
    }

    public void openItemEditor(Player player, CustomItem item) {
        Inventory inv = Bukkit.createInventory(null, 36, MiniMessage.miniMessage().deserialize(
                "<dark_gray>Editing: " + (item.getName() != null ? item.getName() : item.getId())
        ));

        inv.setItem(2, createInfoItem(Material.PAPER, messages.editorItemName(), item.getName()));
        inv.setItem(3, createInfoItem(Material.GOLD_INGOT, messages.editorItemPrice(), String.valueOf(item.getBuyPrice())));
        inv.setItem(4, createInfoItem(Material.GOLD_NUGGET, messages.editorItemSellPrice(), String.valueOf(item.getSellPrice())));
        inv.setItem(5, createInfoItem(Material.HOPPER, messages.editorItemSlot(), String.valueOf(item.getSlot())));

        inv.setItem(11, createToggleItem(Material.LIME_CONCRETE, messages.editorItemEnabled(), item.isEnabled()));
        inv.setItem(12, createToggleItem(item.isCanBuy() ? Material.LIME_CONCRETE : Material.RED_CONCRETE, messages.editorItemCanBuy(), item.isCanBuy()));
        inv.setItem(13, createToggleItem(item.isCanSell() ? Material.LIME_CONCRETE : Material.RED_CONCRETE, messages.editorItemCanSell(), item.isCanSell()));

        ItemStack materialItem = new ItemStack(Material.getMaterial(item.getMaterial().toUpperCase()) != null
                ? Objects.requireNonNull(Material.getMaterial(item.getMaterial().toUpperCase())) : Material.STONE);
        ItemMeta meta = materialItem.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(messages.editorItemMaterial() + ": <white>" + item.getMaterial()));
            materialItem.setItemMeta(meta);
        }
        inv.setItem(14, materialItem);

        ItemStack loreItem = createButton(Material.BOOK, messages.editorItemLore(), item.getLore().isEmpty() ? "<gray>No lore" : String.join("\n", item.getLore()));
        inv.setItem(15, loreItem);

        ItemStack deleteButton = createButton(Material.RED_WOOL, "<red>Delete Item", "<red>Click to delete this item");
        inv.setItem(18, deleteButton);

        ItemStack previewItem = shopService.createItemStack(item, 1);
        inv.setItem(22, previewItem);

        ItemStack buyUp = createArrow(Material.GREEN_STAINED_GLASS_PANE, "Increase Buy Price");
        ItemStack buyDown = createArrow(Material.RED_STAINED_GLASS_PANE, "Decrease Buy Price");
        inv.setItem(28, buyUp);
        inv.setItem(29, buyDown);

        ItemStack sellUp = createArrow(Material.GREEN_STAINED_GLASS_PANE, "Increase Sell Price");
        ItemStack sellDown = createArrow(Material.RED_STAINED_GLASS_PANE, "Decrease Sell Price");
        inv.setItem(30, sellUp);
        inv.setItem(31, sellDown);

        ItemStack slotLeft = createArrow(Material.GREEN_STAINED_GLASS_PANE, "Decrease Slot");
        ItemStack slotRight = createArrow(Material.RED_STAINED_GLASS_PANE, "Increase Slot");
        inv.setItem(32, slotLeft);
        inv.setItem(33, slotRight);

        ItemStack saveButton = createButton(Material.GREEN_CONCRETE, messages.editorSave(), "<green>Click to save");
        inv.setItem(22, saveButton);

        ItemStack closeButton = createButton(Material.RED_CONCRETE, messages.editorCancel(), "<red>Click to close");
        inv.setItem(35, closeButton);

        for (int i = 0; i < 36; i++) {
            if (inv.getItem(i) == null) {
                ItemStack border = createBorder();
                inv.setItem(i, border);
            }
        }

        player.setMetadata("editing_item", new org.bukkit.metadata.FixedMetadataValue(
            Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("SellCustomItems")), item.getId()));

        player.openInventory(inv);
    }

    private ItemStack createEditorItem(CustomItem item) {
        Material material = Material.getMaterial(item.getMaterial().toUpperCase());
        if (material == null) material = Material.STONE;

        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();

        if (meta != null) {
            String name = item.getName() != null && !item.getName().isEmpty() ? item.getName() : item.getId();
            meta.displayName(MiniMessage.miniMessage().deserialize("<white>" + name));

            List<String> lore = new ArrayList<>();
            lore.add("<gray>Buy: <green>$" + item.getBuyPrice());
            lore.add("<gray>Sell: <green>$" + item.getSellPrice());
            lore.add("<gray>Slot: <white>" + item.getSlot());
            lore.add(item.isEnabled() ? "<green>Enabled" : "<red>Disabled");
            lore.add("");
            lore.add("<dark_gray>" + messages.editorClickItem());

            meta.lore(lore.stream().map(l -> MiniMessage.miniMessage().deserialize(l)).toList());
            stack.setItemMeta(meta);
        }

        return stack;
    }

    private ItemStack createButton(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(name));
            meta.lore(List.of(MiniMessage.miniMessage().deserialize(lore)));
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createInfoItem(Material material, String name, String value) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(name));
            meta.lore(List.of(MiniMessage.miniMessage().deserialize("<white>" + (value != null ? value : "None"))));
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createToggleItem(Material material, String name, boolean enabled) {
        Material actualMaterial = enabled ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
        ItemStack item = new ItemStack(actualMaterial);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(name));
            meta.lore(List.of(enabled ? MiniMessage.miniMessage().deserialize("<green>Enabled") : MiniMessage.miniMessage().deserialize("<red>Disabled")));
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createArrow(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(name));
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createBorder() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(" "));
            item.setItemMeta(meta);
        }

        return item;
    }
}