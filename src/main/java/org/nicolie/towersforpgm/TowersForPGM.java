package org.nicolie.towersforpgm;

// import org.nicolie.towersforpgm.Vault.SetupVault;
import org.nicolie.towersforpgm.database.DatabaseManager;
import org.nicolie.towersforpgm.database.TableManager;
import org.nicolie.towersforpgm.draft.AvailablePlayers;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.PickInventory;
import org.nicolie.towersforpgm.draft.Teams;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import org.nicolie.towersforpgm.preparationTime.MatchConfig;
import org.nicolie.towersforpgm.preparationTime.Region;
import org.nicolie.towersforpgm.preparationTime.TorneoListener;
import org.nicolie.towersforpgm.refill.RefillManager;

import tc.oc.pgm.api.player.MatchPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;

public final class TowersForPGM extends JavaPlugin {
    private static TowersForPGM instance; // Instancia de la clase principal
    
// Messages.yml y Language.yml
    private YamlConfiguration messagesConfig; // Configuración de mensajes
    public YamlConfiguration languageConfig; // Configuración de idioma

// preparationTime
    private final Map<String, Region> regions = new HashMap<>(); // Mapa para almacenar las regiones definidas
    private Map<String, MatchConfig> matchConfigs = new HashMap<>(); // Mapa para almacenar MatchConfig por nombre de mundo
    private final TorneoListener torneoListener = new TorneoListener(this); // Listener de torneos
    private boolean preparationEnabled = true; // Protección de partida

// Base de datos
    private DatabaseManager databaseManager; // Administrador de la base de datos
    private boolean isDatabaseActivated = false; // Base de datos activada
    private final Map<String, MatchPlayer> disconnectedPlayers = new HashMap<>(); // Mapa para almacenar los jugadores

// Captains
    private AvailablePlayers availablePlayers;
    private Captains captains;
    private Draft draft; 
    private Teams teams;

// Refill
    private RefillManager refillManager; // Administrador de refill
    private File refillFile;
    private FileConfiguration refillConfig;

    @Override
    public void onEnable() {
        // Guardar la instancia de la clase principal
        instance = this;
        
        // Guardar la configuración por defecto
        saveDefaultConfig();
        saveDefaultMessages();
        loadLanguage();
        saveDefaultMessages();
        loadRegions();
        loadRefillConfig();
        ConfigManager.loadConfig();
        preparationEnabled = getConfig().getBoolean("preparationTime.enabled", false);
        
        // Inicializar el RefillManager
        refillManager = new RefillManager(this);


        // Base de datos
        ConfigManager.loadConfig();
        isDatabaseActivated = getConfig().getBoolean("database.enabled", false);
        if (isDatabaseActivated) {
            // Conectar a MySQL
            databaseManager = new DatabaseManager(this);
            databaseManager.connect();
        
            // Crear tablas al inicio si la base de datos está activada
            createTablesOnStartup();
        } else {
            getLogger().info(getPluginMessage("errors.databaseDisabled"));
        }

        // // Vault
        // SetupVault.setupVault();
        
        // if (SetupVault.getVaultEconomy() != null) {
        //     getLogger().info("Vault economy initialized successfully!");
        // } else {
        //     getLogger().warning("Vault economy could not be initialized. Make sure Vault plugin is installed.");
        // }

        // Inicializar el Match
        MatchManager matchManager = new MatchManager();

        // Inicializar el Draft
        availablePlayers = new AvailablePlayers(matchManager);
        captains = new Captains();
        teams = new Teams(matchManager);
        draft = new Draft(this, matchManager, captains, availablePlayers, teams);
        PickInventory pickInventory = new PickInventory(this, draft, captains, availablePlayers);
        getServer().getPluginManager().registerEvents(pickInventory, this);
        // Registrar comandos
        Commands commandManager = new Commands(this);
        commandManager.registerCommands(availablePlayers, captains, draft, matchManager, pickInventory, refillManager, teams, torneoListener);

        // Registrar eventos
        Events eventManager = new Events(this);
        eventManager.registerEvents(availablePlayers, captains, draft, matchManager, pickInventory, refillManager, teams, torneoListener);
    }
        
