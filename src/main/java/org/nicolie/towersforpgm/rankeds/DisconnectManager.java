package org.nicolie.towersforpgm.rankeds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.Sounds;

public class DisconnectManager {
  private static final TowersForPGM plugin = TowersForPGM.getInstance();
  private static final Map<String, BukkitTask> activeTimers = new HashMap<>();
  private static final Map<String, Sanction> activeSanctions = new HashMap<>();
  private static Teams teams;

  public static void setTeams(Teams teamsInstance) {
    teams = teamsInstance;
  }

  public static void checkOfflinePlayersOnMatchStart(Match match) {
    if (!isValidRankedMatch(match) || teams == null) return;
    for (int teamNumber = 1; teamNumber <= 2; teamNumber++) {
      for (String playerName : teams.getTeamOfflinePlayers(teamNumber)) {
        startDisconnectTimer(match, playerName, teamNumber);
      }
      for (MatchPlayer player : teams.getTeamOnlinePlayers(teamNumber)) {
        if (!player.getBukkit().isOnline()) {
          startDisconnectTimer(match, player);
        }
      }
    }
  }

  public static void startDisconnectTimer(Match match, MatchPlayer player) {
    if (!isValidRankedMatch(match) || player == null || teams == null) return;

    String playerName = player.getNameLegacy();
    int teamNumber = getPlayerTeamNumber(player);
    if (teamNumber == -1) return;

    startDisconnectTimer(match, playerName, teamNumber);
  }

  private static void startDisconnectTimer(Match match, String playerName, int teamNumber) {
    cancelDisconnectTimer(playerName);

    int seconds = Math.max(1, plugin.config().ranked().getDisconnectTime());
    final int[] timeRemaining = {seconds};

    resetTeamForfeits(match, teamNumber);

    sendCountdownMessage(match, playerName, timeRemaining[0]);

    BukkitTask task = Bukkit.getScheduler()
        .runTaskTimer(
            TowersForPGM.getInstance(),
            new DisconnectTimerTask(match, playerName, teamNumber, timeRemaining),
            0L,
            20L);

    activeTimers.put(playerName, task);
  }

  private static class DisconnectTimerTask implements Runnable {
    private final Match match;
    private final String playerName;
    private final int teamNumber;
    private final int[] timeRemaining;

    public DisconnectTimerTask(
        Match match, String playerName, int teamNumber, int[] timeRemaining) {
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

  public static void cancelDisconnectTimer(String playerName) {
    if (playerName == null) return;
    BukkitTask task = activeTimers.remove(playerName);
    if (task != null) task.cancel();
  }

  private static void applyDisconnectSanction(Match match, String playerName, int teamNumber) {
    activeSanctions.put(match.getId(), new Sanction(playerName, teamNumber));

    resetTeamForfeits(match, teamNumber);

    String message = LanguageManager.message("ranked.prefix")
        + LanguageManager.message("ranked.disconnect.notice").replace("{player}", playerName);
    match.sendMessage(Component.text(message));
    match.playSound(Sounds.ALERT);
  }

  private static boolean isValidRankedMatch(Match match) {
    return match != null && Queue.isRanked() && !match.isFinished();
  }

  private static boolean isPlayerOnline(String playerName) {
    return Bukkit.getPlayer(playerName) != null;
  }

  private static int getPlayerTeamNumber(MatchPlayer player) {
    if (teams == null || player.getParty() == null) return -1;

    if (player.getParty() instanceof tc.oc.pgm.teams.Team) {
      tc.oc.pgm.teams.Team team = (tc.oc.pgm.teams.Team) player.getParty();
      return teams.getTeamNumber(team);
    }
    return -1;
  }

  private static void sendCountdownMessage(Match match, String playerName, int timeRemaining) {
    String message = LanguageManager.message("ranked.prefix")
        + LanguageManager.message("ranked.disconnect.countdown")
            .replace("{player}", playerName)
            .replace("{time}", formatTime(timeRemaining));
    match.sendMessage(Component.text(message));
  }

  private static void sendReconnectedMessage(Match match, String playerName) {
    String message = LanguageManager.message("ranked.prefix")
        + LanguageManager.message("ranked.disconnect.reconnected").replace("{player}", playerName);
    match.sendMessage(Component.text(message));
  }

  private static void resetTeamForfeits(Match match, int teamNumber) {
    if (teams == null) return;
    tc.oc.pgm.teams.Team team = teams.getTeam(teamNumber);
    if (team != null) {
      org.nicolie.towersforpgm.commands.ranked.ForfeitCommand.resetTeamForfeits(team);
    }
  }

  public static boolean isSanctionActive(Match match) {
    if (match == null) return false;
    return activeSanctions.containsKey(match.getId());
  }

  public static boolean isSanctionForTeam(Match match, Party team) {
    if (match == null || teams == null) return false;
    Sanction sanction = activeSanctions.get(match.getId());
    if (sanction == null) return false;

    if (team instanceof tc.oc.pgm.teams.Team) {
      int teamNumber = teams.getTeamNumber((tc.oc.pgm.teams.Team) team);
      return teamNumber == sanction.teamNumber;
    }
    return false;
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

  public static void clearMatch(Match match) {
    if (match == null) return;
    Sanction sanction = activeSanctions.remove(match.getId());
    if (sanction != null) {
      cancelDisconnectTimer(sanction.playerName);
    }
  }

  public static void clearAll() {
    // Cancel all active timers
    for (BukkitTask task : new HashSet<>(activeTimers.values())) {
      if (task != null) task.cancel();
    }
    activeTimers.clear();
    activeSanctions.clear();
  }

  private static String formatTime(int totalSeconds) {
    int minutes = Math.max(0, totalSeconds) / 60;
    int seconds = Math.max(0, totalSeconds) % 60;
    return String.format("%02d:%02d", minutes, seconds);
  }

  static class Sanction {
    final String playerName;
    final int teamNumber;

    Sanction(String playerName, int teamNumber) {
      this.playerName = playerName;
      this.teamNumber = teamNumber;
    }
  }
}
