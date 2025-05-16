package org.nicolie.towersforpgm.listeners;

import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.bukkit.event.EventHandler;
import tc.oc.pgm.api.match.event.MatchStartEvent;

import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;

public class MatchStartListener implements Listener{
    private final PreparationListener preparationListener;
    private final RefillManager refillManager;
    private final TowersForPGM plugin;
    private final Captains captains;

    public MatchStartListener(PreparationListener preparationListener, RefillManager refillManager, Captains captains) {
        this.captains = captains;
        this.preparationListener = preparationListener;
        this.refillManager = refillManager;
        this.plugin = TowersForPGM.getInstance();
        
    }

    @EventHandler
    public void onMatchStart(MatchStartEvent event) {
        String worldName = event.getMatch().getWorld().getName();
        captains.setReadyActive(false);
        captains.setReady1(false, null);
        captains.setReady2(false, null);
        refillManager.startRefillTask(worldName);
        if (plugin.isPreparationEnabled()) {
            preparationListener.startProtection(null, event.getMatch());
        }
    }
}