package org.nicolie.towersforpgm.draft.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.nicolie.towersforpgm.draft.reroll.DraftReroll;
import org.nicolie.towersforpgm.draft.state.DraftPhase;
import org.nicolie.towersforpgm.session.draft.DraftContext;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

public class RerollItemListener implements Listener {

  private final DraftContext ctx;

  public RerollItemListener(DraftContext ctx) {
    this.ctx = ctx;
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_AIR
        && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

    Player player = event.getPlayer();
    ItemStack item = event.getItem();

    if (!DraftReroll.isRerollItem(item)) return;
    if (ctx.phase() == DraftPhase.IDLE) return;

    event.setCancelled(true);

    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);

    if (ctx.phase() == DraftPhase.CAPTAINS) {
      boolean success = ctx.rerollManager().onPlayerRequestReroll(player.getUniqueId());
      if (!success) matchPlayer.sendWarning(Component.translatable("draft.reroll.denied"));
      return;
    }

    if (ctx.phase() == DraftPhase.REROLL) {
      ctx.rerollOptionsGUI().reopenGUI(player);
    }
  }
}
