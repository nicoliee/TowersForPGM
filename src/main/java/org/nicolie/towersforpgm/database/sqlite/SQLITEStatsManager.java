package org.nicolie.towersforpgm.database.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.tables.TableInfo;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.database.models.top.Top;
import org.nicolie.towersforpgm.database.models.top.TopResult;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;

public class SQLITEStatsManager {
  private static final TowersForPGM plugin = TowersForPGM.getInstance();

  public static void updateStats(
      String table, List<Stats> playerStatsList, List<PlayerEloChange> eloChangeList) {
    TableInfo tableInfo = plugin.config().databaseTables().getTableInfo(table);
    if (playerStatsList.isEmpty() || table == null || table.isEmpty() || tableInfo == null) {
      return;
    }

    boolean isRanked = tableInfo != null && tableInfo.isRanked();

    try (Connection conn = plugin.getDatabaseConnection()) {
      // PASO 1: Leer todos los datos actuales ANTES de abrir la transacción de escritura
      java.util.Map<String, Stats> existingStats = new java.util.HashMap<>();

      try (PreparedStatement ps =
          conn.prepareStatement("SELECT * FROM " + table + " WHERE username = ?")) {
        for (Stats stat : playerStatsList) {
          ps.setString(1, stat.getUsername());
          try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
              Stats oldStat = new Stats(
                  rs.getString("username"),
                  rs.getInt("kills"),
                  rs.getInt("maxKills"),
                  rs.getInt("deaths"),
                  rs.getInt("assists"),
                  rs.getDouble("damageDone"),
                  rs.getDouble("damageTaken"),
                  rs.getInt("points"),
                  rs.getInt("maxPoints"),
                  rs.getInt("wins"),
                  rs.getInt("games"),
                  rs.getInt("winstreak"),
                  rs.getInt("maxWinstreak"),
                  0,
                  0,
                  0);
              existingStats.put(stat.getUsername(), oldStat);
            }
          }
        }
      }

      // PASO 2: Abrir transacción de escritura
      conn.setAutoCommit(false);

      // Columnas básicas + winstreaks + ratios
      List<String> columns = new ArrayList<>(Arrays.asList(
          "username",
          "kills",
          "maxKills",
          "deaths",
          "assists",
          "damageDone",
          "damageTaken",
          "points",
          "maxPoints",
          "games",
          "wins",
          "winstreak",
          "maxWinstreak",
          "killsPerGame",
          "deathsPerGame",
          "assistsPerGame",
          "damageDonePerGame",
          "damageTakenPerGame",
          "pointsPerGame",
          "kdRatio",
          "winrate",
          "wlRatio"));

      if (isRanked) {
        columns.addAll(Arrays.asList("elo", "lastElo", "maxElo"));
      }

      // Generar placeholders
      String placeholders = String.join(",", Collections.nCopies(columns.size(), "?"));

