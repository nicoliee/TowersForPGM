package org.nicolie.towersforpgm.commands.draft;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.draft.core.Matchmaking;
import org.nicolie.towersforpgm.rankeds.Queue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.Audience;

public class BalanceCommand implements CommandExecutor {
  private final Matchmaking matchmaking;

  public BalanceCommand(Matchmaking matchmaking) {
    this.matchmaking = matchmaking;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    Audience audience = Audience.get(sender);
    if (!(sender instanceof Player)) {
      audience.sendWarning(Component.translatable("command.onlyPlayers"));
      return true;
    }
    Match match = PGM.get().getMatchManager().getMatch(sender);
    if (match.isRunning() || match.isFinished()) {
      audience.sendWarning(Component.translatable("admin.start.matchRunning"));
      return true;
    }

    if (args.length < 2) {
      audience.sendWarning(Component.translatable(
          "command.incorrectUsage", Component.text("/balance <playerName1> <playerName2>")));
      return true;
    }
    if (Bukkit.getOnlinePlayers().size() < 2) {
      audience.sendWarning(Component.translatable("draft.notEnoughPlayers"));
      return true;
    }
    if (args[0].equalsIgnoreCase(args[1])) {
      audience.sendWarning(Component.translatable(
          "command.incorrectUsage", Component.text("/balance <playerName1> <playerName2>")));
      return true;
    }
    if (Bukkit.getPlayer(args[0]) == null || Bukkit.getPlayer(args[1]) == null) {
      audience.sendWarning(Component.translatable("draft.captains.offline"));
      return true;
    }
    if (Queue.isRanked()) {
      audience.sendWarning(Component.translatable("ranked.notAllowed"));
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
