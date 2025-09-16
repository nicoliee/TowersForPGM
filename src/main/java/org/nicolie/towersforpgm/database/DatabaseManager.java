package org.nicolie.towersforpgm.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import org.nicolie.towersforpgm.TowersForPGM;

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
        plugin
            .getLogger()
            .warning("Faltan credenciales de la base de datos. No se intentará la conexión.");
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
      config.addDataSourceProperty("cachePrepStmts", "true");
      config.addDataSourceProperty("prepStmtCacheSize", "250");
      config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

      dataSource = new HikariDataSource(config);
      plugin.getLogger().info("Conectado a MySQL con HikariCP.");
    } catch (Exception e) {
      plugin.getLogger().severe("Error al conectar con la base de datos: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public Connection getConnection() throws SQLException {
    int retries = 3;
    while (retries > 0) {
      try {
        if (dataSource == null || dataSource.isClosed()) {
          plugin
              .getLogger()
              .warning("El pool de conexiones está cerrado. Reintentando conexión...");
          connect(); // Reintenta conectar si el pool está cerrado
        }

        Connection connection = dataSource.getConnection();

        if (connection == null || !connection.isValid(2)) {
          throw new SQLException("La conexión obtenida no es válida.");
        }

        return connection;
      } catch (SQLException e) {
        retries--;
        plugin
            .getLogger()
            .warning("Error al obtener la conexión a la base de datos. Reintentando... (" + retries
                + " intentos restantes)");
        try {
          Thread.sleep(2000); // Espera antes de reintentar
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
    }
    throw new SQLException(
        "No se pudo obtener conexión a la base de datos después de varios intentos.");
  }

  public void disconnect() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
      plugin.getLogger().info("Conexión de MySQL cerrada.");
    }
  }
}
