package org.nicolie.towersforpgm.matchbot.embeds;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import me.tbg.match.bot.configs.MessagesConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.ChatColor;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;
import org.nicolie.towersforpgm.rankeds.Rank;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;

public class Forfeit {
  public static EmbedBuilder create(
      Match match,
      String table,
      List<PlayerEloChange> eloList,
      String sanctionedUser,
      String sanctionedTeamName,
      int penaltyDelta) {

    MatchInfo info = MatchInfo.getMatchInfo(match);
    String mapName = info.getMap();
    String duration = info.getDuration();

    EmbedBuilder embed = new EmbedBuilder()
        .setColor(Color.ORANGE)
        .setTitle(
            LanguageManager.langMessage("ranked.matchbot.cancelled").replace("{table}", table))
        .setTimestamp(Instant.now())
        .setAuthor(
            MessagesConfig.message("author.name"), null, MessagesConfig.message("author.icon_url"))
        .setDescription(MessagesConfig.message("embeds.finish.description")
            .replace(
                "<timestamp>",
                "<t:" + Instant.now().getEpochSecond() + ":f>, <t:"
                    + Instant.now().getEpochSecond() + ":R>"))
        .addField("🗺️ " + MessagesConfig.message("embeds.finish.map"), mapName, true)
        .addField("⏱️ " + MessagesConfig.message("embeds.finish.duration"), duration, true);

    // Index elo by username (case-insensitive)
    Map<String, PlayerEloChange> eloByUser = eloList == null
        ? java.util.Collections.emptyMap()
        : eloList.stream()
            .collect(Collectors.toMap(e -> e.getUsername().toLowerCase(), e -> e, (a, b) -> a));

    List<Team> teams = new ArrayList<>(match.needModule(TeamMatchModule.class).getTeams());
    String team1Name = teams.size() > 0 ? teams.get(0).getDefaultName() : "Team 1";
    String team2Name = teams.size() > 1 ? teams.get(1).getDefaultName() : "Team 2";
    List<String> team1Players = new ArrayList<>();
    List<String> team2Players = new ArrayList<>();
    if (teams.size() > 0)
      for (MatchPlayer mp : teams.get(0).getPlayers()) team1Players.add(mp.getNameLegacy());
    if (teams.size() > 1)
      for (MatchPlayer mp : teams.get(1).getPlayers()) team2Players.add(mp.getNameLegacy());

    // Ensure sanctioned user is added to the appropriate team list
    if (sanctionedUser != null && !sanctionedUser.isEmpty()) {
      String strippedSanctionedTeam = ChatColor.stripColor(sanctionedTeamName);
      String t1 = ChatColor.stripColor(team1Name);
      String t2 = ChatColor.stripColor(team2Name);
      boolean goesToTeam1 = strippedSanctionedTeam != null
          && t1 != null
          && strippedSanctionedTeam.equalsIgnoreCase(t1);
      boolean goesToTeam2 = strippedSanctionedTeam != null
          && t2 != null
          && strippedSanctionedTeam.equalsIgnoreCase(t2);
      if (goesToTeam1) {
        if (!team1Players.contains(sanctionedUser)) team1Players.add(sanctionedUser);
      } else if (goesToTeam2) {
        if (!team2Players.contains(sanctionedUser)) team2Players.add(sanctionedUser);
      } else {
        // Fallback: if not matching, add to the smaller team to maintain balance visually
        if (team1Players.size() <= team2Players.size()) {
          if (!team1Players.contains(sanctionedUser)) team1Players.add(sanctionedUser);
        } else if (!team2Players.contains(sanctionedUser)) {
          team2Players.add(sanctionedUser);
        }
      }
    }

    // Build team fields
    embed.addField(
        team1Name, buildTeamField(team1Players, eloByUser, sanctionedUser, penaltyDelta), true);
    embed.addField(
        team2Name, buildTeamField(team2Players, eloByUser, sanctionedUser, penaltyDelta), true);

    return embed;
  }

  private static String buildTeamField(
      List<String> players,
      Map<String, PlayerEloChange> eloByUser,
      String sanctionedUser,
      int penaltyDelta) {
    StringBuilder sb = new StringBuilder();
    for (String name : players) {
      PlayerEloChange e = eloByUser.get(name.toLowerCase());
      int elo = e != null ? e.getCurrentElo() : 0;
      String rank = Rank.getRankByElo(elo).getPrefixedRank(false);
      String deltaText = "+0";
      if (sanctionedUser != null && name.equalsIgnoreCase(sanctionedUser)) {
        int delta = penaltyDelta; // likely negative
        deltaText = (delta > 0 ? "+" : "") + delta;
      }
      sb.append("**")
          .append(rank)
          .append(" ")
          .append(name)
          .append("**: ")
          .append(elo)
          .append(" (")
          .append(deltaText)
          .append(")\n");
    }
    return sb.toString();
  }
}
