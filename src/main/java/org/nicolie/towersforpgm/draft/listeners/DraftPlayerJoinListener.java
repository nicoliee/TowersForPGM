package org.nicolie.towersforpgm.draft.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.nicolie.towersforpgm.draft.components.DraftPhase;
import org.nicolie.towersforpgm.draft.core.AvailablePlayers;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Draft;
import org.nicolie.towersforpgm.draft.core.Teams;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.start.StartCountdown;

public class DraftPlayerJoinListener implements Listener {
  private final AvailablePlayers availablePlayers;
  private final Captains captains;
  private final Teams teams;

  public DraftPlayerJoinListener(
      AvailablePlayers availablePlayers, Teams teams, Captains captains) {
    this.availablePlayers = availablePlayers;
    this.captains = captains;
    this.teams = teams;
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    String username = player.getName();

    if (Draft.getPhase() == DraftPhase.RUNNING) {
      handleDraftRunningJoin(player, username);
    }

    assignTeamIfNeeded(player);
  }

  private void handleDraftRunningJoin(Player player, String username) {
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    Draft.showBossBarToPlayer(matchPlayer);
    availablePlayers.handleReconnect(player);

    if (teams.isPlayerInAnyTeam(username)) {
      teams.handleReconnect(player);
    }

    if ((teams.getTeamOfflinePlayers(1).size() - 1 == 0)
        || (teams.getTeamOfflinePlayers(2).size() - 1 == 0)) {
      Match match = PGM.get().getMatchManager().getMatch(player);
      match.getCountdown().cancelAll(StartCountdown.class);
    }
  }

  private void assignTeamIfNeeded(Player player) {
    if (teams.isPlayerInTeam(player.getName(), 1)
        || (captains.getCaptain1() != null
            && captains.getCaptain1().equals(player.getUniqueId()))) {
      teams.assignTeam(player, 1);
    } else if (teams.isPlayerInTeam(player.getName(), 2)
        || (captains.getCaptain2() != null
            && captains.getCaptain2().equals(player.getUniqueId()))) {
      teams.assignTeam(player, 2);
    }
  }
}
