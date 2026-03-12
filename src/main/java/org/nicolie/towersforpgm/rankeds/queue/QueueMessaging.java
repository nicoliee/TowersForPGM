package org.nicolie.towersforpgm.rankeds.queue;

import net.kyori.adventure.text.Component;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.match.Match;

public class QueueMessaging {

  private final QueueState queueState;
  private final QueueManager queueManager;

  public QueueMessaging(QueueManager queueManager) {
    this.queueState = QueueState.getInstance();
    this.queueManager = queueManager;
  }

  public void sendQueueMessage(Match match, String playerName, boolean isLeave) {
    int targetSize = queueManager.getValidRankedSize(match);
    String messageKey = isLeave ? "ranked.leaveQueue" : "ranked.joinQueue";
    Component message = Queue.RANKED_PREFIX
        .append(Component.space())
        .append(Component.translatable(
            messageKey,
            MatchManager.getPrefixedName(playerName),
            Component.text(queueState.getQueueSize()),
            Component.text(targetSize)));
    match.sendMessage(message);
  }
}
