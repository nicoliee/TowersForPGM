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

        List<String> statements = new ArrayList<>();

        // 1. Crear tabla base si no existe
        statements.add(buildCreateTableSQL(tableName, isRanked));

        // 2. Consultar columnas existentes
        Set<String> existingColumns = getExistingColumns(conn, tableName);

        // 3. Crear columnas calculadas solo si no existen
        for (String column : CALCULATED_COLUMNS) {
          if (!existingColumns.contains(column)) {
            String sql = getCalculatedColumnSQL(tableName, column);
            if (sql != null) statements.add(sql);
          }
        }

        // 4. Consultar índices existentes
        Set<String> existingIndexes = getExistingIndexes(conn, tableName);

        // 5. Crear índices para todas las columnas solo si no existen
        List<String> allColumns = new ArrayList<>(COLUMNS);
        if (isRanked) allColumns.addAll(RANKED_COLUMNS);
        allColumns.addAll(CALCULATED_COLUMNS);

        for (String column : allColumns) {
          String idxName = "idx_" + column;
          if (!existingIndexes.contains(idxName)) {
            statements.add(
                "CREATE INDEX IF NOT EXISTS " + idxName + " ON " + tableName + " (" + column + ")");
          }
        }

        // 6. Ejecutar solo las operaciones necesarias
        executeStatements(conn, statements, tableName);

      } catch (SQLException e) {
        TowersForPGM.getInstance()
            .getLogger()
            .log(Level.SEVERE, "Error conectando a la base de datos", e);
      }
    });
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
    int createdCount = 0; // Contador de columnas/índices realmente creados
    try {
      conn.setAutoCommit(false);
      for (String sql : statements) {
        // No contar CREATE TABLE IF NOT EXISTS como columna/índice creado
        boolean isCreateTable = sql.trim().toUpperCase().startsWith("CREATE TABLE");
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
          stmt.executeUpdate();
          if (!isCreateTable) {
            createdCount++; // Solo contar si no es CREATE TABLE
          }
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
      if (createdCount > 0) {
        TowersForPGM.getInstance()
            .getLogger()
            .info("Creadas " + createdCount + " nuevas columnas o índices en " + tableName);
      }
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
