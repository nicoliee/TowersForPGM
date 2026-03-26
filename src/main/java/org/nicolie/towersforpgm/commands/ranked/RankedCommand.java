package org.nicolie.towersforpgm.commands.ranked;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.rankeds.listeners.QueueJoinListener;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.Permissions;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.text.TextFormatter;

public class RankedCommand {
  private final Queue queue;

  public RankedCommand(Queue queue) {
    this.queue = queue;
  }

  @Command("ranked join")
  @CommandDescription("Join the ranked queue")
  public void rankedJoinCommand(Audience audience, Player sender) {
    handleJoinLeaveCommand(audience, sender, true);
  }

  @Command("ranked leave")
  @CommandDescription("Leave the ranked queue")
  public void rankedLeaveCommand(Audience audience, Player sender) {
    handleJoinLeaveCommand(audience, sender, false);
  }

  @Command("ranked list")
  @CommandDescription("List players in the ranked queue")
  public void rankedListCommand(Audience audience, CommandSender sender) {
    handleListCommand(audience, sender);
  }

  @Command("ranked start")
  @CommandDescription("Start the ranked match countdown")
  @Permission(Permissions.ADMIN)
  public void rankedStartCommand(Audience audience, CommandSender sender) {
    handleStartStopCommand(audience, sender, true);
  }

  @Command("ranked stop")
  @CommandDescription("Stop the ranked match countdown")
  @Permission(Permissions.ADMIN)
  public void rankedStopCommand(Audience audience, CommandSender sender) {
    handleStartStopCommand(audience, sender, false);
  }

  @Command("ranked reload")
  @CommandDescription("Reload the ranked queue")
  @Permission(Permissions.ADMIN)
  public void rankedReloadCommand(Audience audience, CommandSender sender) {
    QueueJoinListener.reloadQueueFromVoice(null);
  }

  private void handleJoinLeaveCommand(Audience audience, Player sender, boolean isJoin) {
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
