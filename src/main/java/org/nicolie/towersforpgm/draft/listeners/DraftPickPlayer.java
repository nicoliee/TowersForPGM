package org.nicolie.towersforpgm.draft.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.draft.events.DraftPickPlayerEvent;
import org.nicolie.towersforpgm.draft.pick.gui.PicksGUIManager;

public class DraftPickPlayer implements Listener {

  @EventHandler
  public void onDraftPickPlayer(DraftPickPlayerEvent event) {
    PicksGUIManager.giveItem(event.getMatch());
  }
}
