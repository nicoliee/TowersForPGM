package org.nicolie.towersforpgm.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.embeds.MatchInfo;
import org.nicolie.towersforpgm.matchbot.embeds.RankedFinish;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;
import org.nicolie.towersforpgm.rankeds.Elo;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.stats.StatsMatchModule;

public class MatchFinishListener implements Listener {
  private final TowersForPGM plugin;
  private final PreparationListener preparationListener;
  private final RefillManager refillManager;
  private final Draft draft;

  public MatchFinishListener(
      TowersForPGM plugin,
      PreparationListener preparationListener,
      RefillManager refillManager,
      Draft draft) {
    this.plugin = plugin;
    this.preparationListener = preparationListener;
    this.refillManager = refillManager;
    this.draft = draft;
  }

  @EventHandler
  public void onMatchFinish(MatchFinishEvent event) {
    preparationListener.stopProtection(null, event.getMatch());
    refillManager.clearWorldData(event.getMatch().getWorld().getName());
    draft.cleanLists();
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
    SendMessage.sendToDevelopers(LanguageManager.langMessage("stats.consoleCancel")
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

    if (scoreMatchModule != null && statsModule != null) {
      List<Stats> playerStatsList = new ArrayList<>();

      List<MatchPlayer> allPlayers = new ArrayList<>();
      allPlayers.addAll(event.getMatch().getParticipants());
      allPlayers.addAll(plugin.getDisconnectedPlayers().values());

      List<MatchPlayer> winners = new ArrayList<>();
      List<MatchPlayer> losers = new ArrayList<>();

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

        playerStatsList.add(new Stats(
            player.getNameLegacy(),
            playerStats != null ? playerStats.getKills() : 0, // Kills
            playerStats != null ? playerStats.getDeaths() : 0, // Deaths
            playerStats != null ? playerStats.getAssists() : 0, // Assists
            playerStats != null
                ? ((playerStats.getDamageDone() + playerStats.getBowDamage()) / 2)
                : 0, // Damage done
            playerStats != null
                ? ((playerStats.getDamageTaken() + playerStats.getBowDamageTaken()) / 2)
                : 0, // Damage taken
            0, // bowAccuracy (not used)
            totalPoints, // Points
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
        String table = ConfigManager.getActiveTable(map);
        Boolean ranked = Queue.isRanked();
        Boolean rankedTable = ConfigManager.isRankedTable(table);

        if (ranked && rankedTable) {
          Map<String, List<Stats>> basicStats = RankedFinish.getPlayerStats(match, allPlayers);
          MatchInfo matchInfo = MatchInfo.getMatchInfo(match);
          Elo.addWin(winners, losers).thenAccept(eloChanges -> {
            for (PlayerEloChange eloChange : eloChanges) {
              eloChange.sendMessage();
            }

            if (TowersForPGM.getInstance().isMatchBotEnabled()) {
              Map<String, List<Stats>> statsWithElo = RankedFinish.addElo(basicStats, eloChanges);
              EmbedBuilder embed = RankedFinish.create(matchInfo, table, statsWithElo);
              DiscordBot.sendMatchEmbed(embed, match, MatchBotConfig.getDiscordChannel(), null);
            }
            StatsManager.updateStats(table, playerStatsList, eloChanges);
          });
        } else {
          StatsManager.updateStats(table, playerStatsList, null);
        }
      }
    }
  }
}