      String sql = "INSERT OR REPLACE INTO " + table + " (" + String.join(",", columns)
          + ") VALUES (" + placeholders + ")";

      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        int batchSize = 0;
        for (Stats stat : playerStatsList) {

          // Obtener valores antiguos del mapa precargado
          Stats oldStat = existingStats.get(stat.getUsername());
          int oldKills = 0,
              oldMaxKills = 0,
              oldDeaths = 0,
              oldAssists = 0,
              oldPoints = 0,
              oldMaxPoints = 0,
              oldGames = 0,
              oldWins = 0;
          double oldDamageDone = 0, oldDamageTaken = 0;
          int oldWinstreak = 0, oldMaxWinstreak = 0;
          boolean exists = false;

          if (oldStat != null) {
            exists = true;
            oldKills = oldStat.getKills();
            oldMaxKills = oldStat.getMaxKills();
            oldDeaths = oldStat.getDeaths();
            oldAssists = oldStat.getAssists();
            oldPoints = oldStat.getPoints();
            oldMaxPoints = oldStat.getMaxPoints();
            oldGames = oldStat.getGames();
            oldWins = oldStat.getWins();
            oldDamageDone = oldStat.getDamageDone();
            oldDamageTaken = oldStat.getDamageTaken();
            oldWinstreak = oldStat.getWinstreak();
            oldMaxWinstreak = oldStat.getMaxWinstreak();
          }

          // Sumar estadísticas
          int kills = stat.getKills() + oldKills;
          int maxKills =
              Math.max(stat.getKills(), oldMaxKills); // Delta de esta partida vs histórico
          int deaths = stat.getDeaths() + oldDeaths;
          int assists = stat.getAssists() + oldAssists;
          int points = stat.getPoints() + oldPoints;
          int maxPoints =
              Math.max(stat.getPoints(), oldMaxPoints); // Delta de esta partida vs histórico
          int games = stat.getGames() + oldGames;
          int wins = stat.getWins() + oldWins;
          double damageDone = stat.getDamageDone() + oldDamageDone;
          double damageTaken = stat.getDamageTaken() + oldDamageTaken;

          // Calcular winstreak y maxWinstreak
          int winsDiff = stat.getWins();
          if (exists) winsDiff = wins - oldWins;

          int winstreak =
              (exists && winsDiff > 0) ? oldWinstreak + winsDiff : (winsDiff > 0 ? winsDiff : 0);
          int maxWinstreak = Math.max(oldMaxWinstreak, winstreak);

          // Calcular stats derivados
          double killsPerGame = games > 0 ? (double) kills / games : 0;
          double deathsPerGame = games > 0 ? (double) deaths / games : 0;
          double assistsPerGame = games > 0 ? (double) assists / games : 0;
          double damageDonePerGame = games > 0 ? damageDone / games : 0;
          double damageTakenPerGame = games > 0 ? damageTaken / games : 0;
          double pointsPerGame = games > 0 ? (double) points / games : 0;
          double kdRatio = deaths > 0 ? (double) kills / deaths : kills;
          double winrate = games > 0 ? (double) wins * 100 / games : 0;
          int losses = games - wins;
          double wlRatio = losses > 0 ? (double) wins / losses : wins;

          // Preparar statement
          int idx = 1;
          stmt.setString(idx++, stat.getUsername());
          stmt.setInt(idx++, kills);
          stmt.setInt(idx++, maxKills);
          stmt.setInt(idx++, deaths);
          stmt.setInt(idx++, assists);
          stmt.setDouble(idx++, damageDone);
          stmt.setDouble(idx++, damageTaken);
          stmt.setInt(idx++, points);
          stmt.setInt(idx++, maxPoints);
          stmt.setInt(idx++, games);
          stmt.setInt(idx++, wins);
          stmt.setInt(idx++, winstreak);
          stmt.setInt(idx++, maxWinstreak);
          stmt.setDouble(idx++, killsPerGame);
          stmt.setDouble(idx++, deathsPerGame);
          stmt.setDouble(idx++, assistsPerGame);
          stmt.setDouble(idx++, damageDonePerGame);
          stmt.setDouble(idx++, damageTakenPerGame);
          stmt.setDouble(idx++, pointsPerGame);
          stmt.setDouble(idx++, kdRatio);
          stmt.setDouble(idx++, winrate);
          stmt.setDouble(idx++, wlRatio);

          if (isRanked) {
            PlayerEloChange elo = eloChangeList.stream()
                .filter(e -> e.getUsername().equals(stat.getUsername()))
                .findFirst()
                .orElse(new PlayerEloChange(stat.getUsername(), 0, 0, 0, 0));
            stmt.setInt(idx++, elo.getNewElo());
            stmt.setInt(idx++, elo.getCurrentElo());
            stmt.setInt(idx++, Math.max(elo.getMaxElo(), elo.getNewElo()));
          }

          stmt.addBatch();
          batchSize++;

          // Ejecutar en batches de 50 jugadores
          if (batchSize % 50 == 0) {
            stmt.executeBatch();
            conn.commit();
          }
        }

