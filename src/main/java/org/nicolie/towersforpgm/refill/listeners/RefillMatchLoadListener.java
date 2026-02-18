package org.nicolie.towersforpgm.refill.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.refill.RefillManager;
import tc.oc.pgm.api.match.event.MatchLoadEvent;

public class RefillMatchLoadListener implements Listener {
  private final RefillManager refillManager;

  public RefillMatchLoadListener(RefillManager refillManager) {
    this.refillManager = refillManager;
  }

  @EventHandler
  public void onMatchLoad(MatchLoadEvent event) {
    String map = event.getMatch().getMap().getName();
    String world = event.getMatch().getWorld().getName();
    refillManager.loadChests(map, world);
  }
}
