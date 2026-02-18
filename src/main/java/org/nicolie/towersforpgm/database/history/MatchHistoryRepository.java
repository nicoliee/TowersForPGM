package org.nicolie.towersforpgm.database.history;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.database.models.history.MatchHistory;
import org.nicolie.towersforpgm.database.models.history.MatchStats;
import org.nicolie.towersforpgm.database.models.history.PlayerHistory;
import org.nicolie.towersforpgm.database.models.history.TeamInfo;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;

/**
 * Repositorio para acceso a datos de historial de partidas. Maneja todas las operaciones de base de
 * datos.
 */
public class MatchHistoryRepository {

  private static final Executor DB_EXECUTOR = Executors.newFixedThreadPool(8, r -> {
    Thread t = new Thread(r, "match-db-exec");
    t.setDaemon(true);
    return t;
  });

  /**
   * Inserta un match en la tabla matches_history.
   *
   * @param conn Conexión a la base de datos
   * @param matchId ID del match
   * @param table Nombre de la tabla
   * @param matchInfo Información del match
   * @param ranked Si es ranked o no
   * @throws SQLException Si hay error en la inserción
   */
  public void insertMatch(
      Connection conn,
      String matchId,
      String table,
      org.nicolie.towersforpgm.matchbot.embeds.MatchInfo matchInfo,
      boolean ranked)
      throws SQLException {

    String insertMatch =
        "INSERT INTO matches_history (match_id, table_name, map_name, duration_seconds, "
            + "ranked, scores_text, winners_text, finished_at) VALUES (?,?,?,?,?,?,?,?)";

    try (PreparedStatement pm = conn.prepareStatement(insertMatch)) {
      TowersForPGM plugin = TowersForPGM.getInstance();
      if ("MySQL".equals(plugin.getCurrentDatabaseType())) {
        pm.setQueryTimeout(plugin.getMySQLDatabaseManager().TIMEOUT / 1000);
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
  }

  /**
   * Inserta los jugadores de un match en la tabla match_players_history.
   *
   * @param conn Conexión a la base de datos
   * @param matchId ID del match
   * @param rawStats Estadísticas básicas de los jugadores
   * @param eloChanges Cambios de ELO (puede ser null)
   * @param playerTeamMap Mapa de jugador -> TeamInfo (puede ser null)
   * @param playerMatchStats Mapa de jugador -> MatchStats (puede ser null)
   * @throws SQLException Si hay error en la inserción
   */
  public void insertPlayers(
      Connection conn,
      String matchId,
      List<Stats> rawStats,
      List<PlayerEloChange> eloChanges,
      Map<String, TeamInfo> playerTeamMap,
      Map<String, MatchStats> playerMatchStats)
      throws SQLException {

    // Verificar si las columnas extendidas existen en la base de datos
    boolean hasExtendedColumns = checkIfExtendedColumnsExist(conn);
    boolean hasExtendedInfo =
        playerTeamMap != null && playerMatchStats != null && hasExtendedColumns;

    String insertPlayer;
    if (hasExtendedInfo) {
      insertPlayer = "INSERT INTO match_players_history ("
          + "match_id, username, "
          // K/D stats
          + "kills, deaths, assists, "
          // Bow stats
          + "longest_bow_kill, bow_damage, bow_damage_taken, shots_taken, shots_hit, "
          // Damage stats
          + "damage_done, damage_taken, "
          // Objective stats
          + "destroyable_pieces_broken, monuments_destroyed, "
          + "flags_captured, flag_pickups, cores_leaked, "
          + "wools_captured, wools_touched, longest_flag_hold_millis, "
          + "points, "
          // Match info
          + "win, game, winstreak_delta, elo_delta, maxElo_after, "
          // Team info
          + "team_name, team_color_hex, team_score, elo_before, "
          // Additional stats
          + "killstreak, max_killstreak"
          + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    } else {
      // Modo legacy/retrocompatible
      insertPlayer = "INSERT INTO match_players_history ("
          + "match_id, username, kills, deaths, assists, "
          + "damage_done, damage_taken, points, "
          + "win, game, winstreak_delta, elo_delta, maxElo_after"
          + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
    }

    try (PreparedStatement pp = conn.prepareStatement(insertPlayer)) {
      TowersForPGM plugin = TowersForPGM.getInstance();
      if ("MySQL".equals(plugin.getCurrentDatabaseType())) {
        pp.setQueryTimeout(5);
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
        int eloBefore = change != null ? change.getCurrentElo() : 0;
        int wsDelta = s.getWinstreak();

        if (hasExtendedInfo) {
          TeamInfo teamInfo = playerTeamMap.get(s.getUsername());
          MatchStats matchStats = playerMatchStats.get(s.getUsername());

          int paramIndex = 1;
          pp.setString(paramIndex++, matchId);
          pp.setString(paramIndex++, s.getUsername());

          // K/D stats
          pp.setInt(paramIndex++, matchStats != null ? matchStats.getKills() : s.getKills());
          pp.setInt(paramIndex++, matchStats != null ? matchStats.getDeaths() : s.getDeaths());
          pp.setInt(paramIndex++, matchStats != null ? matchStats.getAssists() : s.getAssists());

          // Bow stats
          if (matchStats != null) {
            pp.setInt(paramIndex++, matchStats.getLongestBowKill());
            pp.setDouble(paramIndex++, matchStats.getBowDamage());
            pp.setDouble(paramIndex++, matchStats.getBowDamageTaken());
            pp.setInt(paramIndex++, matchStats.getShotsTaken());
            pp.setInt(paramIndex++, matchStats.getShotsHit());
          } else {
            pp.setInt(paramIndex++, 0);
            pp.setDouble(paramIndex++, 0.0);
            pp.setDouble(paramIndex++, 0.0);
            pp.setInt(paramIndex++, 0);
            pp.setInt(paramIndex++, 0);
          }

          // Damage stats
          pp.setDouble(
              paramIndex++, matchStats != null ? matchStats.getDamageDone() : s.getDamageDone());
          pp.setDouble(
              paramIndex++, matchStats != null ? matchStats.getDamageTaken() : s.getDamageTaken());

          // Objective stats
          if (matchStats != null) {
            pp.setInt(paramIndex++, matchStats.getDestroyablePiecesBroken());
            pp.setInt(paramIndex++, matchStats.getMonumentsDestroyed());
            pp.setInt(paramIndex++, matchStats.getFlagsCaptured());
            pp.setInt(paramIndex++, matchStats.getFlagPickups());
            pp.setInt(paramIndex++, matchStats.getCoresLeaked());
            pp.setInt(paramIndex++, matchStats.getWoolsCaptured());
            pp.setInt(paramIndex++, matchStats.getWoolsTouched());
            pp.setLong(paramIndex++, matchStats.getLongestFlagHoldMillis());
          } else {
            pp.setInt(paramIndex++, 0);
            pp.setInt(paramIndex++, 0);
            pp.setInt(paramIndex++, 0);
            pp.setInt(paramIndex++, 0);
            pp.setInt(paramIndex++, 0);
            pp.setInt(paramIndex++, 0);
            pp.setInt(paramIndex++, 0);
            pp.setLong(paramIndex++, 0L);
          }

          pp.setInt(paramIndex++, s.getPoints());

          // Match info
          pp.setInt(paramIndex++, s.getWins());
          pp.setInt(paramIndex++, s.getGames());
          pp.setInt(paramIndex++, wsDelta);
          pp.setInt(paramIndex++, eloDelta);
          pp.setInt(paramIndex++, maxEloAfter);

          // Team info
          pp.setString(paramIndex++, teamInfo != null ? teamInfo.getTeamName() : null);
          pp.setString(paramIndex++, teamInfo != null ? teamInfo.getColorHex() : null);
          if (teamInfo != null && teamInfo.getScore() != null) {
            pp.setInt(paramIndex++, teamInfo.getScore());
          } else {
            pp.setNull(paramIndex++, java.sql.Types.INTEGER);
          }

          if (change != null) {
            pp.setInt(paramIndex++, eloBefore);
          } else {
            pp.setNull(paramIndex++, java.sql.Types.INTEGER);
          }

          // Additional stats
          if (matchStats != null) {
            pp.setInt(paramIndex++, matchStats.getKillstreak());
            pp.setInt(paramIndex++, matchStats.getMaxKillstreak());
          } else {
            pp.setInt(paramIndex++, 0);
            pp.setInt(paramIndex++, 0);
          }

        } else {
          // Modo legacy
          int paramIndex = 1;
          pp.setString(paramIndex++, matchId);
          pp.setString(paramIndex++, s.getUsername());
          pp.setInt(paramIndex++, s.getKills());
          pp.setInt(paramIndex++, s.getDeaths());
          pp.setInt(paramIndex++, s.getAssists());
          pp.setDouble(paramIndex++, s.getDamageDone());
          pp.setDouble(paramIndex++, s.getDamageTaken());
          pp.setInt(paramIndex++, s.getPoints());
          pp.setInt(paramIndex++, s.getWins());
          pp.setInt(paramIndex++, s.getGames());
          pp.setInt(paramIndex++, wsDelta);
          pp.setInt(paramIndex++, eloDelta);
          pp.setInt(paramIndex++, maxEloAfter);
        }

        pp.addBatch();
      }
      pp.executeBatch();
    }
  }

  /**
   * Actualiza las estadísticas de los jugadores en la tabla principal (rollback).
   *
   * @param conn Conexión a la base de datos
   * @param table Nombre de la tabla
   * @param history Historial del match a revertir
   * @throws SQLException Si hay error en la actualización
   */
  public void updatePlayerStatsForRollback(Connection conn, String table, MatchHistory history)
      throws SQLException {

    String updateSql = "UPDATE "
        + table
        + " SET "
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

    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
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
          ps.setInt(paramIndex++, eloDelta);
        }
        ps.setString(paramIndex, ph.getUsername());
        ps.addBatch();
      }
      ps.executeBatch();
    }
  }

  /**
   * Elimina un match del historial.
   *
   * @param conn Conexión a la base de datos
   * @param matchId ID del match a eliminar
   * @throws SQLException Si hay error en la eliminación
   */
  public void deleteMatch(Connection conn, String matchId) throws SQLException {
    String deletePlayers = "DELETE FROM match_players_history WHERE match_id = ?";
    String deleteMatch = "DELETE FROM matches_history WHERE match_id = ?";

    try (PreparedStatement dp = conn.prepareStatement(deletePlayers);
        PreparedStatement dm = conn.prepareStatement(deleteMatch)) {
      dp.setString(1, matchId);
      dp.executeUpdate();
      dm.setString(1, matchId);
      dm.executeUpdate();
    }
  }

  /**
   * Obtiene el conteo de matches para un día específico.
   *
   * @param table Nombre de la tabla
   * @param datePart Parte de la fecha (formato dd/MM/yyyy)
   * @return Número de matches en ese día
   */
  public int getCountForDay(String table, String datePart) {
    String serverId = TowersForPGM.getInstance().config().database().getId();
    String prefix = serverId + "-" + table + "-" + datePart + "-";

    try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection()) {
      String sql = "SELECT COUNT(*) FROM matches_history WHERE match_id LIKE ?";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, prefix + "%");
        try (ResultSet rs = ps.executeQuery()) {
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

  /**
   * Obtiene un match del historial por su ID.
   *
   * @param matchId ID del match
   * @return MatchHistory o null si no existe
   */
  public CompletableFuture<MatchHistory> getMatch(String matchId) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            String matchSql = "SELECT * FROM matches_history WHERE match_id = ?";
            try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection();
                PreparedStatement pm = conn.prepareStatement(matchSql)) {
              pm.setString(1, matchId);
              try (ResultSet rs = pm.executeQuery()) {
                if (!rs.next()) return null;

                String tableName = rs.getString("table_name");
                String mapName = rs.getString("map_name");
                int duration = rs.getInt("duration_seconds");
                boolean ranked = rs.getInt("ranked") == 1;
                String scores = rs.getString("scores_text");
                String winners = rs.getString("winners_text");
                long finishedAt = rs.getLong("finished_at");

                List<PlayerHistory> players = new ArrayList<>();
                List<TeamInfo> teams = new ArrayList<>();
                Map<String, TeamInfo> teamsMap = new HashMap<>();

                String playerSql = "SELECT * FROM match_players_history WHERE match_id = ?";
                try (PreparedStatement pp = conn.prepareStatement(playerSql)) {
                  pp.setString(1, matchId);
                  try (ResultSet prs = pp.executeQuery()) {
                    boolean hasExtendedColumns = hasColumn(prs, "team_name");

                    while (prs.next()) {
                      String username = prs.getString("username");
                      int kills = prs.getInt("kills");
                      int deaths = prs.getInt("deaths");
                      int assists = prs.getInt("assists");
                      double damageDone = prs.getDouble("damage_done");
                      double damageTaken = prs.getDouble("damage_taken");
                      int points = prs.getInt("points");
                      int win = prs.getInt("win");
                      int game = prs.getInt("game");
                      int winstreakDelta = prs.getInt("winstreak_delta");
                      Integer eloDelta = prs.getInt("elo_delta");
                      Integer maxEloAfter = prs.getInt("maxElo_after");

                      if (hasExtendedColumns) {
                        String teamName = prs.getString("team_name");
                        String teamColorHex = getStringOrNull(prs, "team_color_hex");
                        Integer teamScore = getIntOrNull(prs, "team_score");
                        Integer eloBefore = getIntOrNull(prs, "elo_before");

                        MatchStats matchStats = null;
                        if (hasColumn(prs, "max_killstreak")) {
                          matchStats = buildMatchStatsFromResultSet(prs, username, teamName);
                        }

                        if (teamName != null && !teamsMap.containsKey(teamName)) {
                          TeamInfo teamInfo =
                              new TeamInfo(teamName, teamColorHex, win == 1, teamScore);
                          teamsMap.put(teamName, teamInfo);
                          teams.add(teamInfo);
                        }

                        players.add(new PlayerHistory(
                            username,
                            kills,
                            deaths,
                            assists,
                            damageDone,
                            damageTaken,
                            points,
                            win,
                            game,
                            winstreakDelta,
                            eloDelta,
                            maxEloAfter,
                            teamName,
                            teamColorHex,
                            eloBefore,
                            matchStats));
                      } else {
                        players.add(new PlayerHistory(
                            username,
                            kills,
                            deaths,
                            assists,
                            damageDone,
                            damageTaken,
                            points,
                            win,
                            game,
                            winstreakDelta,
                            eloDelta,
                            maxEloAfter));
                      }
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
                    finishedAt,
                    teams.isEmpty() ? null : teams);
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

  /**
   * Obtiene los IDs de matches recientes.
   *
   * @param userInput Filtro opcional
   * @return Lista de IDs de matches
   */
  public CompletableFuture<List<String>> getRecentMatchIds(String userInput) {
    return CompletableFuture.supplyAsync(
        () -> {
          List<String> matchIds = new ArrayList<>();
          try {
            try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection()) {
              String sql;
              if (userInput == null || userInput.trim().isEmpty()) {
                sql = "SELECT match_id FROM matches_history ORDER BY created_at DESC LIMIT 25";
              } else {
                sql = "SELECT match_id FROM matches_history WHERE match_id LIKE ? "
                    + "ORDER BY created_at DESC LIMIT 25";
              }
              try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (userInput != null && !userInput.trim().isEmpty()) {
                  ps.setString(1, "%" + userInput + "%");
                }
                try (ResultSet rs = ps.executeQuery()) {
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

  // Métodos auxiliares

  private boolean hasColumn(ResultSet rs, String columnName) {
    try {
      rs.findColumn(columnName);
      return true;
    } catch (SQLException e) {
      return false;
    }
  }

  private String getStringOrNull(ResultSet rs, String columnName) {
    try {
      return rs.getString(columnName);
    } catch (SQLException e) {
      return null;
    }
  }

  private Integer getIntOrNull(ResultSet rs, String columnName) {
    try {
      int value = rs.getInt(columnName);
      return rs.wasNull() ? null : value;
    } catch (SQLException e) {
      return null;
    }
  }

  private MatchStats buildMatchStatsFromResultSet(ResultSet prs, String username, String teamName)
      throws SQLException {
    int kills = prs.getInt("kills");
    int deaths = prs.getInt("deaths");
    int assists = prs.getInt("assists");
    int killstreak = getIntOrNull(prs, "killstreak") != null ? prs.getInt("killstreak") : 0;
    int maxKillstreak = prs.getInt("max_killstreak");
    int longestBowKill =
        getIntOrNull(prs, "longest_bow_kill") != null ? prs.getInt("longest_bow_kill") : 0;
    double bowDamage = prs.getDouble("bow_damage");
    double bowDamageTaken = prs.getDouble("bow_damage_taken");
    int shotsTaken = prs.getInt("shots_taken");
    int shotsHit = prs.getInt("shots_hit");
    double damageDone = prs.getDouble("damage_done");
    double damageTaken = prs.getDouble("damage_taken");
    int destroyablePiecesBroken = getIntOrNull(prs, "destroyable_pieces_broken") != null
        ? prs.getInt("destroyable_pieces_broken")
        : 0;
    int monumentsDestroyed =
        getIntOrNull(prs, "monuments_destroyed") != null ? prs.getInt("monuments_destroyed") : 0;
    int flagsCaptured =
        getIntOrNull(prs, "flags_captured") != null ? prs.getInt("flags_captured") : 0;
    int flagPickups = getIntOrNull(prs, "flag_pickups") != null ? prs.getInt("flag_pickups") : 0;
    int coresLeaked = getIntOrNull(prs, "cores_leaked") != null ? prs.getInt("cores_leaked") : 0;
    int woolsCaptured =
        getIntOrNull(prs, "wools_captured") != null ? prs.getInt("wools_captured") : 0;
    int woolsTouched = getIntOrNull(prs, "wools_touched") != null ? prs.getInt("wools_touched") : 0;
    long longestFlagHoldMillis = getIntOrNull(prs, "longest_flag_hold_millis") != null
        ? prs.getLong("longest_flag_hold_millis")
        : 0L;
    int totalPoints = prs.getInt("points");

    return new MatchStats(
        username,
        kills,
        deaths,
        assists,
        killstreak,
        maxKillstreak,
        longestBowKill,
        bowDamage,
        bowDamageTaken,
        shotsTaken,
        shotsHit,
        damageDone,
        damageTaken,
        destroyablePiecesBroken,
        monumentsDestroyed,
        flagsCaptured,
        flagPickups,
        coresLeaked,
        woolsCaptured,
        woolsTouched,
        longestFlagHoldMillis,
        totalPoints,
        teamName);
  }

  /**
   * Verifica si las columnas extendidas existen en la tabla match_players_history.
   *
   * @param conn Conexión a la base de datos
   * @return true si las columnas extendidas existen, false en caso contrario
   */
  private boolean checkIfExtendedColumnsExist(Connection conn) {
    try {
      // Intentar consultar una de las columnas nuevas
      String testQuery = "SELECT longest_bow_kill FROM match_players_history LIMIT 1";
      try (PreparedStatement ps = conn.prepareStatement(testQuery)) {
        ps.executeQuery();
        return true; // Si la consulta funciona, las columnas existen
      }
    } catch (SQLException e) {
      // Si falla, las columnas no existen
      return false;
    }
  }
}
