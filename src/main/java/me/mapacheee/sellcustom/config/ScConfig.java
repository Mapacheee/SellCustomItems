package me.mapacheee.sellcustom.config;

import com.thewinterframework.configurate.config.Configurate;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
@Configurate("config")
public record ScConfig(
        @Setting("gui-title") String guiTitle,
        @Setting("gui-rows") int guiRows,
        @Setting("economy-enabled") boolean economyEnabled,
        @Setting("default-buy-margin") double defaultBuyMargin,
        @Setting("default-sell-margin") double defaultSellMargin
) {}