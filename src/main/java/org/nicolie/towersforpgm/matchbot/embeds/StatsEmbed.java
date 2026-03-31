package org.nicolie.towersforpgm.matchbot.embeds;

import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.rankeds.Rank;

public class StatsEmbed {

  public static EmbedBuilder createNormal(String table, String user, Stats stats) {
    EmbedBuilder embed = createBaseEmbed(table, user, stats);

    if (stats == null) return embed;

    addNormalStats(embed, stats);
    return embed;
  }

  public static EmbedBuilder createPerGame(String table, String user, Stats stats) {
    EmbedBuilder embed = createBaseEmbed(table, user, stats);

    if (stats == null) return embed;

    addPerGameStats(embed, stats);
    return embed;
  }

  private static EmbedBuilder createBaseEmbed(String table, String user, Stats stats) {
    String username = stats != null && stats.getUsername() != null ? stats.getUsername() : user;

    EmbedBuilder embed = new EmbedBuilder();
    embed.setTimestamp(Instant.now());
    embed.setThumbnail("https://vzge.me/bust/" + username.toLowerCase());
    embed.setTitle(username.replace("_", "\\_") + " - " + table);

    if (stats == null) {
      embed.setDescription("No se encontraron estadísticas para este usuario.");
    }

    return embed;
  }

  private static void addNormalStats(EmbedBuilder embed, Stats stats) {
    int games = stats.getGames();

    // ELO
    if (stats.getElo() != -9999) {
      int elo = stats.getElo();

      Rank rank = Rank.getRankByElo(elo);
      Rank maxRank = Rank.getRankByElo(stats.getMaxElo());
      embed.setColor(rank.getEmbedColor());

      String eloText;

      if (stats.getMaxElo() == elo) {
        eloText = "Elo: **" + rank.getPrefixedRank(false) + "** " + elo + "!";
      } else {
        eloText = "Elo: **" + rank.getPrefixedRank(false) + "** " + elo + "\nMáximo Elo: **"
            + maxRank.getPrefixedRank(false) + "** " + stats.getMaxElo();
      }

      embed.addField("⭐ Ranked", eloText, false);
    }

    embed.setDescription("**General:**");

    if (games != -9999) embed.addField("⚽ Partidas", String.valueOf(games), true);

    if (stats.getWins() != -9999)
      embed.addField("🏆 Victorias", String.valueOf(stats.getWins()), true);

    if (games > 0 && stats.getWins() != -9999) {
      double winrate = (stats.getWins() * 100.0) / games;
      embed.addField("▶ Winrate", String.format("%.0f%%", winrate), true);
    }

    if (stats.getWinstreak() != -9999) {
      String text = String.valueOf(stats.getWinstreak());

      if (stats.getMaxWinstreak() != -9999 && stats.getMaxWinstreak() != stats.getWinstreak()) {
        text += "\nMáx: " + stats.getMaxWinstreak();
      }

      embed.addField("🔥 Winstreak", text, true);
    }

    if (MatchBotConfig.isStatsPointsEnabled() && stats.getPoints() != -9999) {
      embed.addField("🎯 Puntos", String.valueOf(stats.getPoints()), true);
    }

    embed.addField("_ _", "**PvP:**", false);

    if (stats.getKills() != -9999)
      embed.addField("⚔ Kills", String.valueOf(stats.getKills()), true);

    if (stats.getDeaths() != -9999)
      embed.addField("💀 Muertes", String.valueOf(stats.getDeaths()), true);

    if (stats.getAssists() != -9999)
      embed.addField("💪 Asistencias", String.valueOf(stats.getAssists()), true);

    if (stats.getKills() != -9999 && stats.getDeaths() != -9999) {
      embed.addField("📊 K/D Ratio", String.format("%.2f", stats.getKDRatio()), true);
    }

    if (stats.getDamageDone() != -9999.0)
      embed.addField("💥 Daño hecho", formatNumber(stats.getDamageDone()), true);

    if (stats.getDamageTaken() != -9999.0)
      embed.addField("💨 Daño recibido", formatNumber(stats.getDamageTaken()), true);
  }

  // Formatea números grandes como 1.0K o 1.0M
  private static String formatNumber(double value) {
    double abs = Math.abs(value);
    if (abs >= 1_000_000) {
      return String.format("%.1fM", value / 1_000_000);
    } else if (abs >= 1_000) {
      return String.format("%.1fK", value / 1_000);
    } else {
      return String.format("%.2f", value);
    }
  }

  private static void addPerGameStats(EmbedBuilder embed, Stats stats) {
    int games = stats.getGames();
    embed.setDescription("**Promedios por partida:**");

    if (games <= 0 || games == -9999) return;

    if (MatchBotConfig.isStatsPointsEnabled() && stats.getPoints() != -9999) {
      embed.addField("🎯 Puntos", String.format("%.2f", (double) stats.getPoints() / games), true);
    }

    if (stats.getKills() != -9999) {
      double v = (double) stats.getKills() / games;
      embed.addField("⚔ Kills", String.format("%.2f", v), true);
    }

    if (stats.getDeaths() != -9999) {
      double v = (double) stats.getDeaths() / games;
      embed.addField("💀 Muertes", String.format("%.2f", v), true);
    }

    if (stats.getAssists() != -9999) {
      double v = (double) stats.getAssists() / games;
      embed.addField("💪 Asistencias", String.format("%.2f", v), true);
    }

    if (stats.getDamageDone() != -9999.0) {
      embed.addField("💥 Daño hecho", String.format("%.2f", stats.getDamageDone() / games), true);
    }

    if (stats.getDamageTaken() != -9999.0) {
      embed.addField(
          "💨 Daño recibido", String.format("%.2f", stats.getDamageTaken() / games), true);
    }
  }
}
