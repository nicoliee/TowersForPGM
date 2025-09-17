package org.nicolie.towersforpgm.matchbot;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.tbg.match.bot.MatchBot;
import me.tbg.match.bot.configs.DiscordBot;
import org.bukkit.command.CommandSender;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;
import org.nicolie.towersforpgm.rankeds.Rank;
import org.nicolie.towersforpgm.utils.ConfigManager;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.stats.StatsMatchModule;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;

public class Embeds {

  public static EmbedBuilder notifyPlayers(
      CommandSender sender, Match match, List<PlayerEloChange> eloChange) {
    String rankedRole = ConfigManager.getRankedRoleID();
    if (rankedRole == null) {
      rankedRole = "ranked";
    }
    EmbedBuilder embed = new EmbedBuilder()
        .setColor(Color.BLUE)
        .setTitle(TowersForPGM.getInstance()
            .getLanguageManager()
            .getPluginMessage("ranked.matchbot.available"))
        .setTimestampToNow()
        .setAuthor(
            TowersForPGM.getInstance()
                .getLanguageManager()
                .getConfigurableMessage("matchbot.author"),
            null,
            TowersForPGM.getInstance()
                .getLanguageManager()
                .getConfigurableMessage("matchbot.authorUrl"))
        .setDescription(sender.getName()
            + TowersForPGM.getInstance()
                .getLanguageManager()
                .getPluginMessage("ranked.matchbot.hasTagged")
            + rankedRole + ">")
        .addField(
            "🗺️ "
                + TowersForPGM.getInstance()
                    .getLanguageManager()
                    .getPluginMessage("ranked.matchbot.map"),
            match.getMap().getName(),
            false);

    List<String> playerLines = new ArrayList<>();
    for (PlayerEloChange change : eloChange) {
      String rank = Rank.getRankByElo(change.getCurrentElo()).getPrefixedRank(false);
      String line = "**" + rank + " " + change.getUsername() + "**: " + change.getCurrentElo();
      playerLines.add(line);
    }

    if (!playerLines.isEmpty()) {
      embed.addField(
          "👥 "
              + TowersForPGM.getInstance()
                  .getLanguageManager()
                  .getPluginMessage("ranked.matchbot.online"),
          String.join("\n", playerLines),
          false);
    }
    return embed;
  }

  public static EmbedBuilder createMatchStartEmbed(Match match, List<PlayerEloChange> eloChange) {
    String table = ConfigManager.getRankedDefaultTable();
    DiscordBot bot = MatchBot.getInstance().getBot();
    bot.storeMatchStartData(
        Long.parseLong(match.getId()),
        Instant.now().getEpochSecond(),
        match.getPlayers().size());

    EmbedBuilder embed = new EmbedBuilder()
        .setColor(Color.GREEN)
        .setTitle(TowersForPGM.getInstance()
            .getLanguageManager()
            .getPluginMessage("ranked.matchbot.start")
            .replace("{table}", table))
        .setTimestampToNow()
        .setDescription(TowersForPGM.getInstance()
                .getLanguageManager()
                .getPluginMessage("ranked.matchbot.startedAt")
            + bot.getMatchStartTimestamp(Long.parseLong(match.getId())) + ":f>")
        .addField(
            "🗺️ "
                + TowersForPGM.getInstance()
                    .getLanguageManager()
                    .getPluginMessage("ranked.matchbot.map"),
            match.getMap().getName(),
            false);

    List<Team> teams = new ArrayList<>(match.needModule(TeamMatchModule.class).getTeams());

    for (Team team : teams) {
      StringBuilder teamField = new StringBuilder();
      for (MatchPlayer player : team.getPlayers()) {
        String name = player.getNameLegacy();
        PlayerEloChange change = eloChange != null
            ? eloChange.stream()
                .filter(e -> e.getUsername().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null)
            : null;
        int elo = change != null ? change.getCurrentElo() : 0;
        String rank = Rank.getRankByElo(elo).getPrefixedRank(false);
        teamField
            .append("**")
            .append(rank)
            .append(" ")
            .append(name)
            .append("**: ")
            .append(elo);
        teamField.append("\n");
      }
      if (teamField.length() > 0) {
        embed.addField(team.getDefaultName(), teamField.toString(), true);
      }
    }
    return embed;
  }

