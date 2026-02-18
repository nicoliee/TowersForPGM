package org.nicolie.towersforpgm.rankeds.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.commands.ranked.ForfeitCommand;
import org.nicolie.towersforpgm.rankeds.DisconnectManager;
import org.nicolie.towersforpgm.rankeds.Queue;
import tc.oc.pgm.api.match.event.MatchLoadEvent;

/** Handles ranked-related logic when a match loads. */
public class RankedMatchLoadListener implements Listener {

  @EventHandler
  public void onMatchLoad(MatchLoadEvent event) {
    String map = event.getMatch().getMap().getName();

    Queue.setRanked(false);
    ForfeitCommand.forfeitedPlayers.clear();
    DisconnectManager.clearAll();

    // Clear queue if map is not ranked
    if (!org.nicolie.towersforpgm.TowersForPGM.getInstance()
            .config()
            .ranked()
            .getRankedMaps()
            .contains(map)
        && Queue.getQueueSize() > 0) {
      Queue.clearQueue();
    }
  }
}
