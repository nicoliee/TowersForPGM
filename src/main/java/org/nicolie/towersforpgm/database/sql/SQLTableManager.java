package org.nicolie.towersforpgm.database.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.utils.ConfigManager;

public class SQLTableManager {
  public static final List<String> COLUMNS = Arrays.asList(
      "kills",
      "deaths",
      "assists",
      "damageDone",
      "damageTaken",
      "points",
      "games",
      "wins",
      "winstreak",
      "maxWinstreak");

  public static final List<String> RANKED_COLUMNS = Arrays.asList("elo", "lastElo", "maxElo");

  // Columnas calculadas (generadas)
  public static final List<String> CALCULATED_COLUMNS = Arrays.asList(
      "killsPerGame",
      "deathsPerGame",
      "assistsPerGame",
      "pointsPerGame",
      "damageDonePerGame",
      "damageTakenPerGame",
      "kdRatio",
      "winrate");

  public static void createTable(String tableName) {
    if ("none".equalsIgnoreCase(tableName)) return;

    Bukkit.getScheduler().runTaskAsynchronously(TowersForPGM.getInstance(), () -> {
      try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection()) {

        boolean isRanked = ConfigManager.getRankedTables() != null
            && ConfigManager.getRankedTables().contains(tableName);

        // Verificar si la tabla ya existe
        if (tableExists(conn, tableName)) {
          return;
        }

        // La tabla no existe, crearla completa con columnas e índices
        List<String> statements = new ArrayList<>();

        // 1. Crear tabla base
        statements.add(buildCreateTableSQL(tableName, isRanked));

        // 2. Crear columnas calculadas
        for (String column : CALCULATED_COLUMNS) {
          String sql = getCalculatedColumnSQL(tableName, column);
          if (sql != null) statements.add(sql);
        }

        // 3. Crear índices para todas las columnas
        List<String> allColumns = new ArrayList<>(COLUMNS);
        if (isRanked) allColumns.addAll(RANKED_COLUMNS);
        allColumns.addAll(CALCULATED_COLUMNS);

        // Crear índices individuales
        for (String column : allColumns) {
          String idxName = "idx_" + column;
          statements.add(
              "CREATE INDEX IF NOT EXISTS " + idxName + " ON " + tableName + " (" + column + ")");
        }

        // Crear índice compuesto optimizado para consultas getTop
        statements.add("CREATE INDEX IF NOT EXISTS idx_composite_order ON " + tableName
            + " (kills DESC, username ASC, deaths DESC, assists DESC, points DESC, wins DESC)");

        // 4. Ejecutar todas las operaciones
        executeStatements(conn, statements, tableName);

        TowersForPGM.getInstance()
            .getLogger()
            .info("Tabla " + tableName + " creada exitosamente con todas sus columnas e índices");

      } catch (SQLException e) {
        TowersForPGM.getInstance()
            .getLogger()
            .log(Level.SEVERE, "Error conectando a la base de datos", e);
      }
    });
  }

  public static void verifyStatsTable(String tableName, boolean isRanked) {
    if ("none".equalsIgnoreCase(tableName)) return;

    Bukkit.getScheduler().runTaskAsynchronously(TowersForPGM.getInstance(), () -> {
      try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection()) {

        // Verificar si la tabla existe
        if (!tableExists(conn, tableName)) {
          TowersForPGM.getInstance()
              .getLogger()
              .warning("Tabla " + tableName + " no existe, no se puede verificar");
          return;
        }

        List<String> statements = new ArrayList<>();

        // 1. Consultar columnas existentes
        Set<String> existingColumns = getExistingColumns(conn, tableName);

        // 2. Crear columnas calculadas solo si no existen
        for (String column : CALCULATED_COLUMNS) {
          if (!existingColumns.contains(column)) {
            String sql = getCalculatedColumnSQL(tableName, column);
            if (sql != null) statements.add(sql);
          }
        }

        // 3. Consultar índices existentes
        Set<String> existingIndexes = getExistingIndexes(conn, tableName);

        // 4. Crear índices para todas las columnas solo si no existen
        List<String> allColumns = new ArrayList<>(COLUMNS);
        if (isRanked) allColumns.addAll(RANKED_COLUMNS);
        allColumns.addAll(CALCULATED_COLUMNS);

        // Crear índices individuales
        for (String column : allColumns) {
          String idxName = "idx_" + column;
          if (!existingIndexes.contains(idxName)) {
            statements.add(
                "CREATE INDEX IF NOT EXISTS " + idxName + " ON " + tableName + " (" + column + ")");
          }
        }

        // Crear índice compuesto optimizado para consultas getTop
        String compositeIdxName = "idx_composite_order";
        if (!existingIndexes.contains(compositeIdxName)) {
          statements.add("CREATE INDEX IF NOT EXISTS " + compositeIdxName + " ON " + tableName
              + " (kills DESC, username ASC, deaths DESC, assists DESC, points DESC, wins DESC)");
        }

        // 5. Ejecutar solo las operaciones necesarias
        if (!statements.isEmpty()) {
          executeStatements(conn, statements, tableName);
          TowersForPGM.getInstance()
              .getLogger()
              .info("Verificación completada para " + tableName + ": " + statements.size()
                  + " cambios aplicados");
        } else {
          TowersForPGM.getInstance()
              .getLogger()
              .info("Tabla " + tableName + " está actualizada, no se requieren cambios");
        }

      } catch (SQLException e) {
        TowersForPGM.getInstance()
            .getLogger()
            .log(Level.SEVERE, "Error verificando tabla " + tableName, e);
      }
    });
  }

  public static void createDCAccountsTable() {
    if (!MatchBotConfig.isRankedEnabled()) {
      return;
    }

    Bukkit.getScheduler().runTaskAsynchronously(TowersForPGM.getInstance(), () -> {
      try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection()) {
        String acc_table = MatchBotConfig.getAccountsTable();
        String createTableSQL =
            "CREATE TABLE IF NOT EXISTS " + acc_table + " (" + "uuid VARCHAR(36) PRIMARY KEY, "
                + "discordId VARCHAR(32) NOT NULL UNIQUE, "
                + "registeredAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "INDEX idx_discordId (discordId)"
                + ")";

        try (PreparedStatement stmt = conn.prepareStatement(createTableSQL)) {
          stmt.executeUpdate();
          TowersForPGM.getInstance()
              .getLogger()
              .info("Tabla " + acc_table + " creada exitosamente");
        }

      } catch (SQLException e) {
        TowersForPGM.getInstance()
            .getLogger()
            .log(Level.SEVERE, "Error creando tabla " + MatchBotConfig.getAccountsTable(), e);
      }
    });
  }

  /** Crea tablas de historial de partidas (MySQL) */
  public static void createHistoryTables() {
    Bukkit.getScheduler().runTaskAsynchronously(TowersForPGM.getInstance(), () -> {
      try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection()) {
        // Tabla principal de partidas
        String createMatches = "CREATE TABLE IF NOT EXISTS matches_history ("
            + "match_id VARCHAR(96) PRIMARY KEY,"
            + "table_name VARCHAR(64) NOT NULL,"
            + "map_name VARCHAR(128) NOT NULL,"
            + "duration_seconds INT NOT NULL,"
            + "ranked TINYINT(1) NOT NULL DEFAULT 0,"
            + "scores_text TEXT,"
            + "winners_text VARCHAR(255),"
            + "finished_at BIGINT NOT NULL,"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "INDEX idx_table_date (table_name, created_at)"
            + ")";

        // Tabla de jugadores por partida
        String createPlayers = "CREATE TABLE IF NOT EXISTS match_players_history ("
            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
            + "match_id VARCHAR(96) NOT NULL,"
            + "username VARCHAR(32) NOT NULL,"
            + "kills INT DEFAULT 0,"
            + "deaths INT DEFAULT 0,"
            + "assists INT DEFAULT 0,"
            + "damageDone DOUBLE DEFAULT 0,"
            + "damageTaken DOUBLE DEFAULT 0,"
            + "points INT DEFAULT 0,"
            + "win INT DEFAULT 0,"
            + "game INT DEFAULT 1,"
            + "winstreak_delta INT DEFAULT 0,"
            + "elo_delta INT DEFAULT 0,"
            + "maxElo_after INT DEFAULT 0,"
            + "INDEX idx_match (match_id),"
            + "INDEX idx_user (username),"
            + "FOREIGN KEY (match_id) REFERENCES matches_history(match_id) ON DELETE CASCADE"
            + ")";

        try (PreparedStatement stmt1 = conn.prepareStatement(createMatches);
            PreparedStatement stmt2 = conn.prepareStatement(createPlayers)) {
          stmt1.executeUpdate();
          stmt2.executeUpdate();
        }
      } catch (SQLException e) {
        TowersForPGM.getInstance()
            .getLogger()
            .log(Level.SEVERE, "Error creando tablas de historial (MySQL)", e);
      }
    });
  }

  // Verifica si la tabla existe en la base de datos
  private static boolean tableExists(Connection conn, String tableName) throws SQLException {
    try (PreparedStatement stmt = conn.prepareStatement("SHOW TABLES LIKE ?")) {
      stmt.setString(1, tableName);
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next();
      }
    }
  }

  // Devuelve el conjunto de nombres de columnas existentes en la tabla
  private static Set<String> getExistingColumns(Connection conn, String tableName)
      throws SQLException {
    Set<String> columns = new HashSet<>();
    try (PreparedStatement stmt = conn.prepareStatement("SHOW COLUMNS FROM " + tableName);
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        columns.add(rs.getString(1));
      }
    } catch (SQLException e) {
      // Si la tabla no existe aún, no hay columnas
    }
    return columns;
  }

  // Devuelve el conjunto de nombres de índices existentes en la tabla
  private static Set<String> getExistingIndexes(Connection conn, String tableName)
      throws SQLException {
    Set<String> indexes = new HashSet<>();
    try (PreparedStatement stmt = conn.prepareStatement("SHOW INDEX FROM " + tableName);
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        indexes.add(rs.getString("Key_name"));
      }
    } catch (SQLException e) {
      // Si la tabla no existe aún, no hay índices
    }
    return indexes;
  }

  private static void executeStatements(
      Connection conn, List<String> statements, String tableName) {
    try {
      conn.setAutoCommit(false);
      for (String sql : statements) {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
          stmt.executeUpdate();
        } catch (SQLException e) {
          // Ignorar si columna calculada ya existe o índice ya existe
          String msg = e.getMessage();
          if ((msg != null && msg.contains("Duplicate column name"))
              || (msg != null && msg.contains("already exists"))) {
            continue;
          } else {
            throw e;
          }
        }
      }
      conn.commit();
    } catch (SQLException e) {
      try {
        conn.rollback();
      } catch (SQLException rollbackEx) {
        TowersForPGM.getInstance().getLogger().log(Level.SEVERE, "Error en rollback", rollbackEx);
      }
      TowersForPGM.getInstance()
          .getLogger()
          .log(Level.SEVERE, "Error ejecutando operaciones SQL en " + tableName, e);
    } finally {
      try {
        conn.setAutoCommit(true);
      } catch (SQLException e) {
        TowersForPGM.getInstance().getLogger().log(Level.SEVERE, "Error restaurando autocommit", e);
      }
    }
  }

  private static String buildCreateTableSQL(String tableName, boolean isRanked) {
    StringBuilder sql = new StringBuilder();
    sql.append("CREATE TABLE IF NOT EXISTS ")
        .append(tableName)
        .append(" (")
        .append("username VARCHAR(16) PRIMARY KEY, ");

    for (String col : COLUMNS) {
      sql.append(col).append(col.contains("damage") ? " DOUBLE DEFAULT 0, " : " INT DEFAULT 0, ");
    }

    if (isRanked) {
      for (String col : RANKED_COLUMNS) {
        sql.append(col).append(" INT DEFAULT 0, ");
      }
    }

    // Quitar última coma
    sql.setLength(sql.length() - 2);
    sql.append(")");
    return sql.toString();
  }

  private static String getCalculatedColumnSQL(String tableName, String column) {
    String baseSQL = "ALTER TABLE " + tableName + " ADD COLUMN ";

    switch (column) {
      case "killsPerGame":
        return baseSQL
            + "killsPerGame DOUBLE GENERATED ALWAYS AS (CASE WHEN games=0 THEN 0 ELSE kills/games END) STORED";
      case "deathsPerGame":
        return baseSQL
            + "deathsPerGame DOUBLE GENERATED ALWAYS AS (CASE WHEN games=0 THEN 0 ELSE deaths/games END) STORED";
      case "assistsPerGame":
        return baseSQL
            + "assistsPerGame DOUBLE GENERATED ALWAYS AS (CASE WHEN games=0 THEN 0 ELSE assists/games END) STORED";
      case "pointsPerGame":
        return baseSQL
            + "pointsPerGame DOUBLE GENERATED ALWAYS AS (CASE WHEN games=0 THEN 0 ELSE points/games END) STORED";
      case "damageDonePerGame":
        return baseSQL
            + "damageDonePerGame DOUBLE GENERATED ALWAYS AS (CASE WHEN games=0 THEN 0 ELSE damageDone/games END) STORED";
      case "damageTakenPerGame":
        return baseSQL
            + "damageTakenPerGame DOUBLE GENERATED ALWAYS AS (CASE WHEN games=0 THEN 0 ELSE damageTaken/games END) STORED";
      case "kdRatio":
        return baseSQL
            + "kdRatio DOUBLE GENERATED ALWAYS AS (CASE WHEN deaths=0 THEN kills ELSE kills/deaths END) STORED";
      case "winrate":
        return baseSQL
            + "winrate INT GENERATED ALWAYS AS (CASE WHEN games=0 THEN 0 ELSE ROUND((wins/games)*100) END) STORED";
      default:
        return null;
    }
  }
}
