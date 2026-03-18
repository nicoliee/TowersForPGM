package org.nicolie.towersforpgm.commands.towers.commandUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.tables.TableInfo;
import org.nicolie.towersforpgm.configs.tables.TableType;
import org.nicolie.towersforpgm.database.TableManager;
import org.nicolie.towersforpgm.utils.MatchManager;

public class StatsConfig {

  private final TowersForPGM plugin = TowersForPGM.getInstance();

  public void enabled(Audience audience, Boolean enabled) {

    boolean state = enabled != null ? enabled : !plugin.isStatsCancel();

    if (enabled != null) {
      plugin.setStatsCancel(!enabled);
    }

    audience.sendMessage(
        Component.translatable(state ? "stats.config.enabled" : "stats.config.disabled"));
  }

  public void defaultTable(Audience audience, String table) {

    String state = plugin.config().databaseTables().getDefaultTable();

    if (table != null) {

      TableInfo info = plugin.config().databaseTables().getTableInfo(table);

      if (info == null) {
        audience.sendMessage(
            Component.translatable("stats.config.tables.notFound", Component.text(table)));
        return;
      }

      plugin.config().databaseTables().setDefaultTable(table);
      state = table;
    }

    audience.sendMessage(Component.translatable(
        table != null ? "stats.config.maps.success" : "stats.config.tables.current",
        Component.text(state)));
  }

  public void tableAdd(Audience audience, String table) {

    TableInfo info = plugin.config().databaseTables().getTableInfo(table);

    if (info != null) {
      audience.sendMessage(Component.translatable("stats.config.tables.exists"));
      return;
    }

    plugin.config().databaseTables().addTable(table, false);
    TableManager.createTable(table);

    audience.sendMessage(
        Component.translatable("stats.config.tables.created", Component.text(table)));
  }

  public void tableRemove(Audience audience, String table) {

    TableInfo info = plugin.config().databaseTables().getTableInfo(table);

    if (info == null) {
      audience.sendMessage(
          Component.translatable("stats.config.tables.notFound", Component.text(table)));
      return;
    }

    plugin.config().databaseTables().removeTable(table);

    audience.sendMessage(
        Component.translatable("stats.config.tables.deleted", Component.text(table)));
  }

  public void tables(Audience audience) {

    Set<String> tables = plugin.config().databaseTables().getTables(TableType.ALL);

    if (tables.isEmpty()) {
      audience.sendMessage(Component.translatable("stats.config.tables.noTables"));
      return;
    }

    String active = plugin
        .config()
        .databaseTables()
        .getTable(MatchManager.getMatch().getMap().getName());

    List<String> display = new ArrayList<>();

    for (String t : tables) {

      if (active != null && active.equals(t)) {
        display.add("§e" + t + "§r");
      } else {
        display.add(t);
      }
    }

    String list = String.join(", ", display);

    audience.sendMessage(Component.translatable("stats.config.tables.list", Component.text(list)));
  }

  public void map(Audience audience, String table) {

    String mapName = MatchManager.getMatch().getMap().getName();

    String state = plugin.config().databaseTables().getTable(mapName);

    if (table != null) {

      TableInfo info = plugin.config().databaseTables().getTableInfo(table);

      if (info == null) {
        audience.sendMessage(
            Component.translatable("stats.config.tables.notFound", Component.text(table)));
        return;
      }

      plugin.config().databaseTables().addMapToTable(mapName, table);
      state = table;
    }

    audience.sendMessage(Component.translatable(
        table != null ? "stats.config.maps.added" : "stats.config.maps.current",
        Component.text(mapName),
        Component.text(state)));
  }

  public void mapRemove(Audience audience) {

    String mapName = MatchManager.getMatch().getMap().getName();

    plugin.config().databaseTables().removeMap(mapName);

    audience.sendMessage(
        Component.translatable("stats.config.maps.deleted", Component.text(mapName)));
  }

  public void temp(Audience audience, String table) {

    String state = plugin.config().databaseTables().getTempTable();

    if (table != null) {
      plugin.config().databaseTables().setTempTable(table);
      state = table;
    }

    audience.sendMessage(Component.translatable(
        table != null ? "stats.config.temp.added" : "stats.config.temp.current",
        Component.text(state)));
  }

  public void tempRemove(Audience audience) {

    String state = plugin.config().databaseTables().getTempTable();

    if (state == null) {
      audience.sendMessage(Component.translatable("stats.config.temp.notExists"));
      return;
    }

    plugin.config().databaseTables().removeTempTable();

    audience.sendMessage(Component.translatable("stats.config.temp.removed"));
  }
}
