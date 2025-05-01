package org.nicolie.towersforpgm.commands;

import java.io.File;
import java.util.Arrays;
import java.util.List;


import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.update.AutoUpdate;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;

public class TowersForPGMCommand implements CommandExecutor, TabCompleter {
    private final TowersForPGM plugin;
    private final LanguageManager languageManager;

    public TowersForPGMCommand(TowersForPGM plugin, LanguageManager languageManager) {
        this.languageManager = languageManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(languageManager.getPluginMessage("errors.noPlayer"));
            return true;
        }

        if (!sender.hasPermission("towers.admin")) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("errors.noPermission"));
            return true;
        }

        Player player = (Player) sender;
        
        if (args.length == 0) {
            SendMessage.sendToPlayer(player, "§8[§bTowersForPGM§8] §7Version: " + plugin.getDescription().getVersion());
            return true;
        }

        String argument = args[0].toLowerCase();
        switch (argument) {
            case "setlanguage":
                if (args.length < 2) {
                    SendMessage.sendToPlayer(player, languageManager.getPluginMessage("TowersForPGM.noLanguage"));
                    return true;
                }
                String language = args[1].toLowerCase();
                if (!language.equals("en") && !language.equals("es")) {
                    SendMessage.sendToPlayer(player, languageManager.getPluginMessage("TowersForPGM.invalidLanguage"));
                    return true;
                }
                languageManager.setLanguage(language);
                SendMessage.sendToPlayer(player, languageManager.getPluginMessage("TowersForPGM.languageSet"));
                return true;

            case "reloadmessages":
                File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
                SendMessage.sendToPlayer(player, languageManager.getPluginMessage("messages.reloadStart"));

                if (messagesFile.exists()) {
                    if (!messagesFile.delete()) {
                        SendMessage.sendToPlayer(player, languageManager.getPluginMessage("messages.reloadError"));
                        return true;
                    }
                }
                plugin.saveResource("messages.yml", false);
                languageManager.reloadMessages();
                SendMessage.sendToPlayer(player, languageManager.getPluginMessage("messages.reloadSuccess"));
                return true;
            case "update":
                AutoUpdate update = new AutoUpdate(plugin);
                update.checkForUpdates();
                return true;
            default:
                SendMessage.sendToPlayer(player, "§8[§bTowersForPGM§8] §7Version: " + plugin.getDescription().getVersion());
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("towers.admin")) {
            return Arrays.asList(); // No sugerir nada si no tiene permisos
        }

        if (args.length == 1) {
            return Arrays.asList("reloadMessages", "setLanguage", "update");
        }
        if (args[0].equalsIgnoreCase("setLanguage") && args.length == 2) {
            return Arrays.asList("en", "es");
        }
        return Arrays.asList();
    }
}