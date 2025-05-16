package org.nicolie.towersforpgm.listeners;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchLoadEvent;



import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.MatchManager;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;

public class MatchLoadListener implements Listener {
    private final RefillManager refillManager;
    private final PreparationListener preparationListener;
    private final MatchManager matchManager;
    private final Draft draft;
    private final LanguageManager languageManager;

    public MatchLoadListener(RefillManager refillManager, PreparationListener preparationListener, MatchManager matchManager, Draft draft, LanguageManager languageManager) {
        this.refillManager = refillManager;
        this.preparationListener = preparationListener;
        this.matchManager = matchManager;
        this.draft = draft;
        this.languageManager = languageManager;
    }

    @EventHandler
    public void onMatchLoad(MatchLoadEvent event) {
        Match match = event.getMatch();
        String map = match.getMap().getName();
        String world = match.getWorld().getName();
        TowersForPGM plugin = TowersForPGM.getInstance();
        draft.cleanLists(); // Limpia las listas de jugadores disponibles y capitanes por si no se hizo en FinishEvent
        matchManager.setCurrentMatch(match); // Toma en cuenta que solo hay un mundo en el plugin como lo hace actualmente PGM (10/03/2025)
        // plugin.setCurrentMap(map); // Toma en cuenta que solo hay un mundo en el plugin como lo hace actualmente PGM (10/03/2025)
        refillManager.loadChests(map, world);
        if(preparationListener.isMapInConfig(map) && TowersForPGM.getInstance().isPreparationEnabled()){
            SendMessage.sendToAdmins(languageManager.getPluginMessage("preparation.isAvailable")
                    .replace("{map}", map));
        }else if (preparationListener.isMapInConfig(map) && !TowersForPGM.getInstance().isPreparationEnabled()){
            SendMessage.sendToAdmins(languageManager.getPluginMessage("preparation.isAvailableButDisabled")
                    .replace("{map}", map));
        }
        plugin.getDisconnectedPlayers().clear();
    }
}