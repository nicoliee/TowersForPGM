package org.nicolie.towersforpgm.commands.draft;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
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

public class RemoveCommand implements CommandExecutor, TabCompleter {
  private final Draft draft;
  private final Teams teams;
  private final AvailablePlayers availablePlayers;

  public RemoveCommand(
      Draft draft, Teams teams, Captains captains, AvailablePlayers availablePlayers) {
    this.draft = draft;
    this.teams = teams;
    this.availablePlayers = availablePlayers;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    Audience audience = Audience.get(sender);
    if (!(sender instanceof Player)) {
      audience.sendWarning(Component.translatable("command.onlyPlayers"));
      return true;
    }

    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer((Player) sender);
    if (Draft.getPhase() == DraftPhase.IDLE) {
      audience.sendWarning(Component.translatable("draft.inactive"));
      return true;
    }

    if (args.length != 1) {
      matchPlayer.sendWarning(
          Component.translatable("command.incorrectUsage", Component.text("/remove <playerName>")));
      return true;
    }

    if (Queue.isRanked()) {
      matchPlayer.sendWarning(Component.translatable("ranked.notAllowed"));
      return true;
    }
    String playerName = args[0];
    if (isInvalidPlayer(playerName, matchPlayer)) {
      return true;
    }
    availablePlayers.removePlayer(playerName);
    Match match = PGM.get().getMatchManager().getMatch(sender).getMatch();
    match.sendMessage(
        Component.translatable("draft.remove", MatchManager.getPrefixedName(playerName))
            .color(NamedTextColor.GRAY));
    match.playSound(Sounds.WARNING);
    if (availablePlayers.getAllAvailablePlayers().isEmpty()) {
      draft.endDraft();
    }
    return true;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      List<String> tables = availablePlayers.getAvailablePlayers().stream()
          .map(MatchPlayer::getNameLegacy)
          .collect(Collectors.toList());
      tables.addAll(availablePlayers.getAvailableOfflinePlayers());
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

  private boolean isInvalidPlayer(String playerName, MatchPlayer sender) {
    if (!availablePlayers.getAllAvailablePlayers().contains(playerName)) {
      sender.sendWarning(
          Component.translatable("draft.notInDraft", MatchManager.getPrefixedName(playerName)));
      return true;
    }

    if (teams.isPlayerInAnyTeam(playerName)) {
      sender.sendWarning(Component.translatable(
          "draft.captains.alreadyInTeam", MatchManager.getPrefixedName(playerName)));
      return true;
    }

    return false;
  }
}
