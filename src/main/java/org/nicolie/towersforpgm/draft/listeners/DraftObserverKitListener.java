package org.nicolie.towersforpgm.draft.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.draft.components.DraftPhase;
import org.nicolie.towersforpgm.draft.components.PicksGUI;
import org.nicolie.towersforpgm.draft.core.Draft;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;

public class DraftObserverKitListener implements Listener {
  private final PicksGUI pickInventory;

  public DraftObserverKitListener(PicksGUI pickInventory) {
    this.pickInventory = pickInventory;
  }

  @EventHandler
  public void onObserverKitApply(ObserverKitApplyEvent event) {
    if (Draft.getPhase() == DraftPhase.RUNNING) {
      pickInventory.giveItemToPlayer(event.getPlayer().getBukkit());
    }
  }
}
