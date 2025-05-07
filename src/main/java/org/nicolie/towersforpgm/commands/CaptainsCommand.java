package org.nicolie.towersforpgm.commands;

import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.PickInventory;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;

import tc.oc.pgm.api.PGM;
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
    private final Draft draft;
    private final LanguageManager languageManager;
    private final PickInventory pickInventory;

    public CaptainsCommand(Draft draft, LanguageManager languageManager, PickInventory pickInventory) {
        this.draft = draft;
        this.languageManager = languageManager;
        this.pickInventory = pickInventory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(languageManager.getPluginMessage("errors.noPlayer"));
            return true;
        }
        Match match = PGM.get().getMatchManager().getMatch(sender);

        // Verificar si la partida está en curso
        if (match.isRunning()|| match.isFinished()) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("captains.matchStarted"));
            return true;
        }
        
        // Verificar si los argumentos son suficientes
        if (args.length < 2) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("captains.usage"));
            return true;
        }

        // Verificar si hay suficientes jugadores en línea
        if (Bukkit.getOnlinePlayers().size() < 2) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("captains.notEnoughPlayers"));
            return true;
        }

        // Evitar que los argumentos sean iguales (duplicados)
        if (args[0].equalsIgnoreCase(args[1])) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("captains.usage"));
            return true;
        }

        // Verificar si los capitanes están en línea
        if (Bukkit.getPlayer(args[0]) == null || Bukkit.getPlayer(args[1]) == null) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("captains.offline"));
            return true;
        }

        UUID captain1 = Bukkit.getPlayer(args[0]).getUniqueId();
        UUID captain2 = Bukkit.getPlayer(args[1]).getUniqueId();
        draft.startDraft(captain1, captain2, PGM.get().getMatchManager().getMatch(sender));
        pickInventory.giveItemToPlayers();
        return true;
    }
}