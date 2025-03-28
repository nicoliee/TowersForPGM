package org.nicolie.towersforpgm.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.utils.SendMessage;

// Comando para seleccionar un jugador en el draft
// PGM actualmente solo soporta una partida a la vez, por lo que no se pueden realizar múltiples drafts simultáneamente
public class PickCommand implements CommandExecutor, TabCompleter{
    private Draft draft;
    private final TowersForPGM plugin;

    public PickCommand(Draft draft, TowersForPGM plugin) {
        this.draft = draft;
        this.plugin = plugin;
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
            SendMessage.sendToPlayer(player, plugin.getPluginMessage("picks.usage"));
            return true;
        }

        // Comprobar si el jugador es un capitán
        if (player.equals(draft.getCaptain1()) || player.equals(draft.getCaptain2())) {
            if (args.length == 1) {
                // Verificar si es el turno del jugador
                if ((draft.isCaptain1Turn() && !player.equals(draft.getCaptain1())) ||
                    (!draft.isCaptain1Turn() && !player.equals(draft.getCaptain2()))) {
                    SendMessage.sendToPlayer(player, plugin.getConfigurableMessage("picks.notTurn"));
                    return true;
                }

                // Comprobar si el jugador seleccionado está disponible
                Player pickedPlayer = Bukkit.getPlayer(args[0]);
                String pickedPlayerString = args[0];
                if (!draft.getAvailablePlayers().contains(pickedPlayer) && !draft.getAvailableOfflinePlayers().contains(pickedPlayerString)) {
                    SendMessage.sendToPlayer(player, plugin.getConfigurableMessage("picks.playerPicked").replace("{player}", pickedPlayerString));
                    return true;
                }
                draft.pickPlayer(pickedPlayerString);
                // Cambiar turno
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
            List<String> tables = draft.getAvailablePlayers().stream().map(Player::getName).collect(Collectors.toList());
            tables.addAll(draft.getAvailableOfflinePlayers());
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