package org.nicolie.towersforpgm.database.models.history;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.nicolie.towersforpgm.utils.SendMessage;

public class MatchHistory {
  private final String matchId;
  private final String tableName;
  private final String mapName;
  private final int durationSeconds;
  private final boolean ranked;
  private final String scoresText;
  private final String winnersText;
  private final List<PlayerHistory> players;
  private final long finishedAt;
  private final List<TeamInfo> teams;

  /** Constructor retrocompatible - mantiene la firma original. */
  public MatchHistory(
      String matchId,
      String tableName,
      String mapName,
      int durationSeconds,
      boolean ranked,
      String scoresText,
      String winnersText,
      List<PlayerHistory> players,
      long finishedAt) {
    this(
        matchId,
        tableName,
        mapName,
        durationSeconds,
        ranked,
        scoresText,
        winnersText,
        players,
        finishedAt,
        null);
  }

  /** Constructor completo con información de teams. */
  public MatchHistory(
      String matchId,
      String tableName,
      String mapName,
      int durationSeconds,
      boolean ranked,
      String scoresText,
      String winnersText,
      List<PlayerHistory> players,
      long finishedAt,
      List<TeamInfo> teams) {
    this.matchId = matchId;
    this.tableName = tableName;
    this.mapName = mapName;
    this.durationSeconds = durationSeconds;
    this.ranked = ranked;
    this.scoresText = scoresText;
    this.winnersText = winnersText;
    this.players = players;
    this.finishedAt = finishedAt;
    this.teams = teams;
  }

  public String getMatchId() {
    return matchId;
  }

  public String getTableName() {
    return tableName;
  }

  public String getMapName() {
    return mapName;
  }

  public int getDurationSeconds() {
    return durationSeconds;
  }

  public boolean isRanked() {
    return ranked;
  }

  public String getScoresText() {
    return scoresText;
  }

  public String getWinnersText() {
    return winnersText;
  }

  public List<PlayerHistory> getPlayers() {
    return players;
  }

  public long getFinishedAt() {
    return finishedAt;
  }

  public List<TeamInfo> getTeams() {
    return teams;
  }

  /** Obtiene los jugadores de un team específico por nombre. */
  public List<PlayerHistory> getPlayersByTeam(String teamName) {
    if (players == null || teamName == null) {
      return List.of();
    }
    return players.stream().filter(p -> teamName.equals(p.getTeamName())).toList();
  }

  /** Obtiene los jugadores ganadores. */
  public List<PlayerHistory> getWinners() {
    if (players == null) {
      return List.of();
    }
    return players.stream().filter(p -> p.getWin() == 1).toList();
  }

  /** Obtiene los jugadores perdedores. */
  public List<PlayerHistory> getLosers() {
    if (players == null) {
      return List.of();
    }
    return players.stream().filter(p -> p.getWin() == 0).toList();
  }

  public List<Component> getFormattedInfo() {
    List<Component> lore = new ArrayList<>();
    Component dateComponent = formatTimeAgo(getFinishedAt())
        .color(NamedTextColor.DARK_GRAY)
        .color(NamedTextColor.GRAY)
        .decoration(TextDecoration.ITALIC, false);
    lore.add(dateComponent);

    Component durationComponent = Component.translatable(
            "history.duration",
            Component.text(SendMessage.formatTime(getDurationSeconds()))
                .color(NamedTextColor.DARK_GRAY))
        .color(NamedTextColor.GRAY)
        .decoration(TextDecoration.ITALIC, false);
    lore.add(durationComponent);

    Component scoresComponent = Component.translatable(
            "match.stats.type.generic", Component.translatable("stats.score"), getFormattedScores())
        .color(NamedTextColor.GRAY);
    lore.add(scoresComponent);
    return lore;
  }

  public Component getFormattedScores() {
    if (teams != null && !teams.isEmpty()) {
      Component scoreComponent = Component.empty();
      for (int i = 0; i < teams.size(); i++) {
        TeamInfo team = teams.get(i);
        if (i > 0) {
          scoreComponent = scoreComponent.append(
              Component.text(" - ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        }
        scoreComponent = scoreComponent.append(
            Component.text(team.getScore()).color(TextColor.fromHexString(team.getColorHex())));
      }
      return scoreComponent;
    }
    return Component.text(scoresText);
  }

  private Component formatTimeAgo(long finishedAtMillis) {
    long seconds = Math.max(0, Instant.now().getEpochSecond() - (finishedAtMillis));

    Component unit;
    if (seconds <= 0) {
      unit = Component.translatable("misc.now");
    } else if (seconds < 60) {
      unit = Component.translatable(
          seconds == 1 ? "misc.second" : "misc.seconds", Component.text(seconds));
    } else {
      long minutes = seconds / 60;
      if (minutes < 60) {
        unit = Component.translatable(
            minutes == 1 ? "misc.minute" : "misc.minutes", Component.text(minutes));
      } else {
        long hours = minutes / 60;
        if (hours < 24) {
          unit = Component.translatable(
              hours == 1 ? "misc.hour" : "misc.hours", Component.text(hours));
        } else {
          long days = hours / 24;
          if (days < 30) {
            long weeks = days / 7;
            if (weeks >= 2 && weeks <= 3) {
              unit = Component.translatable(
                  weeks == 1 ? "misc.week" : "misc.weeks", Component.text(weeks));
            } else {
              unit = Component.translatable(
                  days == 1 ? "misc.day" : "misc.days", Component.text(days));
            }
          } else {
            long months = days / 30;
            if (months < 12) {
              unit = Component.translatable(
                  months == 1 ? "misc.month" : "misc.months", Component.text(months));
            } else {
              long years = months / 12;
              if (years > 3) {
                unit = Component.translatable("misc.eon");
              } else {
                unit = Component.translatable(
                    years == 1 ? "misc.year" : "misc.years", Component.text(years));
              }
            }
          }
        }
      }
    }

    return Component.translatable("misc.timeAgo", unit);
  }
}
