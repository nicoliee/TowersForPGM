package org.nicolie.towersforpgm.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.AvailablePlayers;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.Teams;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.start.StartCountdown;

public class PlayerJoinListener implements Listener {
  private final AvailablePlayers availablePlayers;
  private final Captains captains;
  private final Teams teams;
  private final TowersForPGM plugin;

  public PlayerJoinListener(
      TowersForPGM plugin, AvailablePlayers availablePlayers, Teams teams, Captains captains) {
    this.availablePlayers = availablePlayers;
    this.captains = captains;
    this.teams = teams;
    this.plugin = plugin;
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    String username = player.getName();
    if (Draft.isDraftActive()) {
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

    if (teams.isPlayerInTeam(player.getName(), 1)
        || (captains.getCaptain1() != null
            && captains.getCaptain1().equals(player.getUniqueId()))) {
      teams.assignTeam(player, 1);
    } else if (teams.isPlayerInTeam(player.getName(), 2)
        || (captains.getCaptain2() != null
            && captains.getCaptain2().equals(player.getUniqueId()))) {
      teams.assignTeam(player, 2);
    }

    if (plugin.getDisconnectedPlayers().get(player.getName()) != null) {
      plugin.getDisconnectedPlayers().remove(player.getName());
    }
  }
}
