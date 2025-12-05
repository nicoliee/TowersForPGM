package org.nicolie.towersforpgm.database.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.tables.TableInfo;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.database.models.top.Top;
import org.nicolie.towersforpgm.database.models.top.TopResult;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;
import org.nicolie.towersforpgm.utils.LanguageManager;

public class SQLStatsManager {
  private static final TowersForPGM plugin = TowersForPGM.getInstance();

  public static void updateStats(
      String table, List<Stats> playerStatsList, List<PlayerEloChange> eloChange)
      throws SQLException {
    updateStatsWithRetry(table, playerStatsList, eloChange, false);
  }

  private static void updateStatsWithRetry(
      String table, List<Stats> playerStatsList, List<PlayerEloChange> eloChange, boolean isRetry)
      throws SQLException {
    TableInfo tableInfo = plugin.config().databaseTables().getTableInfo(table);
    if (playerStatsList.isEmpty() || table == null || table.isEmpty() || (tableInfo == null)) {
      return;
    }

    String sql = "INSERT INTO " + table
        + " (username, kills, deaths, assists, damageDone, damageTaken, points, wins, games, winstreak, maxWinstreak) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0) "
        + "ON DUPLICATE KEY UPDATE "
        + "kills = kills + VALUES(kills), "
        + "deaths = deaths + VALUES(deaths), "
        + "assists = assists + VALUES(assists), "
        + "damageDone = damageDone + VALUES(damageDone), "
        + "damageTaken = damageTaken + VALUES(damageTaken), "
        + "points = points + VALUES(points), "
        + "wins = wins + VALUES(wins), "
        + "games = games + VALUES(games), "
        + "winstreak = IF(VALUES(winstreak) = 1, winstreak + 1, 0), "
        + "maxWinstreak = GREATEST(maxWinstreak, winstreak)";

    String rankedSql =
        "UPDATE " + table + " SET elo = ?, lastElo = ?, maxElo = ? WHERE username = ?";

    java.util.logging.Logger logger = TowersForPGM.getInstance().getLogger();
    TowersForPGM plugin = TowersForPGM.getInstance();

    SQLDatabaseManager dbManager = plugin.getMySQLDatabaseManager();
    if (dbManager == null || !dbManager.isConnected()) {
      String errorMessage = LanguageManager.message("errors.database.connectionClosed");
      logger.log(Level.SEVERE, errorMessage);
      return;
    }

    long startTime = System.currentTimeMillis();

    // Log del SQL y datos de jugadores
    logger.info("[SQLStatsManager] Ejecutando updateStats para tabla: " + table);
    logger.info("[SQLStatsManager] SQL a ejecutar: " + sql);
    StringBuilder playersInfo = new StringBuilder();
    playersInfo
        .append("[SQLStatsManager] Datos de jugadores a actualizar (")
        .append(playerStatsList.size())
        .append(" jugadores):");
    for (Stats playerStat : playerStatsList) {
      playersInfo
          .append("\n  - ")
          .append(playerStat.getUsername())
          .append(": K=")
          .append(playerStat.getKills())
          .append(", D=")
          .append(playerStat.getDeaths())
          .append(", A=")
          .append(playerStat.getAssists())
          .append(", DD=")
          .append(playerStat.getDamageDone())
          .append(", DT=")
          .append(playerStat.getDamageTaken())
          .append(", P=")
          .append(playerStat.getPoints())
          .append(", W=")
          .append(playerStat.getWins())
          .append(", G=")
          .append(playerStat.getGames())
          .append(", WS=")
          .append(playerStat.getWinstreak());
    }
    logger.info(playersInfo.toString());

    try (Connection conn = dbManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      conn.setAutoCommit(false);
      stmt.setQueryTimeout(plugin.getMySQLDatabaseManager().TIMEOUT / 1000);
      int batchSize = 0;
      for (Stats playerStat : playerStatsList) {
        stmt.setString(1, playerStat.getUsername());
        stmt.setInt(2, playerStat.getKills());
        stmt.setInt(3, playerStat.getDeaths());
        stmt.setInt(4, playerStat.getAssists());
        stmt.setDouble(5, playerStat.getDamageDone());
        stmt.setDouble(6, playerStat.getDamageTaken());
        stmt.setInt(7, playerStat.getPoints());
        stmt.setInt(8, playerStat.getWins());
        stmt.setInt(9, playerStat.getGames());
        stmt.setInt(10, playerStat.getWinstreak());
        stmt.addBatch();
        batchSize++;
        if (batchSize % 100 == 0) {
          stmt.executeBatch();
        }
      }
      stmt.executeBatch();
      conn.commit();

      if (eloChange != null && !eloChange.isEmpty()) {
        logger.info("[SQLStatsManager] SQL para ELO a ejecutar: " + rankedSql);
        StringBuilder eloInfo = new StringBuilder();
        eloInfo
            .append("[SQLStatsManager] Cambios de ELO a aplicar (")
            .append(eloChange.size())
            .append(" jugadores):");
        for (PlayerEloChange change : eloChange) {
          eloInfo
              .append("\n  - ")
              .append(change.getUsername())
              .append(": CurrentELO=")
              .append(change.getCurrentElo())
              .append(", NewELO=")
              .append(change.getNewElo())
              .append(", MaxELO=")
              .append(change.getMaxElo());
        }
        logger.info(eloInfo.toString());

        try (PreparedStatement rankedStmt = conn.prepareStatement(rankedSql)) {
          for (PlayerEloChange change : eloChange) {
            rankedStmt.setInt(1, change.getNewElo());
            rankedStmt.setInt(2, change.getCurrentElo());
            rankedStmt.setInt(3, change.getMaxElo());
            rankedStmt.setString(4, change.getUsername());
            rankedStmt.addBatch();
          }
          rankedStmt.executeBatch();
          conn.commit();
        }
      }

    } catch (SQLException e) {
      long duration = System.currentTimeMillis() - startTime;

      if ("MySQL".equals(plugin.getCurrentDatabaseType())
          && duration >= plugin.getMySQLDatabaseManager().TIMEOUT
          && !isRetry) {
        logger.warning("Timeout detectado en updateStats para tabla " + table + " (duración: "
            + duration + "ms). Intentando recargar base de datos y reintentar...");

        if (plugin.reloadDatabase()) {
          updateStatsWithRetry(table, playerStatsList, eloChange, true); // Retry con flag
          return;
        } else {
          logger.severe(
              "No se pudo recargar la base de datos para retry de updateStats en tabla " + table);
        }
      }

      if (isRetry) {
        logger.severe("No se pudieron subir las estadísticas para tabla " + table
            + " después del retry. Operación fallida definitivamente.");
      }

      // Re-lanzar excepción para que StatsManager pueda manejar el logging
      throw e;
    }
  }