        // Ejecutar el último batch si quedan elementos
        if (batchSize % 50 != 0) {
          stmt.executeBatch();
        }
      }

      conn.commit();
    } catch (SQLException e) {
      TowersForPGM.getInstance()
          .getLogger()
          .log(Level.SEVERE, "Error al actualizar estadísticas en SQLite", e);
    }
  }

  public static Stats getStats(String table, String username) {
    // Validación básica de parámetros
    if (table == null || table.isEmpty() || username == null || username.isEmpty()) {
      TowersForPGM.getInstance()
          .getLogger()
          .log(
              Level.WARNING,
              "[StatsManager SQLite] Parámetros inválidos getStats: table=" + table + ", username="
                  + username);
      return null;
    }

    // La tabla es válida si está en cualquiera de las listas configuradas
    TableInfo tableInfo = plugin.config().databaseTables().getTableInfo(table);
    boolean validTable = tableInfo != null || MatchBotConfig.getTables().contains(table);
    if (!validTable) {
      TowersForPGM.getInstance()
          .getLogger()
          .log(
              Level.WARNING,
              "[StatsManager SQLite] Tabla no registrada en config getStats: " + table);
      return null;
    }

    String sql = "SELECT * FROM `" + table + "` WHERE username = ?";

    try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, username);
      stmt.setQueryTimeout(2); // Optimizar timeout
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          int kills = hasColumn(rs, "kills") ? rs.getInt("kills") : -9999;
          int maxKills = hasColumn(rs, "maxKills") ? rs.getInt("maxKills") : -9999;
          int deaths = hasColumn(rs, "deaths") ? rs.getInt("deaths") : -9999;
          int assists = hasColumn(rs, "assists") ? rs.getInt("assists") : -9999;
          double damageDone = hasColumn(rs, "damageDone") ? rs.getDouble("damageDone") : -9999.0;
          double damageTaken = hasColumn(rs, "damageTaken") ? rs.getDouble("damageTaken") : -9999.0;
          int points = hasColumn(rs, "points") ? rs.getInt("points") : -9999;
          int maxPoints = hasColumn(rs, "maxPoints") ? rs.getInt("maxPoints") : -9999;
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
              maxKills,
              deaths,
              assists,
              damageDone,
              damageTaken,
              points,
              maxPoints,
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
      TowersForPGM.getInstance()
          .getLogger()
          .log(Level.SEVERE, "Error al obtener estadísticas del usuario en SQLite", e);
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

    // SQLite usa subconsulta para obtener el total de registros
    String sql = "SELECT username, `" + dbColumn + "` AS valuePerGame, "
        + "(SELECT COUNT(*) FROM `" + table + "`) AS totalCount "
        + "FROM `" + table + "` ORDER BY `" + dbColumn + "` DESC LIMIT ? OFFSET ?";

    List<Top> topList = new ArrayList<>();
    int totalRecords = 0;
    try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setInt(1, limit);
      stmt.setInt(2, offset);
      stmt.setQueryTimeout(2); // Optimizar timeout

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          String username = rs.getString("username");
          double value = rs.getDouble("valuePerGame");
          if (totalRecords == 0) {
            totalRecords = rs.getInt("totalCount");
          }
          topList.add(new Top(username, value));
        }
      }

    } catch (SQLException e) {
      // Si la columna no existe, retornar resultado vacío en lugar de error
      if (e.getMessage() != null
          && (e.getMessage().contains("no such column")
              || e.getMessage().contains("Unknown column"))) {
        return new TopResult(new ArrayList<>(), 0);
      }
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
      // Primera página - incluir subconsulta para obtener el total en SQLite
      sql = "SELECT username, `" + dbColumn
          + "` AS valuePerGame, (SELECT COUNT(*) FROM `" + table + "`) AS totalCount FROM `" + table
          + "` ORDER BY `"
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
        // Si no conocemos el total, lo incluimos en la consulta con subconsulta
        sql = "SELECT username, `" + dbColumn
            + "` AS valuePerGame, (SELECT COUNT(*) FROM `" + table + "`) AS totalCount FROM `"
            + table + "` WHERE (`"
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
    try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      int idx = 1;
      if (anchorValue != null) {
        stmt.setDouble(idx++, anchorValue);
        stmt.setDouble(idx++, anchorValue);
        stmt.setString(idx++, anchorUser);
      }
      stmt.setInt(idx, limit);
      stmt.setQueryTimeout(2); // Optimizar timeout

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
      if (e.getMessage() != null
          && (e.getMessage().contains("no such column")
              || e.getMessage().contains("Unknown column"))) {
        return new TopResult(new ArrayList<>(), 0);
      }
      TowersForPGM.getInstance()
          .getLogger()
          .log(Level.SEVERE, "Error obteniendo top con keyset pagination", e);
    }
    return new TopResult(list, total);
  }

  // Método helper para verificar si una columna existe en el ResultSet
  private static boolean hasColumn(ResultSet rs, String columnName) {
    try {
      rs.findColumn(columnName);
      return true;
    } catch (SQLException e) {
      return false;
    }
  }

  public static List<PlayerEloChange> getEloForUsernames(String table, List<String> usernames) {
    if (usernames == null || usernames.isEmpty()) {
      TowersForPGM.getInstance()
          .getLogger()
          .info("getEloForUsernames: lista de usernames vacía o nula");
      return java.util.Collections.emptyList();
    }

    String placeholders = String.join(",", java.util.Collections.nCopies(usernames.size(), "?"));
    String sql =
        "SELECT username, elo, maxElo FROM " + table + " WHERE username IN (" + placeholders + ")";

    try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      for (int i = 0; i < usernames.size(); i++) {
        stmt.setString(i + 1, usernames.get(i));
      }
      stmt.setQueryTimeout(2); // Optimizar timeout
      try (ResultSet rs = stmt.executeQuery()) {
        List<PlayerEloChange> result = new java.util.ArrayList<>();
        java.util.Set<String> foundUsernames = new java.util.HashSet<>();
        while (rs.next()) {
          String username = rs.getString("username");
          int elo = rs.getInt("elo");
          int maxElo = rs.getInt("maxElo");
          result.add(new PlayerEloChange(username, elo, 0, 0, maxElo));
          foundUsernames.add(username);
        }

        for (String username : usernames) {
          if (!foundUsernames.contains(username)) {
            result.add(new PlayerEloChange(username, 0, 0, 0, 0));
          }
        }

        // Mantener el orden de entrada
        List<PlayerEloChange> orderedResult = new java.util.ArrayList<>();
        for (String username : usernames) {
          for (PlayerEloChange change : result) {
            if (change.getUsername().equals(username)) {
              orderedResult.add(change);
              break;
            }
          }
        }
        return orderedResult;
      }
    } catch (SQLException e) {
      TowersForPGM.getInstance()
          .getLogger()
          .log(Level.SEVERE, "Error al obtener el elo de los usuarios", e);
      return java.util.Collections.emptyList();
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
      stmt.setQueryTimeout(2); // Optimizar timeout

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

    try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection()) {
      conn.setAutoCommit(false);

      int currentGames = 0;
      int currentElo = 0;
      int currentMaxElo = 0;
      boolean exists = false;
      try (PreparedStatement sel = conn.prepareStatement(
          "SELECT games, elo, maxElo FROM " + table + " WHERE username = ?")) {
        sel.setString(1, username);
        try (ResultSet rs = sel.executeQuery()) {
          if (rs.next()) {
            exists = true;
            currentGames = rs.getInt("games");
            currentElo = rs.getInt("elo");
            currentMaxElo = rs.getInt("maxElo");
          }
        }
      }

      int newGames = currentGames + Math.max(0, gamesToAdd);
      int newElo = (elo != null) ? elo.getNewElo() : currentElo;
      int lastElo = (elo != null) ? elo.getCurrentElo() : currentElo;
      int maxElo = Math.max(
          currentMaxElo, (elo != null) ? Math.max(elo.getMaxElo(), newElo) : currentMaxElo);

      String sql;
      if (exists) {
        sql = "UPDATE " + table
            + " SET games = ?, elo = ?, lastElo = ?, maxElo = ? WHERE username = ?";
      } else {
        sql = "INSERT INTO " + table
            + " (username, games, elo, lastElo, maxElo) VALUES (?, ?, ?, ?, ?)";
      }

      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        if (exists) {
          stmt.setInt(1, newGames);
          stmt.setInt(2, newElo);
          stmt.setInt(3, lastElo);
          stmt.setInt(4, maxElo);
          stmt.setString(5, username);
        } else {
          stmt.setString(1, username);
          stmt.setInt(2, newGames);
          stmt.setInt(3, newElo);
          stmt.setInt(4, lastElo);
          stmt.setInt(5, maxElo);
        }
        stmt.executeUpdate();
      }

      conn.commit();
    } catch (SQLException e) {
      TowersForPGM.getInstance()
          .getLogger()
          .warning("Error applying sanction (SQLite): " + e.getMessage());
    }
  }

  /**
   * Transfiere las estadísticas de una cuenta antigua a una cuenta nueva.
   *
   * <p>Este método: 1. Obtiene las estadísticas de ambas cuentas 2. Fusiona las estadísticas usando
   * StatsMerger (suma acumulables, máximos para records) 3. Actualiza la cuenta destino con las
   * estadísticas fusionadas 4. Elimina la cuenta origen de la base de datos
   *
   * @param table La tabla de estadísticas donde realizar la transferencia
   * @param oldAccount El nombre de usuario de la cuenta origen
   * @param newAccount El nombre de usuario de la cuenta destino
   * @return StatsTransferResult con el resultado de la operación
   */
  public static org.nicolie.towersforpgm.database.models.StatsTransferResult transferStats(
      String table, String oldAccount, String newAccount) {
    // Validaciones básicas
    if (table == null
        || table.isEmpty()
        || oldAccount == null
        || oldAccount.isEmpty()
        || newAccount == null
        || newAccount.isEmpty()) {
      return org.nicolie.towersforpgm.database.models.StatsTransferResult.failure(
          "Parámetros inválidos");
    }

    if (oldAccount.equalsIgnoreCase(newAccount)) {
      return org.nicolie.towersforpgm.database.models.StatsTransferResult.failure(
          "La cuenta origen y destino no pueden ser la misma");
    }

    TableInfo tableInfo = plugin.config().databaseTables().getTableInfo(table);
    boolean validTable = tableInfo != null || MatchBotConfig.getTables().contains(table);
    if (!validTable) {
      return org.nicolie.towersforpgm.database.models.StatsTransferResult.failure(
          "Tabla no válida: " + table);
    }

    try (Connection conn = plugin.getDatabaseConnection()) {
      // Paso 1: Obtener estadísticas de ambas cuentas
      Stats oldStats = getStats(table, oldAccount);
      if (oldStats == null) {
        return org.nicolie.towersforpgm.database.models.StatsTransferResult.failure(
            "La cuenta origen no existe en la tabla: " + oldAccount);
      }

      Stats newStats = getStats(table, newAccount);

      // Paso 2: Fusionar estadísticas
      Stats mergedStats = (newStats == null)
          ? org.nicolie.towersforpgm.database.models.StatsMerger.createFromOld(oldStats, newAccount)
          : org.nicolie.towersforpgm.database.models.StatsMerger.merge(oldStats, newStats);

      // Paso 3: Actualizar la cuenta destino en una transacción
      conn.setAutoCommit(false);

      try {
        // Preparar SQL de inserción/reemplazo
        String replaceSql = "INSERT OR REPLACE INTO " + table
            + " (username, kills, maxKills, deaths, assists, damageDone, damageTaken, "
            + "points, maxPoints, wins, games, winstreak, maxWinstreak, elo, lastElo, maxElo, "
            + "killsPerGame, deathsPerGame, assistsPerGame, damageDonePerGame, damageTakenPerGame, "
            + "pointsPerGame, kdRatio, winrate, wlRatio) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement updateStmt = conn.prepareStatement(replaceSql)) {
          // Calcular estadísticas derivadas
          int games = mergedStats.getGames();
          double killsPerGame = games > 0 ? (double) mergedStats.getKills() / games : 0;
          double deathsPerGame = games > 0 ? (double) mergedStats.getDeaths() / games : 0;
          double assistsPerGame = games > 0 ? (double) mergedStats.getAssists() / games : 0;
          double damageDonePerGame = games > 0 ? mergedStats.getDamageDone() / games : 0;
          double damageTakenPerGame = games > 0 ? mergedStats.getDamageTaken() / games : 0;
          double pointsPerGame = games > 0 ? (double) mergedStats.getPoints() / games : 0;
          double kdRatio = mergedStats.getDeaths() > 0
              ? (double) mergedStats.getKills() / mergedStats.getDeaths()
              : mergedStats.getKills();
          double winrate = games > 0 ? (double) mergedStats.getWins() * 100 / games : 0;
          int losses = games - mergedStats.getWins();
          double wlRatio =
              losses > 0 ? (double) mergedStats.getWins() / losses : mergedStats.getWins();

          updateStmt.setString(1, mergedStats.getUsername());
          updateStmt.setInt(2, mergedStats.getKills());
          updateStmt.setInt(3, mergedStats.getMaxKills());
          updateStmt.setInt(4, mergedStats.getDeaths());
          updateStmt.setInt(5, mergedStats.getAssists());
          updateStmt.setDouble(6, mergedStats.getDamageDone());
          updateStmt.setDouble(7, mergedStats.getDamageTaken());
          updateStmt.setInt(8, mergedStats.getPoints());
          updateStmt.setInt(9, mergedStats.getMaxPoints());
          updateStmt.setInt(10, mergedStats.getWins());
          updateStmt.setInt(11, mergedStats.getGames());
          updateStmt.setInt(12, mergedStats.getWinstreak());
          updateStmt.setInt(13, mergedStats.getMaxWinstreak());
          updateStmt.setInt(14, mergedStats.getElo());
          updateStmt.setInt(15, mergedStats.getLastElo());
          updateStmt.setInt(16, mergedStats.getMaxElo());
          updateStmt.setDouble(17, killsPerGame);
          updateStmt.setDouble(18, deathsPerGame);
          updateStmt.setDouble(19, assistsPerGame);
          updateStmt.setDouble(20, damageDonePerGame);
          updateStmt.setDouble(21, damageTakenPerGame);
          updateStmt.setDouble(22, pointsPerGame);
          updateStmt.setDouble(23, kdRatio);
          updateStmt.setDouble(24, winrate);
          updateStmt.setDouble(25, wlRatio);
          updateStmt.executeUpdate();
        }

        // Paso 4: Eliminar la cuenta origen
        String deleteSql = "DELETE FROM " + table + " WHERE username = ?";
        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
          deleteStmt.setString(1, oldAccount);
          int rowsDeleted = deleteStmt.executeUpdate();

          if (rowsDeleted == 0) {
            conn.rollback();
            return org.nicolie.towersforpgm.database.models.StatsTransferResult.failure(
                "No se pudo eliminar la cuenta origen");
          }
        }

        // Commit de la transacción
        conn.commit();

        plugin
            .getLogger()
            .info("[+] transferStats exitoso: " + oldAccount + " -> " + newAccount + " en tabla "
                + table);

        return org.nicolie.towersforpgm.database.models.StatsTransferResult.success(
            "Transferencia completada exitosamente de " + oldAccount + " a " + newAccount,
            mergedStats);

      } catch (SQLException e) {
        conn.rollback();
        plugin
            .getLogger()
            .severe("[-] Error en transferStats, rollback realizado: " + e.getMessage());
        throw e;
      }

    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Error al transferir estadísticas (SQLite)", e);
      return org.nicolie.towersforpgm.database.models.StatsTransferResult.failure(
          "Error en la base de datos: " + e.getMessage());
    }
  }

  public static List<Integer> getEloHistory(String username, String table) {
    List<Integer> eloHistory = new ArrayList<>();

    // Query para obtener el elo_delta de todas las partidas del jugador, ordenadas por finished_at
    String sql = "SELECT mph.elo_delta "
        + "FROM match_players_history mph "
        + "INNER JOIN matches_history mh ON mph.match_id = mh.match_id "
        + "WHERE mph.username = ? AND mh.table_name = ? "
        + "ORDER BY mh.finished_at ASC";

    try (Connection conn = plugin.getDatabaseConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

      ps.setString(1, username);
      ps.setString(2, table);

      try (ResultSet rs = ps.executeQuery()) {
        int currentElo = 0; // El jugador empieza con 0 de ELO
        boolean hasNonZeroDelta = false; // Flag para verificar si hay algún delta diferente de 0
        while (rs.next()) {
          int eloDelta = rs.getInt("elo_delta");
          if (eloDelta != 0) {
            hasNonZeroDelta = true;
          }
          currentElo += eloDelta; // Acumular el delta
          eloHistory.add(currentElo);
        }

        // Si todos los deltas son 0, retornar lista vacía
        if (!hasNonZeroDelta && !eloHistory.isEmpty()) {
          plugin
              .getLogger()
              .info("[+] getEloHistory (SQLite): " + username + " en " + table
                  + " - Todos los deltas son 0, retornando vacío");
          return new ArrayList<>();
        }
      }

      plugin
          .getLogger()
          .info("[+] getEloHistory (SQLite): " + username + " en " + table + ", "
              + eloHistory.size() + " partidas");

    } catch (SQLException e) {
      plugin
          .getLogger()
          .log(Level.SEVERE, "Error al obtener historial de ELO (SQLite): " + e.getMessage(), e);
    }

    return eloHistory;
  }
}
