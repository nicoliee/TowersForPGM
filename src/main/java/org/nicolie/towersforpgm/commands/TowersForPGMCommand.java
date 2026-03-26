package org.nicolie.towersforpgm.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.suggestion.Suggestions;
import tc.oc.pgm.lib.org.incendo.cloud.context.CommandContext;
import tc.oc.pgm.lib.org.incendo.cloud.context.CommandInput;
import tc.oc.pgm.util.Audience;

public class TowersForPGMCommand {
  private final TowersForPGM plugin = TowersForPGM.getInstance();

  @Command("towerforpgm")
  @CommandDescription("Show plugin version")
  public void version(Audience audience) {
    audience.sendMessage(Component.translatable(
        "system.version", Component.text(plugin.getDescription().getVersion())));
  }

  @Command("towersforpgm setlanguage <language>")
  @CommandDescription("Set the plugin language")
  public void setLanguage(
      Audience audience,
      @Argument(value = "language", suggestions = "supportedLanguages") String language) {
    if (!LanguageManager.getSupportedLanguages().contains(language.toLowerCase())) {
      audience.sendMessage(Component.translatable("system.invalidLanguage"));
      return;
    }
    LanguageManager.setLanguage(language.toLowerCase());
    // No se usa translatable para mostrar el idioma cambiado
    audience.sendMessage(Component.text(LanguageManager.message("system.languageSet")));
  }

  @Command("towersforpgm dbreload")
  @CommandDescription("Reload the database connection")
  public void dbReload(Audience audience) {
    audience.sendMessage(Component.translatable("system.dbReloading"));
    if (plugin.reloadDatabase()) {
      audience.sendMessage(Component.translatable(
          "system.dbReloaded", Component.text(plugin.getCurrentDatabaseType())));
    } else {
      audience.sendMessage(Component.translatable("system.dbReloadFailed"));
    }
  }

  @Suggestions("supportedLanguages")
  public java.util.List<String> supportedLanguagesSuggestions(
      CommandContext<CommandSender> context, CommandInput input) {
    return LanguageManager.getSupportedLanguages();
  }
}
