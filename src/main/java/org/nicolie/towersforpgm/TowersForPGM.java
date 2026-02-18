package org.nicolie.towersforpgm;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.nicolie.towersforpgm.commands.ranked.ForfeitCommand;
import org.nicolie.towersforpgm.commands.ranked.RankedCommand;
import org.nicolie.towersforpgm.commands.ranked.TagCommand;
import org.nicolie.towersforpgm.commands.ranked.UnlinkCommand;
import org.nicolie.towersforpgm.configs.tables.TableType;
import org.nicolie.towersforpgm.database.MatchHistoryManager;
import org.nicolie.towersforpgm.database.TableManager;
import org.nicolie.towersforpgm.database.sql.SQLDatabaseManager;
import org.nicolie.towersforpgm.database.sqlite.SQLITEDatabaseManager;
import org.nicolie.towersforpgm.draft.components.DraftDisplayManager;
import org.nicolie.towersforpgm.draft.components.DraftPickManager;
import org.nicolie.towersforpgm.draft.components.DraftState;
import org.nicolie.towersforpgm.draft.components.DraftTurnManager;
import org.nicolie.towersforpgm.draft.components.PicksGUI;
import org.nicolie.towersforpgm.draft.core.AvailablePlayers;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Draft;
import org.nicolie.towersforpgm.draft.core.Matchmaking;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.draft.core.Utilities;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.commands.AutocompleteHandler;
import org.nicolie.towersforpgm.matchbot.commands.stats.StatsCommand;
import org.nicolie.towersforpgm.matchbot.commands.top.TopCommand;
import org.nicolie.towersforpgm.matchbot.commands.top.TopPaginationListener;
import org.nicolie.towersforpgm.matchbot.rankeds.listeners.QueueJoinListener;
import org.nicolie.towersforpgm.matchbot.rankeds.listeners.RankedFinishListener;
import org.nicolie.towersforpgm.matchbot.rankeds.listeners.RankedListener;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.utils.Commands;
import org.nicolie.towersforpgm.utils.Events;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.match.Match;
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

  // Captains
  private AvailablePlayers availablePlayers;
  private Captains captains;
  private Draft draft;
  private Teams teams;
  private PicksGUI pickInventory;
  private Utilities utilities;

  // Refill
  private RefillManager refillManager; // Administrador de refill

  // MatchBot (opcional)
  private boolean isMatchBotEnabled = false; // Variable para verificar si MatchBot está habilitado
  private File matchBotFile;
  private FileConfiguration matchBotConfig;

  @Override
  public void onEnable() {
    instance = this;
    saveDefaultConfig();
    this.ConfigManager = new org.nicolie.towersforpgm.configs.ConfigManager(this);
    LanguageManager.initialize(this);
    refillManager = new RefillManager();
    preparationListener = new PreparationListener();
    initializeDatabase();

    if (database) {
      createTablesOnStartup();
      // Precargar contadores de matchId en MatchHistoryManager para las tablas configuradas
      try {
        java.util.Set<String> tables = new java.util.HashSet<>();
        tables.addAll(this.config().databaseTables().getTables(TableType.ALL));
        MatchHistoryManager.preloadMatchIdCountersAsync(tables).thenRun(() -> getLogger()
            .info("MatchHistoryManager precargado para tablas: " + tables.size()));
      } catch (Exception e) {
        getLogger().warning("Error precargando MatchHistoryManager: " + e.getMessage());
      }
    } else {
      getLogger().warning("No database connections available!");
    }

    availablePlayers = new AvailablePlayers(this.ConfigManager);
    teams = new Teams();
    captains = new Captains();
    utilities = new Utilities(this.ConfigManager, availablePlayers, captains);

    org.nicolie.towersforpgm.rankeds.DisconnectManager.setTeams(teams);
    DraftState draftState = new DraftState();
    DraftTurnManager draftTurnManager = new DraftTurnManager(draftState, captains, teams);
    DraftPickManager draftPickManager = new DraftPickManager(
        this,
        this.ConfigManager,
        draftState,
        captains,
        availablePlayers,
        teams,
        utilities,
        draftTurnManager);
    DraftDisplayManager draftDisplayManager =
        new DraftDisplayManager(this, this.ConfigManager, captains, teams, utilities);
    draftPickManager.setDisplayManager(draftDisplayManager);
    draftDisplayManager.setPickManager(draftPickManager);

    draft = new Draft(
        this,
        this.ConfigManager,
        draftState,
        draftTurnManager,
        draftPickManager,
        draftDisplayManager,
        captains,
        availablePlayers,
        teams,
        utilities);
    pickInventory = new PicksGUI(this, draft, captains, availablePlayers, teams);
    getServer().getPluginManager().registerEvents(pickInventory, this);

    // Registrar listeners para reroll
    getServer()
        .getPluginManager()
        .registerEvents(new org.nicolie.towersforpgm.draft.listeners.RerollItemListener(), this);
    getServer().getPluginManager().registerEvents(draft.getRerollOptionsGUI(), this);

    // Inicializar el matchmaking
    Matchmaking matchmaking =
        new Matchmaking(this.ConfigManager, availablePlayers, captains, teams, utilities);

    // Registrar Rankeds
    Queue queue = new Queue(draft, matchmaking, teams);
    getCommand("ranked").setExecutor(new RankedCommand(queue, utilities));
    getCommand("forfeit").setExecutor(new ForfeitCommand(teams));
    getCommand("tag").setExecutor(new TagCommand());
    getCommand("unlink").setExecutor(new UnlinkCommand());
    // Registrar comandos
    Commands commandManager = new Commands(this);
    commandManager.registerCommands(
        availablePlayers,
        captains,
        draft,
        matchmaking,
        pickInventory,
        refillManager,
        teams,
        preparationListener);

    // Registrar eventos
    Events eventManager = new Events(this);
    eventManager.registerEvents(
        availablePlayers,
        captains,
        draft,
        queue,
        pickInventory,
        refillManager,
        teams,
        preparationListener);

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

        // Registrar listener del queue de voz
        QueueJoinListener.register();

        // Registrar listeners de gestión de canales de voz para ranked
        getServer().getPluginManager().registerEvents(new RankedListener(), this);
        getServer().getPluginManager().registerEvents(new RankedFinishListener(), this);

        // Limpiar canales de voz [Ranked] al iniciar
        org.bukkit.Bukkit.getScheduler()
            .runTaskLater(
                this,
                () -> {
                  org.nicolie.towersforpgm.matchbot.rankeds.VoiceChannelManager
                      .cleanupRankedChannelsOnStartup();
                },
                40L);
      }
    }
  }

  public org.nicolie.towersforpgm.configs.ConfigManager config() {
    return this.ConfigManager;
  }

  @Override
  public void onDisable() {
    // Limpiar canales de voz [Ranked] al cerrar el plugin
    if (isMatchBotEnabled && MatchBotConfig.isVoiceChatEnabled()) {
      org.nicolie.towersforpgm.matchbot.rankeds.VoiceChannelManager
          .cleanupRankedChannelsOnStartup();
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

  public void updateInventories() {
    pickInventory.updateAllInventories();
  }

  public void giveitem(World world) {
    pickInventory.giveItemToPlayers(world);
  }

  public void giveItemToMatch(Match match) {
    pickInventory.giveItemToMatch(match);
  }

  public void removeItem(World world) {
    pickInventory.removeItemToPlayers(world);
  }

  public Draft getDraft() {
    return draft;
  }

  public org.nicolie.towersforpgm.draft.core.AvailablePlayers getAvailablePlayers() {
    return availablePlayers;
  }

  public void giveItem(Player player) {
    if (player != null) {
      pickInventory.giveItemToPlayer(player);
    }
  }

  // Base de datos
  public SQLDatabaseManager getMySQLDatabaseManager() {
    return mysqlDatabaseManager;
  }

  public SQLITEDatabaseManager getSQLiteDatabaseManager() {
    return sqliteDatabaseManager;
  }

  public java.sql.Connection getDatabaseConnection() throws java.sql.SQLException {
    if ("MySQL".equals(currentDatabaseType) && mysqlDatabaseManager != null) {
      return mysqlDatabaseManager.getConnection();
    } else if ("SQLite".equals(currentDatabaseType) && sqliteDatabaseManager != null) {
      return sqliteDatabaseManager.getConnection();
    }
    throw new java.sql.SQLException("No hay conexiones de base de datos disponibles");
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
        java.sql.Connection conn = ((SQLDatabaseManager) databaseManager).getConnection();
        if (conn != null && !conn.isClosed() && conn.isValid(5)) {
          conn.close();
          return true;
        }
      } else if (databaseManager instanceof SQLITEDatabaseManager) {
        java.sql.Connection conn = ((SQLITEDatabaseManager) databaseManager).getConnection();
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

  // Stats
  public void addDisconnectedPlayer(String username, MatchPlayer player) {
    disconnectedPlayers.put(username, player);
  }

  public void removeDisconnectedPlayer(String username) {
    disconnectedPlayers.remove(username);
  }

  public boolean isStatsCancel() {
    return isStatsCancel;
  }

  public void setStatsCancel(boolean cancel) {
    this.isStatsCancel = cancel;
  }

  // Refill
  public RefillManager getRefillManager() {
    return refillManager;
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

          java.util.Set<String> tables = new java.util.HashSet<>();
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
