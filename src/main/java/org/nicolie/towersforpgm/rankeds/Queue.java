package org.nicolie.towersforpgm.rankeds;

import java.util.List;
import java.util.UUID;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.core.Draft;
import org.nicolie.towersforpgm.draft.core.Matchmaking;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.rankeds.queue.CountdownManager;
import org.nicolie.towersforpgm.rankeds.queue.MatchStarter;
import org.nicolie.towersforpgm.rankeds.queue.QueueManager;
import org.nicolie.towersforpgm.rankeds.queue.QueueMessaging;
import org.nicolie.towersforpgm.rankeds.queue.QueueState;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.player.MatchPlayer;

public class Queue {
  private static Queue instance;

  private final TowersForPGM plugin = TowersForPGM.getInstance();
  private final QueueState queueState;
  private final QueueManager queueManager;
  private final QueueMessaging queueMessaging;
  private final CountdownManager countdownManager;
  private final MatchStarter matchStarter;

  public Queue(Draft draft, Matchmaking matchmaking, Teams teams) {
    this.queueState = QueueState.getInstance();
    this.queueManager = new QueueManager(teams);
    this.matchStarter = new MatchStarter(queueManager, draft, matchmaking);
    this.countdownManager = new CountdownManager(queueManager);
    this.countdownManager.setMatchStarter(matchStarter);
    this.queueMessaging = new QueueMessaging(queueManager);
    instance = this;
  }

  public void addPlayer(MatchPlayer player) {
    if (queueManager.addPlayer(player)) {
      queueMessaging.sendQueueMessage(player.getMatch(), player.getPrefixedName(), false);

      if (queueState.getQueueSize() >= plugin.config().ranked().getRankedMinSize()
          && !queueState.isRanked()
          && !queueState.isCountdownActive()) {
        countdownManager.startRankedCountdown(player.getMatch());
      }
    }
  }

  public void addPlayer(UUID playerUUID, Match match) {
    if (queueManager.addPlayer(playerUUID, match)) {
      String playerName = getPlayerNameForDisplay(playerUUID);
      if (playerName != null) {
        queueMessaging.sendQueueMessage(match, playerName, false);

        if (queueState.getQueueSize() >= plugin.config().ranked().getRankedMinSize()
            && !queueState.isRanked()
            && !queueState.isCountdownActive()) {
          countdownManager.startRankedCountdown(match);
        }
      }
    }
  }

  public void removePlayer(MatchPlayer player) {
    if (queueManager.removePlayer(player)) {
      queueMessaging.sendQueueMessage(player.getMatch(), player.getPrefixedName(), true);
    }
  }

  public boolean removePlayer(UUID playerUUID) {
    Match match = org.nicolie.towersforpgm.utils.MatchManager.getMatch();
    if (queueManager.removePlayer(playerUUID)) {
      String playerName = getPlayerNameForDisplay(playerUUID);
      if (playerName != null && match != null) {
        queueMessaging.sendQueueMessage(match, playerName, true);
      }
      return true;
    }
    return false;
  }

  public boolean processVoiceJoin(UUID playerUUID, Match match) {
    if (match == null) {
      match = org.nicolie.towersforpgm.utils.MatchManager.getMatch();
    }

    boolean added = queueManager.processVoiceJoin(playerUUID, match);

    if (added) {
      String playerName = getPlayerNameForDisplay(playerUUID);
      if (playerName != null && match != null) {
        queueMessaging.sendQueueMessage(match, playerName, false);

        // Start countdown if conditions are met
        if (queueState.getQueueSize() >= plugin.config().ranked().getRankedMinSize()
            && !queueState.isRanked()
            && !queueState.isCountdownActive()) {
          countdownManager.startRankedCountdown(match);
        }
      }
    }

    return added;
  }

  public void startRanked(Match match) {
    if (queueState.isRanked() && !queueState.isCountdownActive()) {
      plugin
          .getLogger()
          .info("[-] Queue: startRanked blocked, draft/matchmaking active, "
              + queueState.getQueueSize() + " in queue");
    }
    countdownManager.startRankedCountdown(match);
  }

  public static void sendRankedStartEmbed(MatchStartEvent event) {
    MatchStarter.sendRankedStartEmbed(event);
  }

  public List<String> getQueueList() {
    return queueManager.getQueueDisplayNames();
  }

  public List<UUID> getQueuePlayers() {
    return queueState.getQueuePlayers();
  }

  public int getTargetSize(Match match) {
    return queueManager.getValidRankedSize(match);
  }

  public static int getQueueSize() {
    return QueueState.getInstance().getQueueSize();
  }

  public static void clearQueue() {
    QueueState.getInstance().reset();
  }

  public static Boolean isRanked() {
    return QueueState.getInstance().isRanked();
  }

  public static void setRanked(Boolean value) {
    QueueState.getInstance().setRanked(value);
  }

  public static void resetRankedState() {
    QueueState.getInstance().reset();
    TowersForPGM.getInstance().getLogger().info("[+] Queue: ranked state reset");
  }

  public static Queue getQueue() {
    return instance;
  }

  public boolean startManualRanked(Match match, int customTime) {
    return countdownManager.startManualRankedCountdown(match, customTime);
  }

  public boolean cancelManualRanked(Match match) {
    return countdownManager.cancelCountdown(match);
  }

  public static boolean isCountdownActive() {
    return QueueState.getInstance().isCountdownActive();
  }

  private String getPlayerNameForDisplay(UUID playerUUID) {
    tc.oc.pgm.api.player.MatchPlayer onlinePlayer =
        tc.oc.pgm.api.PGM.get().getMatchManager().getPlayer(playerUUID);
    if (onlinePlayer != null) {
      return onlinePlayer.getPrefixedName();
    } else {
      org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(playerUUID);
      return offlinePlayer.getName() != null ? "§3" + offlinePlayer.getName() : null;
    }
  }
}
