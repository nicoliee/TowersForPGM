package org.nicolie.towersforpgm.refill.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.refill.RefillManager;
import tc.oc.pgm.api.match.event.MatchFinishEvent;

public class RefillMatchFinishListener implements Listener {
  private final RefillManager refillManager;

  public RefillMatchFinishListener(RefillManager refillManager) {
    this.refillManager = refillManager;
  }

  @EventHandler
  public void onMatchFinish(MatchFinishEvent event) {
    refillManager.clearWorldData(event.getMatch().getWorld().getName());
  }
}
