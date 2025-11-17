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
          return table + "-" + datePart + "-" + next;
        },
        DB_EXECUTOR);
  }

  public static int getCountForDay(String table, String datePart) {
    String prefix = table + "-" + datePart + "-"; // Ej: ranked-10/11/2025-
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
    String prefix = table + "-" + datePart + "-"; // Ej: ranked-10/11/2025-
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
    return CompletableFuture.runAsync(
        () -> {
          try (java.sql.Connection conn = TowersForPGM.getInstance().getDatabaseConnection()) {
            conn.setAutoCommit(false);
            try {
              String insertMatch =
                  "INSERT INTO matches_history (match_id, table_name, map_name, duration_seconds, ranked, scores_text, winners_text, finished_at) VALUES (?,?,?,?,?,?,?,?)";
              try (java.sql.PreparedStatement pm = conn.prepareStatement(insertMatch)) {
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
                for (Stats s : rawStats) {
                  PlayerEloChange change = null;
                  if (eloChanges != null) {
                    change = eloChanges.stream()
                        .filter(c -> c.getUsername().equalsIgnoreCase(s.getUsername()))
                        .findFirst()
                        .orElse(null);
                  }
                  int eloDelta = change != null ? (change.getNewElo() - change.getCurrentElo()) : 0;
                  int maxEloAfter =
                      change != null ? change.getNewElo() : 0;

                  int wsDelta = s.getWinstreak();

                  pp.setString(1, matchId);
                  pp.setString(2, s.getUsername());
                  pp.setInt(4, s.getKills());
                  pp.setInt(5, s.getDeaths());
                  pp.setInt(6, s.getAssists());
                  pp.setDouble(7, s.getDamageDone());
                  pp.setDouble(8, s.getDamageTaken());
                  pp.setInt(9, s.getPoints());
                  pp.setInt(10, s.getWins());
                  pp.setInt(11, s.getGames());
                  pp.setInt(12, wsDelta);
                  pp.setInt(13, eloDelta);
                  pp.setInt(14, maxEloAfter);
                  pp.addBatch();
                }
                pp.executeBatch();
              }
              conn.commit();
            } catch (Exception e) {
              try {
                conn.rollback();
              } catch (Exception ex) {
                // ignore rollback failure
              }
              TowersForPGM.getInstance()
                  .getLogger()
                  .severe("Error guardando historial partida: " + e.getMessage());
            } finally {
              try {
                conn.setAutoCommit(true);
              } catch (Exception ignored) {
              }
            }
          } catch (Exception e) {
            TowersForPGM.getInstance()
                .getLogger()
                .severe("Error conexión historial: " + e.getMessage());
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

            // Borrar registros de historial tras rollback
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

            // Enviar mensaje de éxito en sync
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
