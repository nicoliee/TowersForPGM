package org.nicolie.towersforpgm.commands.commandUtils;

import java.util.List;
import java.util.Set;
import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.tables.TableInfo;
import org.nicolie.towersforpgm.configs.tables.TableType;
import org.nicolie.towersforpgm.database.TableManager;
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
        ? LanguageManager.message("stats.config.enabled")
        : LanguageManager.message("stats.config.disabled");
    SendMessage.sendToPlayer(sender, cancelStatsMessage);
  }

  public void setDefaultTable(CommandSender sender, String table) {
    TableInfo tableInfo = TowersForPGM.getInstance().config().databaseTables().getTableInfo(table);
    if (tableInfo == null) {
      SendMessage.sendToPlayer(
          sender,
          LanguageManager.message("stats.config.tables.notFound").replace("{table}", table));
      return;
    }
    plugin.config().databaseTables().setDefaultTable(table);
    SendMessage.sendToPlayer(
        sender, LanguageManager.message("stats.config.maps.success").replace("{table}", table));
  }

  public void addTable(CommandSender sender, String table) {
    TableInfo tableInfo = TowersForPGM.getInstance().config().databaseTables().getTableInfo(table);
    if (tableInfo != null) {
      SendMessage.sendToPlayer(sender, LanguageManager.message("stats.config.tables.exists"));
      return;
    }

    plugin.config().databaseTables().addTable(table, false);
    TableManager.createTable(table);
    SendMessage.sendToPlayer(
        sender, LanguageManager.message("stats.config.tables.created").replace("{table}", table));
  }

  public void deleteTable(CommandSender sender, String table) {
    TableInfo tableInfo = TowersForPGM.getInstance().config().databaseTables().getTableInfo(table);
    if (tableInfo == null) {
      SendMessage.sendToPlayer(
          sender,
          LanguageManager.message("stats.config.tables.notFound").replace("{table}", table));
      return;
    }
    plugin.config().databaseTables().removeTable(table);
    SendMessage.sendToPlayer(
        sender, LanguageManager.message("stats.config.tables.deleted").replace("{table}", table));
  }

  public void listTables(CommandSender sender) {
    Set<String> tables = plugin.config().databaseTables().getTables(TableType.ALL);
    if (tables.isEmpty()) {
      SendMessage.sendToPlayer(sender, LanguageManager.message("stats.config.tables.noTables"));
      return;
    }
    String active = plugin
        .config()
        .databaseTables()
        .getTable(MatchManager.getMatch().getMap().getName());
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
        sender, LanguageManager.message("stats.config.tables.list").replace("{list}", tablesList));
  }

  public void addTableForMap(CommandSender sender, String table) {
    String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
    TableInfo tableInfo = TowersForPGM.getInstance().config().databaseTables().getTableInfo(table);
    if (tableInfo == null) {
      SendMessage.sendToPlayer(
          sender,
          LanguageManager.message("stats.config.tables.notFound").replace("{table}", table));
      return;
    }
    plugin.config().databaseTables().addMapToTable(mapName, table);
    ;
    SendMessage.sendToPlayer(
        sender,
        LanguageManager.message("stats.config.maps.added")
            .replace("{map}", mapName)
            .replace("{table}", table));
  }

  public void deleteTableForMap(CommandSender sender) {
    String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
    // if (!ConfigManager.getMapTables().containsKey(mapName)) {
    //   SendMessage.sendToPlayer(
    //       sender,
    //       LanguageManager.message("stats.config.maps.notExists").replace("{map}", mapName));
    //   return;
    // }
    plugin.config().databaseTables().removeMap(mapName);
    SendMessage.sendToPlayer(
        sender, LanguageManager.message("stats.config.maps.deleted").replace("{map}", mapName));
  }

  public void addTempTable(CommandSender sender, String table) {
    plugin.config().databaseTables().setTempTable(table);
    SendMessage.sendToPlayer(
        sender, LanguageManager.message("stats.config.temp.added").replace("{table}", table));
  }

  public void removeTempTable(CommandSender sender) {
    if (plugin.config().databaseTables().getTempTable() == null) {
      SendMessage.sendToPlayer(sender, LanguageManager.message("stats.config.temp.notExists"));
      return;
    }
    plugin.config().databaseTables().removeTempTable();
    SendMessage.sendToPlayer(sender, LanguageManager.message("stats.config.temp.removed"));
  }
}
