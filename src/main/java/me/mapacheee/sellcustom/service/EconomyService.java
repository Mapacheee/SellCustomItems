package me.mapacheee.sellcustom.service;

import com.google.inject.Inject;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.sellcustom.config.ScConfig;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.slf4j.Logger;

@Service
public final class EconomyService {

    private final Logger logger;
    private final ScConfig config;
    private Economy economy;

    @Inject
    public EconomyService(Logger logger, ScConfig config) {
        this.logger = logger;
        this.config = config;
        this.economy = initEconomy();
    }

    private Economy initEconomy() {
        if (!config.economyEnabled()) {
            return null;
        }

        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            logger.warn("Vault not found. Economy features disabled.");
            return null;
        }

        var rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            logger.warn("No economy provider found. Economy features disabled.");
            return null;
        }

        economy = rsp.getProvider();
        logger.info("Economy enabled: " + economy.getName());
        return economy;
    }

    public boolean hasMoney(OfflinePlayer player, double amount) {
        if (economy == null) return true;
        return economy.has(player, amount);
    }

    public boolean withdrawMoney(OfflinePlayer player, double amount) {
        if (economy == null) return false;
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    public boolean depositMoney(OfflinePlayer player, double amount) {
        if (economy == null) return false;
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    public double getBalance(OfflinePlayer player) {
        if (economy == null) return 0;
        return economy.getBalance(player);
    }

    public String formatMoney(double amount) {
        if (economy == null) return String.format("$%.2f", amount);
        return economy.format(amount);
    }

    public boolean isEnabled() {
        return economy != null && config.economyEnabled();
    }
}