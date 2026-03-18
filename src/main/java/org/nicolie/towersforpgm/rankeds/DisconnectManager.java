package org.nicolie.towersforpgm.rankeds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.team.Teams;
import org.nicolie.towersforpgm.session.MatchSession;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.Sounds;

public class DisconnectManager {
  // TODO: Refactor, quitar comments y usar Match, no match.getId()
  private static final TowersForPGM plugin = TowersForPGM.getInstance();

  // All state keyed by match.getId()
  private static final Map<String, BukkitTask> activeTimers = new HashMap<>();
  private static final Map<String, Sanction> activeSanctions = new HashMap<>();
  private static final Map<String, Set<UUID>> forfeitedByMatch = new HashMap<>();

  // ── Teams resolution ──────────────────────────────────────────────────────

  private static Teams teamsForMatch(Match match) {
    if (match == null) return null;
    MatchSession session = MatchSessionRegistry.get(match);
    if (session == null) return null;
    return session.teams();
  }

  // ── Forfeit tracking ──────────────────────────────────────────────────────

  /**
   * Registers a forfeit vote for {@code player} in their match.
   *
   * @return false if they had already voted
   */
  public static boolean addForfeit(Match match, MatchPlayer player) {
    return forfeitedByMatch
        .computeIfAbsent(match.getId(), k -> new HashSet<>())
        .add(player.getId());
  }

  public static boolean hasForfeited(Match match, MatchPlayer player) {
    Set<UUID> set = forfeitedByMatch.get(match.getId());
    return set != null && set.contains(player.getId());
  }

  /** Returns true if every non-sanctioned player on {@code team} has voted forfeit. */
  public static boolean allForfeited(Match match, Party team) {
    Set<UUID> set = forfeitedByMatch.get(match.getId());
    if (set == null) return false;
    return team.getPlayers().stream()
        .filter(mp -> !isSanctionedPlayer(match, mp))
        .allMatch(mp -> set.contains(mp.getId()));
  }

  public static void resetTeamForfeits(Match match, Party team) {
    if (team == null) return;
    Set<UUID> set = forfeitedByMatch.get(match.getId());
    if (set != null) team.getPlayers().forEach(mp -> set.remove(mp.getId()));
  }

  // ── Disconnect timers ─────────────────────────────────────────────────────

  public static void checkOfflinePlayersOnMatchStart(Match match) {
    Teams teams = teamsForMatch(match);
    if (!isValidRankedMatch(match) || teams == null) return;

    for (int teamNumber = 1; teamNumber <= 2; teamNumber++) {
      for (String playerName : teams.getTeamOfflinePlayers(teamNumber)) {
        startDisconnectTimer(match, playerName, teamNumber);
      }
      for (MatchPlayer player : teams.getTeamOnlinePlayers(teamNumber)) {
        if (!player.getBukkit().isOnline()) {
          startDisconnectTimer(match, player.getNameLegacy(), teamNumber);
        }
      }
    }
  }

  public static void startDisconnectTimer(Match match, MatchPlayer player) {
    Teams teams = teamsForMatch(match);
    if (!isValidRankedMatch(match) || player == null || teams == null) return;

    int teamNumber = getPlayerTeamNumber(player, teams);
    if (teamNumber == -1) return;

    startDisconnectTimer(match, player.getNameLegacy(), teamNumber);
  }

  private static void startDisconnectTimer(Match match, String playerName, int teamNumber) {
    cancelDisconnectTimer(playerName);

    int seconds = Math.max(1, plugin.config().ranked().getDisconnectTime());
    final int[] timeRemaining = {seconds};

    resetTeamForfeitsForTeamNumber(match, teamNumber);
    sendCountdownMessage(match, playerName, timeRemaining[0]);

    BukkitTask task = Bukkit.getScheduler()
        .runTaskTimer(
            TowersForPGM.getInstance(),
            new DisconnectTimerTask(match, playerName, teamNumber, timeRemaining),
            0L,
            20L);

    activeTimers.put(playerName, task);
  }

  public static void cancelDisconnectTimer(String playerName) {
    if (playerName == null) return;
    BukkitTask task = activeTimers.remove(playerName);
    if (task != null) task.cancel();
  }

  // ── Sanctions ─────────────────────────────────────────────────────────────

  public static boolean isSanctionActive(Match match) {
    if (match == null) return false;
    return activeSanctions.containsKey(match.getId());
  }

