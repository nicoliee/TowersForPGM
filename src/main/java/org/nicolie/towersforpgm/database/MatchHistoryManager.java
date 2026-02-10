package org.nicolie.towersforpgm.database;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.database.models.history.MatchHistory;
import org.nicolie.towersforpgm.database.models.history.PlayerHistory;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;

public class MatchHistoryManager {

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final String SERVER_ID =
      TowersForPGM.getInstance().config().database().getId();

  private static final ConcurrentHashMap<String, AtomicInteger> counters =
      new ConcurrentHashMap<>();
  private static final int COUNTERS_WARNING_THRESHOLD = 1000;

  private static final java.util.concurrent.Executor DB_EXECUTOR =
      java.util.concurrent.Executors.newFixedThreadPool(8, r -> {
        Thread t = new Thread(r, "match-db-exec");
        t.setDaemon(true);
        return t;
      });

  public static CompletableFuture<Void> preloadMatchIdCountersAsync(Collection<String> tables) {
    return CompletableFuture.runAsync(
        () -> {
          String datePart = LocalDate.now().format(DATE_FMT);
          counters.keySet().removeIf(k -> !k.endsWith("|" + datePart));
          for (String table : tables) {
            try {
              int count = getCountForDay(table, datePart);
              counters.put(table + "|" + datePart, new AtomicInteger(count + 1));
              TowersForPGM.getInstance()
                  .getLogger()
                  .info("MatchHistoryManager: " + table + " -> " + (count + 1));
            } catch (Exception e) {
              TowersForPGM.getInstance()
                  .getLogger()
                  .warning("Error precargando matchId counter para tabla " + table + ": "
                      + e.getMessage());
            }
          }
          if (counters.size() > COUNTERS_WARNING_THRESHOLD) {
            TowersForPGM.getInstance()
                .getLogger()
                .warning("MatchHistoryManager: counters map unusually large: " + counters.size());
          }
        },
        DB_EXECUTOR);
  }

  public static CompletableFuture<String> generateMatchId(String table) {
    return CompletableFuture.supplyAsync(
        () -> {
          String datePart = LocalDate.now().format(DATE_FMT);
          String key = table + "|" + datePart;

          counters.keySet().removeIf(k -> !k.endsWith("|" + datePart));

          AtomicInteger counter = counters.computeIfAbsent(key, k -> {
            int nextSeq = getNextSequenceForDay(table, datePart);
            return new AtomicInteger(nextSeq);
          });

          int next = counter.getAndIncrement();
          if (counters.size() > COUNTERS_WARNING_THRESHOLD) {
            TowersForPGM.getInstance()
                .getLogger()
                .warning("MatchHistoryManager: counters map unusually large: " + counters.size());
          }
          return SERVER_ID + "-" + table + "-" + datePart + "-" + next;
        },
        DB_EXECUTOR);
  }

