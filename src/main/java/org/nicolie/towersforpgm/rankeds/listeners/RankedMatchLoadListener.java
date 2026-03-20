package org.nicolie.towersforpgm.rankeds.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.rankeds.queue.RankedQueue;
import tc.oc.pgm.api.match.event.MatchLoadEvent;

public class RankedMatchLoadListener implements Listener {

  @EventHandler
  public void onMatchLoad(MatchLoadEvent event) {
    String map = event.getMatch().getMap().getName();

    Queue.setRanked(false);

    if (!org.nicolie.towersforpgm.TowersForPGM.getInstance()
            .config()
            .ranked()
            .getRankedMaps()
            .contains(map)
        && RankedQueue.getInstance().size() > 0) {
      Queue.clearQueue();
    }
  }
}
