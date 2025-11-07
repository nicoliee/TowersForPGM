package org.nicolie.towersforpgm.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Utilities;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.refill.RefillManager;
import tc.oc.pgm.api.match.event.MatchStartEvent;

public class MatchStartListener implements Listener {
  private final PreparationListener preparationListener;
  private final RefillManager refillManager;
  private final TowersForPGM plugin;
  private final Captains captains;

  public MatchStartListener(
      PreparationListener preparationListener, RefillManager refillManager, Captains captains) {
    this.captains = captains;
    this.preparationListener = preparationListener;
    this.refillManager = refillManager;
    this.plugin = TowersForPGM.getInstance();
  }

  @EventHandler
  public void onMatchStart(MatchStartEvent event) {
    String worldName = event.getMatch().getWorld().getName();

    if (captains.isReadyActive()) {
      Utilities.cancelReadyReminder();
      captains.resetReady();
    }

    refillManager.startRefillTask(worldName);
    if (plugin.isPreparationEnabled()) {
      preparationListener.startProtection(null, event.getMatch());
    }

    Queue.sendRankedStartEmbed(event);
  }
}
