package org.nicolie.towersforpgm.preparationTime.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.match.event.MatchLoadEvent;

public class PreparationMatchLoadListener implements Listener {
  private final TowersForPGM plugin = TowersForPGM.getInstance();
  private final PreparationListener preparationListener;

  public PreparationMatchLoadListener(PreparationListener preparationListener) {
    this.preparationListener = preparationListener;
  }

  @EventHandler
  public void onMatchLoad(MatchLoadEvent event) {
    String map = event.getMatch().getMap().getName();

    if (preparationListener.isMapInConfig(map)
        && plugin.config().preparationTime().isPreparationEnabled()) {
      SendMessage.sendToAdmins(
          LanguageManager.message("preparation.isAvailable").replace("{map}", map));
    } else if (preparationListener.isMapInConfig(map)
        && !plugin.config().preparationTime().isPreparationEnabled()) {
      SendMessage.sendToAdmins(
          LanguageManager.message("preparation.isAvailableButDisabled").replace("{map}", map));
    }
  }
}
