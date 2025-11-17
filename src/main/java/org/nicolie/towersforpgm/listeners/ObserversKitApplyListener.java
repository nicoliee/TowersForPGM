package org.nicolie.towersforpgm.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.PicksGUI;
import org.nicolie.towersforpgm.rankeds.ItemListener;
import org.nicolie.towersforpgm.utils.ConfigManager;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;

public class ObserversKitApplyListener implements Listener {
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

    if (ConfigManager.getRankedMaps().contains(map)) {
      ItemListener.giveRankedItem(player);
    }
  }
}
