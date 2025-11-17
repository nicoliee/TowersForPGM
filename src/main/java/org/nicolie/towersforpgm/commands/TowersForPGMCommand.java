package org.nicolie.towersforpgm.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.MatchHistoryManager;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;

public class TowersForPGMCommand implements CommandExecutor, TabCompleter {
  private final TowersForPGM plugin;

  public TowersForPGMCommand(TowersForPGM plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission("towers.admin")) {
      sender.sendMessage(LanguageManager.langMessage("errors.noPermission"));
      return true;
    }

    if (args.length == 0) {
      sender.sendMessage(
          "§8[§bTowersForPGM§8] §7Version: " + plugin.getDescription().getVersion());
      return true;
    }

    String argument = args[0].toLowerCase();
    switch (argument) {
      case "setlanguage":
        if (args.length < 2) {
          sender.sendMessage(LanguageManager.langMessage("system.noLanguage"));
          return true;
        }
        String language = args[1].toLowerCase();
        if (!LanguageManager.getSupportedLanguages().contains(language)) {
          sender.sendMessage(LanguageManager.langMessage("system.invalidLanguage"));
          return true;
        }
        LanguageManager.setLanguage(language);
        sender.sendMessage(LanguageManager.langMessage("system.languageSet"));
        return true;

      case "reloadmessages":
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        sender.sendMessage(LanguageManager.langMessage("system.reload.start"));

        if (messagesFile.exists()) {
          if (!messagesFile.delete()) {
            sender.sendMessage(LanguageManager.langMessage("system.reload.error"));
            return true;
          }
        }
        plugin.saveResource("messages.yml", false);
        LanguageManager.reload();
        sender.sendMessage(LanguageManager.langMessage("system.reload.success"));
        return true;

      case "dbreload":
        sender.sendMessage("§8[§bTowersForPGM§8] §7Reloading database connection...");
        try {
          plugin.initializeDatabase();

          if (plugin.getIsDatabaseActivated()) {
            // recreate tables and precache match id counters as on enable
            try {
              plugin
                  .getLogger()
                  .info("DB reload successful, creating tables and precaching match ids.");
              // create tables
              ConfigManager.getTables()
                  .forEach(org.nicolie.towersforpgm.database.TableManager::createTable);
              ConfigManager.getRankedTables()
                  .forEach(org.nicolie.towersforpgm.database.TableManager::createTable);
              org.nicolie.towersforpgm.database.TableManager.createHistoryTables();

              java.util.Set<String> tables = new java.util.HashSet<>();
              tables.addAll(ConfigManager.getTables());
              tables.addAll(ConfigManager.getRankedTables());
              MatchHistoryManager.preloadMatchIdCountersAsync(tables);
            } catch (Exception ex) {
              plugin
                  .getLogger()
                  .warning("Error creating tables/precaching after DB reload: " + ex.getMessage());
            }

            sender.sendMessage(
                "§aDatabase reloaded and active (" + plugin.getCurrentDatabaseType() + ")");
          } else {
            sender.sendMessage(
                "§cDatabase not active after reload. Check server logs for details.");
          }
        } catch (Exception e) {
          sender.sendMessage("§cError reloading database: " + e.getMessage());
        }
        return true;
      default:
        sender.sendMessage(
            "§8[§bTowersForPGM§8] §7Version: " + plugin.getDescription().getVersion());
        return true;
    }
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      // Lista de opciones posibles
      List<String> options = Arrays.asList("setlanguage", "reloadmessages", "dbreload");

      // Filtrar las opciones que comienzan con el texto ingresado por el usuario
      String input = args[0].toLowerCase();
      List<String> filteredOptions = new ArrayList<>();
      for (String option : options) {
        if (option.toLowerCase().startsWith(input)) {
          filteredOptions.add(option);
        }
      }
      return filteredOptions;
    }

    if (args.length == 2 && args[0].equalsIgnoreCase("setlanguage")) {
      // Lista de idiomas disponibles
      List<String> languages = LanguageManager.getSupportedLanguages();

      // Filtrar las opciones que comienzan con el texto ingresado por el usuario
      String input = args[1].toLowerCase();
      List<String> filteredLanguages = new ArrayList<>();
      for (String language : languages) {
        if (language.toLowerCase().startsWith(input)) {
          filteredLanguages.add(language);
        }
      }
      return filteredLanguages;
    }
    return null;
  }
}
