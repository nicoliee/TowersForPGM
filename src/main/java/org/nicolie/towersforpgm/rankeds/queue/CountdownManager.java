package org.nicolie.towersforpgm.rankeds.queue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.util.bukkit.Sounds;

/**
 * Manages countdown logic for ranked queue system. Handles the timing and countdown mechanics
 * before matches start.
 */
public class CountdownManager {
  private final TowersForPGM plugin;
  private final QueueState queueState;
  private final QueueManager queueManager;
  private MatchStarter matchStarter; // Will be set after initialization

  public CountdownManager(QueueManager queueManager) {
    this.plugin = TowersForPGM.getInstance();
    this.queueState = QueueState.getInstance();
    this.queueManager = queueManager;
  }

  public void setMatchStarter(MatchStarter matchStarter) {
    this.matchStarter = matchStarter;
  }

  /** Starts the ranked countdown if conditions are met. */
  public boolean startRankedCountdown(Match match) {
    if (!canStartCountdown(match)) {
      return false;
    }

    queueState.setRanked(false);
    queueState.setCountdownActive(true);

    final AtomicInteger countdown = new AtomicInteger(getCountdownTime(match));
    final int maxSize = plugin.config().ranked().getRankedMaxSize();
    final int minSize = plugin.config().ranked().getRankedMinSize();

    BukkitTask countdownTask = createCountdownTask(match, countdown, maxSize, minSize, false);
    queueState.setCountdownTask(countdownTask);

    return true;
  }

  /** Starts a manual ranked countdown with custom time. */
  public boolean startManualRankedCountdown(Match match, int customTime) {
    if (!canStartCountdown(match)) {
      return false;
    }

    if (queueState.getQueueSize() < plugin.config().ranked().getRankedMinSize()) {
      return false;
    }

    queueState.setRanked(false);
    queueState.setCountdownActive(true);

    final AtomicInteger countdown = new AtomicInteger(customTime);
    final int minSize = plugin.config().ranked().getRankedMinSize();

    BukkitTask countdownTask = createCountdownTask(match, countdown, -1, minSize, true);
    queueState.setCountdownTask(countdownTask);

    return true;
  }

  /** Cancels the current countdown. */
  public boolean cancelCountdown(Match match) {
    if (!queueState.isCountdownActive() || queueState.getCountdownTask() == null) {
      return false;
    }

    queueState.getCountdownTask().cancel();
    queueState.setCountdownActive(false);
    queueState.setCountdownTask(null);
    queueState.setRanked(false);

    match.sendWarning(
        Component.text(getRankedPrefix() + LanguageManager.message("ranked.cancelled")));

    return true;
  }

  private boolean canStartCountdown(Match match) {
    return !(match.getPhase() == MatchPhase.RUNNING
        || match.getPhase() == MatchPhase.FINISHED
        || queueState.isCountdownActive()
        || queueState.isRanked());
  }

  private BukkitTask createCountdownTask(
      Match match, AtomicInteger countdown, int maxSize, int minSize, boolean isManual) {
    return Bukkit.getScheduler()
        .runTaskTimer(
            plugin,
            () -> handleCountdownTick(match, countdown, maxSize, minSize, isManual),
            0L,
            20L);
  }

  private void handleCountdownTick(
      Match match, AtomicInteger countdown, int maxSize, int minSize, boolean isManual) {
    int timeLeft = countdown.get();
    List<UUID> disconnectedPlayerUUIDs = queueManager.getDisconnectedPlayersInQueue();

    if (timeLeft <= 0) {
      handleCountdownEnd(match, disconnectedPlayerUUIDs, minSize, isManual);
      return;
    }

    if (queueState.getQueueSize() < minSize || match.isRunning()) {
      cancelCountdown(match);
      return;
    }

    handleDisconnectedPlayers(match, disconnectedPlayerUUIDs, countdown);

    if (!isManual) {
      adjustCountdownTime(countdown, maxSize, disconnectedPlayerUUIDs.isEmpty());
    }

    sendCountdownMessage(match, timeLeft);
    countdown.decrementAndGet();
  }

  private void handleCountdownEnd(
      Match match, List<UUID> disconnectedPlayerUUIDs, int minSize, boolean isManual) {
    queueManager.removeDisconnectedPlayers(match, disconnectedPlayerUUIDs);

    if (queueState.getQueueSize() < minSize) {
      cancelCountdown(match);
      return;
    }

    String table = plugin.config().databaseTables().getRankedDefaultTable();
    plugin.config().databaseTables().setTempTable(table);

    String logMessage = isManual
        ? "[+] Queue: starting manual draft/matchmaking, "
        : "[+] Queue: starting draft/matchmaking, ";
    plugin.getLogger().info(logMessage + queueState.getQueueSize() + " players");

    matchStarter.startMatch(match, table);

    queueState.getCountdownTask().cancel();
    queueState.setCountdownActive(false);
    queueState.setCountdownTask(null);
    queueState.setRanked(true);
  }

  private void handleDisconnectedPlayers(
      Match match, List<UUID> disconnectedPlayerUUIDs, AtomicInteger countdown) {
    if (!disconnectedPlayerUUIDs.isEmpty()) {
      List<String> disconnectedNames = disconnectedPlayerUUIDs.stream()
          .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
          .filter(name -> name != null)
          .collect(Collectors.toList());

      if (countdown.get() > 30) {
        countdown.set(30);
      }

      match.sendActionBar(Component.text(getRankedPrefix()
          + LanguageManager.message("ranked.waitingFor")
              .replace("{player}", String.join(", ", disconnectedNames))));
    }
  }

  private void adjustCountdownTime(AtomicInteger countdown, int maxSize, boolean noDisconnected) {
    if (noDisconnected) {
      int currentTime = countdown.get();
      int queueSize = queueState.getQueueSize();

      if (queueSize >= maxSize && currentTime > 5) {
        countdown.set(5);
      } else if (queueSize < maxSize && currentTime > 15 && currentTime <= 30) {
        countdown.set(15);
      }
    }
  }

  private void sendCountdownMessage(Match match, int timeLeft) {
    match.sendMessage(Component.text(getRankedPrefix()
        + LanguageManager.message("ranked.countdown").replace("{time}", String.valueOf(timeLeft))));
    match.playSound(Sounds.INVENTORY_CLICK);
  }

  private int getCountdownTime(Match match) {
    int minSize = plugin.config().ranked().getRankedMinSize();
    int maxSize = plugin.config().ranked().getRankedMaxSize();

    if (!queueManager.getDisconnectedPlayersInQueue().isEmpty()) {
      return 30;
    }

    if (queueState.getQueueSize() >= maxSize) {
      return 5;
    }

    int playerCount = match.getPlayers().size();
    return (playerCount == minSize || playerCount == minSize + 1) ? 5 : 15;
  }

  private String getRankedPrefix() {
    return LanguageManager.message("ranked.prefix");
  }
}
