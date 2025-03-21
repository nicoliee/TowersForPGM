package org.nicolie.towersforpgm.database;

import org.nicolie.towersforpgm.TowersForPGM;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {
    private final TowersForPGM plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(TowersForPGM plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            if (!plugin.getConfig().getBoolean("database.enabled")) {
                return;
            }

            String host = plugin.getConfig().getString("database.host");
            int port = plugin.getConfig().getInt("database.port");
            String name = plugin.getConfig().getString("database.name");
            String user = plugin.getConfig().getString("database.user");
            String password = plugin.getConfig().getString("database.password");

            if (host == null || name == null || user == null || password == null) {
                plugin.getLogger().warning("Faltan credenciales de la base de datos. No se intentará la conexión.");
                return;
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + name + "?useSSL=false");
            config.setUsername(user);
            config.setPassword(password);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setMaxLifetime(1800000);
            config.setConnectionTimeout(10000);
            dataSource = new HikariDataSource(config);

            plugin.getLogger().info("Conectado a MySQL con HikariCP.");
        } catch (Exception e) {
            plugin.getLogger().severe("Error al conectar con la base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Conexión de MySQL cerrada.");
        }
    }
}
