package org.nicolie.towersforpgm.draft.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.draft.core.Draft;
import tc.oc.pgm.api.match.event.MatchLoadEvent;

/** Handles draft-related logic when a match loads. */
public class DraftMatchLoadListener implements Listener {
  private final Draft draft;

  public DraftMatchLoadListener(Draft draft) {
    this.draft = draft;
  }

  @EventHandler
  public void onMatchLoad(MatchLoadEvent event) {
    draft.cleanLists();
  }
}
