package org.nicolie.towersforpgm.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.components.PicksGUI;
import org.nicolie.towersforpgm.draft.core.Draft;
import org.nicolie.towersforpgm.rankeds.RankedItem;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;

public class ObserversKitApplyListener implements Listener {
  private final TowersForPGM plugin = TowersForPGM.getInstance();
  private final PicksGUI pickInventory;

  public ObserversKitApplyListener(PicksGUI pickInventory) {
    this.pickInventory = pickInventory;
  }

  @EventHandler
  public void onObserversKitApplyListener(ObserverKitApplyEvent event) {
    MatchPlayer player = event.getPlayer();
    Match match = player.getMatch();
    String map = match.getMap().getName();

    if (Draft.isDraftActive() && (match.isFinished() || !match.isRunning())) {
      pickInventory.giveItemToPlayer(player.getBukkit());
    }

    if (plugin.config().ranked().getRankedMaps().contains(map)) {
      RankedItem.giveRankedItem(player);
    }
  }
}
