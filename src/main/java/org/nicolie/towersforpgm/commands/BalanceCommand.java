package org.nicolie.towersforpgm.commands;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.Matchmaking;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class BalanceCommand implements CommandExecutor {
  private final Matchmaking matchmaking;
  private final LanguageManager languageManager = TowersForPGM.getInstance().getLanguageManager();

  public BalanceCommand(Matchmaking matchmaking) {
    this.matchmaking = matchmaking;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(languageManager.getPluginMessage("errors.noPlayer"));
      return true;
    }
    Match match = PGM.get().getMatchManager().getMatch(sender);
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer((Player) sender);
    if (match.isRunning() || match.isFinished()) {
      matchPlayer.sendWarning(
          Component.text(languageManager.getPluginMessage("captains.matchStarted")));
      return true;
    }

    if (args.length < 2) {
      matchPlayer.sendWarning(Component.text(languageManager.getPluginMessage("balance.usage")));
      return true;
    }
    if (Bukkit.getOnlinePlayers().size() < 2) {
      matchPlayer.sendWarning(
          Component.text(languageManager.getPluginMessage("captains.notEnoughPlayers")));
      return true;
    }
    if (args[0].equalsIgnoreCase(args[1])) {
      matchPlayer.sendWarning(Component.text(languageManager.getPluginMessage("captains.usage")));
      return true;
    }
    if (Bukkit.getPlayer(args[0]) == null || Bukkit.getPlayer(args[1]) == null) {
      matchPlayer.sendWarning(Component.text(languageManager.getPluginMessage("captains.offline")));
      return true;
    }
    if (Queue.isRanked()) {
      matchPlayer.sendWarning(
          Component.text(languageManager.getPluginMessage("ranked.notAllowed")));
      return true;
    }

    UUID captain1 = Bukkit.getPlayer(args[0]).getUniqueId();
    UUID captain2 = Bukkit.getPlayer(args[1]).getUniqueId();
    List<MatchPlayer> onlinePlayersExcludingCaptains = match.getPlayers().stream()
        .filter(player -> !player.getId().equals(captain1) && !player.getId().equals(captain2))
        .collect(Collectors.toList());
    matchmaking.startMatchmaking(captain1, captain2, onlinePlayersExcludingCaptains, match);
    return true;
  }
}
