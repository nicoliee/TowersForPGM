package org.nicolie.towersforpgm.rankeds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.Sounds;

public class DisconnectManager {
  private static final Map<UUID, BukkitTask> timers = new HashMap<>();
  private static final Map<String, Sanction> activeSanctions =
      new HashMap<>(); 

  public static void startDisconnectTimer(Match match, MatchPlayer player) {
    if (match == null || player == null) return;
    if (!Queue.isRanked()) return;

    int seconds = Math.max(1, ConfigManager.getDisconnectTime());
    UUID playerId = player.getId();
    cancelDisconnectTimer(playerId);

    final String username = player.getNameLegacy();
    final int[] remaining = new int[] {seconds};

    String msg = LanguageManager.langMessage("ranked.prefix")
        + LanguageManager.langMessage("ranked.disconnect.countdown")
            .replace("{player}", username)
            .replace("{time}", formatTime(remaining[0]));
    match.sendMessage(Component.text(msg));

    BukkitTask task = Bukkit.getScheduler()
        .runTaskTimer(
            TowersForPGM.getInstance(),
            () -> {
              if (match.isFinished() || !Queue.isRanked()) {
                cancelDisconnectTimer(playerId);
                return;
              }

              if (PGM.get().getMatchManager().getPlayer(playerId) != null) {
                cancelDisconnectTimer(playerId);
                return;
              }

              remaining[0]--;
              if (remaining[0] <= 0) {
                cancelDisconnectTimer(playerId);
                onTimeout(match, playerId, username);
              }
            },
            0L,
            20L);

    timers.put(playerId, task);
  }

  public static void cancelDisconnectTimer(UUID playerId) {
    if (playerId == null) return;
    BukkitTask task = timers.remove(playerId);
    if (task != null) task.cancel();
  }

  private static void onTimeout(Match match, UUID playerId, String username) {
    if (PGM.get().getMatchManager().getPlayer(playerId) != null) return;

    if (match == null) return;
    if (!Queue.isRanked()) return;
    if (match.isFinished()) return;

    Party team = null;
    MatchPlayer cached = TowersForPGM.getInstance().getDisconnectedPlayers().get(username);
    if (cached != null) {
      team = cached.getParty();
    }

    activeSanctions.put(match.getId(), new Sanction(playerId, username, team));

    String message = LanguageManager.langMessage("ranked.prefix")
        + LanguageManager.langMessage("ranked.disconnect.notice").replace("{player}", username);
    match.sendMessage(Component.text(message));
    match.playSound(Sounds.ALERT);
  }

  public static boolean isSanctionActive(Match match) {
    if (match == null) return false;
    return activeSanctions.containsKey(match.getId());
  }

  public static boolean isSanctionForTeam(Match match, Party team) {
    if (match == null) return false;
    Sanction s = activeSanctions.get(match.getId());
    if (s == null) return false;
    if (team == null || s.team == null) return true;
    return team.equals(s.team);
  }

  public static UUID getSanctionedPlayerId(Match match) {
    Sanction s = activeSanctions.get(match.getId());
    return s != null ? s.playerId : null;
  }

  public static String getSanctionedUsername(Match match) {
    Sanction s = activeSanctions.get(match.getId());
    return s != null ? s.username : null;
  }

  public static void clearMatch(Match match) {
    if (match == null) return;
    Sanction s = activeSanctions.remove(match.getId());
    if (s != null) {
      cancelDisconnectTimer(s.playerId);
    }
  }

  public static void clearAll() {
    for (BukkitTask task : new HashSet<>(timers.values())) {
      if (task != null) task.cancel();
    }
    timers.clear();
    activeSanctions.clear();
  }

  private static String formatTime(int totalSeconds) {
    int m = Math.max(0, totalSeconds) / 60;
    int s = Math.max(0, totalSeconds) % 60;
    return String.format("%02d:%02d", m, s);
  }

  private static class Sanction {
    private final UUID playerId;
    private final String username;
    private final Party team;

    private Sanction(UUID playerId, String username, Party team) {
      this.playerId = playerId;
      this.username = username;
      this.team = team;
    }
  }
}
