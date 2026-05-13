package me.mapacheee.sellcustom.gui;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.sellcustom.SellCustomItemsPlugin;
import me.mapacheee.sellcustom.config.ScMessages;
import me.mapacheee.sellcustom.data.CustomItem;
import me.mapacheee.sellcustom.service.ShopService;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
import java.util.Map;
import java.util.Objects;

@Service
public final class EditorGui {

    private final ShopService shopService;
    private final ScMessages messages;

    private final NamespacedKey actionKey;
    private final NamespacedKey itemKey;

    @Inject
    public EditorGui(ShopService shopService, Container<ScMessages> messagesContainer) {
        this.shopService = shopService;
        this.messages = messagesContainer.get();
        this.actionKey = new NamespacedKey(SellCustomItemsPlugin.getInstance(), "sc_action");
        this.itemKey = new NamespacedKey(SellCustomItemsPlugin.getInstance(), "sc_item_id");
    }

    public void openMainEditor(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, MiniMessage.miniMessage().deserialize(messages.editorTitle()));

        Map<String, CustomItem> items = shopService.getAllItems();

        int slot = 0;
        for (Map.Entry<String, CustomItem> entry : items.entrySet()) {
            if (slot >= 36) break;
            inv.setItem(slot, createEditorItem(entry.getValue(), entry.getKey()));
            slot++;
        }

        ItemStack addButton = createButton(Material.LIME_WOOL, messages.editorAddItem(), messages.editorAddItemLore());
        tagAction(addButton, "editor_add", null);
        inv.setItem(36, addButton);

        ItemStack saveButton = createButton(Material.GREEN_CONCRETE, messages.editorSave(), messages.editorSaveAllLore());
        tagAction(saveButton, "editor_save", null);
        inv.setItem(38, saveButton);

        ItemStack closeButton = createButton(Material.RED_CONCRETE, messages.editorCancel(), messages.editorCancelLore());
        tagAction(closeButton, "editor_close", null);
        inv.setItem(44, closeButton);

