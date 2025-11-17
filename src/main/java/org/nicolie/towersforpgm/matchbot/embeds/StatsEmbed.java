package org.nicolie.towersforpgm.matchbot.embeds;

import java.util.logging.Level;
import net.dv8tion.jda.api.EmbedBuilder;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.rankeds.Rank;
import org.nicolie.towersforpgm.utils.ConfigManager;

public class StatsEmbed {
  public static EmbedBuilder create(String table, String user, Stats stats) {
    String username = stats != null && stats.getUsername() != null ? stats.getUsername() : user;
    boolean isRankedTable = ConfigManager.getRankedTables().contains(table);

    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle(username + " - " + table);
    embed.setThumbnail("https://mc-heads.net/avatar/" + username.toLowerCase());

    if (stats == null) {
      embed.setDescription("No se encontraron estadísticas para este usuario.");
      return embed;
    }

    try {
      addStatsToEmbed(embed, stats, isRankedTable);
    } catch (Exception e) {
      TowersForPGM.getInstance()
          .getLogger()
          .log(Level.SEVERE, "Error al procesar estadísticas para embed de " + username, e);
      embed.addField("Error", "Hubo un error al procesar las estadísticas.", false);
    }

    return embed;
  }

  public static EmbedBuilder createError(String table, String username, String errorMessage) {
    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle(username + " - " + table);
    embed.setThumbnail("https://mc-heads.net/avatar/" + username.toLowerCase());
    embed.addField("Error", errorMessage, false);
    return embed;
  }

  private static void addStatsToEmbed(EmbedBuilder embed, Stats stats, boolean isRankedTable) {
    int games = stats.getGames();

    if (isRankedTable && stats.getElo() != -9999) {
      int elo = stats.getElo();
      int maxElo = stats.getMaxElo();
      if (maxElo != -9999) {
        Rank rank = Rank.getRankByElo(elo);
        embed.setColor(rank.getEmbedColor());
        embed.addField(
            "Elo:",
            "⭐ **" + rank.getPrefixedRank(false) + "** " + elo + " [" + maxElo + "]",
            false);
      }
    }

    embed.addField("_ _", "**General:**", false);

    if (games != -9999) {
      embed.addField("🏟 Partidas", String.valueOf(games), true);
    }

    int wins = stats.getWins();
    if (wins != -9999) {
      embed.addField("🏆 Victorias", String.valueOf(wins), true);
    }

    if (games > 0 && games != -9999 && wins != -9999) {
      double winrate = (wins * 100.0) / games;
      embed.addField("▶ Winrate", String.format("%.0f", winrate) + "%", true);
    }

    int winstreak = stats.getWinstreak();
    int maxWinstreak = stats.getMaxWinstreak();
    if (winstreak != -9999 && maxWinstreak != -9999) {
      embed.addField("🔥 Winstreak", winstreak + " [" + maxWinstreak + "]", true);
    }

    if (MatchBotConfig.isStatsPointsEnabled()) {
      int points = stats.getPoints();
      if (points != -9999) {
        double pointsPerGame = (games > 0 && games != -9999) ? (double) points / games : 0;
        embed.addField(
            "🎖 Puntos", points + " (" + String.format("%.2f", pointsPerGame) + ")", true);
      }
    }

    embed.addField("_ _", "**PvP:**", false);

    int kills = stats.getKills();
    int deaths = stats.getDeaths();
    int assists = stats.getAssists();
    double damageDone = stats.getDamageDone();
    double damageTaken = stats.getDamageTaken();

    if (kills != -9999) {
      double killsPerGame = (games > 0 && games != -9999) ? (double) kills / games : 0;
      embed.addField("⚔ Kills", kills + " (" + String.format("%.2f", killsPerGame) + ")", true);
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
      embed.addField("▶ K/D Ratio", String.format("%.2f", kdRatio), true);
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
