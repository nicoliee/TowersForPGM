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
import org.nicolie.towersforpgm.update.AutoUpdate;
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
          sender.sendMessage(LanguageManager.langMessage("TowersForPGM.noLanguage"));
          return true;
        }
        String language = args[1].toLowerCase();
        if (!language.equals("en") && !language.equals("es")) {
          sender.sendMessage(LanguageManager.langMessage("TowersForPGM.invalidLanguage"));
          return true;
        }
        LanguageManager.setLanguage(language);
        sender.sendMessage(LanguageManager.langMessage("TowersForPGM.languageSet"));
        return true;

      case "reloadmessages":
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        sender.sendMessage(LanguageManager.langMessage("messages.reloadStart"));

        if (messagesFile.exists()) {
          if (!messagesFile.delete()) {
            sender.sendMessage(LanguageManager.langMessage("messages.reloadError"));
            return true;
          }
        }
        plugin.saveResource("messages.yml", false);
        LanguageManager.reload();
        sender.sendMessage(LanguageManager.langMessage("messages.reloadSuccess"));
        return true;
      case "update":
        AutoUpdate update = new AutoUpdate(plugin);
        update.checkForUpdates();
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
      List<String> options = Arrays.asList("setlanguage", "reloadmessages", "update");

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
      List<String> languages = Arrays.asList("en", "es");

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
