package org.nicolie.towersforpgm.commands;

import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.utils.SendMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import tc.oc.pgm.api.match.Match;
// Comando para iniciar un draft de capitanes, actualmente solo soporta dos equipos "red" y "blue"
// PGM actualmente solo soporta una partida a la vez, por lo que no se pueden realizar múltiples drafts simultáneamente
public class CaptainsCommand implements CommandExecutor {
    private final TowersForPGM plugin;
    private final Draft draft;

    public CaptainsCommand(TowersForPGM plugin, Draft draft) {
        this.plugin = plugin;
        this.draft = draft;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPluginMessage("errors.noPlayer"));
            return true;
        }
        Player player = (Player) sender;
        Match match = TowersForPGM.getInstance().getCurrentMatch();

        // Verificar si la partida está en curso
        if (match.isRunning()|| match.isFinished()) {
            SendMessage.sendToPlayer(player, plugin.getPluginMessage("captains.matchStarted"));
            return true;
        }
        
        // Verificar si los argumentos son suficientes
        if (args.length < 2) {
            SendMessage.sendToPlayer(player, plugin.getPluginMessage("captains.usage"));
            return true;
        }

        // Verificar si hay suficientes jugadores en línea
        if (Bukkit.getOnlinePlayers().size() < 3) {
            SendMessage.sendToPlayer(player, plugin.getPluginMessage("captains.notEnoughPlayers"));
            return true;
        }

        // Evitar que los argumentos sean iguales (duplicados)
        if (args[0].equalsIgnoreCase(args[1])) {
            SendMessage.sendToPlayer(player, plugin.getPluginMessage("captains.usage"));
            return true;
        }

        // Si hay exactamente dos argumentos
        if (args.length == 2) {
            // Comando para agregar un jugador al draft
            if (args[0].equalsIgnoreCase("a")) {
                if (!draft.isDraftActive()) {
                    SendMessage.sendToPlayer(player, plugin.getPluginMessage("picks.noDraft"));
                    return true;
                }

                String playerName = args[1];
                draft.addToDraft(playerName);
                return true;
            }

            // Comando para eliminar un jugador del draft
            if (args[0].equalsIgnoreCase("r")) {
                if (!draft.isDraftActive()) {
                    SendMessage.sendToPlayer(player, plugin.getPluginMessage("picks.noDraft"));
                    return true;
                }

                String playerName = args[1];
                draft.removeFromDraft(playerName);
                return true;
            }

            // Si no es ni "a" ni "r", entonces es para iniciar el draft con dos capitanes
            Player captain1 = Bukkit.getPlayer(args[0]);
            Player captain2 = Bukkit.getPlayer(args[1]);

            // Verificar si ambos capitanes están en línea
            if (captain1 == null || captain2 == null) {
                SendMessage.sendToPlayer(player, plugin.getPluginMessage("captains.offline"));
                return false;
            }

            // Enviar mensaje a todos los jugadores para anunciar los capitanes
            SendMessage.broadcast(plugin.getPluginMessage("captains.captainsHeader"));
            SendMessage.broadcast("&4" + captain1.getName() + " &l&6vs. " + "&9" + captain2.getName());
            SendMessage.broadcast("-------------------------------");
            draft.startDraft(captain1, captain2);
            return true;
        }
        return false;
    }
}