  public static int getCountForDay(String table, String datePart) {
    String prefix =
        SERVER_ID + "-" + table + "-" + datePart + "-"; // Ej: ranked1-AmistosoPGM-10/11/2025-
    try (java.sql.Connection conn = TowersForPGM.getInstance().getDatabaseConnection()) {
      String sql = "SELECT COUNT(*) FROM matches_history WHERE match_id LIKE ?";
      try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, prefix + "%");
        try (java.sql.ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            return rs.getInt(1);
          }
        }
      }
    } catch (Exception e) {
      TowersForPGM.getInstance()
          .getLogger()
          .warning("Error consultando count matchID: " + e.getMessage());
    }
    return 0;
  }

  private static int getNextSequenceForDay(String table, String datePart) {
    String prefix =
        SERVER_ID + "-" + table + "-" + datePart + "-"; // Ej: ranked1-AmistosoPGM-10/11/2025-
    try (java.sql.Connection conn = TowersForPGM.getInstance().getDatabaseConnection()) {
      String sql = "SELECT COUNT(*) FROM matches_history WHERE match_id LIKE ?";
      try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, prefix + "%");
        try (java.sql.ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            return rs.getInt(1) + 1;
          }
        }
      }
    } catch (Exception e) {
      TowersForPGM.getInstance()
          .getLogger()
          .warning("Error generando secuencia matchID: " + e.getMessage());
    }
    return 1;
  }

  public static CompletableFuture<Void> saveMatch(
      String matchId,
      String table,
      org.nicolie.towersforpgm.matchbot.embeds.MatchInfo matchInfo,
      boolean ranked,
      List<Stats> rawStats,
      List<PlayerEloChange> eloChanges) {
    return saveMatchWithRetry(matchId, table, matchInfo, ranked, rawStats, eloChanges, false);
  }

  private static CompletableFuture<Void> saveMatchWithRetry(
      String matchId,
      String table,
      org.nicolie.towersforpgm.matchbot.embeds.MatchInfo matchInfo,
      boolean ranked,
      List<Stats> rawStats,
      List<PlayerEloChange> eloChanges,
      boolean isRetry) {
    return CompletableFuture.runAsync(
        () -> {
          TowersForPGM plugin = TowersForPGM.getInstance();
          long startTime = System.currentTimeMillis();

          try (java.sql.Connection conn = plugin.getDatabaseConnection()) {
            conn.setAutoCommit(false);

            try {
              String insertMatch =
                  "INSERT INTO matches_history (match_id, table_name, map_name, duration_seconds, ranked, scores_text, winners_text, finished_at) VALUES (?,?,?,?,?,?,?,?)";

              try (java.sql.PreparedStatement pm = conn.prepareStatement(insertMatch)) {
                // Solo configurar timeout para MySQL
                if ("MySQL".equals(plugin.getCurrentDatabaseType())) {
                  pm.setQueryTimeout(
                      plugin.getMySQLDatabaseManager().TIMEOUT / 1000); // Timeout de 5 segundos
                }
                pm.setString(1, matchId);
                pm.setString(2, table);
                pm.setString(3, matchInfo.getMap());
                pm.setInt(4, matchInfo.getDurationSeconds());
                pm.setInt(5, ranked ? 1 : 0);
                pm.setString(6, matchInfo.getScoresText());
                pm.setString(7, matchInfo.getWinnersText());
                pm.setLong(8, matchInfo.getFinishedAt());
                pm.executeUpdate();
              }

              String insertPlayer =
                  "INSERT INTO match_players_history (match_id, username, kills, deaths, assists, damageDone, damageTaken, points, win, game, winstreak_delta, elo_delta, maxElo_after) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
              try (java.sql.PreparedStatement pp = conn.prepareStatement(insertPlayer)) {
                // Solo configurar timeout para MySQL
                if ("MySQL".equals(plugin.getCurrentDatabaseType())) {
                  pp.setQueryTimeout(5); // Timeout de 5 segundos
                }
                for (Stats s : rawStats) {
                  PlayerEloChange change = null;
                  if (eloChanges != null) {
                    change = eloChanges.stream()
                        .filter(c -> c.getUsername().equalsIgnoreCase(s.getUsername()))
                        .findFirst()
                        .orElse(null);
                  }
                  int eloDelta = change != null ? (change.getNewElo() - change.getCurrentElo()) : 0;
                  int maxEloAfter = change != null ? change.getNewElo() : 0;

                  int wsDelta = s.getWinstreak();

                  pp.setString(1, matchId);
                  pp.setString(2, s.getUsername());
                  pp.setInt(3, s.getKills());
                  pp.setInt(4, s.getDeaths());
                  pp.setInt(5, s.getAssists());
                  pp.setDouble(6, s.getDamageDone());
                  pp.setDouble(7, s.getDamageTaken());
                  pp.setInt(8, s.getPoints());
                  pp.setInt(9, s.getWins());
                  pp.setInt(10, s.getGames());
                  pp.setInt(11, wsDelta);
                  pp.setInt(12, eloDelta);
                  pp.setInt(13, maxEloAfter);
                  pp.addBatch();
                }
                pp.executeBatch();
              }
              conn.commit();

              // Log successful save
              long duration = System.currentTimeMillis() - startTime;
              plugin
                  .getLogger()
                  .info("[+] " + matchId + ", " + (ranked ? "ranked" : "unranked") + ", " + duration
                      + "ms, DB: " + plugin.getCurrentDatabaseType());

            } catch (Exception e) {

              long duration = System.currentTimeMillis() - startTime;

              if ("MySQL".equals(plugin.getCurrentDatabaseType()) && duration >= 5000 && !isRetry) {
                plugin
                    .getLogger()
                    .warning(
                        "Timeout detectado en saveMatch para matchId " + matchId + " (duración: "
                            + duration + "ms). Intentando recargar base de datos y reintentar...");

                // Intentar recargar la base de datos
                if (plugin.reloadDatabase()) {
                  plugin
                      .getLogger()
                      .info(
                          "Base de datos recargada exitosamente. Reintentando saveMatch para matchId "
                              + matchId);
                  // Llamar recursivamente con retry
                  try {
                    saveMatchWithRetry(
                            matchId, table, matchInfo, ranked, rawStats, eloChanges, true)
                        .get();
                    return; // Si el retry fue exitoso, salir
                  } catch (Exception retryException) {
                    plugin
                        .getLogger()
                        .severe("No se pudo guardar el historial de la partida matchId " + matchId
                            + " después del retry. Operación fallida definitivamente.");
                  }
                } else {
                  plugin
                      .getLogger()
                      .severe(
                          "No se pudo recargar la base de datos para retry de saveMatch matchId "
                              + matchId);
                }
              }

              // Si es un retry fallido o no es caso de timeout, registrar error definitivo
              if (isRetry) {
                plugin
                    .getLogger()
                    .severe("No se pudo guardar el historial de la partida matchId " + matchId
                        + " después del retry. Operación fallida definitivamente.");
              }

              plugin
                  .getLogger()
                  .severe("[-] " + matchId + ", " + (ranked ? "ranked" : "unranked") + ", "
                      + duration + "ms, DB: " + plugin.getCurrentDatabaseType());
            } finally {
              try {
                conn.setAutoCommit(true);
              } catch (Exception ignored) {
              }
            }
          } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            // Log específico para SQLite - error en conexión
            if ("SQLite".equals(plugin.getCurrentDatabaseType())) {
              plugin
                  .getLogger()
                  .severe("[SQLite] Error de conexión en saveMatch para " + matchId + ": "
                      + e.getClass().getSimpleName() + " - " + e.getMessage());
              e.printStackTrace();
            }

            // Si es MySQL y la duración supera los 5000ms (timeout), intentar retry una sola vez
            if ("MySQL".equals(plugin.getCurrentDatabaseType())
                && duration >= plugin.getMySQLDatabaseManager().TIMEOUT
                && !isRetry) {
              plugin
                  .getLogger()
                  .warning("Timeout detectado en conexión saveMatch para matchId " + matchId
                      + " (duración: " + duration
                      + "ms). Intentando recargar base de datos y reintentar...");

              // Intentar recargar la base de datos
              if (plugin.reloadDatabase()) {
                plugin
                    .getLogger()
                    .info(
                        "Base de datos recargada exitosamente. Reintentando saveMatch para matchId "
                            + matchId);
                // Llamar recursivamente con retry
                try {
                  saveMatchWithRetry(matchId, table, matchInfo, ranked, rawStats, eloChanges, true)
                      .get();
                  return; // Si el retry fue exitoso, salir
                } catch (Exception retryException) {
                  plugin
                      .getLogger()
                      .severe("No se pudo guardar el historial de la partida matchId " + matchId
                          + " después del retry. Operación fallida definitivamente.");
                }
              } else {
                plugin
                    .getLogger()
                    .severe("No se pudo recargar la base de datos para retry de saveMatch matchId "
                        + matchId);
              }
            }

            // Si es un retry fallido o no es caso de timeout, registrar error definitivo
            if (isRetry) {
              plugin
                  .getLogger()
                  .severe("No se pudo guardar el historial de la partida matchId " + matchId
                      + " después del retry. Operación fallida definitivamente.");
            }

            plugin
                .getLogger()
                .severe("[-] " + matchId + ", " + (ranked ? "ranked" : "unranked") + ", " + duration
                    + "ms, DB: " + plugin.getCurrentDatabaseType());
          }
        },
        DB_EXECUTOR);
  }

  public static CompletableFuture<MatchHistory> getMatch(String matchId) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            String matchSql = "SELECT * FROM matches_history WHERE match_id = ?";
            try (java.sql.Connection conn = TowersForPGM.getInstance().getDatabaseConnection();
                java.sql.PreparedStatement pm = conn.prepareStatement(matchSql)) {
              pm.setString(1, matchId);
              try (java.sql.ResultSet rs = pm.executeQuery()) {
                if (!rs.next()) return null;
                String tableName = rs.getString("table_name");
                String mapName = rs.getString("map_name");
                int duration = rs.getInt("duration_seconds");
                boolean ranked = rs.getInt("ranked") == 1;
                String scores = rs.getString("scores_text");
                String winners = rs.getString("winners_text");
                long finishedAt = rs.getLong("finished_at");
                List<PlayerHistory> players = new ArrayList<>();
                String playerSql = "SELECT * FROM match_players_history WHERE match_id = ?";
                try (java.sql.PreparedStatement pp = conn.prepareStatement(playerSql)) {
                  pp.setString(1, matchId);
                  try (java.sql.ResultSet prs = pp.executeQuery()) {
                    while (prs.next()) {
                      players.add(new PlayerHistory(
                          prs.getString("username"),
                          prs.getInt("kills"),
                          prs.getInt("deaths"),
                          prs.getInt("assists"),
                          prs.getDouble("damageDone"),
                          prs.getDouble("damageTaken"),
                          prs.getInt("points"),
                          prs.getInt("win"),
                          prs.getInt("game"),
                          prs.getInt("winstreak_delta"),
                          prs.getInt("elo_delta"),
                          prs.getInt("maxElo_after")));
                    }
                  }
                }
                return new MatchHistory(
                    matchId,
                    tableName,
                    mapName,
                    duration,
                    ranked,
                    scores,
                    winners,
                    players,
                    finishedAt);
              }
            }
          } catch (Exception e) {
            TowersForPGM.getInstance()
                .getLogger()
                .warning("Error obteniendo historial: " + e.getMessage());
            return null;
          }
        },
        DB_EXECUTOR);
  }

  public static CompletableFuture<List<String>> getRecentMatchIds(String userInput) {
    return CompletableFuture.supplyAsync(
        () -> {
          List<String> matchIds = new ArrayList<>();
          try {
            try (java.sql.Connection conn = TowersForPGM.getInstance().getDatabaseConnection()) {
              String sql;
              if (userInput == null || userInput.trim().isEmpty()) {
                sql = "SELECT match_id FROM matches_history ORDER BY created_at DESC LIMIT 25";
              } else {
                sql =
                    "SELECT match_id FROM matches_history WHERE match_id LIKE ? ORDER BY created_at DESC LIMIT 25";
              }
              try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                if (userInput != null && !userInput.trim().isEmpty()) {
                  ps.setString(1, "%" + userInput + "%");
                }
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                  while (rs.next()) {
                    matchIds.add(rs.getString("match_id"));
                  }
                }
              }
            }
          } catch (Exception e) {
            TowersForPGM.getInstance()
                .getLogger()
                .warning("Error obteniendo matchIds recientes: " + e.getMessage());
          }
          return matchIds;
        },
        DB_EXECUTOR);
  }

  public static CompletableFuture<Void> rollbackMatch(
      org.bukkit.command.CommandSender sender, MatchHistory history) {
    return CompletableFuture.runAsync(
        () -> {
          String table = history.getTableName();
          try (java.sql.Connection conn = TowersForPGM.getInstance().getDatabaseConnection()) {
            conn.setAutoCommit(false);

            // Preparamos update SQL dinámico según si es ranked o no
            String updateSql = "UPDATE " + table + " SET "
                + "kills = kills - ?, "
                + "deaths = deaths - ?, "
                + "assists = assists - ?, "
                + "damageDone = damageDone - ?, "
                + "damageTaken = damageTaken - ?, "
                + "points = points - ?, "
                + "wins = wins - ?, "
                + "games = games - ?"
                + (history.isRanked() ? ", elo = elo - ?" : "")
                + " WHERE username = ?";

            try (java.sql.PreparedStatement ps = conn.prepareStatement(updateSql)) {
              for (PlayerHistory ph : history.getPlayers()) {
                int paramIndex = 1;
                ps.setInt(paramIndex++, ph.getKills());
                ps.setInt(paramIndex++, ph.getDeaths());
                ps.setInt(paramIndex++, ph.getAssists());
                ps.setDouble(paramIndex++, ph.getDamageDone());
                ps.setDouble(paramIndex++, ph.getDamageTaken());
                ps.setInt(paramIndex++, ph.getPoints());
                ps.setInt(paramIndex++, ph.getWin());
                ps.setInt(paramIndex++, ph.getGame());

                if (history.isRanked()) {
                  int eloDelta = ph.getEloDelta() != null ? ph.getEloDelta() : 0;
                  ps.setInt(paramIndex++, eloDelta); // resta elo delta
                }
                ps.setString(paramIndex, ph.getUsername());
                ps.addBatch();
              }
              ps.executeBatch();
            }

            String deletePlayers = "DELETE FROM match_players_history WHERE match_id = ?";
            String deleteMatch = "DELETE FROM matches_history WHERE match_id = ?";
            try (java.sql.PreparedStatement dp = conn.prepareStatement(deletePlayers);
                java.sql.PreparedStatement dm = conn.prepareStatement(deleteMatch)) {
              dp.setString(1, history.getMatchId());
              dp.executeUpdate();
              dm.setString(1, history.getMatchId());
              dm.executeUpdate();
            }

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
}
