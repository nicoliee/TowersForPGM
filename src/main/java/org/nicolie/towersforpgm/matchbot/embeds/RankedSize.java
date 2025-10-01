package org.nicolie.towersforpgm.matchbot.embeds;

import java.awt.Color;
import java.time.Instant;
import me.tbg.match.bot.configs.DiscordBot;
import me.tbg.match.bot.configs.MessagesConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import org.nicolie.towersforpgm.MatchManager;
import org.nicolie.towersforpgm.TowersForPGM;
import tc.oc.pgm.api.match.Match;

public class RankedSize {

  public static EmbedBuilder createEmbed(int size) {
    Match match = MatchManager.getMatch();
    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle("Ranked");
    String rawMessage = TowersForPGM.getInstance()
        .getLanguageManager()
        .getPluginMessage("ranked.sizeSet")
        .replace("{size}", String.valueOf(size));
    String cleanMessage = rawMessage.replaceAll("§.", "");
    embed.setDescription(cleanMessage);
    embed.addField(
        "🗺️ " + MessagesConfig.message("embeds.finish.map"), match.getMap().getName(), true);
    embed.setAuthor(
        MessagesConfig.message("author.name"), null, MessagesConfig.message("author.icon_url"));
    embed.setColor(Color.GREEN);
    embed.setTimestamp(Instant.now());
    DiscordBot.setEmbedThumbnail(match.getMap(), embed);
    return embed;
  }
}
