package org.nicolie.towersforpgm.utils;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nicolie.towersforpgm.TowersForPGM;

public class LanguageManager {

  private static TowersForPGM plugin;
  private static String currentLanguage;
  private static final Map<String, Map<String, Object>> languageCache = new HashMap<>();
  private static List<String> availableLanguages = new ArrayList<>();

  public static void initialize(TowersForPGM pluginInstance) {
    plugin = pluginInstance;
    currentLanguage = plugin.getConfig().getString("language", "es");
    loadAllLanguages();
  }

  public static void reload() {
    currentLanguage = plugin.getConfig().getString("language", "es");
    loadAllLanguages();
  }

  public static String message(String key) {
    Map<String, Object> langData = languageCache.get(currentLanguage);
    if (langData == null) return key;

    Object value = langData.get(key);
    return value != null ? ChatColor.translateAlternateColorCodes('&', value.toString()) : key;
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
    availableLanguages = getAvailableLanguages();
    languageCache.clear();
    for (String lang : availableLanguages) {
      loadLanguage(lang);
    }
  }

  private static void loadLanguage(String language) {
    Map<String, Object> data = new HashMap<>();

    List<String> files = listLanguageFiles(language);
    for (String file : files) {
      // Prefer overrides in the plugin data folder
      File override = new File(plugin.getDataFolder(), "languages/" + language + "/" + file);
      if (override.exists() && override.isFile()) {
        try {
          YamlConfiguration config = YamlConfiguration.loadConfiguration(override);
          flatten(config, file.replace(".yml", ""), data);
        } catch (Exception e) {
          plugin
              .getLogger()
              .warning("Failed to load override " + override.getPath() + ": " + e.getMessage());
        }
        continue;
      }

      InputStream stream = plugin.getResource("languages/" + language + "/" + file);
      if (stream == null) continue;

      try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(reader);
        flatten(config, file.replace(".yml", ""), data);
      } catch (Exception e) {
        plugin
            .getLogger()
            .warning("Failed to load resource " + language + "/" + file + ": " + e.getMessage());
      }
    }

    languageCache.put(language, data);
  }

  private static List<String> listLanguageFiles(String language) {
    List<String> files = new ArrayList<>();
    Set<String> seen = new HashSet<>();

    File langDir = new File(plugin.getDataFolder(), "languages/" + language);
    if (langDir.exists() && langDir.isDirectory()) {
      File[] list = langDir.listFiles((d, name) -> name.toLowerCase().endsWith(".yml"));
      if (list != null) {
        for (File f : list) {
          if (seen.add(f.getName())) files.add(f.getName());
        }
      }
    }

    String prefix = "languages/" + language + "/";
    try {
      URL codeSource = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
      if (codeSource != null) {
        File code = new File(codeSource.toURI());
        if (code.isFile()) {
          try (JarFile jar = new JarFile(code)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
              JarEntry e = entries.nextElement();
              String name = e.getName();
              if (!e.isDirectory()
                  && name.startsWith(prefix)
                  && name.toLowerCase().endsWith(".yml")) {
                String base = name.substring(prefix.length());
                if (base.length() > 0 && seen.add(base)) files.add(base);
              }
            }
          }
        } else {
          URL res = plugin.getClass().getClassLoader().getResource(prefix);
          if (res != null && res.getProtocol().equals("file")) {
            File dir = new File(res.toURI());
            File[] list = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".yml"));
            if (list != null) for (File f : list) if (seen.add(f.getName())) files.add(f.getName());
          }
        }
      }
    } catch (Exception e) {
      plugin.getLogger().warning("Could not scan plugin jar for language files: " + e.getMessage());
    }

    return files;
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

  public static List<String> getSupportedLanguages() {
    return new ArrayList<>(availableLanguages);
  }

  private static List<String> getAvailableLanguages() {
    Set<String> seen = new HashSet<>();

    File languagesDir = new File(plugin.getDataFolder(), "languages");
    if (languagesDir.exists() && languagesDir.isDirectory()) {
      File[] dirs = languagesDir.listFiles(File::isDirectory);
      if (dirs != null) {
        for (File d : dirs) {
          seen.add(d.getName());
        }
      }
    }

    String prefix = "languages/";
    try {
      URL codeSource = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
      if (codeSource != null) {
        File code = new File(codeSource.toURI());
        if (code.isFile()) {
          try (JarFile jar = new JarFile(code)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
              JarEntry e = entries.nextElement();
              String name = e.getName();
              if (e.isDirectory() && name.startsWith(prefix) && !name.equals(prefix)) {
                String lang = name.substring(prefix.length()).split("/")[0];
                seen.add(lang);
              }
            }
          }
        } else {
          URL res = plugin.getClass().getClassLoader().getResource(prefix);
          if (res != null && res.getProtocol().equals("file")) {
            File dir = new File(res.toURI());
            File[] list = dir.listFiles(File::isDirectory);
            if (list != null) for (File d : list) seen.add(d.getName());
          }
        }
      }
    } catch (Exception e) {
      plugin.getLogger().warning("Could not scan for available languages: " + e.getMessage());
    }

    return new ArrayList<>(seen);
  }
}
