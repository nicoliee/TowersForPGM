package org.nicolie.towersforpgm.database.history;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.database.models.history.MatchHistory;
import org.nicolie.towersforpgm.database.models.history.MatchStats;
import org.nicolie.towersforpgm.database.models.history.TeamInfo;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;

public class MatchHistoryService {

  private static final Executor DB_EXECUTOR = Executors.newFixedThreadPool(8, r -> {
    Thread t = new Thread(r, "match-db-exec");
    t.setDaemon(true);
    return t;
  });

  private final MatchHistoryRepository repository;

  public MatchHistoryService() {
    this.repository = new MatchHistoryRepository();
  }

  public CompletableFuture<Void> saveMatch(
      String matchId,
      String table,
      org.nicolie.towersforpgm.matchbot.embeds.MatchInfo matchInfo,
      boolean ranked,
      List<Stats> rawStats,
      List<PlayerEloChange> eloChanges,
      List<TeamInfo> teams,
      Map<String, TeamInfo> playerTeamMap,
      Map<String, MatchStats> playerMatchStats) {
    return saveMatchWithRetry(
        matchId,
        table,
        matchInfo,
        ranked,
        rawStats,
        eloChanges,
        teams,
        playerTeamMap,
        playerMatchStats,
        false);
  }

  private CompletableFuture<Void> saveMatchWithRetry(
      String matchId,
      String table,
      org.nicolie.towersforpgm.matchbot.embeds.MatchInfo matchInfo,
      boolean ranked,
      List<Stats> rawStats,
      List<PlayerEloChange> eloChanges,
      List<TeamInfo> teams,
      Map<String, TeamInfo> playerTeamMap,
      Map<String, MatchStats> playerMatchStats,
      boolean isRetry) {

    return CompletableFuture.runAsync(
        () -> {
          TowersForPGM plugin = TowersForPGM.getInstance();
          long startTime = System.currentTimeMillis();

          try (Connection conn = plugin.getDatabaseConnection()) {
            conn.setAutoCommit(false);

            try {
              // Insertar match
              repository.insertMatch(conn, matchId, table, matchInfo, ranked);

              // Insertar jugadores
              repository.insertPlayers(
                  conn, matchId, rawStats, eloChanges, playerTeamMap, playerMatchStats);

              conn.commit();

              // Log exitoso
              long duration = System.currentTimeMillis() - startTime;
              plugin
                  .getLogger()
                  .info("[+] "
                      + matchId
                      + ", "
                      + (ranked ? "ranked" : "unranked")
                      + ", "
                      + duration
                      + "ms, DB: "
                      + plugin.getCurrentDatabaseType());

            } catch (Exception e) {
              // Rollback
              try {
                conn.rollback();
              } catch (Exception rollbackEx) {
                plugin.getLogger().severe("Error durante rollback: " + rollbackEx.getMessage());
              }

              long duration = System.currentTimeMillis() - startTime;

              // Log del error
              plugin
                  .getLogger()
                  .severe("Error en saveMatch para matchId "
                      + matchId
                      + ": "
                      + e.getClass().getSimpleName()
                      + " - "
                      + e.getMessage());
              e.printStackTrace();

              // Intentar retry si es MySQL y timeout
              if ("MySQL".equals(plugin.getCurrentDatabaseType())
                  && duration >= plugin.getMySQLDatabaseManager().TIMEOUT
                  && !isRetry) {
                handleTimeoutRetry(
                    matchId,
                    table,
                    matchInfo,
                    ranked,
                    rawStats,
                    eloChanges,
                    teams,
                    playerTeamMap,
                    playerMatchStats,
                    duration);
                return;
              }

              logFinalError(matchId, ranked, duration, plugin, isRetry);

            } finally {
              try {
                conn.setAutoCommit(true);
              } catch (Exception ignored) {
              }
            }

          } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            if ("SQLite".equals(plugin.getCurrentDatabaseType())) {
              plugin
                  .getLogger()
                  .severe("[SQLite] Error de conexión en saveMatch para "
                      + matchId
                      + ": "
                      + e.getClass().getSimpleName()
                      + " - "
                      + e.getMessage());
              e.printStackTrace();
            }

            // Retry para MySQL
            if ("MySQL".equals(plugin.getCurrentDatabaseType())
                && duration >= plugin.getMySQLDatabaseManager().TIMEOUT
                && !isRetry) {
              handleTimeoutRetry(
                  matchId,
                  table,
                  matchInfo,
                  ranked,
                  rawStats,
                  eloChanges,
                  teams,
                  playerTeamMap,
                  playerMatchStats,
                  duration);
              return;
            }

            logFinalError(matchId, ranked, duration, plugin, isRetry);
          }
        },
        DB_EXECUTOR);
  }

  /** Maneja el retry cuando hay timeout. */
  private void handleTimeoutRetry(
      String matchId,
      String table,
      org.nicolie.towersforpgm.matchbot.embeds.MatchInfo matchInfo,
      boolean ranked,
      List<Stats> rawStats,
      List<PlayerEloChange> eloChanges,
      List<TeamInfo> teams,
      Map<String, TeamInfo> playerTeamMap,
      Map<String, MatchStats> playerMatchStats,
      long duration) {

    TowersForPGM plugin = TowersForPGM.getInstance();
    plugin
        .getLogger()
        .warning("Timeout detectado en saveMatch para matchId "
            + matchId
            + " (duración: "
            + duration
            + "ms). Intentando recargar base de datos y reintentar...");

    if (plugin.reloadDatabase()) {
      plugin
          .getLogger()
          .info("Base de datos recargada exitosamente. Reintentando saveMatch para matchId "
              + matchId);
      try {
        saveMatchWithRetry(
                matchId,
                table,
                matchInfo,
                ranked,
                rawStats,
                eloChanges,
                teams,
                playerTeamMap,
                playerMatchStats,
                true)
            .get();
      } catch (Exception retryException) {
        plugin
            .getLogger()
            .severe("No se pudo guardar el historial de la partida matchId "
                + matchId
                + " después del retry. Operación fallida definitivamente.");
      }
    } else {
      plugin
          .getLogger()
          .severe(
              "No se pudo recargar la base de datos para retry de saveMatch matchId " + matchId);
    }
  }

  /** Log de error final. */
  private void logFinalError(
      String matchId, boolean ranked, long duration, TowersForPGM plugin, boolean isRetry) {
    if (isRetry) {
      plugin
          .getLogger()
          .severe("No se pudo guardar el historial de la partida matchId "
              + matchId
              + " después del retry. Operación fallida definitivamente.");
    }

    plugin
        .getLogger()
        .severe("[-] "
            + matchId
            + ", "
            + (ranked ? "ranked" : "unranked")
            + ", "
            + duration
            + "ms, DB: "
            + plugin.getCurrentDatabaseType());
  }

  public CompletableFuture<Void> rollbackMatch(
      org.bukkit.command.CommandSender sender, MatchHistory history) {
    return CompletableFuture.runAsync(
        () -> {
          String table = history.getTableName();
          try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection()) {
            conn.setAutoCommit(false);

            repository.updatePlayerStatsForRollback(conn, table, history);
            repository.deleteMatch(conn, history.getMatchId());

            conn.commit();

            org.bukkit.Bukkit.getScheduler().runTask(TowersForPGM.getInstance(), () -> {
              sender.sendMessage("§aRollback completado para " + history.getMatchId());
            });
          } catch (Exception e) {
            org.bukkit.Bukkit.getScheduler().runTask(TowersForPGM.getInstance(), () -> {
              sender.sendMessage("§cError en rollback: " + e.getMessage());
            });
          }
        },
        DB_EXECUTOR);
  }

  public CompletableFuture<MatchHistory> getMatch(String matchId) {
    return repository.getMatch(matchId);
  }

  public CompletableFuture<List<String>> getRecentMatchIds(String userInput) {
    return repository.getRecentMatchIds(userInput);
  }

  public int getCountForDay(String table, String datePart) {
    return repository.getCountForDay(table, datePart);
  }
}
