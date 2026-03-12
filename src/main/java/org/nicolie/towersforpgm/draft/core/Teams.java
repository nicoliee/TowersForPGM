package org.nicolie.towersforpgm.draft.core;

import java.util.*;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;

public class Teams {

  private final Map<Integer, Set<MatchPlayer>> onlineTeams = new HashMap<>();
  private final Map<Integer, Set<String>> offlineTeams = new HashMap<>();
  private Party team1;
  private Party team2;

  public Teams() {
    onlineTeams.put(1, new LinkedHashSet<>());
    onlineTeams.put(2, new LinkedHashSet<>());
    offlineTeams.put(1, new LinkedHashSet<>());
    offlineTeams.put(2, new LinkedHashSet<>());
  }

  public static boolean validateTeamsForDraft(Match match) {
    if (match == null) return false;

    TeamMatchModule teamModule = match.needModule(TeamMatchModule.class);
    if (teamModule == null) return false;

    return teamModule.getTeams().size() == 2;
  }

  public boolean initializeTeamsFromMatch(Match match) {
    if (!validateTeamsForDraft(match)) return false;
    TeamMatchModule teamModule = match.needModule(TeamMatchModule.class);
    List<Team> participatingTeams = teamModule.getTeams().stream().collect(Collectors.toList());

    this.team1 = participatingTeams.get(0);
    this.team2 = participatingTeams.get(1);
    return true;
  }

  public int getTeamNumber(Party team) {
    if (team == null) return -1;
    if (team.equals(team1)) return 1;
    if (team.equals(team2)) return 2;
    return -1;
  }

  public int getTeamNumber(String playerName) {
    if (playerName == null) return -1;

    if (onlineTeams.get(1).stream().anyMatch(mp -> mp.getNameLegacy().equalsIgnoreCase(playerName))
        || offlineTeams.get(1).contains(playerName)) {
      return 1;
    }
    if (onlineTeams.get(2).stream().anyMatch(mp -> mp.getNameLegacy().equalsIgnoreCase(playerName))
        || offlineTeams.get(2).contains(playerName)) {
      return 2;
    }
    return -1;
  }

  public Party getTeam(int teamNumber) {
    if (teamNumber == 1) return team1;
    if (teamNumber == 2) return team2;
    return null;
  }

  public String getTeamColor(int teamNumber) {
    Party team = getTeam(teamNumber);
    if (team == null) return teamNumber == 1 ? "§4" : "§9"; // Fallback colors
    return team.getColor().toString();
  }

  public String getTeamName(int teamNumber) {
    Party team = getTeam(teamNumber);
    return team != null ? team.getDefaultName() : "Team " + teamNumber;
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
    if (playerName == null) {
      return false;
    }
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
    team1 = null;
    team2 = null;
  }

  public Set<String> getAllTeam(int teamNumber) {
    Set<String> offlineTeam = offlineTeams.get(teamNumber);
    Set<MatchPlayer> onlineTeam = onlineTeams.get(teamNumber);

    if (offlineTeam == null || onlineTeam == null) {
      return Collections.emptySet();
    }

    Set<String> allPlayers = new LinkedHashSet<>(offlineTeam);
    for (MatchPlayer mp : onlineTeam) {
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

    Party targetTeam = getTeam(teamNumber);
    if (targetTeam == null) return;

    MatchPlayer matchPlayer = match.getPlayer(player);
    if (matchPlayer != null) {
      match.setParty(matchPlayer, targetTeam);
    }
  }

  public void assignToObserver(Player player) {
    Match match = PGM.get().getMatchManager().getMatch(player);
    if (match == null) return;

    MatchPlayer matchPlayer = match.getPlayer(player);
    if (matchPlayer != null) {
      match.setParty(matchPlayer, match.getDefaultParty());
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
