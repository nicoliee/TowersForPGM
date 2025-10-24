package org.nicolie.towersforpgm.matchbot.embeds;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.tbg.match.bot.configs.MessagesConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.Stats;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;
import org.nicolie.towersforpgm.rankeds.Rank;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.stats.StatsMatchModule;

public class RankedFinish {
  public static EmbedBuilder create(
      MatchInfo matchInfo, String table, Map<String, List<Stats>> playerStats) {
    EmbedBuilder embed = new EmbedBuilder()
        .setColor(Color.RED)
        .setTitle(LanguageManager.langMessage("ranked.matchbot.finish").replace("{table}", table))
        .setTimestamp(Instant.now())
        .setAuthor(
            MessagesConfig.message("author.name"), null, MessagesConfig.message("author.icon_url"))
        .setDescription(MessagesConfig.message("embeds.finish.description")
            .replace("<timestamp>", "<t:" + Instant.now().getEpochSecond() + ":f>"))
        .addField("🗺️ " + MessagesConfig.message("embeds.finish.map"), matchInfo.getMap(), true)
        .addField(
            "⏱️ " + MessagesConfig.message("embeds.finish.duration"),
            matchInfo.getDuration(),
            true);

    if (matchInfo.hasScorebox()) {
      embed.addField(matchInfo.getScoresFieldTitle(), matchInfo.getScoresText(), true);
    }

    addTeamStatsFields(
        embed, "🏆", MessagesConfig.message("embeds.finish.winner"), playerStats.get("winners"));
    embed.addField(" ", " ", false);
    addTeamStatsFields(
        embed, "⚔️", MessagesConfig.message("embeds.finish.loser"), playerStats.get("losers"));

    return embed;
  }

  private static void addTeamStatsFields(
      EmbedBuilder embed, String titleEmoji, String teamName, List<Stats> statsList) {
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

  public static Map<String, List<Stats>> getPlayerStats(Match match, List<MatchPlayer> allPlayers) {
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
          0, // elo (will be added by addElo if needed)
          0, // lastElo (will be added by addElo if needed)
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

  public static Map<String, List<Stats>> addElo(
      Map<String, List<Stats>> statsMap, List<PlayerEloChange> eloChange) {
    if (eloChange == null) return statsMap;

    Map<String, List<Stats>> updatedStatsMap = new HashMap<>();

    for (Map.Entry<String, List<Stats>> entry : statsMap.entrySet()) {
      List<Stats> statsList = entry.getValue();
      List<Stats> updatedStatsList = new ArrayList<>();

      for (Stats stats : statsList) {
        // Remover los tildes del nombre para la comparación
        String originalName = stats.getUsername().replace("~~", "");
        PlayerEloChange change = eloChange.stream()
            .filter(e -> e.getUsername().equalsIgnoreCase(originalName))
            .findFirst()
            .orElse(null);

        if (change != null) {
          // Crear nuevo objeto Stats con los valores de elo actualizados
          Stats updatedStats = new Stats(
              stats.getUsername(),
              stats.getKills(),
              stats.getDeaths(),
              stats.getAssists(),
              stats.getDamageDone(),
              stats.getDamageTaken(),
              stats.getBowAccuracy(),
              stats.getPoints(),
              stats.getWins(),
              stats.getGames(),
              stats.getWinstreak(),
              stats.getMaxWinstreak(),
              change.getNewElo(),
              change.getCurrentElo(),
              stats.getMaxElo());
          updatedStatsList.add(updatedStats);
        } else {
          // Mantener el objeto original si no hay cambio de elo
          updatedStatsList.add(stats);
        }
      }

      updatedStatsMap.put(entry.getKey(), updatedStatsList);
    }

    return updatedStatsMap;
  }
}
