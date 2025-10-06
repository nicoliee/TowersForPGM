package org.nicolie.towersforpgm.utils;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nicolie.towersforpgm.TowersForPGM;

public class LanguageManager {
  private static final String[] SUPPORTED_LANGUAGES = {"en", "es"};
  private static final String[] LANGUAGE_FILES = {
    "system.yml", "errors.yml", "draft.yml", "stats.yml", "ranked.yml",
    "preparation.yml", "refill.yml", "region.yml", "ready.yml", "join.yml",
    "privatematch.yml", "matchbot.yml", "gui.yml"
  };

  private static TowersForPGM plugin;
  private static String currentLanguage;
  private static final Map<String, Map<String, Object>> languageCache = new HashMap<>();
  private static YamlConfiguration messagesConfig;

  public static void initialize(TowersForPGM pluginInstance) {
    plugin = pluginInstance;
    currentLanguage = plugin.getConfig().getString("language", "es");
    loadAllLanguages();
    loadMessages();
  }

  public static void reload() {
    currentLanguage = plugin.getConfig().getString("language", "es");
    loadAllLanguages();
    loadMessages();
  }

  public static String langMessage(String key) {
    Map<String, Object> langData = languageCache.get(currentLanguage);
    if (langData == null) return key;

    Object value = langData.get(key);
    return value != null ? ChatColor.translateAlternateColorCodes('&', value.toString()) : key;
  }

  public static String message(String key) {
    if (messagesConfig != null) {
      String msg = messagesConfig.getString(key);
      if (msg != null) return ChatColor.translateAlternateColorCodes('&', msg);
    }
    return key;
  }

  public static void setLanguage(String language) {
    if (languageCache.containsKey(language)) {
      currentLanguage = language;
      plugin.getConfig().set("language", language);
      plugin.saveConfig();
    }
  }

  public static String getCurrentLanguage() {
    return currentLanguage;
  }

  private static void loadAllLanguages() {
    languageCache.clear();
    for (String lang : SUPPORTED_LANGUAGES) {
      loadLanguage(lang);
    }
  }

  private static void loadLanguage(String language) {
    Map<String, Object> data = new HashMap<>();

    for (String file : LANGUAGE_FILES) {
      InputStream stream = plugin.getResource("languages/" + language + "/" + file);
      if (stream == null) continue;

      try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(reader);
        flatten(config, file.replace(".yml", ""), data);
      } catch (Exception e) {
        plugin.getLogger().warning("Failed to load " + language + "/" + file);
      }
    }

    languageCache.put(language, data);
  }

  private static void flatten(
      ConfigurationSection section, String prefix, Map<String, Object> target) {
    for (String key : section.getKeys(false)) {
      String fullKey = prefix + "." + key;
      Object value = section.get(key);

      if (value instanceof ConfigurationSection) {
        flatten((ConfigurationSection) value, fullKey, target);
      } else {
        target.put(fullKey, value);
      }
    }
  }

  private static void loadMessages() {
    File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
    if (messagesFile.exists()) {
      messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }
  }
}
