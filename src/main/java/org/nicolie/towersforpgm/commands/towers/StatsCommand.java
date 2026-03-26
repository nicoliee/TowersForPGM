package org.nicolie.towersforpgm.commands.towers;

import java.util.List;
import java.util.Set;
import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.commands.towers.commandUtils.StatsConfig;
import org.nicolie.towersforpgm.configs.tables.TableType;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.*;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.suggestion.Suggestions;
import tc.oc.pgm.util.Audience;

public class StatsCommand {
  private final StatsConfig statsConfig = new StatsConfig();

  @Command("towers stats add <table>")
  @CommandDescription("Configure stats table for a match")
  public void statsAdd(
      Audience audience,
      CommandSender sender,
      @Argument(value = "table", suggestions = "allTables") String table) {
    statsConfig.tableAdd(audience, table);
  }

  @Command("towers stats addMap <table>")
  public void statsAddMap(
      Audience audience,
      CommandSender sender,
      @Argument(value = "table", suggestions = "allTables") String table) {
    statsConfig.map(audience, table);
  }

  @Command("towers stats addTemporary <table>")
  public void statsAddTemporary(
      Audience audience,
      CommandSender sender,
      @Argument(value = "table", suggestions = "allTables") String table) {
    statsConfig.temp(audience, table);
  }

  @Command("towers stats default [table]")
  public void statsDefault(
      Audience audience,
      CommandSender sender,
      @Argument(value = "table", suggestions = "allTables") String table) {
    statsConfig.defaultTable(audience, table);
  }

  @Command("towers stats list")
  public void statsList(Audience audience, CommandSender sender) {
    statsConfig.tables(audience);
  }

  @Command("towers stats remove <table>")
  public void statsRemove(
      Audience audience,
      CommandSender sender,
      @Argument(value = "table", suggestions = "allTables") String table) {
    statsConfig.tableRemove(audience, table);
  }

  @Command("towers stats removeMap")
  public void statsRemoveMap(Audience audience, CommandSender sender) {
    statsConfig.mapRemove(audience);
  }

  @Command("towers stats removeTemporary")
  @CommandDescription("Remove temporary stats table")
  public void statsRemoveTemporary(Audience audience, CommandSender sender) {
    statsConfig.tempRemove(audience);
  }

  @Suggestions("allTables")
  public List<String> allTablesSuggestions(CommandSender sender) {
    Set<String> tables =
        TowersForPGM.getInstance().config().databaseTables().getTables(TableType.ALL);
    return List.copyOf(tables);
  }
}
