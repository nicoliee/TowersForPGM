package org.nicolie.towersforpgm.commands.ranked;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.Permissions;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.suggestion.Suggestions;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.text.TextFormatter;

public class RankedCommand {
  private final Queue queue;

  public RankedCommand(Queue queue) {
    this.queue = queue;
  }

  @Command("ranked <subcommand>")
  @CommandDescription("Main command for ranked matches")
  public void rankedMainCommand(
      Audience audience,
      CommandSender sender,
      @Argument(value = "subcommand", suggestions = "rankedArguments") String subcommand) {
    switch (subcommand.toLowerCase()) {
      case "join":
        handleJoinLeaveCommand(audience, sender, true);
        break;
      case "leave":
        handleJoinLeaveCommand(audience, sender, false);
        break;
      case "list":
        handleListCommand(audience, sender);
        break;
      case "start":
        handleStartStopCommand(audience, sender, true);
        break;
      case "stop":
        handleStartStopCommand(audience, sender, false);
        break;
      default:
        break;
    }
  }

  @Suggestions("rankedArguments")
  public List<String> rankedArgumentsSuggestions() {
    return List.of("join", "leave", "list", "start", "stop");
  }

  private void handleJoinLeaveCommand(Audience audience, CommandSender sender, boolean isJoin) {
    if (!(sender instanceof Player)) {
      audience.sendWarning(Component.translatable("command.onlyPlayers"));
      return;
    }
    if (MatchBotConfig.isVoiceChatEnabled()) return;
    Match match = PGM.get().getMatchManager().getMatch(sender);
    MatchPlayer player = match.getPlayer((Player) sender);
    if (isJoin) {
      queue.addPlayer(player);
    } else {
      queue.removePlayer(player);
    }
  }

  private void handleListCommand(Audience audience, CommandSender sender) {
    Component prefix = Queue.RANKED_PREFIX;
    Match listMatch = sender instanceof Player
        ? PGM.get().getMatchManager().getMatch(sender)
        : PGM.get().getMatchManager().getMatches().next();
    int targetSize = queue.getTargetSize(listMatch);
    Component header = prefix
        .append(Component.space())
        .append(Component.text(Queue.getQueueSize() + "/" + targetSize));
    List<Component> queuePlayers = queue.getQueueList();
    audience.sendMessage(header);
    if (!queuePlayers.isEmpty()) {
      Component playerList = TextFormatter.list(queuePlayers, NamedTextColor.DARK_GRAY);
      audience.sendMessage(playerList);
    }
  }

  private void handleStartStopCommand(Audience audience, CommandSender sender, boolean isStart) {
    if (!sender.hasPermission(Permissions.ADMIN)) {
      audience.sendMessage(Component.translatable("misc.noPermission"));
      return;
    }
    Match match = PGM.get().getMatchManager().getMatch(sender);
    if (isStart) {
      queue.startCountdown(match);
    } else {
      queue.stopCountdown(match);
    }
  }
}
