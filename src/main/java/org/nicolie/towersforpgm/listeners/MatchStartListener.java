package org.nicolie.towersforpgm.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Utilities;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;
import org.nicolie.towersforpgm.rankeds.DisconnectManager;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.refill.RefillManager;
import tc.oc.pgm.api.match.event.MatchStartEvent;

public class MatchStartListener implements Listener {
  private final TowersForPGM plugin = TowersForPGM.getInstance();
  private final PreparationListener preparationListener;
  private final RefillManager refillManager;
  private final Captains captains;

  public MatchStartListener(
      PreparationListener preparationListener, RefillManager refillManager, Captains captains) {
    this.captains = captains;
    this.preparationListener = preparationListener;
    this.refillManager = refillManager;
  }

  @EventHandler
  public void onMatchStart(MatchStartEvent event) {
    String worldName = event.getMatch().getWorld().getName();

    if (captains.isReadyActive()) {
      Utilities.cancelReadyReminder();
      captains.resetReady();
    }

    DisconnectManager.checkOfflinePlayersOnMatchStart(event.getMatch());

    refillManager.startRefillTask(worldName);
    if (plugin.config().preparationTime().isPreparationEnabled()) {
      preparationListener.startProtection(null, event.getMatch());
    }

    Queue.sendRankedStartEmbed(event);
  }
}
