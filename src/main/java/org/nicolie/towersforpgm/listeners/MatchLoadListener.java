package org.nicolie.towersforpgm.listeners;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchLoadEvent;



import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import org.nicolie.towersforpgm.preparationTime.TorneoListener;

public class MatchLoadListener implements Listener {
    private final RefillManager refillManager;
    private final TorneoListener torneoListener;

    public MatchLoadListener(RefillManager refillManager, TorneoListener torneoListener) {
        this.refillManager = refillManager;
        this.torneoListener = torneoListener;
    }

    @EventHandler
    public void onMatchLoad(MatchLoadEvent event) {
        Match match = event.getMatch();
        String map = match.getMap().getName();
        String world = match.getWorld().getName();
        TowersForPGM plugin = TowersForPGM.getInstance();
        plugin.setCurrentMatch(match); // Toma en cuenta que solo hay un mundo en el plugin como lo hace actualmente PGM (10/03/2025)
        plugin.setCurrentMap(map); // Toma en cuenta que solo hay un mundo en el plugin como lo hace actualmente PGM (10/03/2025)
        refillManager.loadChests(map, world);
        if(torneoListener.isMapInConfig(map) && TowersForPGM.getInstance().isPreparationEnabled()){
            SendMessage.sendToAdmins(plugin.getPluginMessage("preparation.isAvailable")
                    .replace("{map}", map));
        }else if (torneoListener.isMapInConfig(map) && !TowersForPGM.getInstance().isPreparationEnabled()){
            SendMessage.sendToAdmins(plugin.getPluginMessage("preparation.isAvailableButDisabled")
                    .replace("{map}", map));
        }
    }
}
