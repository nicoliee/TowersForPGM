package org.nicolie.towersforpgm.commands;


import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;

import tc.oc.pgm.api.PGM;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;

public class RefillCommand implements CommandExecutor, TabCompleter {
    private final LanguageManager languageManager;
    private final RefillManager refillManager;
    private final TowersForPGM plugin = TowersForPGM.getInstance();

    public RefillCommand(LanguageManager languageManager, RefillManager refillManager) {
        this.languageManager = languageManager;
        this.refillManager = refillManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            SendMessage.sendToConsole(languageManager.getPluginMessage("error.noPlayer"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            SendMessage.sendToPlayer(player, languageManager.getPluginMessage("refill.usage"));
            return true;
        }

        String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
        Location loc = player.getLocation();
        String coords = loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();

        switch (args[0].toLowerCase()) {
            case "add":
                addRefillLocation(mapName, coords);
                SendMessage.sendToPlayer(player, languageManager.getPluginMessage("refill.chestSet")
                    .replace("{coords}", coords));
                plugin.saveRefillConfig();
                plugin.reloadRefillConfig();
                return true;

            case "delete":
                boolean removed = removeRefillLocation(mapName, coords);
                if (removed) {
                    SendMessage.sendToPlayer(player, languageManager.getPluginMessage("refill.chestDeleted")
                        .replace("{coords}", coords));
                    plugin.saveRefillConfig();
                    plugin.reloadRefillConfig();
                } else {
                    SendMessage.sendToPlayer(player, languageManager.getPluginMessage("refill.chestNotFound")
                        .replace("{coords}", coords));
                }
                return true;

            case "test":
                String worldName = player.getWorld().getName();
                refillManager.loadChests(mapName, worldName);
                return true;
        }

        SendMessage.sendToPlayer(player, languageManager.getPluginMessage("refill.usage"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Lista de opciones posibles
            List<String> options = Arrays.asList("add", "delete", "test");

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