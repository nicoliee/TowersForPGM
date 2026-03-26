package org.nicolie.towersforpgm.draft.map;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.nicolie.towersforpgm.draft.state.DraftPhase;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import org.nicolie.towersforpgm.session.draft.DraftContext;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public final class MapVoteItemListener implements Listener {

  @SuppressWarnings("deprecation")
  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_AIR
        && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

    Player player = event.getPlayer();
    Match match = PGM.get().getMatchManager().getMatch(player);
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    DraftContext ctx = getContextSilently(match);
    if (ctx == null) return;

    ItemStack item;
    try {
      item = player.getInventory().getItemInMainHand();
    } catch (NoSuchMethodError e) {
      item = player.getItemInHand();
    }

    if (!MapVoteManager.isVoteItem(item, player)) return;

    if (ctx.phase() != DraftPhase.MAP) {
      player.getInventory().remove(item);
      return;
    }

    MapVoteManager voteManager = ctx.mapVoteManager();
    if (voteManager == null) return;

    event.setCancelled(true);

    if (matchPlayer == null) return;

    voteManager.openMenuFor(matchPlayer);
  }

  private DraftContext getContextSilently(Match match) {
    if (match == null) return null;
    var session = MatchSessionRegistry.get(match);
    if (session == null) return null;
    DraftContext ctx = session.getDraft();
    if (ctx == null || ctx.phase() == DraftPhase.IDLE || ctx.phase() == DraftPhase.ENDED) {
      return null;
    }
    return ctx;
  }
}
