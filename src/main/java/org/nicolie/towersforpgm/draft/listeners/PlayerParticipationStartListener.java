package org.nicolie.towersforpgm.draft.listeners;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.events.PlayerParticipationStartEvent;
import tc.oc.pgm.join.JoinRequest;

public class PlayerParticipationStartListener implements Listener {
  private final Teams teams;
  private final Captains captains;
  private final TowersForPGM plugin = TowersForPGM.getInstance();

  public PlayerParticipationStartListener(Teams teams, Captains captains) {
    this.teams = teams;
    this.captains = captains;
  }

  // Meterlo a su team si hay draft
  @EventHandler
  public void onPlayerParticipationStart(PlayerParticipationStartEvent event) {
    if (captains.isMatchWithCaptains()) {
      JoinRequest request = null;
      if (event.getRequest() instanceof JoinRequest) {
        request = (JoinRequest) event.getRequest();
      }

      if (request.isForcedOr(JoinRequest.Flag.FORCE)) {
        return;
      }

      String playerName = event.getPlayer().getBukkit().getName();
      UUID playerUUID = event.getPlayer().getBukkit().getUniqueId();
      boolean isInAnyTeam = teams.isPlayerInAnyTeam(playerName);
      int captainNumber = captains.getCaptainTeam(playerUUID);

      int playerTeam = -1;
      if (teams.isPlayerInTeam(playerName, 1) || captainNumber == 1) {
        playerTeam = 1;
      } else if (teams.isPlayerInTeam(playerName, 2) || captainNumber == 2) {
        playerTeam = 2;
      }

      if (playerTeam != -1) {
        String teamName = teams.getTeamName(playerTeam);
        String message = LanguageManager.message("draft.join.team").replace("{team}", teamName);
        event.cancel(Component.text(message));

        final int finalTeam = playerTeam;
        new BukkitRunnable() {
          @Override
          public void run() {
            teams.assignTeam(event.getPlayer().getBukkit(), finalTeam);
          }
        }.runTaskLater(plugin, 1);
        return;
      }

      if (!isInAnyTeam && captainNumber == -1) {
        event.cancel(Component.text(LanguageManager.message("draft.join.notAllowed")));
        return;
      }
    }
  }
}
