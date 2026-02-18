package org.nicolie.towersforpgm.rankeds.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.rankeds.DisconnectManager;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.rankeds.RankedItem;
import tc.oc.pgm.api.match.event.MatchStartEvent;

/** Handles ranked-related logic when a match starts. */
public class RankedMatchStartListener implements Listener {

  @EventHandler
  public void onMatchStart(MatchStartEvent event) {
    DisconnectManager.checkOfflinePlayersOnMatchStart(event.getMatch());
    RankedItem.clearAllPlayersGoingToQueue();
    Queue.sendRankedStartEmbed(event);
  }
}
