package org.nicolie.towersforpgm;

import org.nicolie.towersforpgm.database.DatabaseManager;
import org.nicolie.towersforpgm.database.TableManager;
import org.nicolie.towersforpgm.draft.AvailablePlayers;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.PickInventory;
import org.nicolie.towersforpgm.draft.Teams;
import org.nicolie.towersforpgm.draft.Utilities;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.preparationTime.MatchConfig;
import org.nicolie.towersforpgm.preparationTime.Region;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.update.AutoUpdate;

import tc.oc.pgm.api.player.MatchPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;
import org.bukkit.World;

public final class TowersForPGM extends JavaPlugin {
    private static TowersForPGM instance; // Instancia de la clase principal

// preparationTime
    private final Map<String, Region> regions = new HashMap<>(); // Mapa para almacenar las regiones definidas
    private Map<String, MatchConfig> matchConfigs = new HashMap<>(); // Mapa para almacenar MatchConfig por nombre de mundo
    private PreparationListener preparationListener;
    private boolean preparationEnabled = true; // Protección de partida

// Base de datos
    private DatabaseManager databaseManager; // Administrador de la base de datos
    private boolean isDatabaseActivated = false; // Base de datos activada
    private final Map<String, MatchPlayer> disconnectedPlayers = new HashMap<>(); // Mapa para almacenar los jugadores
    private boolean isStatsCancel = false; // Variable para cancelar el envío de estadísticas

// Captains
    private AvailablePlayers availablePlayers;
    private Captains captains;
    private Draft draft; 
    private Teams teams;
    private PickInventory pickInventory;
    private Utilities utilities; 

// Refill
    private RefillManager refillManager; // Administrador de refill
    private File refillFile;
    private FileConfiguration refillConfig;

// Language
    private LanguageManager languageManager; // Instancia del LanguageManager

    @Override
    public void onEnable() {
        // Guardar la instancia de la clase principal
        instance = this;
        
        // Guardar la configuración por defecto
        saveDefaultConfig();
        loadRegions();
        loadRefillConfig();
        ConfigManager.loadConfig();
        preparationEnabled = getConfig()
                    .getBoolean("preparationTime.enabled", false);

        // Inicializar LanguageManager
        languageManager = new LanguageManager(this);

        // Inicializar el RefillManager
        refillManager = new RefillManager(languageManager);

        // Inicializar el TorneoListener
        preparationListener = new PreparationListener(languageManager);

        // Base de datos
        isDatabaseActivated = getConfig()
                    .getBoolean("database.enabled", false);
        if (isDatabaseActivated) {
            // Conectar a MySQL
            databaseManager = new DatabaseManager(this);
            databaseManager.connect();
        
            // Crear tablas al inicio si la base de datos está activada
            createTablesOnStartup();
        } else {getLogger().info("Database is disabled.");}

        // Inicializar el Match
        MatchManager matchManager = new MatchManager();

        // Inicializar el Draft
        availablePlayers = new AvailablePlayers(matchManager);
        captains = new Captains();
        teams = new Teams(matchManager);
        utilities = new Utilities(
            availablePlayers, captains, languageManager);
        draft = new Draft(
            captains, availablePlayers, teams, 
            languageManager, matchManager, utilities);
        pickInventory = new PickInventory(
            draft, captains, availablePlayers, teams, languageManager);
        getServer().getPluginManager().registerEvents(pickInventory, this);
        
        // Registrar comandos
        Commands commandManager = new Commands(this);
        commandManager.registerCommands(
            availablePlayers, captains, draft, languageManager, 
            pickInventory, refillManager, teams, preparationListener);

        // Registrar eventos
        Events eventManager = new Events(this);
        eventManager.registerEvents(
            availablePlayers, captains, draft, matchManager, languageManager,
            pickInventory, refillManager, teams, preparationListener);

        // Verificar actualizaciones
        AutoUpdate updateChecker = new AutoUpdate(this);
        if (getConfig().getBoolean("autoupdate", false)) {
            updateChecker.checkForUpdates();
        }
    }
        
    @Override
    public void onDisable() {
        // Desconectar de MySQL
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
    }

    public static TowersForPGM getInstance() {return instance;}
    public LanguageManager getLanguageManager() {return languageManager;}
    public void updateInventories(){pickInventory.updateAllInventories();}
    public void giveitem(World world){pickInventory.giveItemToPlayers(world);}
    public void removeItem(World world){pickInventory.removeItemToPlayers(world);}
    public Draft getDraft() {return draft;}

