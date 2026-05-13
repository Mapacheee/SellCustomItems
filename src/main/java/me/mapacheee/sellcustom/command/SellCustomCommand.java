package me.mapacheee.sellcustom.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.ReloadServiceManager;
import me.mapacheee.sellcustom.config.ScMessages;
import me.mapacheee.sellcustom.data.CustomItem;
import me.mapacheee.sellcustom.data.CustomItemStorage;
import me.mapacheee.sellcustom.gui.EditorGui;
import me.mapacheee.sellcustom.gui.MainShopGui;
import me.mapacheee.sellcustom.service.ShopService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.*;
import org.incendo.cloud.paper.util.sender.Source;

@CommandComponent
public final class SellCustomCommand {

    private final ShopService shopService;
    private final CustomItemStorage itemStorage;
    private final MainShopGui mainShopGui;
    private final EditorGui editorGui;
    private final Container<ScMessages> messages;
    private final ReloadServiceManager reloadServiceManager;

    @Inject
    public SellCustomCommand(ShopService shopService, CustomItemStorage itemStorage, MainShopGui mainShopGui,
                             EditorGui editorGui, Container<ScMessages> messages, ReloadServiceManager reloadServiceManager) {
        this.shopService = shopService;
        this.itemStorage = itemStorage;
        this.mainShopGui = mainShopGui;
        this.editorGui = editorGui;
        this.messages = messages;
        this.reloadServiceManager = reloadServiceManager;
    }

    @Command("buycustomitems")
    public void buyCustomItems(Source source) {
        if (!(source.source() instanceof Player player)) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().playersOnly()));
            return;
        }
        mainShopGui.open(player, 1);
    }

    @Command("sellcustomitems")
    public void sellCustomItemsRoot(Source source) {
        if (!(source.source() instanceof Player player)) return;
        sendHelp(source);
    }

    @Command("sc")
    public void scRoot(Source source) {
        if (!(source.source() instanceof Player player)) return;
        sendHelp(source);
    }

    @Command("sc help")
    public void help(Source source) {
        sendHelp(source);
    }

    private void sendHelp(Source source) {
        send(source, messages.get().helpCommand());
        send(source, messages.get().helpBuy());
        send(source, messages.get().helpEditor());
        send(source, messages.get().helpReload());
    }

    @Command("sc sell <price>")
    @Permission("sellcustom.admin")
    public void sellFromHand(Source source, @Argument("price") double price) {
        if (!(source.source() instanceof Player player)) return;

        double sellPrice = price * 0.5;
        shopService.createItemFromHand(player, price, sellPrice);

        String msg = messages.get().itemCreatedFromHand();
        msg = msg.replace("<buy_price>", String.valueOf(price));
        msg = msg.replace("<sell_price>", String.valueOf(sellPrice));
        send(source, msg);
    }

    @Command("sc add <id> <buy-price> <sell-price>")
    @Permission("sellcustom.admin")
    public void addItem(Source source, @Argument("id") String id, @Argument("buy-price") double buyPrice, @Argument("sell-price") double sellPrice) {
        String normalizedId = id.toLowerCase();

        if (shopService.getItem(normalizedId) != null) {
            send(source, messages.get().itemAlreadyExists());
            return;
        }

        CustomItem newItem = new CustomItem(normalizedId, normalizedId, "DIAMOND", buyPrice, sellPrice);
        newItem.setSlot(shopService.getAllItems().size());
        itemStorage.addItem(newItem);

        send(source, messages.get().itemCreated());
    }

    @Command("sc remove <id>")
    @Permission("sellcustom.admin")
    public void removeItem(Source source, @Argument("id") String id) {
        CustomItem item = shopService.getItem(id);
        if (item == null) {
            send(source, messages.get().itemNotFound());
            return;
        }

        itemStorage.removeItem(id);
        send(source, messages.get().itemDeleted());
    }

    @Command("sc setbuy <id> <price>")
    @Permission("sellcustom.admin")
    public void setBuyPrice(Source source, @Argument("id") String id, @Argument("price") double price) {
        CustomItem item = shopService.getItem(id);
        if (item == null) {
            send(source, messages.get().itemNotFound());
            return;
        }

        item.setBuyPrice(price);
        itemStorage.updateItem(item);

        send(source, messages.get().itemUpdated());
    }

    @Command("sc setsell <id> <price>")
    @Permission("sellcustom.admin")
    public void setSellPrice(Source source, @Argument("id") String id, @Argument("price") double price) {
        CustomItem item = shopService.getItem(id);
        if (item == null) {
            send(source, messages.get().itemNotFound());
            return;
        }

        item.setSellPrice(price);
        itemStorage.updateItem(item);

        send(source, messages.get().itemUpdated());
    }

    @Command("sc setenabled <id> <enabled>")
    @Permission("sellcustom.admin")
    public void setEnabled(Source source, @Argument("id") String id, @Argument("enabled") boolean enabled) {
        CustomItem item = shopService.getItem(id);
        if (item == null) {
            send(source, messages.get().itemNotFound());
            return;
        }

        item.setEnabled(enabled);
        itemStorage.updateItem(item);

        send(source, enabled ? messages.get().itemEnabled() : messages.get().itemDisabled());
    }

    @Command("sc setcanbuy <id> <can-buy>")
    @Permission("sellcustom.admin")
    public void setCanBuy(Source source, @Argument("id") String id, @Argument("can-buy") boolean canBuy) {
        CustomItem item = shopService.getItem(id);
        if (item == null) {
            send(source, messages.get().itemNotFound());
            return;
        }

        item.setCanBuy(canBuy);
        itemStorage.updateItem(item);

        send(source, messages.get().itemUpdated());
    }

    @Command("sc setcansell <id> <can-sell>")
    @Permission("sellcustom.admin")
    public void setCanSell(Source source, @Argument("id") String id, @Argument("can-sell") boolean canSell) {
        CustomItem item = shopService.getItem(id);
        if (item == null) {
            send(source, messages.get().itemNotFound());
            return;
        }

        item.setCanSell(canSell);
        itemStorage.updateItem(item);

        send(source, messages.get().itemUpdated());
    }

    @Command("sc editor")
    @Permission("sellcustom.admin")
    public void openEditor(Source source) {
        if (!(source.source() instanceof Player player)) return;
        editorGui.openMainEditor(player);
    }

    @Command("sc reload")
    @Permission("sellcustom.admin")
    public void reload(Source source) {
        reloadServiceManager.reload();
        send(source, messages.get().pluginReloaded());
    }

    private void send(Source source, String msg) {
        source.source().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + msg));
    }
}