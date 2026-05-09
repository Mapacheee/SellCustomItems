package me.mapacheee.sellcustom.config;

import com.thewinterframework.configurate.config.Configurate;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

@ConfigSerializable
@Configurate("messages")
public record ScMessages(
        @Setting("prefix") String prefix,
        @Setting("no-permission") String noPermission,
        @Setting("plugin-reloaded") String pluginReloaded,
        @Setting("invalid-item") String invalidItem,
        @Setting("item-already-exists") String itemAlreadyExists,
        @Setting("item-not-found") String itemNotFound,
        @Setting("item-created") String itemCreated,
        @Setting("item-deleted") String itemDeleted,
        @Setting("item-updated") String itemUpdated,
        @Setting("item-enabled") String itemEnabled,
        @Setting("item-disabled") String itemDisabled,
        @Setting("buy-success") String buySuccess,
        @Setting("buy-failed-no-money") String buyFailedNoMoney,
        @Setting("buy-failed-inventory-full") String buyFailedInventoryFull,
        @Setting("sell-success") String sellSuccess,
        @Setting("sell-failed-no-items") String sellFailedNoItems,
        @Setting("sell-no-items-found") String sellNoItemsFound,
        @Setting("sell-select-amount") String sellSelectAmount,
        @Setting("balance") String balance,
        @Setting("item-created-from-hand") String itemCreatedFromHand,
        @Setting("editor-title") String editorTitle,
        @Setting("editor-add-item") String editorAddItem,
        @Setting("editor-save") String editorSave,
        @Setting("editor-cancel") String editorCancel,
        @Setting("editor-item-name") String editorItemName,
        @Setting("editor-item-price") String editorItemPrice,
        @Setting("editor-item-sell-price") String editorItemSellPrice,
        @Setting("editor-item-enabled") String editorItemEnabled,
        @Setting("editor-item-can-buy") String editorItemCanBuy,
        @Setting("editor-item-can-sell") String editorItemCanSell,
        @Setting("editor-item-slot") String editorItemSlot,
        @Setting("editor-item-lore") String editorItemLore,
        @Setting("editor-item-material") String editorItemMaterial,
        @Setting("editor-click-item") String editorClickItem,
        @Setting("help-command") String helpCommand,
        @Setting("help-buy") String helpBuy,
        @Setting("help-sell") String helpSell,
        @Setting("help-editor") String helpEditor,
        @Setting("help-reload") String helpReload,
        @Setting("shop-disabled") String shopDisabled,
        @Setting("item-not-buyable") String itemNotBuyable,
        @Setting("item-not-sellable") String itemNotSellable,
        @Setting("confirm-purchase-title") String confirmPurchaseTitle,
        @Setting("confirm-purchase-lore") List<String> confirmPurchaseLore,
        @Setting("confirm-sell-title") String confirmSellTitle,
        @Setting("confirm-sell-lore") List<String> confirmSellLore
) {}