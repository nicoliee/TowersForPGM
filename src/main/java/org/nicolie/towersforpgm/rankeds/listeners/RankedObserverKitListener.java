package org.nicolie.towersforpgm.rankeds.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.rankeds.RankedItem;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;

public class RankedObserverKitListener implements Listener {
  private final TowersForPGM plugin = TowersForPGM.getInstance();

  @EventHandler
  public void onObserverKitApply(ObserverKitApplyEvent event) {
    MatchPlayer player = event.getPlayer();
    Match match = player.getMatch();
    String map = match.getMap().getName();

    if (plugin.config().ranked().getRankedMaps().contains(map)) {
      RankedItem.giveRankedItem(player);
    }
  }
}
