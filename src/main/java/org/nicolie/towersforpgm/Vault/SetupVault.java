package org.nicolie.towersforpgm.Vault;

import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.SendMessage;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

public class SetupVault {
    private static TowersForPGM plugin = TowersForPGM.getInstance();
    private static Economy economy;

    private static boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null)
            economy = economyProvider.getProvider();
        return (economy != null);
    }

    public static void setupVault() {
        if (ConfigManager.isVaultEnabled()) {
            if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
                SendMessage.sendToConsole("§aVault is enabled!");
                if (!setupEconomy()) {
                    SendMessage.sendToConsole("§cVault economy setup failed!");
                }
            }
        }
    }

    public static Economy getVaultEconomy() {
        return economy;
    }

    public static double getCoins(Player player) {
        return (getVaultEconomy() != null) ? getVaultEconomy().getBalance(player) : 0.0D;
    }

    public static boolean addCoins(Player player, double amount) {
        if (getVaultEconomy() != null && amount > 0) {
            EconomyResponse response = getVaultEconomy().depositPlayer(player, amount);
            return response.transactionSuccess();
        }
        return false;
    }

    public static boolean killReward(Player player) {
        if (getVaultEconomy() != null) {
            double amount = ConfigManager.getKillReward();
            if (amount > 0) {
                EconomyResponse response = getVaultEconomy().depositPlayer(player, amount);
                return response.transactionSuccess();
            }
        }
        return false;
    }

    public static boolean winReward(Player player) {
        if (getVaultEconomy() != null) {
            double amount = ConfigManager.getWinReward();
            if (amount > 0) {
                EconomyResponse response = getVaultEconomy().depositPlayer(player, amount);
                return response.transactionSuccess();
            }
        }
        return false;
    }

    public static boolean removeCoins(Player player, double amount) {
        if (getVaultEconomy() != null && amount > 0 && getCoins(player) >= amount) {
            EconomyResponse response = getVaultEconomy().withdrawPlayer(player, amount);
            return response.transactionSuccess();
        }
        return false;
    }

    public static boolean hasEnoughCoins(Player player, double amount) {
        return getCoins(player) >= amount;
    }
}