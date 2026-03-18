package org.nicolie.towersforpgm;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.nicolie.towersforpgm.commands.TowersCommandGraph;
import org.nicolie.towersforpgm.configs.ConfigManager;
import org.nicolie.towersforpgm.configs.tables.TableType;
import org.nicolie.towersforpgm.database.MatchHistoryManager;
import org.nicolie.towersforpgm.database.TableManager;
import org.nicolie.towersforpgm.database.sql.SQLDatabaseManager;
import org.nicolie.towersforpgm.database.sqlite.SQLITEDatabaseManager;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.commands.AutocompleteHandler;
import org.nicolie.towersforpgm.matchbot.commands.stats.StatsCommand;
import org.nicolie.towersforpgm.matchbot.commands.top.TopCommand;
import org.nicolie.towersforpgm.matchbot.commands.top.TopPaginationListener;
import org.nicolie.towersforpgm.matchbot.rankeds.VoiceChannelManager;
import org.nicolie.towersforpgm.matchbot.rankeds.listeners.QueueJoinListener;
import org.nicolie.towersforpgm.matchbot.rankeds.listeners.RankedFinishListener;
import org.nicolie.towersforpgm.matchbot.rankeds.listeners.RankedListener;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import org.nicolie.towersforpgm.translations.PluginTranslator;
import org.nicolie.towersforpgm.translations.TranslationLoader;
import org.nicolie.towersforpgm.utils.Events;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.player.MatchPlayer;

public final class TowersForPGM extends JavaPlugin {
  private static TowersForPGM instance; // Instancia de la clase principal

  // Configs
  private org.nicolie.towersforpgm.configs.ConfigManager ConfigManager;

  // preparationTime
  private PreparationListener preparationListener;

  // Base de datos
  private SQLDatabaseManager mysqlDatabaseManager;
  private SQLITEDatabaseManager sqliteDatabaseManager;
  private boolean database = false; // Base de datos activada
  private String currentDatabaseType = "None"; // Tipo de base de datos actual (MySQL/SQLite/None)
  private final Map<String, MatchPlayer> disconnectedPlayers =
      new HashMap<>(); // Mapa para almacenar los jugadores
  private boolean isStatsCancel = false; // Variable para cancelar el envío de estadísticas

  // Refill
  private RefillManager refillManager; // Administrador de refill

  private Queue queue;

  // MatchBot (opcional)
  private boolean isMatchBotEnabled = false; // Variable para verificar si MatchBot está habilitado
  private File matchBotFile;
  private FileConfiguration matchBotConfig;

  @Override
  public void onLoad() {
    TranslationLoader.register();
  }

  @Override
  public void onEnable() {
    instance = this;
    saveDefaultConfig();
    this.ConfigManager = new org.nicolie.towersforpgm.configs.ConfigManager(this);
    LanguageManager.initialize(this);
    refillManager = new RefillManager();
    preparationListener = new PreparationListener();
    queue = new Queue();
    initializeDatabase();

    if (database) {
      createTablesOnStartup();
      try {
        Set<String> tables = new HashSet<>();
        tables.addAll(this.config().databaseTables().getTables(TableType.ALL));
        MatchHistoryManager.preloadMatchIdCountersAsync(tables).thenRun(() -> getLogger()
            .info("MatchHistoryManager precargado para tablas: " + tables.size()));
      } catch (Exception e) {
        getLogger().warning("Error precargando MatchHistoryManager: " + e.getMessage());
      }
    } else {
      getLogger().warning("No database connections available!");
    }

    MatchSessionRegistry.register(this);
    setupEvents();
    setupCommands();

    if (getServer().getPluginManager().getPlugin("MatchBot") != null
        && getServer().getPluginManager().getPlugin("MatchBot").isEnabled()) {
      getLogger().info("MatchBot plugin found, initializing MatchBot integration.");
      isMatchBotEnabled = true;

      loadMatchBotConfig();

      if (database) {
        TableManager.createDCAccountsTable();
        if (MatchBotConfig.isCommandsEnabled()) {
          StatsCommand.register();
          TopCommand.register();
          TopPaginationListener.register();
          org.nicolie.towersforpgm.matchbot.commands.history.HistoryCommand.register();
          org.nicolie.towersforpgm.matchbot.commands.link.LinkCommand.register();
          AutocompleteHandler.register();
        }

        QueueJoinListener.register();
        getServer().getPluginManager().registerEvents(new RankedListener(), this);
        getServer().getPluginManager().registerEvents(new RankedFinishListener(), this);

        Bukkit.getScheduler()
            .runTaskLater(
                this,
                () -> {
                  VoiceChannelManager.cleanupRankedChannelsOnStartup();
                },
                40L);
      }
    }
  }

