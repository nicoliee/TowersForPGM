package org.nicolie.towersforpgm.database;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.database.models.history.MatchHistory;
import org.nicolie.towersforpgm.database.models.history.PlayerHistory;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;

/** API de alto nivel para guardar y consultar historial de partidas */
public class MatchHistoryManager {

  /**
   * Genera un MatchID con formato <tabla>-
   * <dd>/<MM>/<yyyy>-<i>
   */
  public static CompletableFuture<String> generateMatchId(String table) {
    return CompletableFuture.supplyAsync(() -> {
      LocalDate today = LocalDate.now();
      String datePart = today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
      int next = getNextSequenceForDay(table, datePart);
      return table + "-" + datePart + "-" + next;
    });
  }

  private static int getNextSequenceForDay(String table, String datePart) {
    // Consultar count existente de match_ids que empiezan con <tabla>-<datePart>
    String prefix = table + "-" + datePart + "-"; // Ej: ranked-10/11/2025-
    try {
      java.sql.Connection conn = TowersForPGM.getInstance().getDatabaseConnection();
      String sql = "SELECT COUNT(*) FROM matches_history WHERE match_id LIKE ?";
      try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, prefix + "%");
        try (java.sql.ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            return rs.getInt(1) + 1; // siguiente número secuencial
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

  /** Guarda una partida completa (cabecera + jugadores) */
  public static CompletableFuture<Void> saveMatch(
      String matchId,
      String table,
      org.nicolie.towersforpgm.matchbot.embeds.MatchInfo matchInfo,
      boolean ranked,
      List<Stats> rawStats,
      List<PlayerEloChange> eloChanges) {
    return CompletableFuture.runAsync(() -> {
      try {
        java.sql.Connection conn = TowersForPGM.getInstance().getDatabaseConnection();
        conn.setAutoCommit(false);
        try {
          // Insert cabecera
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

          // Insert jugadores
          String insertPlayer =
              "INSERT INTO match_players_history (match_id, username, team, kills, deaths, assists, damageDone, damageTaken, points, win, game, winstreak_delta, elo_delta, maxElo_after) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
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
              int maxEloAfter = change != null ? change.getNewElo() : 0; // Nuevo elo tras partida

              int wsDelta = s.getWinstreak(); // 1 si ganó, 0 si perdió según creación de Stats

              pp.setString(1, matchId);
              pp.setString(2, s.getUsername());
              pp.setString(3, null); // team: se puede extender más adelante
              pp.setInt(4, s.getKills());
              pp.setInt(5, s.getDeaths());
              pp.setInt(6, s.getAssists());
              pp.setDouble(7, s.getDamageDone());
              pp.setDouble(8, s.getDamageTaken());
              pp.setInt(9, s.getPoints());
              pp.setInt(10, s.getWins());
              pp.setInt(11, s.getGames());
              pp.setInt(12, wsDelta); // delta winstreak (1 si ganó, 0 si perdió)
              pp.setInt(13, eloDelta);
              pp.setInt(14, maxEloAfter);
              pp.addBatch();
            }
            pp.executeBatch();
          }
          conn.commit();
        } catch (Exception e) {
          conn.rollback();
          TowersForPGM.getInstance()
              .getLogger()
              .severe("Error guardando historial partida: " + e.getMessage());
        } finally {
          conn.setAutoCommit(true);
        }
      } catch (Exception e) {
        TowersForPGM.getInstance()
            .getLogger()
            .severe("Error conexión historial: " + e.getMessage());
      }
    });
  }

  /** Obtiene un MatchHistory completo */
  public static CompletableFuture<MatchHistory> getMatch(String matchId) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        java.sql.Connection conn = TowersForPGM.getInstance().getDatabaseConnection();
        String matchSql = "SELECT * FROM matches_history WHERE match_id = ?";
        try (java.sql.PreparedStatement pm = conn.prepareStatement(matchSql)) {
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
                      prs.getString("team"),
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
    });
  }

  /** Obtiene los MatchIDs más recientes filtrados opcionalmente por input del usuario */
  public static List<String> getRecentMatchIds(String userInput) {
    List<String> matchIds = new ArrayList<>();
    try {
      java.sql.Connection conn = TowersForPGM.getInstance().getDatabaseConnection();
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
    } catch (Exception e) {
      TowersForPGM.getInstance()
          .getLogger()
          .warning("Error obteniendo matchIds recientes: " + e.getMessage());
    }
    return matchIds;
  }

  /** Realiza el rollback de una partida: revierte estadísticas y elimina del historial */
  public static CompletableFuture<Void> rollbackMatch(
      org.bukkit.command.CommandSender sender, MatchHistory history) {
    return CompletableFuture.runAsync(() -> {
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
    });
  }
}
