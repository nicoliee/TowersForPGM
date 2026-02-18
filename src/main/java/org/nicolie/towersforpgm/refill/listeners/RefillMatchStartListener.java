package org.nicolie.towersforpgm.refill.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.refill.RefillManager;
import tc.oc.pgm.api.match.event.MatchStartEvent;

public class RefillMatchStartListener implements Listener {
  private final RefillManager refillManager;

  public RefillMatchStartListener(RefillManager refillManager) {
    this.refillManager = refillManager;
  }

  @EventHandler
  public void onMatchStart(MatchStartEvent event) {
    String worldName = event.getMatch().getWorld().getName();
    refillManager.startRefillTask(worldName);
  }
}
