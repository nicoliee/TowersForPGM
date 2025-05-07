package org.nicolie.towersforpgm.utils;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nicolie.towersforpgm.TowersForPGM;

public class LanguageManager {
    private YamlConfiguration messagesConfig; // Configuración de mensajes
    public YamlConfiguration languageConfig; // Configuración de idioma
    public String language; // Idioma actual
    private TowersForPGM plugin; // Instancia del plugin

    public LanguageManager(TowersForPGM plugin) {
        this.plugin = plugin;
        this.language = plugin.getConfig().getString("language", "en"); // Idioma por defecto
        loadLanguage();
        loadMessages();
    }

    // Cargar mensajes desde language.yml
    private void loadLanguage() {
        InputStream languageStream = plugin.getResource("language.yml");
        if (languageStream == null) {
            plugin.getLogger().warning("No se pudo cargar el archivo de idioma.");
            return;
        }
        languageConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(languageStream, StandardCharsets.UTF_8));
    }

    // Cargar mensajes desde messages.yml
    private void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    // Recargar mensajes desde messages.yml
    public void reloadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (messagesFile.exists()) {
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        } else {
            plugin.saveResource("messages.yml", false);
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        }
    }

    // Obtener el idioma global del servidor desde config.yml
    private String getServerLanguage() {
        return plugin.getConfig().getString("language", "es"); // Por defecto en español
    }

    // Establecer el idioma global del servidor
    public void setLanguage(String language) {
        plugin.getConfig().set("language", language);
        plugin.saveConfig();
        loadLanguage();
    }

    // Obtener el mensaje desde language.yml en el idioma seleccionado con la clave dada
    public String getPluginMessage(String key) {
        String language = getServerLanguage();  // Obtener el idioma global del servidor
        String message = languageConfig.getString("messages." + language + "." + key);

        // Verificar si el mensaje es null y retornar un mensaje por defecto o de error
        if (message == null) {
            SendMessage.sendToAdmins("[Language] "+getPluginMessage("errors.messageNotFound").replace("{key}", key));
            return ChatColor.RED + "error";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // Obtener el mensaje configurable desde messages.yml con la clave dada
    public String getConfigurableMessage(String key) {
        String message = getMessagesConfig().getString(key);
        if (message == null) {
            SendMessage.sendToAdmins("[Messages] " + getPluginMessage("errors.messageNotFound").replace("{key}", key));
            return ChatColor.RED + "error";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // Método para obtener el archivo de configuración de mensajes
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
}
