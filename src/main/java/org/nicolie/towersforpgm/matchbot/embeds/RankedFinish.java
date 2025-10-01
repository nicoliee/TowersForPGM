package org.nicolie.towersforpgm.matchbot.embeds;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.tbg.match.bot.configs.DiscordBot;
import me.tbg.match.bot.configs.MessagesConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.Stats;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;
import org.nicolie.towersforpgm.rankeds.Rank;
import org.nicolie.towersforpgm.utils.ConfigManager;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.stats.StatsMatchModule;

public class RankedFinish {
  public static EmbedBuilder create(
      Match match,
      MapInfo map,
      List<Stats> winnerStats,
      List<Stats> loserStats,
      List<PlayerEloChange> eloChange) {
    String table = ConfigManager.getRankedDefaultTable();
    EmbedBuilder embed = new EmbedBuilder()
        .setColor(Color.RED)
        .setTitle(TowersForPGM.getInstance()
            .getLanguageManager()
            .getPluginMessage("ranked.matchbot.finish")
            .replace("{table}", table))
        .setTimestamp(Instant.now())
        .setAuthor(
            MessagesConfig.message("author.name"), null, MessagesConfig.message("author.icon_url"))
        .setDescription(MessagesConfig.message("embeds.finish.description")
            .replace("<timestamp>", "<t:" + Instant.now().getEpochSecond() + ":f>"))
        .addField("🗺️ " + MessagesConfig.message("embeds.finish.map"), map.getName(), true)
        .addField(
            "⏱️ " + MessagesConfig.message("embeds.finish.duration"),
            DiscordBot.parseDuration(match.getDuration()),
            true);

    if (match.getMap().getGamemodes().contains(Gamemode.SCOREBOX)) {
      StringBuilder scores = new StringBuilder();
      for (Map.Entry<Competitor, Double> entry :
          match.getModule(ScoreMatchModule.class).getScores().entrySet()) {
        boolean isWinner = match.getWinners().contains(entry.getKey());
        Competitor team = entry.getKey();
        int score = (int) Math.round(entry.getValue());
        scores.append("**").append(team.getDefaultName()).append(":** ").append(score);
        if (isWinner) {
          scores.append(" 🏆");
        }
        scores.append("\n");
      }
      embed.addField(
          "🏆 " + MessagesConfig.message("embeds.finish.score"), scores.toString(), true);
    }

    addTeamStatsFields(
        embed, "🏆", MessagesConfig.message("embeds.finish.winner"), winnerStats, eloChange);
    embed.addField(" ", " ", false);
    addTeamStatsFields(
        embed, "⚔️", MessagesConfig.message("embeds.finish.loser"), loserStats, eloChange);

    return embed;
  }

