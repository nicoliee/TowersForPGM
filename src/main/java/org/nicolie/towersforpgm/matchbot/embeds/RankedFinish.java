package org.nicolie.towersforpgm.matchbot.embeds;

import java.awt.Color;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import me.tbg.match.bot.configs.DiscordBot;
import me.tbg.match.bot.configs.MessagesConfig;
import me.tbg.match.bot.stats.Stats;
import net.dv8tion.jda.api.EmbedBuilder;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;
import org.nicolie.towersforpgm.utils.LanguageManager;

public class RankedFinish {
  public static void onRankedFinish(
      MatchInfo matchInfo, String table, List<PlayerEloChange> eloChanges) {
    Map<String, List<Stats>> playerStats = matchInfo.getPlayerStats();
    List<Stats> winners = playerStats.get("winners");
    List<Stats> losers = playerStats.get("losers");
    EmbedBuilder embed = create(matchInfo, table, winners, losers, eloChanges);
    DiscordBot.sendMatchEmbed(embed, MatchBotConfig.getDiscordChannel(), null, null);
  }

  public static EmbedBuilder create(
      MatchInfo matchInfo,
      String table,
      List<Stats> winners,
      List<Stats> losers,
      List<PlayerEloChange> eloChanges) {
    EmbedBuilder embed = new EmbedBuilder()
        .setColor(Color.RED)
        .setTitle(LanguageManager.message("ranked.matchbot.finish").replace("{table}", table))
        .setTimestamp(Instant.now())
        .setAuthor(
            MessagesConfig.message("author.name"), null, MessagesConfig.message("author.icon_url"))
        .setDescription(MessagesConfig.message("embeds.finish.description")
            .replace(
                "<timestamp>",
                "<t:" + Instant.now().getEpochSecond() + ":f>, <t:"
                    + Instant.now().getEpochSecond() + ":R>"))
        .addField("🗺️ " + MessagesConfig.message("embeds.finish.map"), matchInfo.getMap(), true)
        .addField(
            "⏱️ " + MessagesConfig.message("embeds.finish.duration"),
            matchInfo.getDuration(),
            true);

    embed.addField(matchInfo.getScoresFieldTitle(), matchInfo.getScoresText(), true);

    addTeamStatsFields(
        embed, "🏆", MessagesConfig.message("embeds.finish.winner"), winners, eloChanges);
    embed.addField(" ", " ", false);
    addTeamStatsFields(
        embed, "⚔️", MessagesConfig.message("embeds.finish.loser"), losers, eloChanges);

    return embed;
  }

  public static void addTeamStatsFields(
      EmbedBuilder embed,
      String titleEmoji,
      String teamName,
      List<Stats> statsList,
      List<PlayerEloChange> eloChanges) {
    final int MAX_FIELD_LENGTH = 1024;
    StringBuilder chunk = new StringBuilder();
    boolean isFirstField = true;
    for (Stats stats : statsList) {
      // Buscar el PlayerEloChange correspondiente
      String playerName = stats.getUsername().replace("~~", "");
      PlayerEloChange eloChange = eloChanges.stream()
          .filter(e -> e.getUsername().equalsIgnoreCase(playerName))
          .findFirst()
          .orElse(null);

      String entry;
      if (eloChange != null) {
        // Usar el formato de PlayerEloChange con los stats del jugador
        String formattedName = stats.getUsername(); // Mantiene el ~~ si está desconectado
        entry = stats.toDiscordFormat(eloChange.discordFormat().replace(playerName, formattedName));
      } else {
        // Fallback al formato por defecto si no hay cambio de elo
        entry = stats.toDiscordFormat();
      }

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
}
