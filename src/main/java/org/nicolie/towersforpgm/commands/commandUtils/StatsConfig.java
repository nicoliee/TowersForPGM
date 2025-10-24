package org.nicolie.towersforpgm.commands.commandUtils;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.TableManager;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.MatchManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;

public class StatsConfig {
  private final TowersForPGM plugin = TowersForPGM.getInstance();

  public void toggleStats(CommandSender sender) {
    boolean isStatsCancel = plugin.isStatsCancel();
    plugin.setStatsCancel(!isStatsCancel);
    String cancelStatsMessage = isStatsCancel
        ? LanguageManager.langMessage("stats.config.enabled")
        : LanguageManager.langMessage("stats.config.disabled");
    SendMessage.sendToPlayer(sender, cancelStatsMessage);
  }

  public void setDefaultTable(CommandSender sender, String table) {
    if (!ConfigManager.getTables().contains(table)) {
      SendMessage.sendToPlayer(
          sender,
          LanguageManager.langMessage("stats.config.tables.notFound").replace("{table}", table));
      return;
    }
    ConfigManager.setDefaultTable(table);
    SendMessage.sendToPlayer(
        sender, LanguageManager.langMessage("stats.config.maps.success").replace("{table}", table));
  }

  public void addTable(CommandSender sender, String table) {
    if (ConfigManager.getTables().contains(table)) {
      SendMessage.sendToPlayer(sender, LanguageManager.langMessage("stats.config.tables.exists"));
      return;
    }

    ConfigManager.addTable(table);
    TableManager.createTable(table);
    SendMessage.sendToPlayer(
        sender,
        LanguageManager.langMessage("stats.config.tables.created").replace("{table}", table));
  }

  public void deleteTable(CommandSender sender, String table) {
    if (!ConfigManager.getTables().contains(table)) {
      SendMessage.sendToPlayer(
          sender,
          LanguageManager.langMessage("stats.config.tables.notFound").replace("{table}", table));
      return;
    }
    ConfigManager.removeTable(table);
    SendMessage.sendToPlayer(
        sender,
        LanguageManager.langMessage("stats.config.tables.deleted").replace("{table}", table));
  }

  public void listTables(CommandSender sender) {
    List<String> tables = ConfigManager.getTables();
    if (tables.isEmpty()) {
      SendMessage.sendToPlayer(sender, LanguageManager.langMessage("stats.config.tables.noTables"));
      return;
    }
    String active =
        ConfigManager.getActiveTable(MatchManager.getMatch().getMap().getName());
    List<String> display = new java.util.ArrayList<>();
    for (String t : tables) {
      if (active != null && active.equals(t)) {
        display.add("§e" + t + "§r");
      } else {
        display.add(t);
      }
    }
    String tablesList = String.join(", ", display);
    SendMessage.sendToPlayer(
        sender,
        LanguageManager.langMessage("stats.config.tables.list").replace("{list}", tablesList));
  }

  public void addTableForMap(CommandSender sender, String table) {
    String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
    if (!ConfigManager.getTables().contains(table)) {
      SendMessage.sendToPlayer(
          sender,
          LanguageManager.langMessage("stats.config.tables.notFound").replace("{table}", table));
      return;
    }
    ConfigManager.addMap(mapName, table);
    SendMessage.sendToPlayer(
        sender,
        LanguageManager.langMessage("stats.config.maps.added")
            .replace("{map}", mapName)
            .replace("{table}", table));
  }

  public void deleteTableForMap(CommandSender sender) {
    String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
    if (!ConfigManager.getMapTables().containsKey(mapName)) {
      SendMessage.sendToPlayer(
          sender,
          LanguageManager.langMessage("stats.config.maps.notExists").replace("{map}", mapName));
      return;
    }
    ConfigManager.removeMap(mapName);
    SendMessage.sendToPlayer(
        sender, LanguageManager.langMessage("stats.config.maps.deleted").replace("{map}", mapName));
  }

  public void addTempTable(CommandSender sender, String table) {
    ConfigManager.addTemp(table);
    SendMessage.sendToPlayer(
        sender, LanguageManager.langMessage("stats.config.temp.added").replace("{table}", table));
  }

  public void removeTempTable(CommandSender sender) {
    if (ConfigManager.getTemp() == null) {
      SendMessage.sendToPlayer(sender, LanguageManager.langMessage("stats.config.temp.notExists"));
      return;
    }
    ConfigManager.removeTemp();
    SendMessage.sendToPlayer(sender, LanguageManager.langMessage("stats.config.temp.removed"));
  }
}
