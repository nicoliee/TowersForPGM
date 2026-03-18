package org.nicolie.towersforpgm.draft.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.pick.gui.PicksGUIManager;
import org.nicolie.towersforpgm.draft.state.DraftPhase;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import org.nicolie.towersforpgm.session.draft.DraftContext;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;

public class DraftObserverKitListener implements Listener {

  @EventHandler
  public void onObserverKitApply(ObserverKitApplyEvent event) {
    Match match = PGM.get().getMatchManager().getMatch(event.getPlayer().getBukkit());
    if (match == null) return;

    var session = MatchSessionRegistry.get(match);
    if (session == null) return;

    DraftContext ctx = session.getDraft();
    if (ctx == null || ctx.phase() != DraftPhase.RUNNING) return;

    Bukkit.getScheduler()
        .runTaskLater(
            TowersForPGM.getInstance(),
            () -> PicksGUIManager.giveItem(event.getPlayer().getBukkit()),
            2);
  }
}
