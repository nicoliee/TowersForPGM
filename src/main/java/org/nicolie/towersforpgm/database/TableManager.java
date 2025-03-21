package org.nicolie.towersforpgm.database;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.SendMessage;
import org.bukkit.Bukkit;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

public class TableManager {
    public static void createTable(String tableName) {
        Bukkit.getScheduler().runTaskAsynchronously(TowersForPGM.getInstance(), () -> {
            String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "username VARCHAR(16) PRIMARY KEY, " +
                    "kills INT DEFAULT 0, " +
                    "deaths INT DEFAULT 0, " +
                    "points INT DEFAULT 0, " +
                    "wins INT DEFAULT 0, " +
                    "games INT DEFAULT 0" +
                    ");";
            
            try (Connection conn = TowersForPGM.getInstance().getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.executeUpdate();
                TowersForPGM.getInstance().getLogger().info("Tabla creada: " + tableName);
                
            } catch (SQLException e) {
                TowersForPGM.getInstance().getLogger().log(Level.SEVERE, "Error ejecutando SQL", e);
                SendMessage.sendToDevelopers("&cError al crear la tabla: &f" + e.getMessage());
            }
        });
    }
}
