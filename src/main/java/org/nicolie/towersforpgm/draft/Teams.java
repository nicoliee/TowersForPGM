package org.nicolie.towersforpgm.draft;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.join.JoinRequest;
import tc.oc.pgm.join.JoinResult;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;

public class Teams {

  private final Map<Integer, Set<MatchPlayer>> onlineTeams = new HashMap<>();
  private final Map<Integer, Set<String>> offlineTeams = new HashMap<>();

  public Teams() {
    onlineTeams.put(1, new HashSet<>());
    onlineTeams.put(2, new HashSet<>());
    offlineTeams.put(1, new HashSet<>());
    offlineTeams.put(2, new HashSet<>());
  }

  public void addPlayerToTeam(String playerName, int teamNumber) {
    Player player = Bukkit.getPlayer(playerName);
    MatchPlayer matchPlayer =
        player != null ? PGM.get().getMatchManager().getMatch(player).getPlayer(player) : null;
    if (matchPlayer != null && playerName.equals(matchPlayer.getNameLegacy())) {
      onlineTeams.get(teamNumber).add(matchPlayer);
    } else {
      offlineTeams.get(teamNumber).add(playerName);
    }
  }

  public void removePlayerFromTeam(String playerName, int teamNumber) {
    Player player = Bukkit.getPlayer(playerName);
    MatchPlayer matchPlayer =
        player != null ? PGM.get().getMatchManager().getMatch(player).getPlayer(player) : null;
    if (matchPlayer != null && onlineTeams.get(teamNumber).remove(matchPlayer)) {
    } else if (offlineTeams.get(teamNumber).remove(playerName)) {
    }
  }

  public boolean isPlayerInTeam(String playerName, int teamNumber) {
    Player player = Bukkit.getPlayer(playerName);
    MatchPlayer matchPlayer =
        player != null ? PGM.get().getMatchManager().getMatch(player).getPlayer(player) : null;

    return onlineTeams.get(teamNumber).contains(matchPlayer)
        || offlineTeams.get(teamNumber).contains(playerName);
  }

  public boolean isPlayerInAnyTeam(String playerName) {
    return isPlayerInTeam(playerName, 1) || isPlayerInTeam(playerName, 2);
  }

  public void clear() {
    onlineTeams.values().forEach(Set::clear);
    offlineTeams.values().forEach(Set::clear);
  }

  public Set<String> getAllTeam(int teamNumber) {
    Set<String> allPlayers = new HashSet<>(offlineTeams.get(teamNumber));
    for (MatchPlayer mp : onlineTeams.get(teamNumber)) {
      allPlayers.add(mp.getPlayer().getNameLegacy());
    }
    return allPlayers;
  }

  public Set<MatchPlayer> getTeamOnlinePlayers(int teamNumber) {
    return Collections.unmodifiableSet(onlineTeams.get(teamNumber));
  }

  public Set<String> getTeamOfflinePlayers(int teamNumber) {
    return Collections.unmodifiableSet(offlineTeams.get(teamNumber));
  }

  public void assignTeam(Player player, int teamNumber) {
    Match match = PGM.get().getMatchManager().getMatch(player);
    if (match == null) return;

    TeamMatchModule teamModule = match.needModule(TeamMatchModule.class);
    if (teamModule == null) return;

    Team targetTeam = teamModule.getTeams().stream()
        .filter(team -> (teamNumber == 1 && team.getDefaultName().equalsIgnoreCase("red"))
            || (teamNumber == 2 && team.getDefaultName().equalsIgnoreCase("blue")))
        .findFirst()
        .orElse(null);

    if (targetTeam == null) return;

    MatchPlayer matchPlayer = match.getPlayer(player);
    if (matchPlayer != null) {
      JoinRequest request = JoinRequest.fromPlayer(matchPlayer, targetTeam, JoinRequest.Flag.FORCE);
      JoinResult result = teamModule.queryJoin(matchPlayer, request);
      teamModule.join(matchPlayer, request, result);
    }
  }

  public void setTeamsSize(int teamSize) {
    Match match = MatchManager.getMatch();
    if (match == null) return;

    TeamMatchModule teamModule = match.needModule(TeamMatchModule.class);
    if (teamModule == null) return;

    teamModule.getTeams().forEach(team -> team.setMaxSize(teamSize, 25));
  }

  public void removeFromAnyTeam(String playerName) {
    if (isPlayerInTeam(playerName, 1)) {
      removePlayerFromTeam(playerName, 1);
    } else if (isPlayerInTeam(playerName, 2)) {
      removePlayerFromTeam(playerName, 2);
    }
  }

  public void removeFromTeams(Match match) {
    if (match == null) return;
    match.getPlayers().forEach(player -> match.setParty(player, match.getDefaultParty()));
  }

  public void handleReconnect(Player player) {
    String name = player.getName();
    if (offlineTeams.get(1).remove(name)) {
      onlineTeams.get(1).add(PGM.get().getMatchManager().getPlayer(player));
    } else if (offlineTeams.get(2).remove(name)) {
      onlineTeams.get(2).add(PGM.get().getMatchManager().getPlayer(player));
    }
  }

  public void forceTeam(MatchPlayer matchPlayer, int teamNumber) {
    String playerName = matchPlayer.getNameLegacy();
    onlineTeams.get(1).removeIf(player -> player.getNameLegacy().equalsIgnoreCase(playerName));
    onlineTeams.get(2).removeIf(player -> player.getNameLegacy().equalsIgnoreCase(playerName));

    onlineTeams.get(teamNumber).add(matchPlayer);
  }
}