  public static boolean isSanctionForTeam(Match match, Party team) {
    Teams teams = teamsForMatch(match);
    if (match == null || teams == null) return false;
    Sanction sanction = activeSanctions.get(match.getId());
    if (sanction == null) return false;
    return teams.getTeamNumber(team) == sanction.teamNumber;
  }

  public static String getSanctionedUsername(Match match) {
    Sanction sanction = activeSanctions.get(match.getId());
    return sanction != null ? sanction.playerName : null;
  }

  public static boolean isSanctionedPlayer(Match match, MatchPlayer player) {
    if (match == null || player == null) return false;
    Sanction sanction = activeSanctions.get(match.getId());
    return sanction != null && sanction.playerName.equals(player.getNameLegacy());
  }

  // ── Cleanup ───────────────────────────────────────────────────────────────

  public static void clearMatch(Match match) {
    if (match == null) return;
    String id = match.getId();
    Sanction sanction = activeSanctions.remove(id);
    if (sanction != null) cancelDisconnectTimer(sanction.playerName);
    forfeitedByMatch.remove(id);
  }

  public static void clearAll() {
    for (BukkitTask task : new HashSet<>(activeTimers.values())) {
      if (task != null) task.cancel();
    }
    activeTimers.clear();
    activeSanctions.clear();
    forfeitedByMatch.clear();
  }

  // ── Timer task ────────────────────────────────────────────────────────────

  private static class DisconnectTimerTask implements Runnable {
    private final Match match;
    private final String playerName;
    private final int teamNumber;
    private final int[] timeRemaining;

    DisconnectTimerTask(Match match, String playerName, int teamNumber, int[] timeRemaining) {
      this.match = match;
      this.playerName = playerName;
      this.teamNumber = teamNumber;
      this.timeRemaining = timeRemaining;
    }

    @Override
    public void run() {
      if (match.isFinished() || !Queue.isRanked()) {
        cancelDisconnectTimer(playerName);
        return;
      }
      if (isPlayerOnline(playerName)) {
        if (!isSanctionActive(match)) {
          cancelDisconnectTimer(playerName);
          sendReconnectedMessage(match, playerName);
        }
        return;
      }
      timeRemaining[0]--;
      if (timeRemaining[0] <= 0) {
        cancelDisconnectTimer(playerName);
        applyDisconnectSanction(match, playerName, teamNumber);
      }
    }
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  private static void applyDisconnectSanction(Match match, String playerName, int teamNumber) {
    activeSanctions.put(match.getId(), new Sanction(playerName, teamNumber));
    resetTeamForfeitsForTeamNumber(match, teamNumber);

    String message = LanguageManager.message("ranked.prefix")
        + LanguageManager.message("ranked.disconnect.notice").replace("{player}", playerName);
    match.sendMessage(Component.text(message));
    match.playSound(Sounds.ALERT);
  }

  private static void resetTeamForfeitsForTeamNumber(Match match, int teamNumber) {
    Teams teams = teamsForMatch(match);
    if (teams == null) return;
    Party team = teams.getTeam(teamNumber);
    if (team != null) resetTeamForfeits(match, team);
  }

  private static boolean isValidRankedMatch(Match match) {
    return match != null && Queue.isRanked() && !match.isFinished();
  }

  private static boolean isPlayerOnline(String playerName) {
    return Bukkit.getPlayer(playerName) != null;
  }

  private static int getPlayerTeamNumber(MatchPlayer player, Teams teams) {
    if (player.getParty() == null) return -1;
    if (player.getParty() instanceof tc.oc.pgm.teams.Team) {
      return teams.getTeamNumber(player.getParty());
    }
    return -1;
  }

  private static void sendCountdownMessage(Match match, String playerName, int timeRemaining) {
    String message = LanguageManager.message("ranked.prefix")
        + LanguageManager.message("ranked.disconnect.countdown")
            .replace("{player}", playerName)
            .replace("{time}", SendMessage.formatTime(timeRemaining));
    match.sendMessage(Component.text(message));
  }

  private static void sendReconnectedMessage(Match match, String playerName) {
    String message = LanguageManager.message("ranked.prefix")
        + LanguageManager.message("ranked.disconnect.reconnected").replace("{player}", playerName);
    match.sendMessage(Component.text(message));
  }

  // ── Sanction record ───────────────────────────────────────────────────────

  static class Sanction {
    final String playerName;
    final int teamNumber;

    Sanction(String playerName, int teamNumber) {
      this.playerName = playerName;
      this.teamNumber = teamNumber;
    }
  }
}
