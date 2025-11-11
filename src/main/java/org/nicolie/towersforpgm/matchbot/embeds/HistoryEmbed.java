package org.nicolie.towersforpgm.matchbot.embeds;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import me.tbg.match.bot.configs.MessagesConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import org.nicolie.towersforpgm.database.models.history.MatchHistory;
import org.nicolie.towersforpgm.database.models.history.PlayerHistory;
import org.nicolie.towersforpgm.rankeds.Rank;
import org.nicolie.towersforpgm.utils.LanguageManager;

public class HistoryEmbed {

  public static EmbedBuilder create(MatchHistory history) {
    String description = LanguageManager.langMessage("matchbot.history.embed-description")
        .replace("{timestamp}", String.valueOf(history.getFinishedAt()))
        .replace("{table}", history.getTableName());

    EmbedBuilder embed = new EmbedBuilder()
        .setColor(Color.BLUE)
        .setTitle(LanguageManager.langMessage("matchbot.history.title") + " `"
            + history.getMatchId() + "`")
        .setDescription(description)
        .setAuthor(
            MessagesConfig.message("author.name"), null, MessagesConfig.message("author.icon_url"))
        .addField(
            "🗺️ " + LanguageManager.langMessage("matchbot.embeds.finish.map"),
            history.getMapName(),
            true)
        .addField(
            "⏱️ " + LanguageManager.langMessage("matchbot.embeds.finish.duration"),
            formatDuration(history.getDurationSeconds()),
            true);

    if (history.getScoresText() != null && !history.getScoresText().isEmpty()) {
      embed.addField(
          "🏆 " + LanguageManager.langMessage("matchbot.embeds.finish.score"),
          history.getScoresText(),
          true);
    } else if (history.getWinnersText() != null && !history.getWinnersText().isEmpty()) {
      embed.addField(
          "🏆 " + LanguageManager.langMessage("matchbot.embeds.finish.winner"),
          history.getWinnersText(),
          true);
    }

    // Separar ganadores y perdedores
    List<PlayerHistory> winners = new ArrayList<>();
    List<PlayerHistory> losers = new ArrayList<>();
    for (PlayerHistory ph : history.getPlayers()) {
      if (ph.getWin() == 1) {
        winners.add(ph);
      } else {
        losers.add(ph);
      }
    }

    if (!winners.isEmpty()) {
      addTeamStatsFields(
          embed,
          "🏆",
          LanguageManager.langMessage("matchbot.embeds.finish.winner"),
          winners,
          history.isRanked());
    }

    if (!losers.isEmpty()) {
      embed.addField(" ", " ", false);
      addTeamStatsFields(
          embed,
          "⚔️",
          LanguageManager.langMessage("matchbot.embeds.finish.loser"),
          losers,
          history.isRanked());
    }

    return embed;
  }

  public static EmbedBuilder createError(String matchId, String errorMessage) {
    return new EmbedBuilder()
        .setColor(Color.RED)
        .setTitle("❌ " + LanguageManager.langMessage("matchbot.history.error-title"))
        .setDescription(errorMessage.replace("{matchid}", matchId))
        .setTimestamp(Instant.now());
  }

  private static void addTeamStatsFields(
      EmbedBuilder embed,
      String titleEmoji,
      String teamName,
      List<PlayerHistory> statsList,
      boolean isRanked) {
    final int MAX_FIELD_LENGTH = 1024;
    StringBuilder chunk = new StringBuilder();
    boolean isFirstField = true;

    String killsLabel = LanguageManager.langMessage("stats.kills");
    String deathsLabel = LanguageManager.langMessage("stats.deaths");
    String assistsLabel = LanguageManager.langMessage("stats.assists");
    String damageDoneLabel = LanguageManager.langMessage("stats.damageDone");
    String damageTakenLabel = LanguageManager.langMessage("stats.damageTaken");
    String pointsLabel = LanguageManager.langMessage("stats.points");

    for (PlayerHistory ph : statsList) {
      StringBuilder entry = new StringBuilder();

      if (isRanked && ph.getMaxEloAfter() != null && ph.getMaxEloAfter() > 0) {
        String rank = Rank.getRankByElo(ph.getMaxEloAfter()).getPrefixedRank(false);
        int elo = ph.getMaxEloAfter();
        int delta = ph.getEloDelta() != null ? ph.getEloDelta() : 0;
        entry
            .append("**")
            .append(rank)
            .append(" ")
            .append(ph.getUsername())
            .append("**: ")
            .append(elo)
            .append(" (")
            .append(delta > 0 ? "+" : "")
            .append(delta)
            .append("): ");
      } else {
        entry.append("**").append(ph.getUsername()).append("**: ");
      }

      entry
          .append(" `")
          .append(killsLabel)
          .append(":` ")
          .append(ph.getKills())
          .append(" | `")
          .append(deathsLabel)
          .append(":` ")
          .append(ph.getDeaths())
          .append(" | `")
          .append(assistsLabel)
          .append(":` ")
          .append(ph.getAssists())
          .append(" | `")
          .append(damageDoneLabel)
          .append(":` ")
          .append(String.format("%.1f", ph.getDamageDone()))
          .append(" ♥")
          .append(" | `")
          .append(damageTakenLabel)
          .append(":` ")
          .append(String.format("%.1f", ph.getDamageTaken()))
          .append(" ♥");

      if (ph.getPoints() != 0) {
        entry.append(" | `").append(pointsLabel).append(":` ").append(ph.getPoints());
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

  private static String formatDuration(int seconds) {
    int minutes = seconds / 60;
    int secs = seconds % 60;
    return String.format("%d:%02d", minutes, secs);
  }
}
