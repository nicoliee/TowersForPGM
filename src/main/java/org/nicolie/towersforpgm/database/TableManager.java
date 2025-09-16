package org.nicolie.towersforpgm.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.ConfigManager;

public class TableManager {
  public static void createTable(String tableName) {
    if ("none".equalsIgnoreCase(tableName)) {
      return;
    }

    Bukkit.getScheduler().runTaskAsynchronously(TowersForPGM.getInstance(), () -> {
      boolean isRanked = ConfigManager.getRankedTables() != null
          && ConfigManager.getRankedTables().contains(tableName);
      StringBuilder sqlBuilder = new StringBuilder();
      sqlBuilder
          .append("CREATE TABLE IF NOT EXISTS ")
          .append(tableName)
          .append(" (")
          .append("username VARCHAR(16) PRIMARY KEY, ")
          .append("kills INT DEFAULT 0, ")
          .append("deaths INT DEFAULT 0, ")
          .append("assists INT DEFAULT 0, ")
          .append("damageDone DOUBLE DEFAULT 0, ")
          .append("damageTaken DOUBLE DEFAULT 0, ")
          .append("points INT DEFAULT 0, ")
          .append("wins INT DEFAULT 0, ")
          .append("games INT DEFAULT 0, ")
          .append("winstreak INT DEFAULT 0, ")
          .append("maxWinstreak INT DEFAULT 0");
      if (isRanked) {
        sqlBuilder.append(", elo INT DEFAULT 0, lastElo INT DEFAULT 0, maxElo INT DEFAULT 0");
      }
      sqlBuilder.append(");");
      String sql = sqlBuilder.toString();

      try (Connection conn = TowersForPGM.getInstance().getDatabaseManager().getConnection();
          PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.executeUpdate();
      } catch (SQLException e) {
        TowersForPGM.getInstance().getLogger().log(Level.SEVERE, "Error ejecutando SQL", e);
      }

      // Lista de columnas requeridas
      String[] requiredColumns;
      if (isRanked) {
        requiredColumns = new String[] {
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
          "elo",
          "lastElo",
          "maxElo"
        };
      } else {
        requiredColumns = new String[] {
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
          "maxWinstreak"
        };
      }

      // Verificar y agregar columnas si no existen
      for (String column : requiredColumns) {
        if (!columnExists(tableName, column)) {
          String alterTable =
              "ALTER TABLE " + tableName + " ADD COLUMN " + column + " DOUBLE DEFAULT 0;";
          if ("username".equals(column)) {
            alterTable =
                "ALTER TABLE " + tableName + " ADD COLUMN " + column + " VARCHAR(16) PRIMARY KEY;";
          } else if ("kills".equals(column)
              || "deaths".equals(column)
              || "assists".equals(column)
              || "points".equals(column)
              || "games".equals(column)
              || "wins".equals(column)
              || "winstreak".equals(column)
              || "maxWinstreak".equals(column)
              || "elo".equals(column)
              || "lastElo".equals(column)
              || "maxElo".equals(column)) {
            alterTable = "ALTER TABLE " + tableName + " ADD COLUMN " + column + " INT DEFAULT 0;";
          }
          try (Connection conn = TowersForPGM.getInstance().getDatabaseManager().getConnection();
              PreparedStatement stmt = conn.prepareStatement(alterTable)) {
            stmt.executeUpdate();
            TowersForPGM.getInstance()
                .getLogger()
                .info("Columna '" + column + "' agregada en " + tableName);
          } catch (SQLException e) {
            TowersForPGM.getInstance()
                .getLogger()
                .log(Level.SEVERE, "Error agregando columna '" + column + "'", e);
          }
        }
      }
    });
  }

  private static boolean columnExists(String tableName, String columnName) {
    String query = "SELECT COUNT(*) FROM information_schema.COLUMNS "
        + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";

    try (Connection conn = TowersForPGM.getInstance().getDatabaseManager().getConnection();
        PreparedStatement stmt = conn.prepareStatement(query)) {

      String databaseName = conn.getCatalog(); // <- obtiene la base de datos en uso

      stmt.setString(1, databaseName);
      stmt.setString(2, tableName);
      stmt.setString(3, columnName);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1) > 0;
        }
      }
    } catch (SQLException e) {
      TowersForPGM.getInstance()
          .getLogger()
          .log(Level.SEVERE, "Error verificando columna '" + columnName + "'", e);
    }
    return false;
  }
}
