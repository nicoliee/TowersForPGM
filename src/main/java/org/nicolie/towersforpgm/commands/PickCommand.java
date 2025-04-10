package org.nicolie.towersforpgm.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.AvailablePlayers;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.PickInventory;
import org.nicolie.towersforpgm.utils.SendMessage;

import tc.oc.pgm.api.player.MatchPlayer;

// Comando para seleccionar un jugador en el draft
// PGM actualmente solo soporta una partida a la vez, por lo que no se pueden realizar múltiples drafts simultáneamente
public class PickCommand implements CommandExecutor, TabCompleter{
    private Draft draft;
    private final Captains captains;
    private final AvailablePlayers availablePlayers;
    private final PickInventory pickInventory;

    private final TowersForPGM plugin;

    public PickCommand(Draft draft, Captains captains, AvailablePlayers availablePlayers, TowersForPGM plugin, PickInventory pickInventory) {
        this.draft = draft;
        this.captains = captains;
        this.availablePlayers = availablePlayers;
        this.plugin = plugin;
        this.pickInventory = pickInventory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Comprobar si el comando fue ejecutado por un jugador
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPluginMessage("errors.noPlayer"));
            return true;
        }

        Player player = (Player) sender;

        // Comprobar si el draft está activo
        if (!draft.isDraftActive()) {
            SendMessage.sendToPlayer(player, plugin.getPluginMessage("picks.noDraft"));
            return true;
        }

        if (args.length == 0) {
            pickInventory.openInventory(player);
            return true;
        }
        

        // Comprobar si el jugador es un capitán
        if (captains.isCaptain(player.getUniqueId())) {
            if (args.length == 1) {
                // Verificar si es el turno del jugador
                if ((draft.isCaptain1Turn() && captains.isCaptain2(player.getUniqueId())) ||
                    (!draft.isCaptain1Turn() && captains.isCaptain1(player.getUniqueId()))) {
                    SendMessage.sendToPlayer(player, plugin.getConfigurableMessage("picks.notTurn"));
                    return true;
                }

                // Comprobar si el jugador seleccionado está disponible
                String inputName = args[0].toLowerCase();

                // Buscar en jugadores online
                MatchPlayer pickedPlayer = availablePlayers.getAvailablePlayers().stream()
                    .filter(p -> p.getNameLegacy().equalsIgnoreCase(inputName))
                    .findFirst()
                    .orElse(null);

                // Buscar en offline si no está en online
                String pickedPlayerString = null;

                if (pickedPlayer != null) {
                    pickedPlayerString = pickedPlayer.getNameLegacy();
                } else {
                    pickedPlayerString = availablePlayers.getAvailableOfflinePlayers().stream()
                        .filter(name -> name.equalsIgnoreCase(inputName))
                        .findFirst()
                        .orElse(null);
                }

                if (pickedPlayerString == null) {
                    SendMessage.sendToPlayer(player, plugin.getConfigurableMessage("picks.playerPicked").replace("{player}", args[0]));
                    return true;
                }

                draft.pickPlayer(pickedPlayerString);
                draft.toggleTurn();
            }
        } else {
            SendMessage.sendToPlayer(player, plugin.getConfigurableMessage("picks.notCaptain"));
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Obtener la lista de tablas desde TowersForPGM.getTables()
            List<String> tables = availablePlayers.getAvailablePlayers().stream().map(MatchPlayer::getNameLegacy).collect(Collectors.toList());
            tables.addAll(availablePlayers.getAvailableOfflinePlayers());
            // Filtrar las opciones que comienzan con el texto ingresado por el usuario
            String input = args[0].toLowerCase();
            List<String> filteredOptions = new ArrayList<>();
            for (String table : tables) {
                if (table.toLowerCase().startsWith(input)) {
                    filteredOptions.add(table);
                }
            }
            return filteredOptions;
        }
        return null;
    }
}