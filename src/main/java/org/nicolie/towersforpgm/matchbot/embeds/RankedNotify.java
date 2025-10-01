package org.nicolie.towersforpgm.matchbot.embeds;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import me.tbg.match.bot.configs.MessagesConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;
import org.nicolie.towersforpgm.rankeds.Rank;
import tc.oc.pgm.api.match.Match;

public class RankedNotify {
  public static EmbedBuilder create(
      CommandSender sender, Match match, List<PlayerEloChange> eloChange) {
    String rankedRole = MatchBotConfig.getRankedRoleId();
    if (rankedRole == null) {
      rankedRole = "ranked";
    }
    EmbedBuilder embed = new EmbedBuilder()
        .setColor(Color.BLUE)
        .setTitle(TowersForPGM.getInstance()
            .getLanguageManager()
            .getPluginMessage("ranked.matchbot.available"))
        .setTimestamp(Instant.now())
        .setAuthor(
            MessagesConfig.message("author.name"), null, MessagesConfig.message("author.icon_url"))
        .setDescription(sender.getName()
            + TowersForPGM.getInstance()
                .getLanguageManager()
                .getPluginMessage("ranked.matchbot.hasTagged")
            + rankedRole + ">")
        .addField(
            "🗺️ " + MessagesConfig.message("embeds.start.map"), match.getMap().getName(), false);

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
}