    public void giveItem(Player player){
        if(player != null){pickInventory.giveItemToPlayer(player);}
    }

// Preparation Time
    // Método para cargar las regiones desde el archivo de configuración
    private void loadRegions() {
        ConfigurationSection section = getConfig().getConfigurationSection("preparationTime.maps");
        if (section == null) {
            getLogger().warning("Preparation time maps not found.");
            return;
        }

        for (String regionName : section.getKeys(false)) {
            ConfigurationSection regionSection = 
                section.getConfigurationSection(regionName);
            if (regionSection == null) continue;

            // Obtener P1 y P2
            Location p1 = parseLocation(
                regionSection.getString("P1"));
            Location p2 = parseLocation(
                regionSection.getString("P2"));

            if (p1 == null || p2 == null) {
                getLogger().warning("Error loading region: " + regionName);
                continue;
            }

            // Obtener Timer y Haste
            int timer = regionSection.getInt("Timer", 0);
            int haste = regionSection.getInt("Haste", 0);

            // Crear la región y almacenarla
            Region region = new Region(p1, p2, timer, haste);
            regions.put(regionName, region);
            getLogger().info("Region loaded: " + regionName);
        }
    }

    // Método para convertir una cadena de texto en una ubicación
    private Location parseLocation(String input) {
        if (input == null) return null;
        String[] parts = input.split(", ");
        if (parts.length != 3) return null;

        try {
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            return new Location(Bukkit.getWorlds().get(0), x, y, z);
        } catch (NumberFormatException e) {
            getLogger().warning("Invalid coordinates: " + input);
            return null;
        }
    }

    // Método para obtener las regiones cargadas
    public Map<String, Region> getRegions() {
        return regions;
    }

    // Método para almacenar la configuración del partido
    public void storeMatchConfig(String worldName, MatchConfig matchConfig) {
        matchConfigs.put(worldName, matchConfig);
    }

    // Método para eliminar la configuración del partido
    public void removeMatchConfig(String worldName) {
        matchConfigs.remove(worldName);
    }

    // Método para obtener la configuración del partido
    public MatchConfig getMatchConfig(String worldName) {
        return matchConfigs.get(worldName);
    }

    // Método para obtener el booleano de protección de partida
    public boolean isPreparationEnabled() {
        return preparationEnabled;
    }

    // Método para establecer el booleano de protección de partida
    public void setPreparationEnabled(boolean enabled) {
        this.preparationEnabled = enabled;
    }

// Base de datos
    // Método para obtener el administrador de la base de datos
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    // Método para crear tablas al inicio
    private void createTablesOnStartup() {
        List<String> tables = ConfigManager.getTables();
        for (String table : tables) {
            TableManager.createTable(table);
        }
    }

    // Método para obtener el booleano de activación de la base de datos
    public boolean getIsDatabaseActivated() {
        return isDatabaseActivated;
    }

    // Método para establecer el booleano de activación de la base de datos
    public void setIsDatabaseActivated(boolean activated) {
        this.isDatabaseActivated = activated;
    }

    // Método para obtener los jugadores desconectados
    public Map<String, MatchPlayer> getDisconnectedPlayers() {
        return disconnectedPlayers;
    }

// Stats
    // Métodos para añadir y eliminar jugadores desconectados
    public void addDisconnectedPlayer(String username, MatchPlayer player) {
        disconnectedPlayers.put(username, player);
    }

    public void removeDisconnectedPlayer(String username) {
        disconnectedPlayers.remove(username);
    }

    // Método para obtener el booleano de cancelación de estadísticas
    public boolean isStatsCancel() {
        return isStatsCancel;
    }

    public void setStatsCancel(boolean cancel) {
        this.isStatsCancel = cancel;
    }
    
// Refill
    public void loadRefillConfig() {
        refillFile = new File(getDataFolder(), "refill.yml");
        
        // Si el archivo no existe, crearlo desde recursos
        if (!refillFile.exists()) {
            saveResource("refill.yml", false);
        }

        refillConfig = YamlConfiguration.loadConfiguration(refillFile);
    }

    public FileConfiguration getRefillConfig() {return refillConfig;}
    public RefillManager getRefillManager() {return refillManager;}

    public void saveRefillConfig() {
        try {
            refillConfig.save(refillFile);
        } catch (IOException e) {
            getLogger().severe("Error al guardar refill.yml: " + e.getMessage());
        }
    }

    public void reloadRefillConfig() {
        refillConfig = YamlConfiguration.loadConfiguration(refillFile);
    }
}