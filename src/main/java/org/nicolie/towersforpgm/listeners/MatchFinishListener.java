package org.nicolie.towersforpgm.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.Stats;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.embeds.RankedFinish;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;
import org.nicolie.towersforpgm.rankeds.Elo;
import org.nicolie.towersforpgm.rankeds.ItemListener;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.stats.StatsMatchModule;

public class MatchFinishListener implements Listener {
  private final TowersForPGM plugin;
  private final LanguageManager languageManager;
  private final PreparationListener preparationListener;
  private final RefillManager refillManager;
  private final Draft draft;

  public MatchFinishListener(
      TowersForPGM plugin,
      PreparationListener preparationListener,
      RefillManager refillManager,
      Draft draft,
      LanguageManager languageManager) {
    this.plugin = plugin;
    this.preparationListener = preparationListener;
    this.refillManager = refillManager;
    this.draft = draft;
    this.languageManager = languageManager;
  }

  @EventHandler
  public void onMatchFinish(MatchFinishEvent event) {
    // Detener eventos de protección, refill y draft
    preparationListener.stopProtection(null, event.getMatch());
    refillManager.clearWorldData(event.getMatch().getWorld().getName());
    draft.cleanLists();
    if (!plugin.getIsDatabaseActivated()) {
      return;
    }
    // Estadísticas
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
    SendMessage.sendToDevelopers(languageManager
        .getPluginMessage("stats.consoleCancel")
        .replace("{id}", String.valueOf(event.getMatch().getId()))
        .replace("{map}", mapName)
        .replace("{size}", String.valueOf(event.getMatch().getParticipants().size())));
    plugin.setStatsCancel(false);
  }

  private void matchStats(MatchFinishEvent event) {
    Match match = event.getMatch();
    String map = match.getMap().getName();
    giveItemToPlayers(match);
    ScoreMatchModule scoreMatchModule = match.getModule(ScoreMatchModule.class);
    StatsMatchModule statsModule = match.getModule(StatsMatchModule.class);

    if (scoreMatchModule != null && statsModule != null) {
      List<Stats> playerStatsList = new ArrayList<>();

      List<MatchPlayer> allPlayers = new ArrayList<>();
      allPlayers.addAll(event.getMatch().getParticipants());
      allPlayers.addAll(plugin.getDisconnectedPlayers().values());

      // Listas para winners y losers
      List<MatchPlayer> winners = new ArrayList<>();
      List<MatchPlayer> losers = new ArrayList<>();

      for (MatchPlayer player : allPlayers) {
        PlayerStats playerStats = statsModule.getPlayerStat(player);
        int totalPoints = match.getMap().getGamemodes().contains(Gamemode.SCOREBOX)
            ? (int) scoreMatchModule.getContribution(player.getId())
            : 0;
        boolean isWinner = event.getMatch().getWinners().contains(player.getCompetitor());

        // Agregar a la lista correspondiente
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
        String consoleMeString;
        Boolean ranked = Queue.isRanked();
        if (ranked) {
          Elo.addWin(winners, losers).thenAccept(eloChanges -> {
            StatsManager.updateStats(table, playerStatsList, eloChanges);
            for (PlayerEloChange eloChange : eloChanges) {
              eloChange.sendMessage();
            }
            if (TowersForPGM.getInstance().isMatchBotEnabled()) {
              Map<String, List<Stats>> statsMap =
                  RankedFinish.getPlayerStats(match, allPlayers, eloChanges);
              EmbedBuilder embed = RankedFinish.create(
                  match,
                  match.getMap(),
                  statsMap.get("winners"),
                  statsMap.get("losers"),
                  eloChanges);
              DiscordBot.sendMatchEmbed(embed, match, MatchBotConfig.getDiscordChannel(), null);
            }
          });
          consoleMeString = "[+Ranked] match-" + event.getMatch().getId() + ": " + map
              + ", stats on table " + table + ": " + playerStatsList.toString();
        } else {
          StatsManager.updateStats(table, playerStatsList, null);
          consoleMeString = "[+] match-" + event.getMatch().getId() + ": " + map
              + ", stats on table " + table + ": " + playerStatsList.toString();
        }
        TowersForPGM.getInstance().getLogger().info(consoleMeString);
        SendMessage.sendToDevelopers(languageManager
            .getPluginMessage("stats.console")
            .replace("{id}", (ranked ? "ranked-" : "") + event.getMatch().getId())
            .replace("{map}", map)
            .replace("{table}", table)
            .replace("{size}", String.valueOf(playerStatsList.size())));
      }
    }
  }

  private void giveItemToPlayers(Match match) {
    String nextMap = PGM.get().getMapOrder().getNextMap().getName();
    if (ConfigManager.getRankedMaps().contains(nextMap)) {
      ItemListener.giveItemToPlayers(match);
    }
  }
}
