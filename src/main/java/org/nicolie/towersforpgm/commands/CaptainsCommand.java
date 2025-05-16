package org.nicolie.towersforpgm.commands;

import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;

import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public CaptainsCommand(Draft draft, LanguageManager languageManager) {
        this.draft = draft;
        this.languageManager = languageManager;
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
        
        // Crear una lista de jugadores en línea excluyendo a los capitanes
        List<MatchPlayer> onlinePlayersExcludingCaptains = PGM.get().getMatchManager().getMatch(sender).getPlayers().stream()
            .filter(player -> !player.getId().equals(captain1) && !player.getId().equals(captain2))
            .collect(Collectors.toList());

        // Iniciar el draft con los capitanes y los jugadores restantes
        draft.startDraft(captain1, captain2, onlinePlayersExcludingCaptains, PGM.get().getMatchManager().getMatch(sender));
        return true;
    }
}