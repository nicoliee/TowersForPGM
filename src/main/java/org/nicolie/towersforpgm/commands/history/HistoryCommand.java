package org.nicolie.towersforpgm.commands.history;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.commands.history.gui.playerHistory.HistoryMenu;
import org.nicolie.towersforpgm.configs.tables.TableType;
import org.nicolie.towersforpgm.database.MatchHistoryManager;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.database.models.history.MatchHistory;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.*;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.suggestion.Suggestions;
import tc.oc.pgm.lib.org.incendo.cloud.context.CommandContext;
import tc.oc.pgm.lib.org.incendo.cloud.context.CommandInput;
import tc.oc.pgm.util.Audience;

public class HistoryCommand {
  private final TowersForPGM plugin;

  public HistoryCommand(TowersForPGM plugin) {
    this.plugin = plugin;
  }

  @Command("history")
  @CommandDescription("Show match history and stats for a player")
  public void history(
      Audience audience,
      Player sender,
      @Flag(
              value = "player",
              aliases = {"p"},
              suggestions = "onlinePlayers")
          String target,
      @Flag(
              value = "table",
              aliases = {"t"},
              suggestions = "tables")
          String table) {

    String resolvedPlayer = (target == null || target.isBlank()) ? sender.getName() : target;
    String resolvedTable = (table == null || table.isBlank()) ? null : table;
    executeHistory(audience, sender, resolvedPlayer, resolvedTable);
  }

  private void executeHistory(Audience audience, Player sender, String target, String table) {
    String targetName = (target == null || target.isBlank()) ? sender.getName() : target;
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(sender);

    String resolvedTable;
    if (table == null || table.isBlank()) {
      String mapName = matchPlayer.getMatch().getMap().getName();
      resolvedTable = plugin.config().databaseTables().getTable(mapName);
    } else {
      resolvedTable = table;
    }

    final String finalTable =
        (resolvedTable != null && resolvedTable.equalsIgnoreCase("none")) ? null : resolvedTable;

    String firstTable = plugin.config().databaseTables().getTables(TableType.ALL).stream()
        .findFirst()
        .orElse(null);
    final String statsTable = finalTable != null ? finalTable : firstTable;

    CompletableFuture<List<MatchHistory>> matchesFuture = MatchHistoryManager.getPlayerMatchHistory(
        targetName, finalTable, HistoryMenu.HISTORY_LIMIT);

    CompletableFuture<Stats> statsFuture = StatsManager.getStats(statsTable, targetName);

    matchesFuture
        .thenCombine(statsFuture, (matches, stats) -> {
          if (stats == null) {
            Bukkit.getScheduler()
                .runTask(
                    plugin,
                    () -> audience.sendWarning(Component.translatable("command.playerNotFound")));
            return null;
          }
          Bukkit.getScheduler().runTask(plugin, () -> {
            new HistoryMenu(matchPlayer, null, finalTable, stats, matches, targetName).open();
          });
          return null;
        })
        .exceptionally(ex -> {
          plugin
              .getLogger()
              .warning("Error cargando historial de " + targetName + ": " + ex.getMessage());
          Bukkit.getScheduler()
              .runTask(
                  plugin,
                  () -> audience.sendWarning(Component.translatable("command.emptyResult")));
          return null;
        });
  }

  @Suggestions("onlinePlayers")
  public List<String> suggestOnlinePlayers(
      CommandContext<CommandSender> context, CommandInput input) {
    String typed = input.remainingInput().toLowerCase();
    return Bukkit.getOnlinePlayers().stream()
        .map(Player::getName)
        .filter(name -> name.toLowerCase().startsWith(typed))
        .toList();
  }

  @Suggestions("tables")
  public List<String> suggestTables(CommandContext<CommandSender> context, CommandInput input) {
    String typed = input.remainingInput().toLowerCase();
    return List.copyOf(plugin.config().databaseTables().getTables(TableType.ALL)).stream()
        .filter(table -> table.toLowerCase().startsWith(typed))
        .toList();
  }
}
