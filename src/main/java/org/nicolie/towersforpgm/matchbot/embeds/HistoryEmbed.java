package org.nicolie.towersforpgm.matchbot.embeds;

import java.awt.Color;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import me.tbg.match.bot.configs.DiscordBot;
import me.tbg.match.bot.configs.MessagesConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import org.nicolie.towersforpgm.database.models.history.MatchHistory;
import org.nicolie.towersforpgm.database.models.history.MatchStats;
import org.nicolie.towersforpgm.database.models.history.PlayerHistory;
import org.nicolie.towersforpgm.database.models.history.TeamInfo;
import org.nicolie.towersforpgm.rankeds.Rank;
import org.nicolie.towersforpgm.utils.LanguageManager;

public class HistoryEmbed {

  public static EmbedBuilder create(MatchHistory history) {
    String description = LanguageManager.message("matchbot.cmd.history.embed-description")
        .replace("{timestamp}", String.valueOf(history.getFinishedAt()))
        .replace("{table}", history.getTableName());

    EmbedBuilder embed = new EmbedBuilder()
        .setColor(Color.BLUE)
        .setTitle(LanguageManager.message("matchbot.cmd.history.title") + " `"
            + history.getMatchId() + "`")
        .setDescription(description)
        .setAuthor(
            MessagesConfig.message("author.name"), null, MessagesConfig.message("author.icon_url"))
        .addField("🗺️ " + MessagesConfig.message("embeds.finish.map"), history.getMapName(), true)
        .addField(
            "⏱️ " + MessagesConfig.message("embeds.finish.duration"),
            DiscordBot.parseDuration(Duration.ofSeconds(history.getDurationSeconds())),
            true);

    // Usar el nuevo sistema de teams si está disponible
    if (history.getTeams() != null && !history.getTeams().isEmpty()) {
      // Mostrar scores en un field separado
      StringBuilder scoresText = new StringBuilder();
      for (TeamInfo team : history.getTeams()) {
        if (team.getScore() != null) {
          if (scoresText.length() > 0) {
            scoresText.append("\n");
          }
          if (team.isWinner()) {
            scoresText.append("🏆 ");
          }
          scoresText.append(team.getTeamName()).append(": ").append(team.getScore());
        }
      }

      if (scoresText.length() > 0) {
        embed.addField(
            "🏆 " + MessagesConfig.message("embeds.finish.score"), scoresText.toString(), true);
      }

      // Agrupar jugadores por team
      for (TeamInfo team : history.getTeams()) {
        List<PlayerHistory> teamPlayers = history.getPlayersByTeam(team.getTeamName());

        if (!teamPlayers.isEmpty()) {
          embed.addField(" ", " ", false); // Separador

          String teamTitle = (team.isWinner() ? "🏆 " : "⚔️ ") + team.getTeamName();

          addTeamStatsFields(embed, teamTitle, teamPlayers, history.isRanked());
        }
      }
    } else {
      // Fallback al sistema antiguo (retrocompatibilidad)
      if (history.getScoresText() != null && !history.getScoresText().isEmpty()) {
        embed.addField(
            "🏆 " + MessagesConfig.message("embeds.finish.score"), history.getScoresText(), true);
      } else if (history.getWinnersText() != null && !history.getWinnersText().isEmpty()) {
        embed.addField(
            "🏆 " + MessagesConfig.message("embeds.finish.winner"), history.getWinnersText(), true);
      }

      // Separar por winners/losers (sistema antiguo)
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
        addTeamStatsFieldsLegacy(
            embed,
            "🏆",
            MessagesConfig.message("embeds.finish.winner"),
            winners,
            history.isRanked());
      }

      if (!losers.isEmpty()) {
        embed.addField(" ", " ", false);
        addTeamStatsFieldsLegacy(
            embed, "⚔️", MessagesConfig.message("embeds.finish.loser"), losers, history.isRanked());
      }
    }

    return embed;
  }

  public static EmbedBuilder createError(String matchId, String errorMessage) {
    return new EmbedBuilder()
        .setColor(Color.RED)
        .setTitle("❌ " + LanguageManager.message("matchbot.cmd.history.error-title"))
        .setDescription(errorMessage.replace("{matchid}", matchId))
        .setTimestamp(Instant.now());
  }

  private static void addTeamStatsFields(
      EmbedBuilder embed, String teamTitle, List<PlayerHistory> statsList, boolean isRanked) {
    final int MAX_FIELD_LENGTH = 1024;
    StringBuilder chunk = new StringBuilder();
    boolean isFirstField = true;

    for (PlayerHistory ph : statsList) {
      String entry = toDiscordFormat(ph, isRanked);

      if (chunk.length() + entry.length() > MAX_FIELD_LENGTH) {
        embed.addField(isFirstField ? teamTitle : "\u200B", chunk.toString(), false);
        chunk = new StringBuilder();
        isFirstField = false;
      }

      chunk.append(entry);
    }

    if (chunk.length() != 0) {
      embed.addField(isFirstField ? teamTitle : "\u200B", chunk.toString(), false);
    }
  }

  // Método legacy para retrocompatibilidad con el sistema antiguo
  private static void addTeamStatsFieldsLegacy(
      EmbedBuilder embed,
      String titleEmoji,
      String teamName,
      List<PlayerHistory> statsList,
      boolean isRanked) {
    final int MAX_FIELD_LENGTH = 1024;
    StringBuilder chunk = new StringBuilder();
    boolean isFirstField = true;

    for (PlayerHistory ph : statsList) {
      String entry = toDiscordFormat(ph, isRanked);

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

  private static String toDiscordFormat(PlayerHistory ph, boolean isRanked) {
    MatchStats matchStats = ph.getMatchStats();

    if (matchStats != null) {
      if (isRanked && ph.getMaxEloAfter() != null && ph.getMaxEloAfter() > 0) {
        String rank = Rank.getRankByElo(ph.getMaxEloAfter()).getPrefixedRank(false);
        int elo = ph.getMaxEloAfter();
        int delta = ph.getEloDelta() != null ? ph.getEloDelta() : 0;
        String eloPrefix =
            rank + " " + ph.getUsername() + " " + elo + " (" + (delta > 0 ? "+" : "") + delta + ")";
        return matchStats.toStats().toDiscordFormat(eloPrefix);
      } else {
        return matchStats.toStats().toDiscordFormat();
      }
    }
    return null;
  }
}