  @Override
  public void onDisable() {
    if (isMatchBotEnabled && MatchBotConfig.isVoiceChatEnabled()) {
      VoiceChannelManager.cleanupRankedChannelsOnStartup();
    }

    // Desconectar de las bases de datos
    if (mysqlDatabaseManager != null) {
      mysqlDatabaseManager.disconnect();
    }
    if (sqliteDatabaseManager != null) {
      sqliteDatabaseManager.disconnect();
    }
  }

  public static TowersForPGM getInstance() {
    return instance;
  }

  public static PluginTranslator getTranslator() {
    return PluginTranslator.getInstance();
  }

  public ConfigManager config() {
    return this.ConfigManager;
  }

  public RefillManager refillManager() {
    return refillManager;
  }

  public Queue getQueue() {
    return queue;
  }

  private void setupCommands() {
    try {
      new TowersCommandGraph(this);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void setupEvents() {
    Events eventManager = new Events(this);
    eventManager.registerEvents(queue, refillManager, preparationListener);
  }

  // Base de datos
  public SQLDatabaseManager getMySQLDatabaseManager() {
    return mysqlDatabaseManager;
  }

  public SQLITEDatabaseManager getSQLiteDatabaseManager() {
    return sqliteDatabaseManager;
  }

  public Connection getDatabaseConnection() throws SQLException {
    if ("MySQL".equals(currentDatabaseType) && mysqlDatabaseManager != null) {
      return mysqlDatabaseManager.getConnection();
    } else if ("SQLite".equals(currentDatabaseType) && sqliteDatabaseManager != null) {
      return sqliteDatabaseManager.getConnection();
    }
    throw new SQLException("No hay conexiones de base de datos disponibles");
  }

  private void createTablesOnStartup() {
    config()
        .databaseTables()
        .getTables(TableType.ALL)
        .forEach(org.nicolie.towersforpgm.database.TableManager::createTable);
    this.config()
        .databaseTables()
        .getTables(TableType.RANKED)
        .forEach(org.nicolie.towersforpgm.database.TableManager::createTable);
    org.nicolie.towersforpgm.database.TableManager.createHistoryTables();
  }

  // Método para inicializar (o recargar) la base de datos
  public void initializeDatabase() {
    database = false; // Por defecto desactivada
    currentDatabaseType = "None";
    // Si había conexiones previas, cerrarlas antes de reintentar
    try {
      if (mysqlDatabaseManager != null) {
        try {
          mysqlDatabaseManager.disconnect();
        } catch (Exception ignored) {
        }
        mysqlDatabaseManager = null;
      }
      if (sqliteDatabaseManager != null) {
        try {
          sqliteDatabaseManager.disconnect();
        } catch (Exception ignored) {
        }
        sqliteDatabaseManager = null;
      }
    } catch (Exception e) {
      getLogger().warning("Error cerrando conexiones previas: " + e.getMessage());
    }

    // Verificar si MySQL está habilitado en la configuración
    if (getConfig().getBoolean("database.enabled", false)) {
      mysqlDatabaseManager = new SQLDatabaseManager(this);
      try {
        mysqlDatabaseManager.connect();
        // Verificar si la conexión MySQL realmente funciona
        if (testDatabaseConnection(mysqlDatabaseManager)) {
          database = true;
          currentDatabaseType = "MySQL";
          getLogger().info("Usando MySQL como base de datos.");
          return;
        }
      } catch (Exception e) {
        getLogger().warning("No se pudo conectar a MySQL: " + e.getMessage());
      }
    }

    // Si MySQL falló o no está habilitado, intentar SQLite
    sqliteDatabaseManager = new SQLITEDatabaseManager(this);
    try {
      // Forzar habilitación de SQLite para fallback
      getConfig().set("database.sqlite.enabled", true);
      sqliteDatabaseManager.connect();
      // Verificar si la conexión SQLite realmente funciona
      if (testDatabaseConnection(sqliteDatabaseManager)) {
        database = true;
        currentDatabaseType = "SQLite";
        getLogger().info("Usando SQLite como base de datos (fallback).");
      }
    } catch (Exception e) {
      getLogger().severe("No se pudo conectar a SQLite: " + e.getMessage());
    }

    if (!database) {
      getLogger().severe("ADVERTENCIA: No hay conexiones de base de datos disponibles!");
    }
  }

  // Método para probar la conexión de base de datos
  private boolean testDatabaseConnection(Object databaseManager) {
    try {
      if (databaseManager instanceof SQLDatabaseManager) {
        Connection conn = ((SQLDatabaseManager) databaseManager).getConnection();
        if (conn != null && !conn.isClosed() && conn.isValid(5)) {
          conn.close();
          return true;
        }
      } else if (databaseManager instanceof SQLITEDatabaseManager) {
        Connection conn = ((SQLITEDatabaseManager) databaseManager).getConnection();
        // Para SQLite, solo verificar que la conexión no esté cerrada
        // ya que isValid() no está implementado en el driver SQLite
        if (conn != null && !conn.isClosed()) {
          // Intentar ejecutar una consulta simple para verificar la conexión
          try {
            conn.createStatement().executeQuery("SELECT 1").close();
            conn.close();
            return true;
          } catch (Exception queryException) {
            if (!conn.isClosed()) {
              conn.close();
            }
            throw queryException;
          }
        }
      }
    } catch (Exception e) {
      getLogger().warning("Error probando conexión de base de datos: " + e.getMessage());
    }
    return false;
  }

  public boolean getIsDatabaseActivated() {
    return database;
  }

  public String getCurrentDatabaseType() {
    return currentDatabaseType;
  }

  public void setIsDatabaseActivated(boolean activated) {
    this.database = activated;
  }

  public Map<String, MatchPlayer> getDisconnectedPlayers() {
    return disconnectedPlayers;
  }

  public boolean isStatsCancel() {
    return isStatsCancel;
  }

  public void setStatsCancel(boolean cancel) {
    this.isStatsCancel = cancel;
  }

  // MatchBot
  public boolean isMatchBotEnabled() {
    return isMatchBotEnabled;
  }

  public void loadMatchBotConfig() {
    matchBotFile = new File(getDataFolder(), "matchbot.yml");

    if (!matchBotFile.exists()) {
      saveResource("matchbot.yml", false);
    }

    matchBotConfig = YamlConfiguration.loadConfiguration(matchBotFile);
    MatchBotConfig.loadConfig(matchBotConfig);

    getLogger().info("MatchBot configuration loaded successfully.");
  }

  public FileConfiguration getMatchBotConfig() {
    return matchBotConfig;
  }

  public void saveMatchBotConfig() {
    try {
      matchBotConfig.save(matchBotFile);
    } catch (IOException e) {
      getLogger().severe("Error al guardar matchbot.yml: " + e.getMessage());
    }
  }

  public void reloadMatchBotConfig() {
    matchBotConfig = YamlConfiguration.loadConfiguration(matchBotFile);
    MatchBotConfig.loadConfig(matchBotConfig);
  }

  public boolean reloadDatabase() {
    getLogger().info("Reloading database connection...");
    try {
      initializeDatabase();

      if (getIsDatabaseActivated()) {
        try {
          getLogger().info("DB reload successful, creating tables and precaching match ids.");
          config()
              .databaseTables()
              .getTables(TableType.ALL)
              .forEach(org.nicolie.towersforpgm.database.TableManager::createTable);
          org.nicolie.towersforpgm.database.TableManager.createHistoryTables();

          Set<String> tables = new HashSet<>();
          tables.addAll(this.config().databaseTables().getTables(TableType.ALL));
          MatchHistoryManager.preloadMatchIdCountersAsync(tables);
        } catch (Exception ex) {
          getLogger()
              .warning("Error creating tables/precaching after DB reload: " + ex.getMessage());
        }

        getLogger().info("Database reloaded and active (" + getCurrentDatabaseType() + ")");
        return true;
      } else {
        getLogger().warning("Database not active after reload. Check server logs for details.");
        return false;
      }
    } catch (Exception e) {
      getLogger().severe("Error reloading database: " + e.getMessage());
      return false;
    }
  }
}
