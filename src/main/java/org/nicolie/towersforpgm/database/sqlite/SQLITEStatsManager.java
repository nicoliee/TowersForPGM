package org.nicolie.towersforpgm.database.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.Stats;
import org.nicolie.towersforpgm.database.Top;
import org.nicolie.towersforpgm.database.TopResult;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.SendMessage;

public class SQLITEStatsManager {
  // Executor dedicado para consultas SQLite asíncronas
  private static final ExecutorService SQL_EXECUTOR = Executors.newFixedThreadPool(4, r -> {
    Thread t = new Thread(r);
    t.setName("TowersForPGM-SQLite-Async");
    t.setDaemon(true);
    return t;
  });

  // Llamar a este método al apagar el plugin para liberar recursos
  public static void shutdownSqlExecutor() {
    SQL_EXECUTOR.shutdown();
  }

  public static void updateStats(
      String table, List<Stats> playerStatsList, List<PlayerEloChange> eloChangeList) {

    if (playerStatsList.isEmpty()
        || table == null
        || table.isEmpty()
        || !ConfigManager.getTables().contains(table)) {
      return;
    }

    boolean isRanked = ConfigManager.getRankedTables().contains(table);

    SQL_EXECUTOR.execute(() -> {
      try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection()) {
        conn.setAutoCommit(false);

        // Columnas básicas + winstreaks + ratios
        List<String> columns = new ArrayList<>(Arrays.asList(
            "username",
            "kills",
            "deaths",
            "assists",
            "damageDone",
            "damageTaken",
            "points",
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
            "winrate"));

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

            // Leer valores actuales si existen
            int oldKills = 0,
                oldDeaths = 0,
                oldAssists = 0,
                oldPoints = 0,
                oldGames = 0,
                oldWins = 0;
            double oldDamageDone = 0, oldDamageTaken = 0;
            int oldWinstreak = 0, oldMaxWinstreak = 0;
            boolean exists = false;

            try (PreparedStatement ps =
                conn.prepareStatement("SELECT * FROM " + table + " WHERE username = ?")) {
              ps.setString(1, stat.getUsername());
              try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                  exists = true;
                  oldKills = rs.getInt("kills");
                  oldDeaths = rs.getInt("deaths");
                  oldAssists = rs.getInt("assists");
                  oldPoints = rs.getInt("points");
                  oldGames = rs.getInt("games");
                  oldWins = rs.getInt("wins");
                  oldDamageDone = rs.getDouble("damageDone");
                  oldDamageTaken = rs.getDouble("damageTaken");
                  oldWinstreak = rs.getInt("winstreak");
                  oldMaxWinstreak = rs.getInt("maxWinstreak");
                }
              }
            }

            // Sumar estadísticas
            int kills = stat.getKills() + oldKills;
            int deaths = stat.getDeaths() + oldDeaths;
            int assists = stat.getAssists() + oldAssists;
            int points = stat.getPoints() + oldPoints;
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

            // Preparar statement
            int idx = 1;
            stmt.setString(idx++, stat.getUsername());
            stmt.setInt(idx++, kills);
            stmt.setInt(idx++, deaths);
            stmt.setInt(idx++, assists);
            stmt.setDouble(idx++, damageDone);
            stmt.setDouble(idx++, damageTaken);
            stmt.setInt(idx++, points);
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
        SendMessage.sendToDevelopers(org.nicolie.towersforpgm.utils.LanguageManager.langMessage(
            "errors.database.updateStats"));
      }
    });
  }

  public static CompletableFuture<Stats> getStats(String table, String username) {
    // Validación básica de parámetros
    if (table == null || table.isEmpty() || username == null || username.isEmpty()) {
      TowersForPGM.getInstance()
          .getLogger()
          .log(
              Level.WARNING,
              "[StatsManager SQLite] Parámetros inválidos getStats: table=" + table + ", username="
                  + username);
      return CompletableFuture.completedFuture(null);
    }

    // La tabla es válida si está en cualquiera de las listas configuradas
    boolean validTable =
        org.nicolie.towersforpgm.utils.ConfigManager.getTables().contains(table)
            || MatchBotConfig.getTables().contains(table);
    if (!validTable) {
      TowersForPGM.getInstance()
          .getLogger()
          .log(
              Level.WARNING,
              "[StatsManager SQLite] Tabla no registrada en config getStats: " + table);
      return CompletableFuture.completedFuture(null);
    }

    String sql = "SELECT * FROM `" + table + "` WHERE username = ?";

    return CompletableFuture.supplyAsync(
        () -> {
          try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setQueryTimeout(10); // segundos
            try (ResultSet rs = stmt.executeQuery()) {
              if (rs.next()) {
                int kills = hasColumn(rs, "kills") ? rs.getInt("kills") : -9999;
                int deaths = hasColumn(rs, "deaths") ? rs.getInt("deaths") : -9999;
                int assists = hasColumn(rs, "assists") ? rs.getInt("assists") : -9999;
                double damageDone =
                    hasColumn(rs, "damageDone") ? rs.getDouble("damageDone") : -9999.0;
                double damageTaken =
                    hasColumn(rs, "damageTaken") ? rs.getDouble("damageTaken") : -9999.0;
                int points = hasColumn(rs, "points") ? rs.getInt("points") : -9999;
                int wins = hasColumn(rs, "wins") ? rs.getInt("wins") : -9999;
                int games = hasColumn(rs, "games") ? rs.getInt("games") : -9999;
                int winstreak = hasColumn(rs, "winstreak") ? rs.getInt("winstreak") : -9999;
                int maxWinstreak =
                    hasColumn(rs, "maxWinstreak") ? rs.getInt("maxWinstreak") : -9999;
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
            TowersForPGM.getInstance()
                .getLogger()
                .log(Level.SEVERE, "Error al obtener estadísticas del usuario en SQLite", e);
            SendMessage.sendToDevelopers(org.nicolie.towersforpgm.utils.LanguageManager.langMessage(
                "errors.database.getStats"));
            return null;
          }
        },
        SQL_EXECUTOR);
  }

  public static CompletableFuture<TopResult> getTop(
      String table, String dbColumn, int limit, int page) {
    // Validación básica de parámetros
    if (table == null
        || table.isEmpty()
        || dbColumn == null
        || dbColumn.isEmpty()
        || limit <= 0
        || page < 1) {
      return CompletableFuture.completedFuture(new TopResult(new ArrayList<>(), 0));
    }

    boolean validTable =
        org.nicolie.towersforpgm.utils.ConfigManager.getTables().contains(table)
            || MatchBotConfig.getTables().contains(table);
    if (!validTable) return CompletableFuture.completedFuture(new TopResult(new ArrayList<>(), 0));

    int offset = (page - 1) * limit;

    // SQLite usa subconsulta para obtener el total de registros
    String sql = "SELECT username, `" + dbColumn + "` AS valuePerGame, "
        + "(SELECT COUNT(*) FROM `" + table + "`) AS totalCount "
        + "FROM `" + table + "` ORDER BY `" + dbColumn + "` DESC LIMIT ? OFFSET ?";

    return CompletableFuture.supplyAsync(
        () -> {
          List<Top> topList = new ArrayList<>();
          int totalRecords = 0;
          try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            stmt.setQueryTimeout(10);

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
          }
          return new TopResult(topList, totalRecords);
        },
        SQL_EXECUTOR);
  }

  public static CompletableFuture<TopResult> getTop(
      String table,
      String dbColumn,
      int limit,
      Double lastValue,
      String lastUser,
      Integer totalRecords) {
    if (table == null || table.isEmpty() || dbColumn == null || dbColumn.isEmpty() || limit <= 0) {
      return CompletableFuture.completedFuture(new TopResult(new ArrayList<>(), 0));
    }

    boolean validTable =
        org.nicolie.towersforpgm.utils.ConfigManager.getTables().contains(table)
            || MatchBotConfig.getTables().contains(table);
    if (!validTable) return CompletableFuture.completedFuture(new TopResult(new ArrayList<>(), 0));

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

    return CompletableFuture.supplyAsync(
        () -> {
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
            stmt.setQueryTimeout(10);

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
            TowersForPGM.getInstance()
                .getLogger()
                .log(Level.SEVERE, "Error obteniendo top con keyset pagination", e);
          }
          return new TopResult(list, total);
        },
        SQL_EXECUTOR);
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

  public static CompletableFuture<List<PlayerEloChange>> getEloForUsernames(
      String table, List<String> usernames) {
    if (usernames == null || usernames.isEmpty()) {
      TowersForPGM.getInstance()
          .getLogger()
          .info("getEloForUsernames: lista de usernames vacía o nula");
      return CompletableFuture.completedFuture(java.util.Collections.emptyList());
    }

    String placeholders = String.join(",", java.util.Collections.nCopies(usernames.size(), "?"));
    String sql =
        "SELECT username, elo, maxElo FROM " + table + " WHERE username IN (" + placeholders + ")";

    return CompletableFuture.supplyAsync(
        () -> {
          try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < usernames.size(); i++) {
              stmt.setString(i + 1, usernames.get(i));
            }
            stmt.setQueryTimeout(10); // segundos
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
        },
        SQL_EXECUTOR);
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
      stmt.setQueryTimeout(10); // segundos

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
}
