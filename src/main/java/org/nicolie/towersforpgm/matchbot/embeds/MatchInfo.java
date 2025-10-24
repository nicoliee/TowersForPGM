package org.nicolie.towersforpgm.matchbot.embeds;

import java.util.Map;
import me.tbg.match.bot.configs.DiscordBot;
import me.tbg.match.bot.configs.MessagesConfig;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.score.ScoreMatchModule;

public class MatchInfo {
  private final String duration;
  private final String mapName;
  private final String scoresText;
  private final boolean hasScorebox;

  private MatchInfo(String duration, String mapName, String scoresText, boolean hasScorebox) {
    this.duration = duration;
    this.mapName = mapName;
    this.scoresText = scoresText;
    this.hasScorebox = hasScorebox;
  }

  public static MatchInfo getMatchInfo(Match match) {
    String duration = DiscordBot.parseDuration(match.getDuration());
    String mapName = match.getMap().getName();
    String scoresText = null;
    boolean hasScorebox = false;

    if (match.getMap().getGamemodes().contains(Gamemode.SCOREBOX)) {
      hasScorebox = true;
      StringBuilder scores = new StringBuilder();
      for (Map.Entry<Competitor, Double> entry :
          match.getModule(ScoreMatchModule.class).getScores().entrySet()) {
        boolean isWinner = match.getWinners().contains(entry.getKey());
        Competitor team = entry.getKey();
        int score = (int) Math.round(entry.getValue());
        scores.append("**").append(team.getDefaultName()).append(":** ").append(score);
        if (isWinner) {
          scores.append(" 🏆");
        }
        scores.append("\n");
      }
      scoresText = scores.toString();
    }

    return new MatchInfo(duration, mapName, scoresText, hasScorebox);
  }

  public String getDuration() {
    return duration;
  }

  public String getMap() {
    return mapName;
  }

  public String getScoresText() {
    return scoresText;
  }

  public boolean hasScorebox() {
    return hasScorebox;
  }

  public String getScoresFieldTitle() {
    return "🏆 " + MessagesConfig.message("embeds.finish.score");
  }
}
