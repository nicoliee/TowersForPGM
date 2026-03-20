package org.nicolie.towersforpgm.rankeds.disconnect;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.nicolie.towersforpgm.draft.team.Teams;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.session.MatchSession;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.Sounds;

public final class ForfeitManager {

  private ForfeitManager() {}

  public enum ForfeitResult {
    NOT_RANKED,
    ALREADY_VOTED,
    VOTED,
    TEAM_FORFEITED
  }

  public static ForfeitResult vote(Match match, MatchPlayer player) {
    if (!Queue.isRanked() || player.isObserving()) return ForfeitResult.NOT_RANKED;

    if (!DisconnectManager.addForfeit(match, player)) return ForfeitResult.ALREADY_VOTED;

    Party team = player.getParty();

    match.sendMessage(Queue.RANKED_PREFIX
        .append(Component.space())
        .append(Component.translatable("ranked.forfeit", player.getName())));
    match.playSound(Sounds.ALERT);

    if (!DisconnectManager.allForfeited(match, team)) return ForfeitResult.VOTED;

    resolveTeamForfeit(match, team);
    return ForfeitResult.TEAM_FORFEITED;
  }

  private static void resolveTeamForfeit(Match match, Party losingTeam) {
    Teams teams = teamsFor(match);
    int losingTeamNumber = teams != null ? teams.getTeamNumber(losingTeam) : -1;
    int winningTeamNumber = losingTeamNumber == 1 ? 2 : 1;
    String winningTeamName = teams != null ? teams.getTeamName(winningTeamNumber) : "";

    boolean sanctionedForThisTeam = DisconnectManager.isSanctionActive(match)
        && DisconnectManager.isSanctionForTeam(match, losingTeam);

    if (sanctionedForThisTeam) {
      SanctionHandler.apply(match, losingTeam, teams);
    } else {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "end " + winningTeamName);
    }

    DisconnectManager.clearMatch(match);
  }

  private static Teams teamsFor(Match match) {
    MatchSession session = MatchSessionRegistry.get(match);
    return session != null ? session.teams() : null;
  }
}
