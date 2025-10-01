package org.nicolie.towersforpgm.matchbot.embeds;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;
import org.nicolie.towersforpgm.rankeds.Rank;
import org.nicolie.towersforpgm.utils.ConfigManager;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;

public class RankedStart {
  public static EmbedBuilder create(Match match, List<PlayerEloChange> eloChange) {
    String table = ConfigManager.getRankedDefaultTable();
    DiscordBot.storeMatchStartData(Long.parseLong(match.getId()), Instant.now().getEpochSecond());

    EmbedBuilder embed = new EmbedBuilder()
        .setColor(Color.GREEN)
        .setTitle(TowersForPGM.getInstance()
            .getLanguageManager()
            .getPluginMessage("ranked.matchbot.start")
            .replace("{table}", table))
        .setTimestamp(Instant.now())
        .setDescription(me.tbg.match.bot.configs.MessagesConfig.message("embeds.start.description")
            .replace(
                "<timestamp>",
                "<t:" + DiscordBot.getMatchStartTimestamp(Long.parseLong(match.getId())) + ":f>"))
        .addField(
            "🗺️ " + me.tbg.match.bot.configs.MessagesConfig.message("embeds.start.map"),
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
}
