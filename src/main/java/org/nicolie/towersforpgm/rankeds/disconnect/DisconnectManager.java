package org.nicolie.towersforpgm.rankeds.disconnect;

import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.session.MatchSession;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import org.nicolie.towersforpgm.session.ranked.RankedSession;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;

public final class DisconnectManager {

  public static boolean addForfeit(Match match, MatchPlayer player) {
    RankedSession session = getSession(match);
    return session != null && session.addForfeit(player);
  }

  public static boolean hasForfeited(Match match, MatchPlayer player) {
    RankedSession session = getSession(match);
    return session != null && session.hasForfeited(player);
  }

  public static boolean allForfeited(Match match, Party team) {
    RankedSession session = getSession(match);
    return session != null && session.allForfeited(team);
  }

  public static void resetTeamForfeits(Match match, Party team) {
    RankedSession session = getSession(match);
    if (session != null) session.resetTeamForfeits(team);
  }

  public static void checkOfflinePlayersOnMatchStart(Match match) {
    if (!isValidRankedMatch(match)) return;
    RankedSession session = getOrCreateSession(match);
    if (session != null) session.checkOfflinePlayers();
  }

  public static void startDisconnectTimer(Match match, MatchPlayer player) {
    if (!isValidRankedMatch(match) || player == null) return;

    RankedSession session = getOrCreateSession(match);
    if (session == null) return;

    int teamNumber = getPlayerTeamNumber(match, player);
    if (teamNumber == -1) return;

    session.startDisconnectTimer(player.getNameLegacy(), teamNumber);
  }

  public static void cancelDisconnectTimer(Match match, String playerName) {
    RankedSession session = getSession(match);
    if (session != null) session.cancelDisconnectTimer(playerName);
  }

  public static boolean isSanctionActive(Match match) {
    RankedSession session = getSession(match);
    return session != null && session.isSanctionActive();
  }

  public static boolean isSanctionForTeam(Match match, Party team) {
    RankedSession session = getSession(match);
    return session != null && session.isSanctionForTeam(team);
  }

  public static String getSanctionedUsername(Match match) {
    RankedSession session = getSession(match);
    return session != null ? session.getSanctionedUsername() : null;
  }

  public static boolean isSanctionedPlayer(Match match, MatchPlayer player) {
    RankedSession session = getSession(match);
    return session != null && session.isSanctionedPlayer(player);
  }

  public static void clearMatch(Match match) {
    RankedSession session = getSession(match);
    if (session != null) session.destroy();
  }

  private static RankedSession getSession(Match match) {
    if (match == null) return null;
    MatchSession session = MatchSessionRegistry.get(match);
    return session != null ? session.getRanked() : null;
  }

  private static RankedSession getOrCreateSession(Match match) {
    if (match == null) return null;
    MatchSession matchSession = MatchSessionRegistry.of(match);
    if (matchSession.getRanked() == null) return matchSession.startRanked();
    return matchSession.getRanked();
  }

  private static boolean isValidRankedMatch(Match match) {
    return match != null && Queue.isRanked() && !match.isFinished();
  }

  private static int getPlayerTeamNumber(Match match, MatchPlayer player) {
    if (player.getParty() == null) return -1;
    if (!(player.getParty() instanceof tc.oc.pgm.teams.Team)) return -1;
    MatchSession session = MatchSessionRegistry.get(match);
    if (session == null || session.teams() == null) return -1;
    return session.teams().getTeamNumber(player.getParty());
  }
}
