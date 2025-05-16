package org.nicolie.towersforpgm.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.draft.AvailablePlayers;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.PickInventory;
import org.nicolie.towersforpgm.draft.Teams;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;

import tc.oc.pgm.api.player.MatchPlayer;

// Comando para seleccionar un jugador en el draft
// PGM actualmente solo soporta una partida a la vez, por lo que no se pueden realizar múltiples drafts simultáneamente
public class PickCommand implements CommandExecutor, TabCompleter{
    private Draft draft;
    private final Captains captains;
    private final AvailablePlayers availablePlayers;
    private final Teams teams;
    private final LanguageManager languageManager;
    private final PickInventory pickInventory;

    public PickCommand(Draft draft, Captains captains, AvailablePlayers availablePlayers, Teams teams, LanguageManager languageManager, PickInventory pickInventory) {
        this.draft = draft;
        this.captains = captains;
        this.availablePlayers = availablePlayers;
        this.languageManager = languageManager;
        this.teams = teams;
        this.pickInventory = pickInventory;
    }

    private String validatePlayerToPick(String inputName, Player player) {
        // Validar si el jugador ya fue elegido
        if (teams.isPlayerInAnyTeam(inputName)) {
            return languageManager.getConfigurableMessage("picks.alreadyPicked").replace("{player}", inputName);
        }

        // Validar si el jugador seleccionado está en la lista de disponibles
        String pickedPlayerString = availablePlayers.getAvailablePlayers().stream()
            .map(MatchPlayer::getNameLegacy)
            .filter(name -> name.equalsIgnoreCase(inputName))
            .findFirst()
            .orElseGet(() -> availablePlayers.getAvailableOfflinePlayers().stream()
                .filter(name -> name.equalsIgnoreCase(inputName))
                .findFirst()
                .orElse(null));

        if (pickedPlayerString == null) {
            return languageManager.getConfigurableMessage("picks.notInList").replace("{player}", inputName);
        }

        return null; // No hay errores
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Comprobar si el comando fue ejecutado por un jugador
        if (!(sender instanceof Player)) {
            sender.sendMessage(languageManager.getPluginMessage("errors.noPlayer"));
            return true;
        }

        Player player = (Player) sender;

        // Comprobar si el draft está activo
        if (!Draft.isDraftActive()) {
            SendMessage.sendToPlayer(player, languageManager.getPluginMessage("picks.noDraft"));
            return true;
        }

        if (args.length == 0) {
            pickInventory.openInventory(player);
            pickInventory.giveItemToPlayer(player);
            return true;
        }
        

        // Comprobar si el jugador es un capitán
        if (captains.isCaptain(player.getUniqueId())) {
            if (args.length == 1) {
                // Verificar si es el turno del jugador
                if ((captains.isCaptain1Turn() && captains.isCaptain2(player.getUniqueId())) ||
                    (!captains.isCaptain1Turn() && captains.isCaptain1(player.getUniqueId()))) {
                    SendMessage.sendToPlayer(player, languageManager.getConfigurableMessage("picks.notTurn"));
                    return true;
                }

                String inputName = args[0].toLowerCase();

                // Validar el jugador a seleccionar
                String validationError = validatePlayerToPick(inputName, player);
                if (validationError != null) {
                    SendMessage.sendToPlayer(player, validationError);
                    return true;
                }

                // Ejecutar la selección del jugador
                draft.pickPlayer(inputName);
            }
        } else {
            SendMessage.sendToPlayer(player, languageManager.getConfigurableMessage("picks.notCaptain"));
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
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