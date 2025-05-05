package org.nicolie.towersforpgm.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.preparationTime.Region;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;

import tc.oc.pgm.api.PGM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Comando para configurar las regiones de protección y timers por mapa
// PGM actualmente solo soporta una partida a la vez pero este plugin está pensado para soportar múltiples partidas simultáneas
// Aún así PGM solo tiene un mundo, por lo que se asume que solo hay un mundo a la hora de llamar a los métodos de configuración

public class ConfigCommand implements CommandExecutor, TabCompleter{
    private final LanguageManager languageManager;
    private final TowersForPGM plugin = TowersForPGM.getInstance();
    public ConfigCommand(LanguageManager languageManager) {
        this.languageManager = languageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(languageManager.getPluginMessage("noPlayer"));
            return true;
        }
        Player player = (Player) sender;
        String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
        FileConfiguration config = plugin.getConfig();
        if (args.length == 0) {
                    SendMessage.sendToPlayer(player, " &c/config <add|delete|haste|help|list|max|min|timer>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add":
                handleAddCommand(player, config, mapName);
                break;

            case "delete":
                handleDeleteCommand(player, config, mapName);
                break;

            case "draftsuggestions":
                handleDraftSuggestionsCommand(player);
                break;

            case "drafttimer":
                handleDraftTimerCommand(player);
                break;

            case "haste":
                handleHasteCommand(player, config, mapName, args);
                break;

            case "list":
                handleListCommand(player, config);
                break;

            case "max":
                if (!config.contains("preparationTime.maps." + mapName)) {
                    SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.worldError"));
                    return true;
                }
                if (args.length == 4) {
                    try {
                        int x = Integer.parseInt(args[1]);
                        int y = Integer.parseInt(args[2]);
                        int z = Integer.parseInt(args[3]);
                        
                        // Actualizamos la coordenada en el archivo config.yml
                        setCoordinates(config, mapName, "P2", x, y, z);
            
                        // Actualizamos la región en memoria
                        Region region = plugin.getRegions().get(mapName);
                        if (region != null) {
                            region.setP2(new Location(player.getWorld(), x, y, z));
                        }
            
                        plugin.saveConfig(); // Guardamos la configuración en el archivo
                        SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.maxSet")
                            .replace("{x}", String.valueOf(x))
                            .replace("{y}", String.valueOf(y))
                            .replace("{z}", String.valueOf(z)));
                    } catch (NumberFormatException e) {
                        SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.usage"));
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "/config max <x> <y> <z>");
                }
                break;

            case "min":
                if (!config.contains("preparationTime.maps." + mapName)) {
                    SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.worldError"));
                    return true;
                }
                if (args.length == 4) {
                    try {
                        int x = Integer.parseInt(args[1]);
                        int y = Integer.parseInt(args[2]);
                        int z = Integer.parseInt(args[3]);
                        
                        // Actualizamos la coordenada en el archivo config.yml
                        setCoordinates(config, mapName, "P1", x, y, z);
            
                        // Actualizamos la región en memoria
                        Region region = plugin.getRegions().get(mapName);
                        if (region != null) {
                            region.setP1(new Location(player.getWorld(), x, y, z));
                        }
            
                        plugin.saveConfig(); // Guardamos la configuración en el archivo
                        SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.minSet")
                            .replace("{x}", String.valueOf(x))
                            .replace("{y}", String.valueOf(y))
                            .replace("{z}", String.valueOf(z)));
                    } catch (NumberFormatException e) {
                        SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.usage"));
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "/config min <x> <y> <z>");
                }
                break;

            case "timer":
                handleTimerCommand(player, config, mapName, args);
                break;

