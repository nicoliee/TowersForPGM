package org.nicolie.towersforpgm.session.ranked;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.team.Teams;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.Sounds;

public final class RankedSession {

  private final Match match;
  private final TowersForPGM plugin;
  private final Teams teams;

  // Disconnect timers: playerName -> future
  private final Map<String, ScheduledFuture<?>> disconnectTimers = new HashMap<>();
  // Sanctions: playerName -> teamNumber
  private final Map<String, Sanction> sanctions = new HashMap<>();
  // Forfeit votes: playerUUID
  private final Set<UUID> forfeitVotes = new HashSet<>();

  private boolean active = false;

  // Shared executor para disconnect timers (no ligado a Bukkit)
  private static final ScheduledThreadPoolExecutor TIMER_EXECUTOR =
      new ScheduledThreadPoolExecutor(1, r -> {
        Thread t = new Thread(r, "ranked-disconnect-timer");
        t.setDaemon(true);
        return t;
      });

  static {
    TIMER_EXECUTOR.setRemoveOnCancelPolicy(true);
  }

  public RankedSession(Match match, TowersForPGM plugin, Teams teams) {
    this.match = match;
    this.plugin = plugin;
    this.teams = teams;
  }

  public void activate() {
    this.active = true;
  }

  public boolean isActive() {
    return active;
  }

  public void startDisconnectTimer(String playerName, int teamNumber) {
    cancelDisconnectTimer(playerName);

    int seconds = Math.max(1, plugin.config().ranked().getDisconnectTime());
    final int[] timeRemaining = {seconds};

    resetTeamForfeits(teams.getTeam(teamNumber));
    sendCountdownMessage(playerName, timeRemaining[0]);

    ScheduledFuture<?> future = TIMER_EXECUTOR.scheduleAtFixedRate(
        new DisconnectTimerTask(playerName, teamNumber, timeRemaining), 1, 1, TimeUnit.SECONDS);

    disconnectTimers.put(playerName, future);
  }

  public void cancelDisconnectTimer(String playerName) {
    if (playerName == null) return;
    ScheduledFuture<?> future = disconnectTimers.remove(playerName);
    if (future != null) future.cancel(false);
  }

  public void checkOfflinePlayers() {
    for (int teamNumber = 1; teamNumber <= 2; teamNumber++) {
      for (String playerName : teams.getTeamOfflinePlayers(teamNumber)) {
        startDisconnectTimer(playerName, teamNumber);
      }
      for (MatchPlayer player : teams.getTeamOnlinePlayers(teamNumber)) {
        if (!player.getBukkit().isOnline()) {
          startDisconnectTimer(player.getNameLegacy(), teamNumber);
        }
      }
    }
  }

  public boolean isSanctionActive() {
    return !sanctions.isEmpty();
  }

  public boolean isSanctionForTeam(Party team) {
    int teamNumber = teams.getTeamNumber(team);
    return sanctions.values().stream().anyMatch(s -> s.teamNumber == teamNumber);
  }

  public String getSanctionedUsername() {
    return sanctions.isEmpty() ? null : sanctions.keySet().iterator().next();
  }

  public boolean isSanctionedPlayer(MatchPlayer player) {
    return player != null && sanctions.containsKey(player.getNameLegacy());
  }

  public boolean addForfeit(MatchPlayer player) {
    return forfeitVotes.add(player.getId());
  }

  public boolean hasForfeited(MatchPlayer player) {
    return forfeitVotes.contains(player.getId());
  }

  public boolean allForfeited(Party team) {
    return team.getPlayers().stream()
        .filter(mp -> !isSanctionedPlayer(mp))
        .allMatch(mp -> forfeitVotes.contains(mp.getId()));
  }

  public void resetTeamForfeits(Party team) {
    if (team == null) return;
    team.getPlayers().forEach(mp -> forfeitVotes.remove(mp.getId()));
  }

  public void destroy() {
    active = false;
    disconnectTimers.values().forEach(f -> f.cancel(false));
    disconnectTimers.clear();
    sanctions.clear();
    forfeitVotes.clear();
  }

  private void applyDisconnectSanction(String playerName, int teamNumber) {
    sanctions.put(playerName, new Sanction(playerName, teamNumber));
    resetTeamForfeits(teams.getTeam(teamNumber));

    Component message = Queue.RANKED_PREFIX.append(
        Component.translatable("ranked.disconnect.notice", Component.text(playerName)));

    Bukkit.getScheduler().runTask(plugin, () -> {
      match.sendMessage(message);
      match.playSound(Sounds.ALERT);
    });
  }

  private void sendCountdownMessage(String playerName, int timeRemaining) {
    Component message = Queue.RANKED_PREFIX.append(Component.translatable(
        "ranked.disconnect.countdown",
        Component.text(playerName),
        Component.text(SendMessage.formatTime(timeRemaining))));

    Bukkit.getScheduler().runTask(plugin, () -> match.sendMessage(message));
  }

  private void sendReconnectedMessage(String playerName) {
    Component message = Queue.RANKED_PREFIX.append(
        Component.translatable("ranked.disconnect.reconnected", Component.text(playerName)));

    Bukkit.getScheduler().runTask(plugin, () -> match.sendMessage(message));
  }

  private class DisconnectTimerTask implements Runnable {
    private final String playerName;
    private final int teamNumber;
    private final int[] timeRemaining;

    DisconnectTimerTask(String playerName, int teamNumber, int[] timeRemaining) {
      this.playerName = playerName;
      this.teamNumber = teamNumber;
      this.timeRemaining = timeRemaining;
    }

    @Override
    public void run() {
      if (match.isFinished()) {
        cancelDisconnectTimer(playerName);
        return;
      }

      if (Bukkit.getPlayer(playerName) != null) {
        if (!isSanctionActive()) {
          cancelDisconnectTimer(playerName);
          sendReconnectedMessage(playerName);
        }
        return;
      }

      timeRemaining[0]--;
      if (timeRemaining[0] <= 0) {
        cancelDisconnectTimer(playerName);
        applyDisconnectSanction(playerName, teamNumber);
      }
    }
  }

  public static final class Sanction {
    public final String playerName;
    public final int teamNumber;

    Sanction(String playerName, int teamNumber) {
      this.playerName = playerName;
      this.teamNumber = teamNumber;
    }
  }
}
