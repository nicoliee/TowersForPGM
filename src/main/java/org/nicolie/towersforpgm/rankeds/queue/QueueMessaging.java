package org.nicolie.towersforpgm.rankeds.queue;

import net.kyori.adventure.text.Component;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.match.Match;

public class QueueMessaging {
  private final TowersForPGM plugin;
  private final QueueState queueState;
  private final QueueManager queueManager;

  public QueueMessaging(QueueManager queueManager) {
    this.plugin = TowersForPGM.getInstance();
    this.queueState = QueueState.getInstance();
    this.queueManager = queueManager;
  }

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

  public void sendDisconnectMessage(Match match, String playerName) {
    sendQueueMessage(match, "§3" + (playerName != null ? playerName : "Unknown"), true);
  }

  public void sendMatchInProgressMessage(tc.oc.pgm.api.player.MatchPlayer player) {
    player.sendWarning(
        Component.text(getRankedPrefix() + LanguageManager.message("ranked.matchInProgress")));
  }

  public void sendAlreadyInQueueMessage(tc.oc.pgm.api.player.MatchPlayer player) {
    player.sendWarning(
        Component.text(getRankedPrefix() + LanguageManager.message("ranked.alreadyInQueue")));
  }

  public void sendNotRankedMapMessage(tc.oc.pgm.api.player.MatchPlayer player, String map) {
    player.sendWarning(Component.text(
        getRankedPrefix() + LanguageManager.message("ranked.notRankedMap").replace("{map}", map)));
  }

  public void sendNotInQueueMessage(tc.oc.pgm.api.player.MatchPlayer player) {
    player.sendWarning(
        Component.text(getRankedPrefix() + LanguageManager.message("ranked.notInQueue")));
  }

  public void sendCancelledMessage(Match match) {
    match.sendWarning(
        Component.text(getRankedPrefix() + LanguageManager.message("ranked.cancelled")));
  }

  public void sendCountdownMessage(Match match, int timeLeft) {
    match.sendMessage(Component.text(getRankedPrefix()
        + LanguageManager.message("ranked.countdown").replace("{time}", String.valueOf(timeLeft))));
  }

  public void sendWaitingForPlayersMessage(Match match, String playerNames) {
    match.sendActionBar(Component.text(getRankedPrefix()
        + LanguageManager.message("ranked.waitingFor").replace("{player}", playerNames)));
  }

  public void sendDataErrorMessage(Match match) {
    match.sendWarning(
        Component.text(getRankedPrefix() + LanguageManager.message("ranked.error.getData")));
  }

  private String getRankedPrefix() {
    return LanguageManager.message("ranked.prefix");
  }
}
