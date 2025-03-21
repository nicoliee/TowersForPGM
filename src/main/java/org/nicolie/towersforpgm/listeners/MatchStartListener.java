package org.nicolie.towersforpgm.listeners;

import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.bukkit.event.EventHandler;
import tc.oc.pgm.api.match.event.MatchStartEvent;

import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.preparationTime.TorneoListener;

public class MatchStartListener implements Listener{
    private final TorneoListener torneoListener;
    private final RefillManager refillManager;
    private final TowersForPGM plugin;

    public MatchStartListener(TorneoListener torneoListener, RefillManager refillManager) {
        this.torneoListener = torneoListener;
        this.refillManager = refillManager;
        this.plugin = TowersForPGM.getInstance();
        
    }

    @EventHandler
    public void onMatchStart(MatchStartEvent event) {
        String matchName = event.getMatch().getMap().getName();
        String worldName = event.getMatch().getWorld().getName();
        refillManager.startRefillTask(worldName);
        if (plugin.isPreparationEnabled()) {
            torneoListener.startProtection(null, matchName, worldName);
        }
    }
}