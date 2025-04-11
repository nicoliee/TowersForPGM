package org.nicolie.towersforpgm.commands;

import org.nicolie.towersforpgm.MatchManager;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.PickInventory;
import org.nicolie.towersforpgm.utils.SendMessage;

import tc.oc.pgm.api.match.Match;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

// Comando para iniciar un draft de capitanes, actualmente solo soporta dos equipos "red" y "blue"
// PGM actualmente solo soporta una partida a la vez, por lo que no se pueden realizar múltiples drafts simultáneamente
public class CaptainsCommand implements CommandExecutor {
    private final TowersForPGM plugin;
    private final Draft draft;
    private final MatchManager matchManager;
    private final PickInventory pickInventory;

    public CaptainsCommand(TowersForPGM plugin, Draft draft, MatchManager matchManager, PickInventory pickInventory) {
        this.plugin = plugin;
        this.draft = draft;
        this.matchManager = matchManager;
        this.pickInventory = pickInventory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPluginMessage("errors.noPlayer"));
            return true;
        }
        Player player = (Player) sender;
        Match match = matchManager.getMatch();

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
        if (Bukkit.getOnlinePlayers().size() < 4) {
            SendMessage.sendToPlayer(player, plugin.getPluginMessage("captains.notEnoughPlayers"));
            return true;
        }

        // Evitar que los argumentos sean iguales (duplicados)
        if (args[0].equalsIgnoreCase(args[1])) {
            SendMessage.sendToPlayer(player, plugin.getPluginMessage("captains.usage"));
            return true;
        }

        // Verificar si los capitanes están en línea
        if (Bukkit.getPlayer(args[0]) == null || Bukkit.getPlayer(args[1]) == null) {
            SendMessage.sendToPlayer(player, plugin.getPluginMessage("captains.offline"));
            return true;
        }

        UUID captain1 = Bukkit.getPlayer(args[0]).getUniqueId();
        UUID captain2 = Bukkit.getPlayer(args[1]).getUniqueId();

        // Enviar mensaje a todos los jugadores para anunciar los capitanes
        SendMessage.broadcast(plugin.getPluginMessage("captains.captainsHeader"));
        SendMessage.broadcast("&4" + Bukkit.getPlayer(captain1).getName() + " &l&6vs. " + "&9" + Bukkit.getPlayer(captain2).getName());
        SendMessage.broadcast("§8§m---------------------------------");
        draft.startDraft(captain1, captain2);
        pickInventory.giveItemToPlayers();
        return true;
    }
}