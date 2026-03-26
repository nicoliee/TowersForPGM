package org.nicolie.towersforpgm.rankeds.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.rankeds.RankedItem;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;

public class RankedObserverKitListener implements Listener {

  @EventHandler
  public void onObserverKitApply(ObserverKitApplyEvent event) {
    MatchPlayer player = event.getPlayer();
    RankedItem.giveRankedItem(player);
  }
}
