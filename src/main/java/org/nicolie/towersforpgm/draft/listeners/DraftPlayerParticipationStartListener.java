package org.nicolie.towersforpgm.draft.listeners;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import org.nicolie.towersforpgm.session.draft.DraftContext;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.events.PlayerParticipationStartEvent;
import tc.oc.pgm.join.JoinRequest;

public class DraftPlayerParticipationStartListener implements Listener {

  @EventHandler
  public void onPlayerParticipationStart(PlayerParticipationStartEvent event) {
    var session = MatchSessionRegistry.get(event.getMatch());
    if (session == null) return;

    DraftContext ctx = session.getDraft();
    if (ctx == null || !ctx.captains().isMatchWithCaptains()) return;

    JoinRequest request =
        event.getRequest() instanceof JoinRequest ? (JoinRequest) event.getRequest() : null;
    if (request.isForcedOr(JoinRequest.Flag.FORCE)) return;

    event.cancel(Component.translatable("draft.join.notAllowed"));

    String playerName = event.getPlayer().getBukkit().getName();
    UUID playerUUID = event.getPlayer().getBukkit().getUniqueId();
    int captainNumber = ctx.getCaptainNumber(playerUUID);

    int playerTeam = -1;
    if (ctx.teams().isPlayerInTeam(playerName, 1) || captainNumber == 1) {
      playerTeam = 1;
    } else if (ctx.teams().isPlayerInTeam(playerName, 2) || captainNumber == 2) {
      playerTeam = 2;
    }

    if (playerTeam != -1) {
      Party team = ctx.teams().getTeam(playerTeam);
      event.cancel(
          Component.translatable("draft.join.team", ctx.captainName(playerTeam), team.getName())
              .color(NamedTextColor.GRAY));

      final int finalTeam = playerTeam;
      new BukkitRunnable() {
        @Override
        public void run() {
          ctx.teams().assignTeam(event.getPlayer().getBukkit(), finalTeam);
        }
      }.runTaskLater(TowersForPGM.getInstance(), 1);
      return;
    }
  }
}