  public static EmbedBuilder createMatchFinishEmbed(
      Match match,
      MapInfo map,
      List<EloStats> winnerStats,
      List<EloStats> loserStats,
      List<PlayerEloChange> eloChange) {
    String table = ConfigManager.getRankedDefaultTable();
    EmbedBuilder embed = new EmbedBuilder()
        .setColor(Color.RED)
        .setTitle(TowersForPGM.getInstance()
            .getLanguageManager()
            .getPluginMessage("ranked.matchbot.finish")
            .replace("{table}", table))
        .setTimestampToNow()
        .setAuthor(
            TowersForPGM.getInstance()
                .getLanguageManager()
                .getConfigurableMessage("matchbot.author"),
            null,
            TowersForPGM.getInstance()
                .getLanguageManager()
                .getConfigurableMessage("matchbot.authorUrl"))
        .setDescription(TowersForPGM.getInstance()
                .getLanguageManager()
                .getPluginMessage("ranked.matchbot.finishedAt")
            + Instant.now().getEpochSecond() + ":f>")
        .addField(
            "🗺️ "
                + TowersForPGM.getInstance()
                    .getLanguageManager()
                    .getPluginMessage("ranked.matchbot.map"),
            map.getName(),
            true)
        .addField(
            "⏱️ "
                + TowersForPGM.getInstance()
                    .getLanguageManager()
                    .getPluginMessage("ranked.matchbot.duration"),
            DiscordBot.parseDuration(match.getDuration()),
            true);

    if (DiscordBot.getMapGamemodes(match).contains("scorebox")) {
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
          "🏆 "
              + TowersForPGM.getInstance()
                  .getLanguageManager()
                  .getPluginMessage("ranked.matchbot.points"),
          scores.toString(),
          true);
    }

    addTeamStatsFields(
        embed,
        "🏆",
        TowersForPGM.getInstance().getLanguageManager().getPluginMessage("ranked.matchbot.winner"),
        winnerStats,
        eloChange);
    embed.addField(" ", " ");
    addTeamStatsFields(
        embed,
        "⚔️",
        TowersForPGM.getInstance().getLanguageManager().getPluginMessage("ranked.matchbot.loser"),
        loserStats,
        eloChange);

    return embed;
  }

  private static void addTeamStatsFields(
      EmbedBuilder embed,
      String titleEmoji,
      String teamName,
      List<EloStats> statsList,
      List<PlayerEloChange> eloChange) {
    final int MAX_FIELD_LENGTH = 1024;
    StringBuilder chunk = new StringBuilder();
    boolean isFirstField = true;

    String killsLabel = TowersForPGM.getInstance()
        .getLanguageManager()
        .getPluginMessage("ranked.matchbot.stats.kills");
    String deathsLabel = TowersForPGM.getInstance()
        .getLanguageManager()
        .getPluginMessage("ranked.matchbot.stats.deaths");
    String assistsLabel = TowersForPGM.getInstance()
        .getLanguageManager()
        .getPluginMessage("ranked.matchbot.stats.assists");
    String damageDoneLabel = TowersForPGM.getInstance()
        .getLanguageManager()
        .getPluginMessage("ranked.matchbot.stats.damageDone");
    String damageTakenLabel = TowersForPGM.getInstance()
        .getLanguageManager()
        .getPluginMessage("ranked.matchbot.stats.damageTaken");
    String pointsLabel = TowersForPGM.getInstance()
        .getLanguageManager()
        .getPluginMessage("ranked.matchbot.stats.points");
    String bowAccuracyLabel = TowersForPGM.getInstance()
        .getLanguageManager()
        .getPluginMessage("ranked.matchbot.stats.bowAccuracy");

    for (EloStats stats : statsList) {
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
        embed.addField(isFirstField ? titleEmoji + " " + teamName : "\u200B", chunk.toString());
        chunk = new StringBuilder();
        isFirstField = false;
      }

      chunk.append(entry);
    }

    if (chunk.length() != 0) {
      embed.addField(isFirstField ? titleEmoji + " " + teamName : "\u200B", chunk.toString());
    }
  }

  public static Map<String, List<EloStats>> getPlayerStats(
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

    List<EloStats> winnerStats = new ArrayList<>();
    List<EloStats> loserStats = new ArrayList<>();

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
      int delta = change != null ? change.getEloChange() : 0;
      EloStats stats = new EloStats(
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
          elo,
          delta);
      if (isWinner) {
        winnerStats.add(stats);
      } else {
        loserStats.add(stats);
      }
    }
    Map<String, List<EloStats>> statsMap = new HashMap<>();
    statsMap.put("winners", winnerStats);
    statsMap.put("losers", loserStats);
    return statsMap;
  }
}
