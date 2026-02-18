package org.nicolie.towersforpgm.preparationTime.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;
import tc.oc.pgm.api.match.event.MatchStartEvent;

/** Handles preparation time logic when a match starts. */
public class PreparationMatchStartListener implements Listener {
  private final TowersForPGM plugin = TowersForPGM.getInstance();
  private final PreparationListener preparationListener;

  public PreparationMatchStartListener(PreparationListener preparationListener) {
    this.preparationListener = preparationListener;
  }

  @EventHandler
  public void onMatchStart(MatchStartEvent event) {
    if (plugin.config().preparationTime().isPreparationEnabled()) {
      preparationListener.startProtection(null, event.getMatch());
    }
  }
}
