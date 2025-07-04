package org.nicolie.towersforpgm.listeners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.rankeds.ItemListener;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.ConfigManager;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;

public class MatchAfterLoadListener implements Listener {
    private final Queue queue;

    public MatchAfterLoadListener(Queue queue) {
        this.queue = queue;
    }

    @EventHandler
    public void onMatchAfterLoad(MatchAfterLoadEvent event) {
        Match match = event.getMatch();
        if (ConfigManager.isPrivateMatch(event.getMatch().getMap().getName())) {
            setPrivateMatch(match);
        }
        if (ConfigManager.getRankedMaps().contains(match.getMap().getName())) {
            ItemListener.giveItemToPlayers(match);
        }
        if (queue.getQueueSize() >= ConfigManager.getRankedSize()) {
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
        if (ConfigManager.isPrivateMatch(map)){
            List<Team> teams = new ArrayList<>(teamModule.getTeams());
            for (Team team : teams) {
                team.setMaxSize(0, 25);
            }
        }
    }
}