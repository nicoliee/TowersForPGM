package org.nicolie.towersforpgm.database.history;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.nicolie.towersforpgm.database.models.history.TeamInfo;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.score.ScoreMatchModule;

public class TeamInfoExtractor {

  private static final Set<Gamemode> SCORE_BLACKLIST = new HashSet<>();

  static {
    SCORE_BLACKLIST.add(Gamemode.FREE_FOR_ALL);
  }

  public static List<TeamInfo> extractTeamInfo(Match match) {
    List<TeamInfo> teams = new ArrayList<>();
    if (match == null) return teams;

    // Verificar si el gamemode está en la blacklist
    boolean shouldCalculateScore =
        match.getMap().getGamemodes().stream().noneMatch(SCORE_BLACKLIST::contains);

    // Obtener scores si existe ScoreMatchModule y el gamemode lo permite
    ScoreMatchModule scoreModule = match.getModule(ScoreMatchModule.class);
    Map<Competitor, Double> scores =
        (scoreModule != null && shouldCalculateScore) ? scoreModule.getScores() : null;

    for (Competitor competitor : match.getCompetitors()) {
      String teamName = competitor.getDefaultName();
      boolean isWinner = match.getWinners().contains(competitor);

      // Obtener color del team en formato hexadecimal
      String colorHex = String.format("#%06X", competitor.getFullColor().asRGB() & 0xFFFFFF);

      // Obtener score del equipo
      Integer score = null;
      if (scores != null && scores.containsKey(competitor)) {
        score = (int) Math.floor(scores.get(competitor));
      } else if (!shouldCalculateScore) {
        // Si no se debe calcular score, usar victoria/derrota
        score = isWinner ? 1 : 0;
      }

      teams.add(new TeamInfo(teamName, colorHex, isWinner, score));
    }

    return teams;
  }

  /**
   * Crea un mapa de jugador -> TeamInfo para acceso rápido.
   *
   * @param match El match del cual extraer la información
   * @return Mapa con username como key y TeamInfo como valor
   */
  public static Map<String, TeamInfo> createPlayerTeamMap(Match match) {
    Map<String, TeamInfo> playerTeamMap = new HashMap<>();
    List<TeamInfo> teams = extractTeamInfo(match);

    for (Competitor competitor : match.getCompetitors()) {
      String teamName = competitor.getDefaultName();
      TeamInfo teamInfo = teams.stream()
          .filter(t -> t.getTeamName().equals(teamName))
          .findFirst()
          .orElse(null);

      if (teamInfo != null) {
        for (MatchPlayer player : competitor.getPlayers()) {
          playerTeamMap.put(player.getNameLegacy(), teamInfo);
        }
      }
    }

    return playerTeamMap;
  }
}
