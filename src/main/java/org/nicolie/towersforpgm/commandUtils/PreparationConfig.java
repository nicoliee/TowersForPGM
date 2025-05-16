package org.nicolie.towersforpgm.commandUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.preparationTime.Region;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;

import tc.oc.pgm.api.PGM;

public class PreparationConfig {
    private final LanguageManager languageManager;
    private final TowersForPGM plugin = TowersForPGM.getInstance();
    public PreparationConfig(LanguageManager languageManager) {
        this.languageManager = languageManager;
    }

    public void togglePreparation(CommandSender sender) {
        boolean newState = !plugin.isPreparationEnabled();
        plugin.setPreparationEnabled(newState);
        String messageKey = newState ? "preparation.enabled" : "preparation.disabled";
        SendMessage.sendToPlayer(sender, languageManager.getPluginMessage(messageKey));
    }

    public void handleAddCommand(CommandSender sender) {
        FileConfiguration config = plugin.getConfig();
        String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
        if (config.getConfigurationSection("preparationTime.maps").getKeys(false).contains(mapName)) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("region.mapAlreadyAdded")
                    .replace("{map}", mapName));
            return;
        }

        String basePath = "preparationTime.maps." + mapName;
        // Crear una instancia de Region
        Region region = new Region(null, null, 0, 0);

        // Asignar valores por defecto a la región
        Location p1 = new Location(Bukkit.getWorld(mapName), 0, 64, 0); // Coordenadas P1 por defecto
        Location p2 = new Location(Bukkit.getWorld(mapName), 100, 64, 100); // Coordenadas P2 por defecto
        region.setP1(p1);
        region.setP2(p2);
        region.setTimer(1); // Temporizador por defecto (1 minuto)
        region.setHaste(1); // Haste por defecto (1 minuto)

        // Guardar estos valores en el archivo de configuración
        config.set(basePath + ".P1", +p1.getBlockX() + ", " + p1.getBlockY() + ", " + p1.getBlockZ());
        config.set(basePath + ".P2", +p2.getBlockX() + ", " + p2.getBlockY() + ", " + p2.getBlockZ());
        config.set(basePath + ".Timer", region.getTimer());
        config.set(basePath + ".Haste", region.getHaste());

        // Guardar la configuración
        plugin.saveConfig();

        // Almacenar la región en el mapa de regiones
        plugin.getRegions().put(mapName, region);

        SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("region.mapSuccess")
                .replace("{map}", mapName));
    }

    public void handleRemoveCommand(CommandSender sender) {
        FileConfiguration config = plugin.getConfig();
        String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
        if (!config.getConfigurationSection("preparationTime.maps").getKeys(false).contains(mapName)) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("region.mapError")
                    .replace("{map}", mapName));
            return;
        }

        // Eliminar la configuración del archivo
        config.set("preparationTime.maps." + mapName, null);
        plugin.saveConfig();

        // Eliminar la región del mapa de regiones
        plugin.getRegions().remove(mapName);

        SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("region.mapDeleted")
                .replace("{map}", mapName));
    }

    public void handleCoordinates(CommandSender sender, boolean isMax, int x, int y, int z) {
        FileConfiguration config = plugin.getConfig();
        String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
        World world = PGM.get().getMatchManager().getMatch(sender).getWorld();
        if (!config.getConfigurationSection("preparationTime.maps").getKeys(false).contains(mapName)) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("preparation.noRegion")
                    .replace("{map}", mapName));
            return;
        }
        String key = isMax ? "P2" : "P1";
        setCoordinates(config, mapName, key, x, y, z);
        Region region = plugin.getRegions().get(mapName);
        if (region != null) {
            region.setP2(new Location(world, x, y, z));
        }
        plugin.saveConfig();
        String message = isMax ? "region.maxSet" : "region.minSet";
        SendMessage.sendToPlayer(sender, languageManager.getPluginMessage(message)
                .replace("{map}", mapName)
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y))
                .replace("{z}", String.valueOf(z)));
    }

    private void setCoordinates(FileConfiguration config, String mapName, String key, int x, int y, int z) {
        String basePath = "preparationTime.maps." + mapName + "." + key;
        config.set(basePath, x + ", " + y + ", " + z);
        plugin.saveConfig();
    }

    public void handleTimerCommand(CommandSender sender, int timer) {
        FileConfiguration config = plugin.getConfig();
        String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();

        if (!config.getConfigurationSection("preparationTime.maps").getKeys(false).contains(mapName)) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("region.mapError")
                    .replace("{map}", mapName));
            return;
        }
        if (timer <= 0) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("region.usage"));
            return;
        }

        // Actualizar la configuración en config.yml
        config.set("preparationTime.maps." + mapName + ".Timer", timer);
        plugin.saveConfig();

        // Obtener la región y actualizar el timer
        Region region = plugin.getRegions().get(mapName);
        if (region != null) {
            region.setTimer(timer);
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("region.timerSet")
                    .replace("{map}", mapName)
                    .replace("{timer}", SendMessage.formatTime(timer*60)));
        } else {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("region.mapError"));
        }
    }

    public void handleHasteCommand(CommandSender sender, int haste) {
        FileConfiguration config = plugin.getConfig();
        String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
        if (!config.getConfigurationSection("preparationTime.maps").getKeys(false).contains(mapName)) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("region.mapError"));
            return;
        }
        if (haste <= 0) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("region.usage"));
            return;
        }

        // Actualizar la configuración en config.yml
        config.set("preparationTime.maps." + mapName + ".Haste", haste);
        plugin.saveConfig();

        // Obtener la región y actualizar el haste
        Region region = plugin.getRegions().get(mapName);
        if (region != null) {
            region.setHaste(haste);
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("region.hasteSet")
                    .replace("{map}", mapName)
                    .replace("{timer}", SendMessage.formatTime(haste*60)));
        } else {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("region.regionNotFound"));
        }
    }

    public void handleListCommand(CommandSender sender) {

        FileConfiguration config = plugin.getConfig();
        if (!config.contains("preparationTime.maps")
                || config.getConfigurationSection("preparationTime.maps").getKeys(false).isEmpty()) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("region.mapsNotFound"));
            return;
        }

        SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("region.header"));
        for (String mapName : config.getConfigurationSection("preparationTime.maps.").getKeys(false)) {
            String basePath = "preparationTime.maps." + mapName;
            String database = ConfigManager.getTableForMap(mapName);
            String p1 = config.getString(basePath + ".P1", "No definido");
            String p2 = config.getString(basePath + ".P2", "No definido");
            int timer = config.getInt(basePath + ".Timer", -1);
            int haste = config.getInt(basePath + ".Haste", -1);

            sender.sendMessage(ChatColor.GREEN + mapName);
            sender.sendMessage(ChatColor.AQUA + "  Base de datos: " + ChatColor.WHITE + database);
            sender.sendMessage(ChatColor.AQUA + "  Coordenadas mínimas: " + ChatColor.WHITE + p1);
            sender.sendMessage(ChatColor.AQUA + "  Coordenadas máximas: " + ChatColor.WHITE + p2);
            sender.sendMessage(ChatColor.AQUA + "  Temporizador: " + ChatColor.WHITE
                    + (timer > 0 ? timer + " minutos" : "No definido"));
                    sender.sendMessage(ChatColor.AQUA + "  Temporizador de haste: " + ChatColor.WHITE
                    + (haste > 0 ? haste + " minutos" : "No definido"));
        }
    }
}
