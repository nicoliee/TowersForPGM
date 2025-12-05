package org.nicolie.towersforpgm.listeners;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.rankeds.Queue;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;

public class MatchAfterLoadListener implements Listener {
  private final TowersForPGM plugin = TowersForPGM.getInstance();
  private final Queue queue;

  public MatchAfterLoadListener(Queue queue) {
    this.queue = queue;
  }

  @EventHandler
  public void onMatchAfterLoad(MatchAfterLoadEvent event) {
    Match match = event.getMatch();
    if (plugin.config().privateMatch().isPrivateMatch(event.getMatch().getMap().getName())) {
      setPrivateMatch(match);
    }
    if (Queue.getQueueSize() >= plugin.config().ranked().getRankedMinSize()) {
      queue.startRanked(match);
    }
  }

  private void setPrivateMatch(Match match) {
    if (match == null) {
      return;
    }
    TeamMatchModule teamModule = match.getModule(TeamMatchModule.class);
    if (teamModule == null) {
      return;
    }
    String map = match.getMap().getName();
    if (plugin.config().privateMatch().isPrivateMatch(map)) {
      List<Team> teams = new ArrayList<>(teamModule.getTeams());
      for (Team team : teams) {
        team.setMaxSize(0, 25);
      }
    }
  }
}
