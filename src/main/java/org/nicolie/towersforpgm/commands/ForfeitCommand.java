package org.nicolie.towersforpgm.commands;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.Sounds;

public class ForfeitCommand implements CommandExecutor {
  public static Set<UUID> forfeitedPlayers = new HashSet<>();
  private final LanguageManager languageManager;

  public ForfeitCommand(LanguageManager languageManager) {
    this.languageManager = languageManager;
  }

  // Pensado SOLAMENTE para 2 equipos
  // "red" y "blue"

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      SendMessage.sendToConsole(languageManager.getPluginMessage("errors.noPlayer"));
      return true;
    }
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer((Player) sender);
    if (!Queue.isRanked() || matchPlayer.isObserving()) {
      matchPlayer.sendWarning(Component.text(languageManager.getPluginMessage("ranked.noForfeit")));
      return true;
    }
    Match match = PGM.get().getMatchManager().getMatch(sender);
    Party team = matchPlayer.getParty();
    if (!forfeitedPlayers.add(matchPlayer.getId())) {
      matchPlayer.sendWarning(
          Component.text(languageManager.getPluginMessage("ranked.alreadyForfeited")));
      return true;
    }

    // Verifica si todos los jugadores del equipo se han rendido
    boolean allForfeited =
        team.getPlayers().stream().allMatch(mp -> forfeitedPlayers.contains(mp.getId()));
    String winningTeam = team.getNameLegacy().equalsIgnoreCase("red") ? "blue" : "red";
    if (allForfeited) {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "end " + winningTeam);
      forfeitedPlayers.clear();
    } else {
      match.sendMessage(Component.text(Queue.RANKED_PREFIX
          + languageManager
              .getPluginMessage("ranked.forfeit")
              .replace("{player}", matchPlayer.getPrefixedName())));
      match.playSound(Sounds.ALERT);
    }
    return true;
  }
}
