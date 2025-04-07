package org.nicolie.towersforpgm.database;
import org.nicolie.towersforpgm.TowersForPGM;
import org.bukkit.Bukkit;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

public class TableManager {
    public static void createTable(String tableName) {
        if ("none".equalsIgnoreCase(tableName)) {
            return;
        }
    
        Bukkit.getScheduler().runTaskAsynchronously(TowersForPGM.getInstance(), () -> {
            String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "username VARCHAR(16) PRIMARY KEY, " +
                    "kills INT DEFAULT 0, " +
                    "deaths INT DEFAULT 0, " +
                    "assists INT DEFAULT 0, " +
                    "points INT DEFAULT 0, " +
                    "wins INT DEFAULT 0, " +
                    "games INT DEFAULT 0" +
                    ");";
            
            try (Connection conn = TowersForPGM.getInstance().getDatabaseManager().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.executeUpdate();
            } catch (SQLException e) {
                TowersForPGM.getInstance().getLogger().log(Level.SEVERE, "Error ejecutando SQL", e);
            }
    
            // Lista de columnas requeridas
            String[] requiredColumns = {"username", "kills", "deaths", "assists", "points", "games", "wins"};
            
            // Verificar y agregar columnas si no existen
            for (String column : requiredColumns) {
                if (!columnExists(tableName, column)) {
                    String alterTable = "ALTER TABLE " + tableName + " ADD COLUMN " + column + " INT DEFAULT 0;";
                    if ("username".equals(column)) {
                        alterTable = "ALTER TABLE " + tableName + " ADD COLUMN " + column + " VARCHAR(16) PRIMARY KEY;";
                    }
                    try (Connection conn = TowersForPGM.getInstance().getDatabaseManager().getConnection();
                        PreparedStatement stmt = conn.prepareStatement(alterTable)) {
                        stmt.executeUpdate();
                        TowersForPGM.getInstance().getLogger().info("Columna '" + column + "' agregada en " + tableName);
                    } catch (SQLException e) {
                        TowersForPGM.getInstance().getLogger().log(Level.SEVERE, "Error agregando columna '" + column + "'", e);
                    }
                }
            }
        });
    }
    
    private static boolean columnExists(String tableName, String columnName) {
        String query = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                       "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
    
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
            TowersForPGM.getInstance().getLogger().log(Level.SEVERE, "Error verificando columna '" + columnName + "'", e);
        }
        return false;
    }       
}