package org.nicolie.towersforpgm.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.MatchHistoryManager;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.database.models.history.MatchStats;
import org.nicolie.towersforpgm.database.models.history.TeamInfo;
import org.nicolie.towersforpgm.matchbot.embeds.MatchInfo;
import org.nicolie.towersforpgm.matchbot.embeds.RankedFinish;
import org.nicolie.towersforpgm.rankeds.Elo;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.stats.StatsMatchModule;
import tc.oc.pgm.teams.TeamMatchModule;

public class MatchFinishListener implements Listener {
  private final TowersForPGM plugin = TowersForPGM.getInstance();

  public MatchFinishListener() {}

  @EventHandler
  public void onMatchFinish(MatchFinishEvent event) {
    if (!plugin.getIsDatabaseActivated()) return;
    if (plugin.isStatsCancel()) {
      cancelStats(event);
    } else {
      matchStats(event);
    }
  }

  private void cancelStats(MatchFinishEvent event) {
    String mapName = event.getMatch().getMap().getName();
    TowersForPGM.getInstance()
        .getLogger()
        .info("[-] Stats cancelled for match-" + event.getMatch().getId() + ": " + mapName
            + ", stats not sent to database.");
    SendMessage.sendToDevelopers(LanguageManager.message("stats.consoleCancel")
        .replace("{id}", String.valueOf(event.getMatch().getId()))
        .replace("{map}", mapName)
        .replace("{size}", String.valueOf(event.getMatch().getParticipants().size())));
    plugin.setStatsCancel(false);
  }

  private void matchStats(MatchFinishEvent event) {
    Match match = event.getMatch();
    String map = match.getMap().getName();
    ScoreMatchModule scoreMatchModule = match.getModule(ScoreMatchModule.class);
    StatsMatchModule statsModule = match.getModule(StatsMatchModule.class);
    TeamMatchModule teamsModule = match.getModule(TeamMatchModule.class);

    if (teamsModule == null) {
      TowersForPGM.getInstance()
          .getLogger()
          .info("[-] Stats cancelled for match-" + event.getMatch().getId() + ": " + map
              + " (FFA mode - no teams module)");
      return;
    }

    if (scoreMatchModule != null && statsModule != null) {
      List<Stats> playerStatsList = new ArrayList<>();

      List<MatchPlayer> allPlayers = new ArrayList<>();
      allPlayers.addAll(event.getMatch().getParticipants());
      allPlayers.addAll(plugin.getDisconnectedPlayers().values());

      List<MatchPlayer> winners = new ArrayList<>();
      List<MatchPlayer> losers = new ArrayList<>();

      List<TeamInfo> teams = MatchHistoryManager.extractTeamInfo(match);
      Map<String, TeamInfo> playerTeamMap = MatchHistoryManager.createPlayerTeamMap(match);
      Map<String, MatchStats> playerMatchStats = new HashMap<>();

      for (MatchPlayer player : allPlayers) {
        PlayerStats playerStats = statsModule.getPlayerStat(player);
        int totalPoints = match.getMap().getGamemodes().contains(Gamemode.SCOREBOX)
            ? (int) scoreMatchModule.getContribution(player.getId())
            : 0;
        boolean isWinner = event.getMatch().getWinners().contains(player.getCompetitor());

        if (isWinner) {
          winners.add(player);
        } else {
          losers.add(player);
        }
        String teamName =
            player.getCompetitor() != null ? player.getCompetitor().getDefaultName() : null;

        MatchStats matchStats = MatchHistoryManager.createMatchStats(
            playerStats, player.getNameLegacy(), totalPoints, teamName);
        playerMatchStats.put(player.getNameLegacy(), matchStats);

        playerStatsList.add(new Stats(
            player.getNameLegacy(),
            playerStats != null ? playerStats.getKills() : 0, // Kills
            0, // maxKills (not used)
            playerStats != null ? playerStats.getDeaths() : 0, // Deaths
            playerStats != null ? playerStats.getAssists() : 0, // Assists
            playerStats != null
                ? ((playerStats.getDamageDone() + playerStats.getBowDamage()) / 2)
                : 0, // Damage done
            playerStats != null
                ? ((playerStats.getDamageTaken() + playerStats.getBowDamageTaken()) / 2)
                : 0, // Damage taken
            totalPoints, // Points
            0, // Max Points (not used)
            isWinner ? 1 : 0, // Wins
            1, // Games played
            isWinner ? 1 : 0, // Winstreak
            0, // maxWinstreak (not used)
            0, // elo (not used)
            0, // lastElo (not used)
            0 // maxElo (not used)
            ));
      }
      if (!playerStatsList.isEmpty()) {
        String table = plugin.config().databaseTables().getTable(map);
        Boolean ranked = Queue.isRanked();
        Boolean rankedTable = plugin.config().databaseTables().currentTableIsRanked();
        MatchInfo matchInfo = MatchInfo.getMatchInfo(match);

        if (ranked && rankedTable) {
          handleRankedMatch(
              match,
              table,
              matchInfo,
              allPlayers,
              winners,
              losers,
              playerStatsList,
              teams,
              playerTeamMap,
              playerMatchStats);
        } else {
          handleUnrankedMatch(
              table, matchInfo, playerStatsList, teams, playerTeamMap, playerMatchStats);
        }
      }
    }
  }

  private void handleRankedMatch(
      Match match,
      String table,
      MatchInfo matchInfo,
      List<MatchPlayer> allPlayers,
      List<MatchPlayer> winners,
      List<MatchPlayer> losers,
      List<Stats> playerStatsList,
      List<TeamInfo> teams,
      Map<String, TeamInfo> playerTeamMap,
      Map<String, MatchStats> playerMatchStats) {
    Elo.addWin(winners, losers).thenAccept(eloChanges -> {
      saveMatchHistory(
          table,
          matchInfo,
          true,
          playerStatsList,
          eloChanges,
          teams,
          playerTeamMap,
          playerMatchStats);

      for (PlayerEloChange eloChange : eloChanges) {
        eloChange.sendMessage();
        eloChange.applyDiscordRoleChange();
      }

      if (TowersForPGM.getInstance().isMatchBotEnabled()) {
        RankedFinish.onRankedFinish(matchInfo, table, eloChanges);
      }

      StatsManager.updateStats(table, playerStatsList, eloChanges);
    });
  }

  private void handleUnrankedMatch(
      String table,
      MatchInfo matchInfo,
      List<Stats> playerStatsList,
      List<TeamInfo> teams,
      Map<String, TeamInfo> playerTeamMap,
      Map<String, MatchStats> playerMatchStats) {
    saveMatchHistory(
        table, matchInfo, false, playerStatsList, null, teams, playerTeamMap, playerMatchStats);
    StatsManager.updateStats(table, playerStatsList, null);
  }

  private void saveMatchHistory(
      String table,
      MatchInfo matchInfo,
      boolean ranked,
      List<Stats> playerStatsList,
      List<PlayerEloChange> eloChanges,
      List<TeamInfo> teams,
      Map<String, TeamInfo> playerTeamMap,
      Map<String, MatchStats> playerMatchStats) {
    MatchHistoryManager.generateMatchId(table)
        .thenCompose(matchId -> MatchHistoryManager.saveMatch(
            matchId,
            table,
            matchInfo,
            ranked,
            playerStatsList,
            eloChanges,
            teams,
            playerTeamMap,
            playerMatchStats))
        .exceptionally(ex -> {
          TowersForPGM.getInstance()
              .getLogger()
              .warning("Error guardando historial " + (ranked ? "ranked" : "unranked") + ": "
                  + ex.getMessage());
          return null;
        });
  }
}
