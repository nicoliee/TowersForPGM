package org.nicolie.towersforpgm.commands;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import org.nicolie.towersforpgm.preparationTime.TorneoListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import tc.oc.pgm.api.PGM;

public class PreparationTimeCommand implements CommandExecutor, TabCompleter{
    private final TorneoListener torneoListener;
    private final LanguageManager languageManager;
    private final TowersForPGM plugin = TowersForPGM.getInstance();

    public PreparationTimeCommand(LanguageManager languageManager, TorneoListener torneoListener) {
        this.languageManager = languageManager;
        this.torneoListener = torneoListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        // Verificar que el comando lo ejecute un jugador
        if (!(sender instanceof Player)) {
            SendMessage.sendToPlayer(player, languageManager.getPluginMessage("errors.noPlayer"));
            return true;
        }

        // Verificar que haya suficientes argumentos
        if (args.length < 1) {
            SendMessage.sendToPlayer(player, languageManager.getPluginMessage("preparation.usage"));
            return true;
        }
        String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
        String worldName = player.getWorld().getName();
        // Procesar el comando dependiendo del primer argumento
        String action = args[0].toLowerCase();

        switch (action) {
            case "enabled":
                plugin.setPreparationEnabled(true);
                SendMessage.sendToPlayer(player, languageManager.getPluginMessage("preparation.enabled"));
                break;

            case "disabled":
                plugin.setPreparationEnabled(false);
                SendMessage.sendToPlayer(player, languageManager.getPluginMessage("preparation.disabled"));
                break;

            case "on":
                torneoListener.startProtection(player, mapName, worldName);
                break;
            
            case "off":
                torneoListener.stopProtection(player, worldName);
                break;

            default:
                SendMessage.sendToPlayer(player, languageManager.getPluginMessage("preparation.usage"));
                return true;
        }
        return true;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = Arrays.asList("on", "off", "enabled", "disabled");
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
}