  private static void addTeamStatsFields(
      EmbedBuilder embed,
      String titleEmoji,
      String teamName,
      List<Stats> statsList,
      List<PlayerEloChange> eloChange) {
    final int MAX_FIELD_LENGTH = 1024;
    StringBuilder chunk = new StringBuilder();
    boolean isFirstField = true;

    String killsLabel = MessagesConfig.message("stats.kills");
    String deathsLabel = MessagesConfig.message("stats.deaths");
    String assistsLabel = MessagesConfig.message("stats.assists");
    String damageDoneLabel = MessagesConfig.message("stats.damageDone");
    String damageTakenLabel = MessagesConfig.message("stats.damageTaken");
    String pointsLabel = MessagesConfig.message("stats.points");
    String bowAccuracyLabel = MessagesConfig.message("stats.bowAccuracy");

    for (Stats stats : statsList) {
      String rank = Rank.getRankByElo(stats.getElo()).getPrefixedRank(false);
      int elo = stats.getElo();
      int delta = stats.getEloChange();

      StringBuilder entry = new StringBuilder("**" + rank + " " + stats.getUsername() + "**: " + elo
          + " (" + (delta > 0 ? "+" : "") + delta + "): "
          + " `" + killsLabel + ":` " + stats.getKills()
          + " | `" + deathsLabel + ":` " + stats.getDeaths()
          + " | `" + assistsLabel + ":` " + stats.getAssists()
          + " | `" + damageDoneLabel + ":` " + String.format("%.1f", stats.getDamageDone()) + " ♥"
          + " | `" + damageTakenLabel + ":` " + String.format("%.1f", stats.getDamageTaken())
          + " ♥");

      if (Double.isNaN(stats.getBowAccuracy())) {
        entry.append(" | `" + bowAccuracyLabel + ":` NaN");
      } else {
        entry.append(" | `" + bowAccuracyLabel + ":` "
            + String.format("%.1f", stats.getBowAccuracy()) + "%");
      }

      if (stats.getPoints() != 0) {
        entry.append(" | `" + pointsLabel + ":` " + stats.getPoints());
      }

      entry.append("\n\n");

      if (chunk.length() + entry.length() > MAX_FIELD_LENGTH) {
        embed.addField(
            isFirstField ? titleEmoji + " " + teamName : "\u200B", chunk.toString(), false);
        chunk = new StringBuilder();
        isFirstField = false;
      }

      chunk.append(entry);
    }

    if (chunk.length() != 0) {
      embed.addField(
          isFirstField ? titleEmoji + " " + teamName : "\u200B", chunk.toString(), false);
    }
  }

  public static Map<String, List<Stats>> getPlayerStats(
      Match match, List<MatchPlayer> allPlayers, List<PlayerEloChange> eloChange) {
    ScoreMatchModule scoreMatchModule = match.getModule(ScoreMatchModule.class);
    StatsMatchModule statsModule = match.getModule(StatsMatchModule.class);

    // Obtener lista de desconectados
    List<String> disconnected = new ArrayList<>();
    try {
      for (Object obj : TowersForPGM.getInstance().getDisconnectedPlayers().values()) {
        disconnected.add(String.valueOf(obj));
      }
    } catch (Exception ignored) {
    }

    List<Stats> winnerStats = new ArrayList<>();
    List<Stats> loserStats = new ArrayList<>();

    // Recopilamos estadísticas de los jugadores
    for (MatchPlayer player : allPlayers) {
      PlayerStats playerStats = statsModule.getPlayerStat(player);
      int totalPoints = (int) scoreMatchModule.getContribution(player.getId());
      boolean isWinner = match.getWinners().contains(player.getCompetitor());
      String displayName = player.getNameLegacy();
      // Si está desconectado, tachar el nombre
      if (disconnected.contains(displayName)) {
        displayName = "~~" + displayName + "~~";
      }
      PlayerEloChange change = eloChange != null
          ? eloChange.stream()
              .filter(e -> e.getUsername().equalsIgnoreCase(player.getNameLegacy()))
              .findFirst()
              .orElse(null)
          : null;
      int elo = change != null ? change.getNewElo() : 0;
      int lastElo = change != null ? change.getCurrentElo() : 0;
      Stats stats = new Stats(
          displayName,
          playerStats != null ? playerStats.getKills() : 0,
          playerStats != null ? playerStats.getDeaths() : 0,
          playerStats != null ? playerStats.getAssists() : 0,
          playerStats != null
              ? ((playerStats.getDamageDone() + playerStats.getBowDamage()) / 2)
              : 0,
          playerStats != null
              ? ((playerStats.getDamageTaken() + playerStats.getBowDamageTaken()) / 2)
              : 0,
          playerStats != null ? playerStats.getArrowAccuracy() : 0,
          totalPoints,
          0, // wins (not used)
          0, // games (not used)
          0, // winstreak (not used)
          0, // maxWinstreak (not used)
          elo,
          lastElo,
          0); // maxElo (not used)
      if (isWinner) {
        winnerStats.add(stats);
      } else {
        loserStats.add(stats);
      }
    }
    Map<String, List<Stats>> statsMap = new HashMap<>();
    statsMap.put("winners", winnerStats);
    statsMap.put("losers", loserStats);
    return statsMap;
  }
}
