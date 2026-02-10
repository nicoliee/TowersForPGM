package org.nicolie.towersforpgm.matchbot.embeds;

import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.rankeds.Rank;

public class StatsEmbed {

  public static EmbedBuilder create(String table, String user, Stats stats) {
    String username = stats != null && stats.getUsername() != null ? stats.getUsername() : user;

    EmbedBuilder embed = new EmbedBuilder();
    embed.setTimestamp(Instant.now());
    embed.setThumbnail("https://vzge.me/bust/" + username.toLowerCase());
    embed.setTitle(username.replace("_", "\\_") + " - " + table);
    if (stats == null) {
      embed.setDescription("No se encontraron estadísticas para este usuario.");
      return embed;
    }

    addStatsToEmbed(embed, stats);

    return embed;
  }

  public static EmbedBuilder createError(String table, String username, String errorMessage) {
    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle(username.replace("_", "\\_") + " - " + table);
    embed.setThumbnail("https://mc-heads.net/avatar/" + username.toLowerCase());
    embed.addField("Error", errorMessage, false);
    return embed;
  }

  private static void addStatsToEmbed(EmbedBuilder embed, Stats stats) {
    int games = stats.getGames();
    if (stats.getElo() != -9999) {
      int elo = stats.getElo();
      int maxElo = stats.getMaxElo();
      if (maxElo != -9999) {
        Rank rank = Rank.getRankByElo(elo);
        embed.setColor(rank.getEmbedColor());
        embed.addField(
            "⭐ Elo:",
            "**" + rank.getPrefixedRank(false) + "** " + elo + " [" + maxElo + "]",
            false);
      }
    }
    embed.setDescription("**General:**");
    if (games != -9999) {
      embed.addField("⚽ Partidas", String.valueOf(games), true);
    }

    int wins = stats.getWins();
    if (wins != -9999) {
      embed.addField("🏆 Victorias", String.valueOf(wins), true);
    }

    if (games > 0 && games != -9999 && wins != -9999) {
      double winrate = (wins * 100.0) / games;
      embed.addField("▶ Winrate", String.format("%.0f", winrate) + "%", true);
    }

    if (games > 0 && games != -9999 && wins != -9999) {
      int losses = stats.getGames() - wins;
      double wlratio = losses > 0 ? (double) wins / losses : 0.0;
      embed.addField("📊 W/L Ratio", String.format("%.2f", wlratio), true);
    }

    int winstreak = stats.getWinstreak();
    int maxWinstreak = stats.getMaxWinstreak();
    if (winstreak != -9999 && maxWinstreak != -9999) {
      embed.addField("🔥 Winstreak", winstreak + " [" + maxWinstreak + "]", true);
    }

    if (MatchBotConfig.isStatsPointsEnabled()) {
      int points = stats.getPoints();
      int maxPoints = stats.getMaxPoints();
      if (points != -9999) {
        double pointsPerGame = (games > 0 && games != -9999) ? (double) points / games : 0;
        String pointsText = points + " (" + String.format("%.2f", pointsPerGame) + ")";
        if (maxPoints != -9999) {
          pointsText += " [" + maxPoints + "]";
        }
        embed.addField("🎯 Puntos", pointsText, true);
      }
    }

    embed.addField("_ _", "**PvP:**", false);

    int kills = stats.getKills();
    int deaths = stats.getDeaths();
    int assists = stats.getAssists();
    double damageDone = stats.getDamageDone();
    double damageTaken = stats.getDamageTaken();

    if (kills != -9999) {
      int maxKills = stats.getMaxKills();
      double killsPerGame = (games > 0 && games != -9999) ? (double) kills / games : 0;
      String killsText = kills + " (" + String.format("%.2f", killsPerGame) + ")";
      if (maxKills != -9999) {
        killsText += " [" + maxKills + "]";
      }
      embed.addField("⚔ Kills", killsText, true);
    }

    if (deaths != -9999) {
      double deathsPerGame = (games > 0 && games != -9999) ? (double) deaths / games : 0;
      embed.addField(
          "💀 Muertes", deaths + " (" + String.format("%.2f", deathsPerGame) + ")", true);
    }

    if (assists != -9999) {
      double assistsPerGame = (games > 0 && games != -9999) ? (double) assists / games : 0;
      embed.addField(
          "💪 Asistencias", assists + " (" + String.format("%.2f", assistsPerGame) + ")", true);
    }

    if (kills != -9999 && deaths != -9999) {
      double kdRatio = stats.getKDRatio();
      embed.addField("📊 K/D Ratio", String.format("%.2f", kdRatio), true);
    }

    if (damageDone != -9999.0) {
      double damageDonePerGame = (games > 0 && games != -9999) ? damageDone / games : 0;
      embed.addField("💥 Daño hecho", String.format("%.2f", damageDonePerGame), true);
    }

    if (damageTaken != -9999.0) {
      double damageTakenPerGame = (games > 0 && games != -9999) ? damageTaken / games : 0;
      embed.addField("💨 Daño recibido", String.format("%.2f", damageTakenPerGame), true);
    }
  }
}
