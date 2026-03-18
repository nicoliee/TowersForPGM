package org.nicolie.towersforpgm.commands;

import net.kyori.adventure.text.Component;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.suggestion.Suggestions;
import tc.oc.pgm.util.Audience;

public class TowersForPGMCommand {
  private final TowersForPGM plugin = TowersForPGM.getInstance();

  @Command("towerforpgm")
  @CommandDescription("Show plugin version")
  public void version(Audience audience) {
    audience.sendMessage(Component.text(
        "§8[§bTowersForPGM§8] §7Version: " + plugin.getDescription().getVersion()));
  }

  @Command("towersforpgm setlanguage <language>")
  @CommandDescription("Set the plugin language")
  public void setLanguage(
      Audience audience,
      @Argument(value = "language", suggestions = "supportedLanguages") String language) {
    if (!LanguageManager.getSupportedLanguages().contains(language.toLowerCase())) {
      audience.sendMessage(Component.text(LanguageManager.message("system.invalidLanguage")));
      return;
    }
    LanguageManager.setLanguage(language.toLowerCase());
    audience.sendMessage(Component.text(LanguageManager.message("system.languageSet")));
  }

  @Command("towersforpgm dbreload")
  @CommandDescription("Reload the database connection")
  public void dbReload(Audience audience) {
    audience.sendMessage(Component.text("§8[§bTowersForPGM§8] §7Reloading database connection..."));
    if (plugin.reloadDatabase()) {
      audience.sendMessage(Component.text(
          "§aDatabase reloaded and active (" + plugin.getCurrentDatabaseType() + ")"));
    } else {
      audience.sendMessage(
          Component.text("§cDatabase not active after reload. Check server logs for details."));
    }
  }

  @Suggestions("supportedLanguages")
  public java.util.List<String> supportedLanguagesSuggestions() {
    return LanguageManager.getSupportedLanguages();
  }
}