  public static Stats getStats(String table, String username) {
    // Validación básica de parámetros
    if (table == null || table.isEmpty() || username == null || username.isEmpty()) {
      return null;
    }
    TableInfo tableInfo = plugin.config().databaseTables().getTableInfo(table);
    // La tabla es válida si está en cualquiera de las listas configuradas
    boolean validTable = tableInfo != null || MatchBotConfig.getTables().contains(table);
    if (!validTable) {
      return null;
    }

    String sql = "SELECT * FROM `" + table + "` WHERE username = ?";

    try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, username);
      stmt.setQueryTimeout(2); // Reducir timeout drásticamente
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          int kills = hasColumn(rs, "kills") ? rs.getInt("kills") : -9999;
          int deaths = hasColumn(rs, "deaths") ? rs.getInt("deaths") : -9999;
          int assists = hasColumn(rs, "assists") ? rs.getInt("assists") : -9999;
          double damageDone = hasColumn(rs, "damageDone") ? rs.getDouble("damageDone") : -9999.0;
          double damageTaken = hasColumn(rs, "damageTaken") ? rs.getDouble("damageTaken") : -9999.0;
          int points = hasColumn(rs, "points") ? rs.getInt("points") : -9999;
          int wins = hasColumn(rs, "wins") ? rs.getInt("wins") : -9999;
          int games = hasColumn(rs, "games") ? rs.getInt("games") : -9999;
          int winstreak = hasColumn(rs, "winstreak") ? rs.getInt("winstreak") : -9999;
          int maxWinstreak = hasColumn(rs, "maxWinstreak") ? rs.getInt("maxWinstreak") : -9999;
          int elo = hasColumn(rs, "elo") ? rs.getInt("elo") : -9999;
          int lastElo = hasColumn(rs, "lastElo") ? rs.getInt("lastElo") : -9999;
          int maxElo = hasColumn(rs, "maxElo") ? rs.getInt("maxElo") : -9999;