        player.openInventory(inv);
        player.setMetadata("sc_gui", new org.bukkit.metadata.FixedMetadataValue(
                Bukkit.getPluginManager().getPlugin("SellCustomItems"), "editor_main"));
    }

    public void openItemEditor(Player player, CustomItem item) {
        String itemName = item.getName() != null && !item.getName().isEmpty() ? item.getName() : item.getId();
        Inventory inv = Bukkit.createInventory(null, 36, MiniMessage.miniMessage().deserialize(
                messages.editorItemTitle().replace("<item_name>", itemName != null ? itemName : "")
        ));

        ItemStack nameItem = createInfoItem(Material.PAPER, messages.editorItemName(), item.getName());
        tagAction(nameItem, "editor_edit_name", item.getId());
        inv.setItem(1, nameItem);

        ItemStack buyItem = createInfoItem(Material.GOLD_INGOT, messages.editorItemPrice(), String.valueOf(item.getBuyPrice()));
        tagAction(buyItem, "editor_edit_buy_price", item.getId());
        inv.setItem(2, buyItem);

        ItemStack sellItem = createInfoItem(Material.GOLD_NUGGET, messages.editorItemSellPrice(), String.valueOf(item.getSellPrice()));
        tagAction(sellItem, "editor_edit_sell_price", item.getId());
        inv.setItem(3, sellItem);

        ItemStack slotItem = createInfoItem(Material.HOPPER, messages.editorItemSlot(), String.valueOf(item.getSlot()));
        tagAction(slotItem, "editor_edit_slot", item.getId());
        inv.setItem(4, slotItem);

        ItemStack enabledToggle = createToggleItem(item.isEnabled() ? Material.LIME_CONCRETE : Material.RED_CONCRETE, messages.editorItemEnabled(), item.isEnabled());
        tagAction(enabledToggle, "editor_toggle_enabled", item.getId());
        inv.setItem(10, enabledToggle);

        ItemStack buyToggle = createToggleItem(item.isCanBuy() ? Material.LIME_CONCRETE : Material.RED_CONCRETE, messages.editorItemCanBuy(), item.isCanBuy());
        tagAction(buyToggle, "editor_toggle_can_buy", item.getId());
        inv.setItem(11, buyToggle);

        ItemStack sellToggle = createToggleItem(item.isCanSell() ? Material.LIME_CONCRETE : Material.RED_CONCRETE, messages.editorItemCanSell(), item.isCanSell());
        tagAction(sellToggle, "editor_toggle_can_sell", item.getId());
        inv.setItem(12, sellToggle);

        ItemStack materialItem = new ItemStack(Material.getMaterial(item.getMaterial().toUpperCase()) != null
                ? Objects.requireNonNull(Material.getMaterial(item.getMaterial().toUpperCase())) : Material.STONE);
        ItemMeta meta = materialItem.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(messages.editorItemMaterial() + " <white>" + item.getMaterial()));
            materialItem.setItemMeta(meta);
        }
        tagAction(materialItem, "editor_edit_material", item.getId());
        inv.setItem(13, materialItem);

        ItemStack loreItem = createButtonWithLoreLines(
                Material.BOOK,
                messages.editorItemLore(),
                item.getLore().isEmpty() ? List.of("<gray>" + messages.editorNone()) : item.getLore()
        );
        tagAction(loreItem, "editor_edit_lore", item.getId());
        inv.setItem(14, loreItem);

        ItemStack deleteButton = createButton(Material.RED_WOOL, messages.editorDelete(), messages.editorDeleteLore());
        tagAction(deleteButton, "editor_delete", item.getId());
        inv.setItem(16, deleteButton);

        ItemStack previewItem = shopService.createItemStack(item, 1);
        inv.setItem(22, previewItem);

        ItemStack saveButton = createButton(Material.GREEN_CONCRETE, messages.editorSave(), messages.editorSaveLore());
        tagAction(saveButton, "editor_save", null);
        inv.setItem(27, saveButton);

        ItemStack closeButton = createButton(Material.RED_CONCRETE, messages.editorCancel(), messages.editorCancelLore());
        tagAction(closeButton, "editor_close", null);
        inv.setItem(35, closeButton);

        for (int i = 0; i < 36; i++) {
            if (inv.getItem(i) == null) {
                ItemStack border = createBorder();
                inv.setItem(i, border);
            }
        }

        player.setMetadata("editing_item", new org.bukkit.metadata.FixedMetadataValue(
            Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("SellCustomItems")), item.getId()));
        player.setMetadata("sc_gui", new org.bukkit.metadata.FixedMetadataValue(
                Bukkit.getPluginManager().getPlugin("SellCustomItems"), "editor_item"));

        player.openInventory(inv);
    }

    private ItemStack createEditorItem(CustomItem item, String fallbackId) {
        Material material = Material.getMaterial(item.getMaterial().toUpperCase());
        if (material == null) material = Material.STONE;

        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();

        if (meta != null) {
            String displayId = (item.getId() != null && !item.getId().isEmpty()) ? item.getId() : fallbackId;
            String name = item.getName() != null && !item.getName().isEmpty() ? item.getName() : displayId;
            meta.displayName(MiniMessage.miniMessage().deserialize("<white>" + name));

            List<String> lore = new ArrayList<>();
            lore.add(messages.editorListBuy().replace("<value>", String.valueOf(item.getBuyPrice())));
            lore.add(messages.editorListSell().replace("<value>", String.valueOf(item.getSellPrice())));
            lore.add(messages.editorListSlot().replace("<value>", String.valueOf(item.getSlot())));
            lore.add(item.isEnabled() ? messages.editorListEnabled() : messages.editorListDisabled());
            lore.add("");
            lore.add(messages.editorClickItem());

            meta.lore(lore.stream().map(l -> MiniMessage.miniMessage().deserialize(l)).toList());
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "editor_item");
            if (displayId != null && !displayId.isEmpty()) {
                meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, displayId);
            }
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

    private ItemStack createButtonWithLoreLines(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(name));
            List<net.kyori.adventure.text.Component> lore = loreLines.stream()
                    .map(line -> MiniMessage.miniMessage().deserialize(line))
                    .toList();
            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createInfoItem(Material material, String name, String value) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(name));
            String displayValue = value != null ? value : messages.editorNone();
            meta.lore(List.of(MiniMessage.miniMessage().deserialize("<white>" + displayValue)));
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
            String state = enabled ? messages.editorListEnabled() : messages.editorListDisabled();
            meta.lore(List.of(MiniMessage.miniMessage().deserialize(state)));
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
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "noop");
            item.setItemMeta(meta);
        }

        return item;
    }

    private void tagAction(ItemStack item, String action, String itemId) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        if (action != null && !action.isEmpty()) {
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        }
        if (itemId != null && !itemId.isEmpty()) {
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, itemId);
        }
        item.setItemMeta(meta);
    }
}
