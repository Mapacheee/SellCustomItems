package me.mapacheee.sellcustom.listener;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.paper.listener.ListenerComponent;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.mapacheee.sellcustom.SellCustomItemsPlugin;
import me.mapacheee.sellcustom.config.ScMessages;
import me.mapacheee.sellcustom.data.CustomItem;
import me.mapacheee.sellcustom.data.CustomItemStorage;
import me.mapacheee.sellcustom.gui.EditorGui;
import me.mapacheee.sellcustom.gui.MainShopGui;
import me.mapacheee.sellcustom.service.EconomyService;
import me.mapacheee.sellcustom.service.ShopService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.time.Duration;
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
    private final ScMessages messages;

    private final NamespacedKey actionKey;
    private final NamespacedKey itemKey;
    private static final String META_EDIT_FIELD = "editor_input_field";
    private static final String META_EDIT_ITEM = "editor_input_item";

    @Inject
    public ShopListener(ShopService shopService, EconomyService economyService, MainShopGui mainShopGui,
                        EditorGui editorGui, CustomItemStorage itemStorage, Container<ScMessages> messagesContainer) {
        this.shopService = shopService;
        this.economyService = economyService;
        this.mainShopGui = mainShopGui;
        this.editorGui = editorGui;
        this.itemStorage = itemStorage;
        this.messages = messagesContainer.get();
        this.actionKey = new NamespacedKey(SellCustomItemsPlugin.getInstance(), "sc_action");
        this.itemKey = new NamespacedKey(SellCustomItemsPlugin.getInstance(), "sc_item_id");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (player.hasMetadata("sc_gui") && event.getClickedInventory() == event.getView().getTopInventory()) {
            event.setCancelled(true);
        } else if (player.hasMetadata("sc_gui") && event.getClickedInventory() == player.getInventory() && event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }

        boolean shiftClick = event.isShiftClick();

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String action = pdc.get(actionKey, PersistentDataType.STRING);
        String itemId = pdc.get(itemKey, PersistentDataType.STRING);

        if (action == null && itemId == null) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        event.setCancelled(true);

        if (action != null && action.startsWith("editor_")) {
            handleEditorAction(player, action, itemId);
            return;
        }

        if ("prev".equals(action) || "next".equals(action)) {
            int currentPage = 1;
            if (player.hasMetadata("shop_page")) {
                currentPage = player.getMetadata("shop_page").getFirst().asInt();
            }
            mainShopGui.open(player, "prev".equals(action) ? currentPage - 1 : currentPage + 1);
            return;
        }

        if ("shop_item".equals(action) && itemId != null) {
            CustomItem item = shopService.getItem(itemId);
            if (item == null) return;
            if (!item.isCanSell()) return;

            int available = shopService.countItemsInInventory(player, item);
            if (available <= 0) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(messages.sellFailedNoItems()));
                return;
            }

            int toSell = shiftClick ? available : 1;
            int sold = shopService.sellItem(player, item, toSell);
            if (sold > 0) {
                String msg = applyPlaceholders(messages.sellSuccess(), item, sold, item.getSellPrice() * sold);
                player.sendMessage(MiniMessage.miniMessage().deserialize(msg));
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize(messages.sellFailedNoItems()));
            }
            return;
        }
    }

    private void handleEditorAction(Player player, String action, String itemId) {
        if ("editor_add".equals(action)) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType() == Material.AIR) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(messages.editorInputEmptyHand()));
                return;
            }
            beginEditorInput(player, new CustomItem("pending", "", hand.getType().name(), 0, 0), "create_from_hand",
                    messages.editorInputTitle(),
                    messages.editorInputSubtitle(),
                    messages.editorInputPrompt(),
                    Map.of(
                            "field", messages.editorFieldBuyPrice(),
                            "hint", messages.editorHintBuyPrice(),
                            "example", messages.editorExamplePrice()
                    ));
            return;
        }

        if ("editor_save".equals(action)) {
            itemStorage.save();
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.editorSaveSuccess()));
            editorGui.openMainEditor(player);
            return;
        }

        if ("editor_close".equals(action)) {
            player.closeInventory();
            return;
        }

        if ("editor_delete".equals(action) && itemId != null) {
            itemStorage.removeItem(itemId);
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.editorDeleteSuccess()));
            editorGui.openMainEditor(player);
            return;
        }

        if (("editor_toggle_enabled".equals(action) || "editor_toggle_can_buy".equals(action) || "editor_toggle_can_sell".equals(action)) && itemId != null) {
            CustomItem item = shopService.getItem(itemId);
            if (item != null) {
                if ("editor_toggle_enabled".equals(action)) {
                    item.setEnabled(!item.isEnabled());
                } else if ("editor_toggle_can_buy".equals(action)) {
                    item.setCanBuy(!item.isCanBuy());
                } else if ("editor_toggle_can_sell".equals(action)) {
                    item.setCanSell(!item.isCanSell());
                }
                itemStorage.updateItem(item);
                player.sendMessage(MiniMessage.miniMessage().deserialize(messages.editorToggleSuccess()));
                editorGui.openItemEditor(player, item);
            }
            return;
        }

        if ("editor_item".equals(action) && itemId != null) {
            CustomItem item = shopService.getItem(itemId);
            if (item != null) {
                editorGui.openItemEditor(player, item);
            }
            return;
        }

        if (itemId == null) return;
        CustomItem item = shopService.getItem(itemId);
        if (item == null) return;

        if ("editor_edit_name".equals(action)) {
            beginEditorInput(player, item, "name",
                    messages.editorInputTitle(),
                    messages.editorInputSubtitle(),
                    messages.editorInputPrompt(),
                    Map.of(
                            "field", messages.editorFieldName(),
                            "hint", messages.editorHintName(),
                            "example", messages.editorExampleName()
                    ));
            return;
        }

        if ("editor_edit_buy_price".equals(action)) {
            beginEditorInput(player, item, "buy_price",
                    messages.editorInputTitle(),
                    messages.editorInputSubtitle(),
                    messages.editorInputPrompt(),
                    Map.of(
                            "field", messages.editorFieldBuyPrice(),
                            "hint", messages.editorHintBuyPrice(),
                            "example", messages.editorExamplePrice()
                    ));
            return;
        }

        if ("editor_edit_sell_price".equals(action)) {
            beginEditorInput(player, item, "sell_price",
                    messages.editorInputTitle(),
                    messages.editorInputSubtitle(),
                    messages.editorInputPrompt(),
                    Map.of(
                            "field", messages.editorFieldSellPrice(),
                            "hint", messages.editorHintSellPrice(),
                            "example", messages.editorExamplePrice()
                    ));
            return;
        }

        if ("editor_edit_slot".equals(action)) {
            beginEditorInput(player, item, "slot",
                    messages.editorInputTitle(),
                    messages.editorInputSubtitle(),
                    messages.editorInputPrompt(),
                    Map.of(
                            "field", messages.editorFieldSlot(),
                            "hint", messages.editorHintSlot(),
                            "example", messages.editorExampleSlot()
                    ));
            return;
        }

        if ("editor_edit_lore".equals(action)) {
            beginEditorInput(player, item, "lore",
                    messages.editorInputTitle(),
                    messages.editorInputSubtitle(),
                    messages.editorInputPrompt(),
                    Map.of(
                            "field", messages.editorFieldLore(),
                            "hint", messages.editorInputLoreHint(),
                            "example", messages.editorExampleLore()
                    ));
            return;
        }

        if ("editor_edit_material".equals(action)) {
            beginEditorInput(player, item, "material",
                    messages.editorInputTitle(),
                    messages.editorInputSubtitle(),
                    messages.editorInputPrompt(),
                    Map.of(
                            "field", messages.editorFieldMaterial(),
                            "hint", messages.editorInputMaterialHint(),
                            "example", messages.editorExampleMaterial()
                    ));
        }
    }

    private String applyPlaceholders(String message, CustomItem item, int amount, double price) {
        String itemName = item.getName() != null && !item.getName().isEmpty() ? item.getName() : item.getId();
        String result = message.replace("<item_name>", itemName != null ? itemName : "");
        result = result.replace("<amount>", String.valueOf(amount));
        result = result.replace("<price>", economyService.formatMoney(price));
        return result;
    }

    private String applyPlaceholders(String message, Map<String, String> placeholders) {
        String result = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return result;
    }

    private void beginEditorInput(Player player, CustomItem item, String field, String title, String subtitle, String chatPrompt, Map<String, String> placeholders) {
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);

        String titleText = applyPlaceholders(title, placeholders);
        String subtitleText = applyPlaceholders(subtitle, placeholders);
        Component titleComponent = MiniMessage.miniMessage().deserialize(titleText);
        Component subtitleComponent = MiniMessage.miniMessage().deserialize(subtitleText);
        Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500));
        player.showTitle(Title.title(titleComponent, subtitleComponent, times));

        player.sendMessage(MiniMessage.miniMessage().deserialize(applyPlaceholders(chatPrompt, placeholders)));
        if (placeholders.containsKey("example")) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    applyPlaceholders(messages.editorInputExample(), placeholders)));
        }

        var plugin = Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("SellCustomItems"));
        player.setMetadata(META_EDIT_FIELD, new org.bukkit.metadata.FixedMetadataValue(plugin, field));
        player.setMetadata(META_EDIT_ITEM, new org.bukkit.metadata.FixedMetadataValue(plugin, item.getId()));
    }

    @EventHandler
    public void onEditorChatInput(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!player.hasMetadata(META_EDIT_FIELD) || !player.hasMetadata(META_EDIT_ITEM)) return;

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        handleEditorChat(player, message);
    }

    private void handleEditorChat(Player player, String input) {
        String field = player.getMetadata(META_EDIT_FIELD).getFirst().asString();
        String itemId = player.getMetadata(META_EDIT_ITEM).getFirst().asString();

        Bukkit.getScheduler().runTask(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("SellCustomItems")),
                () -> applyEditorInput(player, field, itemId, input));
    }

    private void applyEditorInput(Player player, String field, String itemId, String input) {
        if (input.equalsIgnoreCase("cancel")) {
            clearEditorInput(player);
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.editorInputCancelled()));
            return;
        }

        if ("create_from_hand".equals(field)) {
            Double value = parseDouble(input);
            if (value == null) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(messages.editorInputInvalidNumber()));
                return;
            }
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType() == Material.AIR) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(messages.editorInputEmptyHand()));
                return;
            }
            double sellPrice = value * 0.5;
            shopService.createItemFromHand(player, value, sellPrice);
            clearEditorInput(player);
            String msg = messages.itemCreatedFromHand()
                    .replace("<buy_price>", String.valueOf(value))
                    .replace("<sell_price>", String.valueOf(sellPrice));
            player.sendMessage(MiniMessage.miniMessage().deserialize(msg));
            editorGui.openMainEditor(player);
            return;
        }

        CustomItem item = shopService.getItem(itemId);
        if (item == null) {
            clearEditorInput(player);
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.editorInputItemNotFound()));
            return;
        }

        switch (field) {
            case "name" -> {
                item.setName(input);
            }
            case "buy_price" -> {
                Double value = parseDouble(input);
                if (value == null) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(messages.editorInputInvalidNumber()));
                    return;
                }
                item.setBuyPrice(value);
            }
            case "sell_price" -> {
                Double value = parseDouble(input);
                if (value == null) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(messages.editorInputInvalidNumber()));
                    return;
                }
                item.setSellPrice(value);
            }
            case "slot" -> {
                Integer value = parseInt(input);
                if (value == null || value < 0) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(messages.editorInputInvalidSlot()));
                    return;
                }
                item.setSlot(value);
            }
            case "lore" -> {
                List<String> lines = new java.util.ArrayList<>();
                if (!input.isEmpty()) {
                    for (String part : input.split("\\|")) {
                        String line = part.trim();
                        if (!line.isEmpty()) {
                            lines.add(line);
                        }
                    }
                }
                item.setLore(lines);
            }
            case "material" -> {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(messages.editorInputEmptyHand()));
                    return;
                }
                if (!input.equalsIgnoreCase("ok")) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(messages.editorInputConfirmMaterial()));
                    return;
                }
                item.setMaterial(hand.getType().name());
                if (hand.getType() == Material.PLAYER_HEAD) {
                    item.setHeadTexture(shopService.extractHeadTexture(hand));
                } else {
                    item.setHeadTexture(null);
                }
            }
            default -> {
                player.sendMessage(MiniMessage.miniMessage().deserialize(messages.editorInputUnsupportedField()));
                return;
            }
        }

        itemStorage.updateItem(item);
        clearEditorInput(player);
        player.sendMessage(MiniMessage.miniMessage().deserialize(messages.editorInputUpdated()));
        editorGui.openItemEditor(player, item);
    }

    private void clearEditorInput(Player player) {
        var plugin = Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("SellCustomItems"));
        player.removeMetadata(META_EDIT_FIELD, plugin);
        player.removeMetadata(META_EDIT_ITEM, plugin);
    }

    private Double parseDouble(String input) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseInt(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            return null;
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
        if (player.hasMetadata("sc_gui")) {
            player.removeMetadata("sc_gui", Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("SellCustomItems")));
        }
    }
}
