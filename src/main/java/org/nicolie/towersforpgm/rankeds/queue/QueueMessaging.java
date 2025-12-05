package org.nicolie.towersforpgm.rankeds.queue;

import net.kyori.adventure.text.Component;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.match.Match;

/**
 * Handles all messaging functionality for the queue system. Centralizes queue-related messages and
 * notifications.
 */
public class QueueMessaging {
  private final TowersForPGM plugin;
  private final QueueState queueState;
  private final QueueManager queueManager;

  public QueueMessaging(QueueManager queueManager) {
    this.plugin = TowersForPGM.getInstance();
    this.queueState = QueueState.getInstance();
    this.queueManager = queueManager;
  }

  /** Sends queue message when player joins or leaves. */
  public void sendQueueMessage(Match match, String playerName, boolean isLeave) {
    int targetSize = queueManager.getValidRankedSize(match);
    String messageKey = isLeave ? "ranked.leftQueue" : "ranked.joinedQueue";

    Component message = Component.text(getRankedPrefix()
        + LanguageManager.message(messageKey)
            .replace("{player}", playerName)
            .replace("{size}", String.valueOf(queueState.getQueueSize()))
            .replace(
                "{target}",
                String.valueOf(
                    targetSize > 0 ? targetSize : plugin.config().ranked().getRankedMinSize())));

    match.sendMessage(message);
  }

  /** Sends queue message for disconnect notification. */
  public void sendDisconnectMessage(Match match, String playerName) {
    sendQueueMessage(match, "§3" + (playerName != null ? playerName : "Unknown"), true);
  }

  /** Sends error message for match in progress. */
  public void sendMatchInProgressMessage(tc.oc.pgm.api.player.MatchPlayer player) {
    player.sendWarning(
        Component.text(getRankedPrefix() + LanguageManager.message("ranked.matchInProgress")));
  }

  /** Sends error message for already in queue. */
  public void sendAlreadyInQueueMessage(tc.oc.pgm.api.player.MatchPlayer player) {
    player.sendWarning(
        Component.text(getRankedPrefix() + LanguageManager.message("ranked.alreadyInQueue")));
  }

  /** Sends error message for not ranked map. */
  public void sendNotRankedMapMessage(tc.oc.pgm.api.player.MatchPlayer player, String map) {
    player.sendWarning(Component.text(
        getRankedPrefix() + LanguageManager.message("ranked.notRankedMap").replace("{map}", map)));
  }

  /** Sends error message for not in queue. */
  public void sendNotInQueueMessage(tc.oc.pgm.api.player.MatchPlayer player) {
    player.sendWarning(
        Component.text(getRankedPrefix() + LanguageManager.message("ranked.notInQueue")));
  }

  /** Sends cancelled message. */
  public void sendCancelledMessage(Match match) {
    match.sendWarning(
        Component.text(getRankedPrefix() + LanguageManager.message("ranked.cancelled")));
  }

  /** Sends countdown message. */
  public void sendCountdownMessage(Match match, int timeLeft) {
    match.sendMessage(Component.text(getRankedPrefix()
        + LanguageManager.message("ranked.countdown").replace("{time}", String.valueOf(timeLeft))));
  }

  /** Sends waiting for players message. */
  public void sendWaitingForPlayersMessage(Match match, String playerNames) {
    match.sendActionBar(Component.text(getRankedPrefix()
        + LanguageManager.message("ranked.waitingFor").replace("{player}", playerNames)));
  }

  /** Sends error message for data retrieval failure. */
  public void sendDataErrorMessage(Match match) {
    match.sendWarning(
        Component.text(getRankedPrefix() + LanguageManager.message("ranked.error.getData")));
  }

  private String getRankedPrefix() {
    return LanguageManager.message("ranked.prefix");
  }
}
