package org.nicolie.towersforpgm.matchbot.embeds;

import java.util.List;
import java.util.Map;
import me.tbg.match.bot.configs.DiscordBot;
import me.tbg.match.bot.configs.MessagesConfig;
import me.tbg.match.bot.stats.GetStats;
import me.tbg.match.bot.stats.Stats;
import org.nicolie.towersforpgm.TowersForPGM;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.score.ScoreMatchModule;

public class MatchInfo {
  private final String duration;
  private final String mapName;
  private final String scoresText;
  private final boolean hasScorebox;
  private final int durationSeconds;
  private final String winnersText;
  private final long finishedAt;
  private final Map<String, List<Stats>> playerStats;

  private MatchInfo(
      String duration,
      String mapName,
      String scoresText,
      boolean hasScorebox,
      int durationSeconds,
      String winnersText,
      long finishedAt,
      Map<String, List<Stats>> playerStats) {
    this.duration = duration;
    this.mapName = mapName;
    this.scoresText = scoresText;
    this.hasScorebox = hasScorebox;
    this.durationSeconds = durationSeconds;
    this.winnersText = winnersText;
    this.finishedAt = finishedAt;
    this.playerStats = playerStats;
  }

  public static MatchInfo getMatchInfo(Match match) {
    String duration = TowersForPGM.getInstance().isMatchBotEnabled()
        ? DiscordBot.parseDuration(match.getDuration())
        : match.getDuration().toMinutes() + " minutes";
    ;
    String mapName = match.getMap().getName();
    String scoresText = null;
    boolean hasScorebox = false;
    int durationSeconds = (int) match.getDuration().getSeconds();
    long finishedAt = java.time.Instant.now().getEpochSecond();
    Map<String, List<Stats>> playerStats =
        TowersForPGM.getInstance().isMatchBotEnabled() ? GetStats.getPlayerStats(match) : null;
    // Calcular winnersText
    String winnersText = match.getWinners().stream()
        .map(Competitor::getDefaultName)
        .reduce((a, b) -> a + ", " + b)
        .orElse("");

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

    return new MatchInfo(
        duration,
        mapName,
        scoresText,
        hasScorebox,
        durationSeconds,
        winnersText,
        finishedAt,
        playerStats);
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

  public int getDurationSeconds() {
    return durationSeconds;
  }

  public String getWinnersText() {
    return winnersText;
  }

  public long getFinishedAt() {
    return finishedAt;
  }

  public String getScoresFieldTitle() {
    return "🏆 " + MessagesConfig.message("embeds.finish.score");
  }

  public Map<String, List<Stats>> getPlayerStats() {
    return playerStats;
  }
}
