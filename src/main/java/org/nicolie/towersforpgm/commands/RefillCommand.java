package org.nicolie.towersforpgm.commands;


import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.nicolie.towersforpgm.MatchManager;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import org.bukkit.entity.Player;
import org.bukkit.Location;

public class RefillCommand implements CommandExecutor {
    private final RefillManager refillManager;
    private final MatchManager matchManager;
    private final TowersForPGM plugin = TowersForPGM.getInstance();
    public RefillCommand(RefillManager refillManager, MatchManager matchManager) {
        this.refillManager = refillManager;
        this.matchManager = matchManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            SendMessage.sendToConsole(plugin.getPluginMessage("error.noPlayer"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            SendMessage.sendToPlayer(player, plugin.getPluginMessage("refill.usage"));
            return true;
        }

        String mapName = matchManager.getMatch().getMap().getName();
        Location loc = player.getLocation();
        String coords = loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();

        switch (args[0].toLowerCase()) {
            case "add":
                addRefillLocation(mapName, coords);
                SendMessage.sendToPlayer(player, plugin.getPluginMessage("refill.chestSet")
                    .replace("{coords}", coords));
                plugin.saveRefillConfig();
                plugin.reloadRefillConfig();
                return true;

            case "delete":
                boolean removed = removeRefillLocation(mapName, coords);
                if (removed) {
                    SendMessage.sendToPlayer(player, plugin.getPluginMessage("refill.chestDeleted")
                        .replace("{coords}", coords));
                    plugin.saveRefillConfig();
                    plugin.reloadRefillConfig();
                } else {
                    SendMessage.sendToPlayer(player, plugin.getPluginMessage("refill.chestNotFound")
                        .replace("{coords}", coords));
                }
                return true;

            case "test":
                String worldName = player.getWorld().getName();
                refillManager.loadChests(mapName, worldName);
                return true;
        }

        SendMessage.sendToPlayer(player, plugin.getPluginMessage("refill.usage"));
        return true;
    }

    private void addRefillLocation(String mapName, String coords) {
        FileConfiguration refillConfig = plugin.getRefillConfig();

        // Si el mapa no tiene una sección, crearla
        if (!refillConfig.contains("refill." + mapName)) {
            refillConfig.createSection("refill." + mapName);
        }

        // Contar cuántas coordenadas hay para este mapa (c1, c2, ...)
        int i = 1;
        while (refillConfig.contains("refill." + mapName + ".c" + i)) {
            i++;
        }

        // Agregar la nueva coordenada
        refillConfig.set("refill." + mapName + ".c" + i, coords);
    }

    private boolean removeRefillLocation(String mapName, String coords) {
        FileConfiguration refillConfig = plugin.getRefillConfig();
        boolean removed = false;

        for (int i = 1; refillConfig.contains("refill." + mapName + ".c" + i); i++) {
            String storedCoords = refillConfig.getString("refill." + mapName + ".c" + i);
            if (storedCoords.equals(coords)) {
                refillConfig.set("refill." + mapName + ".c" + i, null);
                removed = true;
                break;
            }
        }
        return removed;
    }
}