          return new Stats(
              rs.getString("username"),
              kills,
              deaths,
              assists,
              damageDone,
              damageTaken,
              0,
              points,
              wins,
              games,
              winstreak,
              maxWinstreak,
              elo,
              lastElo,
              maxElo);
        } else {
          return null;
        }
      }
    } catch (SQLException e) {
      TowersForPGM.getInstance();
      String errorMessage = LanguageManager.message("errors.database.getStats");
      TowersForPGM.getInstance().getLogger().log(Level.SEVERE, errorMessage, e);
      return null;
    }
  }

  public static TopResult getTop(String table, String dbColumn, int limit, int page) {
    // Validación básica de parámetros
    if (table == null
        || table.isEmpty()
        || dbColumn == null
        || dbColumn.isEmpty()
        || limit <= 0
        || page < 1) {
      return new TopResult(new ArrayList<>(), 0);
    }
    TableInfo tableInfo = plugin.config().databaseTables().getTableInfo(table);
    boolean validTable = tableInfo != null || MatchBotConfig.getTables().contains(table);
    if (!validTable) return new TopResult(new ArrayList<>(), 0);

    int offset = (page - 1) * limit;

    // Optimizar: Separar consulta de datos y de count total
    String dataSql = "SELECT username, `" + dbColumn + "` AS valuePerGame FROM `" + table
        + "` ORDER BY `" + dbColumn + "` DESC LIMIT ? OFFSET ?";

    // Solo obtener count si es la primera página
    String countSql = "SELECT COUNT(*) as total FROM `" + table + "`";

    List<Top> topList = new ArrayList<>();
    int totalRecords = 0;
    SQLDatabaseManager dbManager = TowersForPGM.getInstance().getMySQLDatabaseManager();
    if (dbManager == null || !dbManager.isConnected()) {
      String errorMessage = LanguageManager.message("errors.database.connectionClosed");
      TowersForPGM.getInstance().getLogger().log(Level.SEVERE, errorMessage);
      return new TopResult(new ArrayList<>(), 0);
    }

    try (Connection conn = dbManager.getConnection()) {
      // Obtener datos principales (siempre)
      try (PreparedStatement stmt = conn.prepareStatement(dataSql)) {
        stmt.setInt(1, limit);
        stmt.setInt(2, offset);
        stmt.setQueryTimeout(2); // Timeout agresivo

        try (ResultSet rs = stmt.executeQuery()) {
          while (rs.next()) {
            String username = rs.getString("username");
            double value = rs.getDouble("valuePerGame");
            topList.add(new Top(username, value));
          }
        }
      }

      // Solo obtener count total si es la primera página
      if (page == 1) {
        try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
          countStmt.setQueryTimeout(2);
          try (ResultSet rs = countStmt.executeQuery()) {
            if (rs.next()) {
              totalRecords = rs.getInt("total");
            }
          }
        }
      }

    } catch (SQLException e) {
      // Si la columna no existe, retornar resultado vacío en lugar de error
      if (e.getMessage() != null && e.getMessage().contains("Unknown column")) {
        return new TopResult(new ArrayList<>(), 0);
      }
      TowersForPGM.getInstance().getLogger().log(Level.SEVERE, "Error en getTop optimizado", e);
    }
    return new TopResult(topList, totalRecords);
  }

  public static TopResult getTop(
      String table,
      String dbColumn,
      int limit,
      Double lastValue,
      String lastUser,
      Integer totalRecords) {
    if (table == null || table.isEmpty() || dbColumn == null || dbColumn.isEmpty() || limit <= 0) {
      return new TopResult(new ArrayList<>(), 0);
    }
    TableInfo tableInfo = plugin.config().databaseTables().getTableInfo(table);
    boolean validTable = tableInfo != null || MatchBotConfig.getTables().contains(table);
    if (!validTable) return new TopResult(new ArrayList<>(), 0);

    String sql;
    if (lastValue == null) {
      // Primera página - incluir COUNT(*) OVER() para obtener el total
      sql = "SELECT username, `" + dbColumn
          + "` AS valuePerGame, COUNT(*) OVER() AS totalCount FROM `" + table + "` ORDER BY `"
          + dbColumn + "` DESC, `username` ASC LIMIT ?";
    } else {
      // Páginas siguientes - usar keyset pagination
      if (totalRecords != null) {
        // Si ya conocemos el total, no necesitamos calcularlo otra vez
        sql = "SELECT username, `" + dbColumn
            + "` AS valuePerGame FROM `" + table + "` WHERE (`" + dbColumn
            + "` < ? OR (`" + dbColumn + "` = ? AND `username` > ?)) ORDER BY `" + dbColumn
            + "` DESC, `username` ASC LIMIT ?";
      } else {
        // Si no conocemos el total, lo incluimos en la consulta
        sql = "SELECT username, `" + dbColumn
            + "` AS valuePerGame, COUNT(*) OVER() AS totalCount FROM `" + table + "` WHERE (`"
            + dbColumn
            + "` < ? OR (`" + dbColumn + "` = ? AND `username` > ?)) ORDER BY `" + dbColumn
            + "` DESC, `username` ASC LIMIT ?";
      }
    }

    final Double anchorValue = lastValue;
    final String anchorUser = lastUser;
    final Integer knownTotal = totalRecords;

    List<Top> list = new ArrayList<>();
    int total = knownTotal != null ? knownTotal : 0;
    SQLDatabaseManager dbManager = TowersForPGM.getInstance().getMySQLDatabaseManager();
    if (dbManager == null || !dbManager.isConnected()) {
      String errorMessage = LanguageManager.message("errors.database.connectionClosed");
      TowersForPGM.getInstance().getLogger().log(Level.SEVERE, errorMessage);
      return new TopResult(new ArrayList<>(), 0);
    }
    try (Connection conn = dbManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      int idx = 1;
      if (anchorValue != null) {
        stmt.setDouble(idx++, anchorValue);
        stmt.setDouble(idx++, anchorValue);
        stmt.setString(idx++, anchorUser);
      }
      stmt.setInt(idx, limit);
      stmt.setQueryTimeout(2); // Timeout agresivo

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          String username = rs.getString("username");
          double value = rs.getDouble("valuePerGame");
          list.add(new Top(username, value));

          // Solo obtener el total de la primera fila si no lo conocíamos
          if (total == 0 && hasColumn(rs, "totalCount")) {
            total = rs.getInt("totalCount");
          }
        }
      }
    } catch (SQLException e) {
      // Si la columna no existe, retornar resultado vacío en lugar de error
      if (e.getMessage() != null && e.getMessage().contains("Unknown column")) {
        return new TopResult(new ArrayList<>(), 0);
      }
      TowersForPGM.getInstance()
          .getLogger()
          .log(Level.SEVERE, "Error obteniendo top con keyset pagination", e);
    }
    return new TopResult(list, total);
  }

  // Helper para verificar si la columna existe en el ResultSet sin lanzar excepción
  private static boolean hasColumn(ResultSet rs, String column) {
    try {
      rs.findColumn(column);
      return true;
    } catch (SQLException e) {
      return false;
    }
  }

  public static List<PlayerEloChange> getEloForUsernames(String table, List<String> usernames)
      throws SQLException {
    return getEloForUsernamesWithRetry(table, usernames, false);
  }

  private static List<PlayerEloChange> getEloForUsernamesWithRetry(
      String table, List<String> usernames, boolean isRetry) throws SQLException {
    if (usernames == null || usernames.isEmpty()) {
      return Collections.emptyList();
    }

    String placeholders = usernames.stream().map(u -> "?").collect(Collectors.joining(","));
    String sql =
        "SELECT username, elo, maxElo FROM " + table + " WHERE username IN (" + placeholders + ")";

    java.util.logging.Logger logger = TowersForPGM.getInstance().getLogger();
    TowersForPGM plugin = TowersForPGM.getInstance();
    long startTime = System.currentTimeMillis();

    try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      for (int i = 0; i < usernames.size(); i++) {
        stmt.setString(i + 1, usernames.get(i));
      }
      stmt.setQueryTimeout(plugin.getMySQLDatabaseManager().TIMEOUT / 1000);
      try (ResultSet rs = stmt.executeQuery()) {
        List<org.nicolie.towersforpgm.rankeds.PlayerEloChange> result = new java.util.ArrayList<>();
        java.util.Set<String> foundUsernames = new java.util.HashSet<>();
        while (rs.next()) {
          String username = rs.getString("username");
          int elo = rs.getInt("elo");
          int maxElo = rs.getInt("maxElo");
          result.add(
              new org.nicolie.towersforpgm.rankeds.PlayerEloChange(username, elo, 0, 0, maxElo));
          foundUsernames.add(username);
        }

        for (String username : usernames) {
          if (!foundUsernames.contains(username)) {
            result.add(new org.nicolie.towersforpgm.rankeds.PlayerEloChange(username, 0, 0, 0, 0));
          }
        }

        // Mantener el orden de entrada
        List<org.nicolie.towersforpgm.rankeds.PlayerEloChange> orderedResult =
            new java.util.ArrayList<>();
        for (String username : usernames) {
          for (org.nicolie.towersforpgm.rankeds.PlayerEloChange change : result) {
            if (change.getUsername().equals(username)) {
              orderedResult.add(change);
              break;
            }
          }
        }

        return orderedResult;
      } catch (SQLException e) {
        long duration = System.currentTimeMillis() - startTime;

        if ("MySQL".equals(plugin.getCurrentDatabaseType())
            && duration >= plugin.getMySQLDatabaseManager().TIMEOUT
            && !isRetry) {
          logger.warning("Timeout detectado en getEloForUsernames para tabla " + table
              + " Intentando recargar base de datos y reintentar...");

          if (plugin.reloadDatabase()) {
            return getEloForUsernamesWithRetry(table, usernames, true); // Retry con flag
          } else {
            logger.severe(
                "No se pudo recargar la base de datos para retry de getEloForUsernames en tabla "
                    + table);
          }
        }

        if (isRetry) {
          logger.severe("No se pudieron obtener los elos para tabla " + table
              + " después del retry. Operación fallida definitivamente.");
        }

        // Re-lanzar excepción para que StatsManager pueda manejar el logging
        throw e;
      }
    } catch (SQLException e) {
      long duration = System.currentTimeMillis() - startTime;

      if ("MySQL".equals(plugin.getCurrentDatabaseType())
          && duration >= plugin.getMySQLDatabaseManager().TIMEOUT
          && !isRetry) {
        logger.warning("Timeout detectado en getEloForUsernames para tabla " + table
            + " (duración: " + duration + "ms). Intentando recargar base de datos y reintentar...");

        if (plugin.reloadDatabase()) {
          return getEloForUsernamesWithRetry(table, usernames, true); // Retry con flag
        } else {
          logger.severe(
              "No se pudo recargar la base de datos para retry de getEloForUsernames en tabla "
                  + table);
        }
      }

      if (isRetry) {
        logger.severe("No se pudieron obtener los elos para tabla " + table
            + " después del retry. Operación fallida definitivamente.");
      }

      // Re-lanzar excepción para que StatsManager pueda manejar el logging
      throw e;
    }
  }

  public static List<String> getAllUsernamesFiltered(String filter) {
    List<String> tables = MatchBotConfig.getTables();
    if (tables.isEmpty()) {
      return new java.util.ArrayList<>();
    }

    // Construir una consulta UNION ALL para todas las tablas con filtro
    StringBuilder sqlBuilder = new StringBuilder();
    sqlBuilder.append("SELECT DISTINCT username FROM (");

    for (int i = 0; i < tables.size(); i++) {
      if (i > 0) {
        sqlBuilder.append(" UNION ALL ");
      }
      sqlBuilder
          .append("SELECT username FROM ")
          .append(tables.get(i))
          .append(" WHERE username LIKE ?");
    }

    sqlBuilder.append(") AS combined_tables ORDER BY username LIMIT 25");

    java.util.Set<String> usernameSet = new java.util.LinkedHashSet<>();

    try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection();
        PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {
      stmt.setQueryTimeout(2); // Reducir timeout

      // Establecer el parámetro para cada tabla en la consulta UNION
      String filterPattern = filter + "%";
      for (int i = 1; i <= tables.size(); i++) {
        stmt.setString(i, filterPattern);
      }

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next() && usernameSet.size() < 25) {
          usernameSet.add(rs.getString("username"));
        }
      }
    } catch (SQLException e) {
      TowersForPGM.getInstance()
          .getLogger()
          .log(Level.WARNING, "Error al obtener usernames filtrados de las tablas", e);
    }

    return new java.util.ArrayList<>(usernameSet);
  }

  public static void applySanction(
      String table,
      String username,
      int gamesToAdd,
      org.nicolie.towersforpgm.rankeds.PlayerEloChange elo) {
    TableInfo tableInfo = plugin.config().databaseTables().getTableInfo(table);
    if (table == null || table.isEmpty() || username == null || username.isEmpty()) return;
    if (tableInfo == null) return;

    String upsertSql = "INSERT INTO " + table
        + " (username, games, elo, lastElo, maxElo) VALUES (?, ?, ?, ?, ?) "
        + "ON DUPLICATE KEY UPDATE games = games + VALUES(games), elo = ?, lastElo = ?, maxElo = GREATEST(maxElo, VALUES(maxElo))";

    SQLDatabaseManager dbManager = TowersForPGM.getInstance().getMySQLDatabaseManager();
    if (dbManager == null || !dbManager.isConnected()) {
      return;
    }

    try (Connection conn = dbManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(upsertSql)) {
      int newElo = (elo != null) ? elo.getNewElo() : 0;
      int lastElo = (elo != null) ? elo.getCurrentElo() : 0;
      int maxElo = (elo != null) ? Math.max(elo.getMaxElo(), newElo) : 0;

      stmt.setString(1, username);
      stmt.setInt(2, Math.max(0, gamesToAdd));
      stmt.setInt(3, newElo);
      stmt.setInt(4, lastElo);
      stmt.setInt(5, maxElo);
      stmt.setInt(6, newElo);
      stmt.setInt(7, lastElo);
      stmt.executeUpdate();
    } catch (SQLException e) {
      // log quietly; sanction is best-effort
      TowersForPGM.getInstance()
          .getLogger()
          .warning("Error applying sanction (MySQL): " + e.getMessage());
    }
  }
}
