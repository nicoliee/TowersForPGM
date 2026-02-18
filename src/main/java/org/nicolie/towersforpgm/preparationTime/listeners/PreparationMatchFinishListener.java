package org.nicolie.towersforpgm.preparationTime.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;
import tc.oc.pgm.api.match.event.MatchFinishEvent;

public class PreparationMatchFinishListener implements Listener {
  private final PreparationListener preparationListener;

  public PreparationMatchFinishListener(PreparationListener preparationListener) {
    this.preparationListener = preparationListener;
  }

  @EventHandler
  public void onMatchFinish(MatchFinishEvent event) {
    preparationListener.stopProtection(null, event.getMatch());
  }
}