    @Override
    public void onDisable() {
        // Desconectar de MySQL
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
    }

    public static TowersForPGM getInstance() {
        return instance;
    }

// Mensajes de language.yml y messages.yml
    // Cargar mensajes desde language.yml
    private void loadLanguage() {
        InputStream languageStream = getResource("language.yml");
        if (languageStream == null) {
            getLogger().warning("No se pudo cargar el archivo de idioma.");
            return;
        }
        languageConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(languageStream, StandardCharsets.UTF_8));
    }

    // Obtener el idioma global del servidor desde config.yml
    private String getServerLanguage() {
        return getConfig().getString("language", "es"); // Por defecto en español
    }

    // Establecer el idioma global del servidor
    public void setLanguage(String language) {
        getConfig().set("language", language);
        saveConfig();
        loadLanguage();
    }

    // Obtener el mensaje desde language.yml en el idioma seleccionado con la clave dada
    public String getPluginMessage(String key) {
        String language = getServerLanguage();  // Obtener el idioma global del servidor
        String message = languageConfig.getString("messages." + language + "." + key);

        // Verificar si el mensaje es null y retornar un mensaje por defecto o de error
        if (message == null) {
            SendMessage.sendToAdmins(getPluginMessage("errors.messageNotFound").replace("{key}", key));
            return ChatColor.RED + getPluginMessage("messageNotFoundToPlayers");
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // Obtener el mensaje configurable desde messages.yml con la clave dada
    public String getConfigurableMessage(String key) {
        String message = getMessagesConfig().getString(key);
        if (message == null) {
            SendMessage.sendToAdmins(getPluginMessage("errors.messageNotFound").replace("{key}", key));
            return ChatColor.RED + "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // Método para guardar el archivo de configuración de mensajes
    public void saveDefaultMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    // Método para obtener el archivo de configuración de mensajes
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

// Preparation Time
    // Método para cargar las regiones desde el archivo de configuración
    private void loadRegions() {
        ConfigurationSection section = getConfig().getConfigurationSection("preparationTime.maps");
        if (section == null) {
            getLogger().warning(getPluginMessage("errors.preparationTimeNotFound"));
            return;
        }

        for (String regionName : section.getKeys(false)) {
            ConfigurationSection regionSection = section.getConfigurationSection(regionName);
            if (regionSection == null) continue;

            // Obtener P1 y P2
            Location p1 = parseLocation(regionSection.getString("P1"));
            Location p2 = parseLocation(regionSection.getString("P2"));

            if (p1 == null || p2 == null) {
                getLogger().warning(getPluginMessage("errors.regionError") + regionName);
                continue;
            }

            // Obtener Timer y Haste
            int timer = regionSection.getInt("Timer", 0);
            int haste = regionSection.getInt("Haste", 0);

            // Crear la región y almacenarla
            Region region = new Region(p1, p2, timer, haste);
            regions.put(regionName, region);
            getLogger().info(getPluginMessage("errors.regionLoaded") + regionName);
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
            return new Location(Bukkit.getWorlds().get(0), x, y, z); // Usa el mundo por defecto
        } catch (NumberFormatException e) {
            String message = getPluginMessage("errors.invalidCoords");
            getLogger().warning((message != null ? message : "Invalid coordinates: ") + input);
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

// Captains
    // Métodos para añadir y eliminar jugadores desconectados
    public void addDisconnectedPlayer(String username, MatchPlayer player) {
        disconnectedPlayers.put(username, player);
    }

    public void removeDisconnectedPlayer(String username) {
        disconnectedPlayers.remove(username);
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

    public FileConfiguration getRefillConfig() {
        return refillConfig;
    }

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