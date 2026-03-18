package org.nicolie.towersforpgm.draft.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.nicolie.towersforpgm.draft.state.DraftPhase;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import org.nicolie.towersforpgm.session.draft.DraftContext;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.start.StartCountdown;

public class DraftPlayerJoinListener implements Listener {

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    Match match = PGM.get().getMatchManager().getMatch(player);
    if (match == null) return;

    var session = MatchSessionRegistry.get(match);
    if (session == null) return;

    DraftContext ctx = session.getDraft();
    if (ctx == null) return;

    if (ctx.phase() == DraftPhase.RUNNING) {
      handleDraftRunningJoin(player, ctx, match);
    }

    assignTeamIfNeeded(player, ctx);
  }

  private void handleDraftRunningJoin(Player player, DraftContext ctx, Match match) {
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    ctx.showBossBarTo(matchPlayer);
    ctx.availablePlayers().handleReconnect(player);

    String username = player.getName();
    if (ctx.teams().isPlayerInAnyTeam(username)) {
      ctx.teams().handleReconnect(player);
    }

    if (ctx.teams().getTeamOfflinePlayers(1).size() - 1 == 0
        || ctx.teams().getTeamOfflinePlayers(2).size() - 1 == 0) {
      match.getCountdown().cancelAll(StartCountdown.class);
    }
  }

  private void assignTeamIfNeeded(Player player, DraftContext ctx) {
    int captainNumber = ctx.getCaptainNumber(player.getUniqueId());

    if (ctx.teams().isPlayerInTeam(player.getName(), 1) || captainNumber == 1) {
      ctx.teams().assignTeam(player, 1);
    } else if (ctx.teams().isPlayerInTeam(player.getName(), 2) || captainNumber == 2) {
      ctx.teams().assignTeam(player, 2);
    }
  }
}
