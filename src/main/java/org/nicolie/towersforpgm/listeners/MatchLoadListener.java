package org.nicolie.towersforpgm.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.match.event.MatchLoadEvent;

public class MatchLoadListener implements Listener {

  @EventHandler
  public void onMatchLoad(MatchLoadEvent event) {
    MatchManager.setCurrentMatch(event.getMatch());

    TowersForPGM.getInstance().getDisconnectedPlayers().clear();

    TowersForPGM.getInstance().setStatsCancel(false);

    TowersForPGM.getInstance().config().databaseTables().removeTempTable();
  }
}
