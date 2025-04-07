package org.nicolie.towersforpgm.commands;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.update.AutoUpdate;
import org.nicolie.towersforpgm.utils.SendMessage;

public class TowersForPGMCommand implements CommandExecutor, TabCompleter {
    private final TowersForPGM plugin;

    public TowersForPGMCommand(TowersForPGM plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPluginMessage("errors.noPlayer"));
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            SendMessage.sendToPlayer(player, "§8[§bTowersForPGM§8] §7Version: " + plugin.getDescription().getVersion());
            return true;
        }

        String argument = args[0].toLowerCase();
        AutoUpdate updateChecker = new AutoUpdate(plugin);
        switch (argument) {
            case "forceUpdate":
                updateChecker.forceUpdate();
            case "setlanguage":
                if (args.length < 2) {
                    SendMessage.sendToPlayer(player, plugin.getPluginMessage("TowersForPGM.noLanguage"));
                    return true;
                }
                String language = args[1].toLowerCase();
                if (!language.equals("en") && !language.equals("es")) {
                    SendMessage.sendToPlayer(player, plugin.getPluginMessage("TowersForPGM.invalidLanguage"));
                    return true;
                }
                plugin.setLanguage(language);
                SendMessage.sendToPlayer(player, plugin.getPluginMessage("TowersForPGM.languageSet"));
                return true;

            case "reloadmessages":
                File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
                SendMessage.sendToPlayer(player, plugin.getPluginMessage("messages.reloadStart"));

                if (messagesFile.exists()) {
                    if (!messagesFile.delete()) {
                        SendMessage.sendToPlayer(player, plugin.getPluginMessage("messages.reloadError"));
                        return true;
                    }
                }

                plugin.saveResource("messages.yml", false);
                plugin.saveDefaultMessages();
                SendMessage.sendToPlayer(player, plugin.getPluginMessage("messages.reloadSuccess"));
                return true;

            case "update":
                String currentVersion = plugin.getDescription().getVersion();
                SendMessage.sendToPlayer(player, "§8[§bTowersForPGM§8] §7v" + currentVersion);
                updateChecker.checkForUpdates();
                return true;

            case "configreplace":
                SendMessage.sendToPlayer(player, plugin.getPluginMessage("configReplace.start"));
                String zipFileUrl = "https://github.com/nicoliee/configForTowers/archive/refs/heads/main.zip";
                File pluginsFolder = new File("plugins");

                try {
                    downloadFile(zipFileUrl, new File(pluginsFolder, "configForTowers.zip"));
                    unzipFile(new File(pluginsFolder, "configForTowers.zip"), pluginsFolder);
                    new File(pluginsFolder, "configForTowers.zip").delete();
                } catch (IOException e) {
                    e.printStackTrace();
                    SendMessage.sendToPlayer(player, plugin.getPluginMessage("configReplace.error"));
                    return true;
                }
                SendMessage.sendToPlayer(player, plugin.getPluginMessage("configReplace.success"));
                return true;

            default:
                SendMessage.sendToPlayer(player, "§8[§bTowersForPGM§8] §7Version: " + plugin.getDescription().getVersion());
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("towers.admin")) {
            return Arrays.asList(); // No sugerir nada si no tiene permisos
        }

        if (args.length == 1) {
            return Arrays.asList("reloadMessages", "setLanguage", "update");
        }
        if (args[0].equalsIgnoreCase("setLanguage") && args.length == 2) {
            return Arrays.asList("en", "es");
        }
        return Arrays.asList();
    }

    private void downloadFile(String fileUrl, File destinationFile) throws IOException {
        URL url = new URL(fileUrl);
        try (InputStream in = url.openStream(); FileOutputStream out = new FileOutputStream(destinationFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    private void unzipFile(File zipFile, File destinationFolder) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry = zipIn.getNextEntry();
            
            // Si la primera entrada tiene un prefijo, se utiliza para ajustar las rutas
            String mainDirPrefix = entry.getName().split("/")[0] + "/";
            
            // Recorre todas las entradas del ZIP
            while (entry != null) {
                String fileName = entry.getName();
                
                // Elimina el prefijo del nombre del archivo para evitar el directorio principal
                if (fileName.startsWith(mainDirPrefix)) {
                    fileName = fileName.substring(mainDirPrefix.length());
                }
                
                // Crea los archivos o directorios en el destino
                File file = new File(destinationFolder, fileName);
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    // Asegúrate de crear el directorio si no existe
                    file.getParentFile().mkdirs();
                    
                    try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = zipIn.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }
}