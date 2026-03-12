package org.nicolie.towersforpgm.draft.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.components.DraftPhase;
import org.nicolie.towersforpgm.draft.core.Draft;
import org.nicolie.towersforpgm.draft.picksMenu.PicksGUIManager;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;

public class DraftObserverKitListener implements Listener {

  public DraftObserverKitListener() {}

  @EventHandler
  public void onObserverKitApply(ObserverKitApplyEvent event) {
    if (Draft.getPhase() == DraftPhase.RUNNING) {
      Bukkit.getScheduler()
          .runTaskLater(
              TowersForPGM.getInstance(),
              () -> {
                /*Hay alguna manera de hacer esto mejor?
                Lo hago así porque sino mi item es reemplazado */
                PicksGUIManager.giveItem(event.getPlayer().getBukkit());
              },
              2);
    }
  }
}
