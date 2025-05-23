package org.nicolie.towersforpgm.update;

import org.nicolie.towersforpgm.utils.SendMessage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoUpdate {

    private final JavaPlugin plugin;
    private static final String url = "https://api.github.com/repos/nicoliee/TowersForPGM/releases/latest";
    public AutoUpdate(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void checkForUpdates() {
        String currentVersion = plugin.getDescription().getVersion();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Abrir conexión a la API de GitHub
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                // Leer la respuesta de la API
                StringBuilder responseBuilder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                }
                String response = responseBuilder.toString();

                // Obtener la última versión y la URL de descarga del JSON
                String latestVersion = parseVersionFromJson(response);
                if (latestVersion == null) {
                    plugin.getLogger().severe("JSON error: Could not obtain the latest version.");
                    SendMessage.sendToAdmins("§8[§bTowersForPGM§8] §cError.");
                    return;
                }

                if (compareVersions(currentVersion, latestVersion) >= 0) {
                    // Si la versión actual es igual o mayor que la última
                    plugin.getLogger().info("TowersForPGM is up to date.");
                    SendMessage.sendToAdmins("§8[§bTowersForPGM§8] §aUp to date.");
                } else {
                    plugin.getLogger().info("New version available: " + latestVersion);
                    SendMessage.sendToAdmins("§8[§bTowersForPGM§8] §eNew version available: §7v" + latestVersion);

                    String downloadUrl = parseDownloadUrlFromJson(response);
                    if (downloadUrl == null) {
                        plugin.getLogger().severe("URL error: Could not obtain the download URL.");
                        SendMessage.sendToAdmins("§8[§bTowersForPGM§8] §cError.");
                        return;
                    }

                    // Descargar la nueva versión
                    SendMessage.sendToAdmins("§8[§bTowersForPGM§8] §eDownloading...");
                    downloadNewVersion(downloadUrl);
                    plugin.getLogger().info("New version downloaded.");
                    SendMessage.sendToAdmins("§8[§bTowersForPGM§8] §aDownloaded. Reload the server.");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error: " + e.getMessage());
                SendMessage.sendToAdmins("§8[§bTowersForPGM§8] §cError.");
            }
        });
    }

    public void forceUpdate() {    
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Abrir conexión a la API de GitHub
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
    
                // Leer la respuesta de la API
                StringBuilder responseBuilder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                }
                String response = responseBuilder.toString();
    
                // Obtener la URL de descarga del JSON
                String downloadUrl = parseDownloadUrlFromJson(response);
                if (downloadUrl == null) {
                    plugin.getLogger().severe("URL error: Could not obtain the download URL.");
                    SendMessage.sendToAdmins("§8[§bTowersForPGM§8] §cError.");
                    return;
                }
    
                // Descargar la nueva versión
                downloadNewVersion(downloadUrl);
                plugin.getLogger().info("New version downloaded.");
                SendMessage.sendToAdmins("§8[§bTowersForPGM§8] §aDownloaded. Reload the server.");
    
            } catch (Exception e) {
                plugin.getLogger().severe("Error: " + e.getMessage());
                SendMessage.sendToAdmins("§8[§bTowersForPGM§8] §cError.");
            }
        });
    }

    private String parseVersionFromJson(String json) {
        try {
            // Buscar el índice de "tag_name"
            int tagNameIndex = json.indexOf("\"tag_name\":\"");
            if (tagNameIndex == -1) return null;
    
            // Calcular las posiciones de inicio y fin del valor de "tag_name"
            int valueStart = tagNameIndex + "\"tag_name\":\"".length();
            int valueEnd = json.indexOf("\"", valueStart);
            if (valueEnd == -1) return null;
    
            // Extraer y retornar el valor
            return json.substring(valueStart, valueEnd);
        } catch (Exception e) {
            plugin.getLogger().severe("Error: JSON " + e.getMessage());
            return null;
        }
    }

    private String parseDownloadUrlFromJson(String json) {
        try {
            // Buscar el índice del array de "assets"
            int assetsStartIndex = json.indexOf("\"assets\":");
            if (assetsStartIndex == -1) return null;

            // Cortar desde "assets" en adelante
            String assetsSubstring = json.substring(assetsStartIndex);

            // Buscar el primer "browser_download_url"
            int urlStartIndex = assetsSubstring.indexOf("\"browser_download_url\":\"");
            if (urlStartIndex == -1) return null;

            // Cortar desde el inicio de la URL
            int urlValueStart = urlStartIndex + "\"browser_download_url\":\"".length();
            int urlEndIndex = assetsSubstring.indexOf("\"", urlValueStart);
            if (urlEndIndex == -1) return null;

            // Extraer y retornar la URL
            return assetsSubstring.substring(urlValueStart, urlEndIndex);
        } catch (Exception e) {
            plugin.getLogger().severe("Error: JSON " + e.getMessage());
            return null;
        }
    }


    private void downloadNewVersion(String downloadUrl) {
        try {
            // Configurar la ruta para guardar el nuevo archivo .jar
            File pluginFile = new File("plugins/" + plugin.getName() + ".jar");

            // Abrir una conexión para descargar el archivo
            try (BufferedInputStream in = new BufferedInputStream(new URL(downloadUrl).openStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(pluginFile)) {
                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }
            }

            plugin.getLogger().info("El nuevo archivo del plugin ha sido descargado con éxito.");
        } catch (IOException e) {
            plugin.getLogger().severe("Error al descargar la nueva versión: " + e.getMessage());
        }
    }

    /**
     * Compara dos versiones en formato "x.y.z".
     * Devuelve:
     * - Un valor negativo si `version1` < `version2`
     * - Cero si `version1` == `version2`
     * - Un valor positivo si `version1` > `version2`
     */
    private int compareVersions(String version1, String version2) {
        // Elimina el prefijo "v" u otros caracteres no numéricos
        version1 = version1.replaceAll("[^0-9.]", "");
        version2 = version2.replaceAll("[^0-9.]", "");

        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int v1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int v2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (v1 != v2) {
                return v1 - v2;
            }
        }
        return 0; // Las versiones son iguales
    }
}