            default:
                    player.sendMessage(ChatColor.RED + "/config <add|delete|haste|list|max|min|timer>");
    }
    return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Lista de opciones posibles
            List<String> options = Arrays.asList("add", "delete", "draftSuggestions", "draftTimer","haste", "list", "max", "min", "timer");

            // Filtrar las opciones que comienzan con el texto ingresado por el usuario
            String input = args[0].toLowerCase();
            List<String> filteredOptions = new ArrayList<>();
            for (String option : options) {
                if (option.toLowerCase().startsWith(input)) {
                    filteredOptions.add(option);
                }
            }
            return filteredOptions;
        }
        return null;
    }

    private void handleAddCommand(Player player, FileConfiguration config, String mapName) {
        if (config.contains("preparationTime.maps." + mapName)) {
            SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.mapAlreadyAdded")
                    .replace("{map}", mapName));
            return;
        }

        String basePath = "preparationTime.maps." + mapName;

        // Crear una instancia de Region
        Region region = new Region(null, null, 0, 0);
        
        // Asignar valores por defecto a la región
        Location p1 = new Location(Bukkit.getWorld(mapName), 0, 64, 0);  // Coordenadas P1 por defecto
        Location p2 = new Location(Bukkit.getWorld(mapName), 100, 64, 100);  // Coordenadas P2 por defecto
        region.setP1(p1);
        region.setP2(p2);
        region.setTimer(1);  // Temporizador por defecto (1 minuto)
        region.setHaste(1);  // Haste por defecto (1 minuto)

        // Guardar estos valores en el archivo de configuración
        config.set(basePath + ".P1", + p1.getBlockX() + ", " + p1.getBlockY() + ", " + p1.getBlockZ());
        config.set(basePath + ".P2",  + p2.getBlockX() + ", " + p2.getBlockY() + ", " + p2.getBlockZ());
        config.set(basePath + ".Timer", region.getTimer());
        config.set(basePath + ".Haste", region.getHaste());

        // Guardar la configuración
        plugin.saveConfig();

        // Almacenar la región en el mapa de regiones
        plugin.getRegions().put(mapName, region);

        SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.mapSuccess")
                .replace("{map}", mapName));
    }

    private void handleDeleteCommand(Player player, FileConfiguration config, String mapName) {
        if (!config.contains("preparationTime.maps." + mapName)) {
            SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.mapError"));
            return;
        }
    
        // Eliminar la configuración del archivo
        config.set("preparationTime.maps." + mapName, null);
        plugin.saveConfig();
    
        // Eliminar la región del mapa de regiones
        plugin.getRegions().remove(mapName);
    
        SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.mapDeleted")
                .replace("{map}", mapName));
    }
    

    private void setCoordinates(FileConfiguration config, String mapName, String key, int x, int y, int z) {
        String basePath = "preparationTime.maps." + mapName + "." + key;
        config.set(basePath, x + ", " + y + ", " + z);
        plugin.saveConfig();
    }    

    private void handleTimerCommand(Player player, FileConfiguration config, String mapName, String[] args) {
        if (!config.contains("preparationTime.maps." + mapName)) {
            SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.mapError"));
            return;
        }
    
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "/config timer {mins}");
            return;
        }
    
        try {
            int timer = Integer.parseInt(args[1]);
            if (timer <= 0) {
                SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.usage"));
                return;
            }
    
            // Actualizar la configuración en config.yml
            config.set("preparationTime.maps." + mapName + ".Timer", timer);
            plugin.saveConfig();
    
            // Obtener la región y actualizar el timer
            Region region = plugin.getRegions().get(mapName);
            if (region != null) {
                region.setTimer(timer);
                SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.timerSet")
                        .replace("{timer}", String.valueOf(timer)));
            } else {
                SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.regionNotFound"));
            }
        } catch (NumberFormatException e) {
            SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.usage"));
        }
    }

    private void handleHasteCommand(Player player, FileConfiguration config, String mapName, String[] args) {
        if (!config.contains("preparationTime.maps." + mapName)) {
            SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.mapError"));
            return;
        }
    
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "/config haste {mins}");
            return;
        }
    
        try {
            int haste = Integer.parseInt(args[1]);
            if (haste <= 0) {
                SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.usage"));
                return;
            }
    
            // Actualizar la configuración en config.yml
            config.set("preparationTime.maps." + mapName + ".Haste", haste);
            plugin.saveConfig();
    
            // Obtener la región y actualizar el haste
            Region region = plugin.getRegions().get(mapName);
            if (region != null) {
                region.setHaste(haste);
                SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.hasteSet")
                        .replace("{timer}", String.valueOf(haste)));
            } else {
                SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.regionNotFound"));
            }
        } catch (NumberFormatException e) {
            SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.usage"));
        }
    }

    private void handleListCommand(Player player, FileConfiguration config) {
        if (!config.contains("preparationTime.maps") || config.getConfigurationSection("preparationTime.maps").getKeys(false).isEmpty()) {
            SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.mapsNotFound"));
            return;
        }
    
        SendMessage.sendToPlayer(player, languageManager.getPluginMessage("region.header"));
        for (String mapName : config.getConfigurationSection("preparationTime.maps.").getKeys(false)) {
            String basePath = "preparationTime.maps." + mapName;
            String database = ConfigManager.getTableForMap(mapName);
            String p1 = config.getString(basePath + ".P1", "No definido");
            String p2 = config.getString(basePath + ".P2", "No definido");
            int timer = config.getInt(basePath + ".Timer", -1);
            int haste = config.getInt(basePath + ".Haste", -1);
    
            player.sendMessage(ChatColor.GREEN + mapName);
            player.sendMessage(ChatColor.AQUA + "  Base de datos: " + ChatColor.WHITE + database);
            player.sendMessage(ChatColor.AQUA + "  Coordenadas mínimas: " + ChatColor.WHITE + p1);
            player.sendMessage(ChatColor.AQUA + "  Coordenadas máximas: " + ChatColor.WHITE + p2);
            player.sendMessage(ChatColor.AQUA + "  Temporizador: " + ChatColor.WHITE + (timer > 0 ? timer + " minutos" : "No definido"));
            player.sendMessage(ChatColor.AQUA + "  Temporizador de haste: " + ChatColor.WHITE + (haste > 0 ? haste + " minutos" : "No definido"));
        }
    }

    private void handleDraftSuggestionsCommand(Player player) {
        boolean isDraftSuggestions = ConfigManager.isDraftSuggestions();
        ConfigManager.setDraftSuggestions(!isDraftSuggestions);
        String message = isDraftSuggestions ? "Draft suggestions disabled." : "Draft suggestions enabled.";
        SendMessage.sendToPlayer(player, message);
    }

    private void handleDraftTimerCommand(Player player) {
        boolean isDraftTimer = ConfigManager.isDraftTimer();
        ConfigManager.setDraftTimer(!isDraftTimer);
        String message = isDraftTimer ? "Draft timer disabled." : "Draft timer enabled.";
        SendMessage.sendToPlayer(player, message);
    }
}