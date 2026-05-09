package me.mapacheee.sellcustom.data;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public final class CustomItem {

    @Setting("id")
    private String id;

    @Setting("name")
    private String name;

    @Setting("material")
    private String material;

    @Setting("buy-price")
    private double buyPrice;

    @Setting("sell-price")
    private double sellPrice;

    @Setting("can-buy")
    private boolean canBuy;

    @Setting("can-sell")
    private boolean canSell;

    @Setting("lore")
    private List<String> lore = new ArrayList<>();

    @Setting("amount")
    private int amount = 1;

    @Setting("custom-model-data")
    private int customModelData = 0;

    @Setting("enabled")
    private boolean enabled = true;

    @Setting("slot")
    private int slot = -1;

    public CustomItem() {}

    public CustomItem(String id, String name, String material, double buyPrice, double sellPrice) {
        this.id = id;
        this.name = name;
        this.material = material;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.canBuy = true;
        this.canSell = true;
        this.enabled = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public double getBuyPrice() { return buyPrice; }
    public void setBuyPrice(double buyPrice) { this.buyPrice = buyPrice; }

    public double getSellPrice() { return sellPrice; }
    public void setSellPrice(double sellPrice) { this.sellPrice = sellPrice; }

    public boolean isCanBuy() { return canBuy; }
    public void setCanBuy(boolean canBuy) { this.canBuy = canBuy; }

    public boolean isCanSell() { return canSell; }
    public void setCanSell(boolean canSell) { this.canSell = canSell; }

    public List<String> getLore() { return lore; }
    public void setLore(List<String> lore) { this.lore = lore; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public int getCustomModelData() { return customModelData; }
    public void setCustomModelData(int customModelData) { this.customModelData = customModelData; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }
}