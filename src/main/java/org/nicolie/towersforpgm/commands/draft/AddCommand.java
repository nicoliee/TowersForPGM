package org.nicolie.towersforpgm.commands.draft;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.draft.components.DraftPhase;
import org.nicolie.towersforpgm.draft.core.AvailablePlayers;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Draft;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.bukkit.Sounds;

public class AddCommand implements CommandExecutor {
  private final AvailablePlayers availablePlayers;
  private final Captains captains;
  private final Teams teams;

  public AddCommand(AvailablePlayers availablePlayers, Captains captains, Teams teams) {
    this.availablePlayers = availablePlayers;
    this.captains = captains;
    this.teams = teams;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    Audience audience = Audience.get(sender);
    if (!(sender instanceof Player)) {
      audience.sendWarning(Component.translatable("command.onlyPlayers"));
      return true;
    }
    if (Draft.getPhase() == DraftPhase.IDLE) {
      audience.sendWarning(Component.translatable("draft.inactive"));
      return true;
    }

    if (Queue.isRanked()) {
      audience.sendWarning(Component.translatable("ranked.notAllowed"));
      return true;
    }

    if (args.length < 1) {
      audience.sendWarning(Component.translatable(
          "command.incorrectUsage", Component.text("/draft add <playerName>")));
      return true;
    }

    String playerName = args[0];

    if (isInvalidPlayer(playerName, sender)) {
      return true;
    }

    availablePlayers.addPlayer(playerName);
    Match match = PGM.get().getMatchManager().getMatch(sender).getMatch();
    match.sendMessage(Component.translatable("draft.add", MatchManager.getPrefixedName(playerName))
        .color(NamedTextColor.GRAY));
    match.playSound(Sounds.ALERT);
    return true;
  }

  private boolean isInvalidPlayer(String playerName, CommandSender sender) {
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer((Player) sender);
    if ((captains.getCaptain1Name() != null
            && captains.getCaptain1Name().equalsIgnoreCase(playerName))
        || (captains.getCaptain2Name() != null
            && captains.getCaptain2Name().equalsIgnoreCase(playerName))) {
      matchPlayer.sendWarning(Component.translatable("draft.add.captain"));
      return true;
    }

    if (teams.isPlayerInAnyTeam(playerName)) {
      matchPlayer.sendWarning(
          Component.translatable("draft.alreadyInTeam", MatchManager.getPrefixedName(playerName)));
      return true;
    }

    if (availablePlayers.getAllAvailablePlayers().contains(playerName)) {
      matchPlayer.sendWarning(
          Component.translatable("draft.alreadyInDraft", MatchManager.getPrefixedName(playerName)));
      return true;
    }
    return false;
  }
}
