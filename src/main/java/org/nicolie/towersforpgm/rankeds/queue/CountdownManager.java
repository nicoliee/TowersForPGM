package org.nicolie.towersforpgm.rankeds.queue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.util.bukkit.Sounds;
import tc.oc.pgm.util.text.TextFormatter;

public class CountdownManager {
  private final TowersForPGM plugin;
  private final QueueState queueState;
  private final QueueManager queueManager;
  private MatchStarter matchStarter;

  public CountdownManager(QueueManager queueManager) {
    this.plugin = TowersForPGM.getInstance();
    this.queueState = QueueState.getInstance();
    this.queueManager = queueManager;
  }

  public void setMatchStarter(MatchStarter matchStarter) {
    this.matchStarter = matchStarter;
  }

  public boolean startRankedCountdown(Match match) {
    if (!canStartCountdown(match)) {
      return false;
    }

    queueState.setRanked(false);
    queueState.setCountdownActive(true);

    final AtomicInteger countdown = new AtomicInteger(getCountdownTime(match));
    final int maxSize = plugin.config().ranked().getRankedMaxSize();
    final int minSize = plugin.config().ranked().getRankedMinSize();

    BukkitTask countdownTask = createCountdownTask(match, countdown, maxSize, minSize);
    queueState.setCountdownTask(countdownTask);

    return true;
  }

  public boolean cancelCountdown(Match match) {
    if (!queueState.isCountdownActive() || queueState.getCountdownTask() == null) {
      return false;
    }

    queueState.getCountdownTask().cancel();
    queueState.setCountdownActive(false);
    queueState.setCountdownTask(null);
    queueState.setRanked(false);

    match.playSound(Sounds.WARNING);

    return true;
  }

  private boolean canStartCountdown(Match match) {
    return !(match.getPhase() == MatchPhase.RUNNING
        || match.getPhase() == MatchPhase.FINISHED
        || queueState.isCountdownActive()
        || queueState.isRanked());
  }

  private BukkitTask createCountdownTask(
      Match match, AtomicInteger countdown, int maxSize, int minSize) {
    Component message = Queue.RANKED_PREFIX
        .append(Component.space())
        .append(Component.translatable(
                "ranked.countdown", Component.text(countdown.get()).color(NamedTextColor.AQUA))
            .color(NamedTextColor.DARK_AQUA));
    match.sendMessage(message);
    return Bukkit.getScheduler()
        .runTaskTimer(
            plugin, () -> handleCountdownTick(match, countdown, maxSize, minSize), 0L, 20L);
  }

  private void handleCountdownTick(Match match, AtomicInteger countdown, int maxSize, int minSize) {
    int timeLeft = countdown.get();
    int timer = getCountdownTime(match);
    if (timer < timeLeft) {
      countdown.set(timer);
      timeLeft = timer;
    }
    List<UUID> disconnectedPlayerUUIDs = queueManager.getDisconnectedPlayersInQueue();
    if (timeLeft <= 0) {
      handleCountdownEnd(match, disconnectedPlayerUUIDs, minSize);
      return;
    }

    if (queueState.getQueueSize() < minSize || match.isRunning()) {
      cancelCountdown(match);
      return;
    }

    sendCountdownMessage(match, timeLeft, disconnectedPlayerUUIDs);
    countdown.decrementAndGet();
  }

  private void handleCountdownEnd(Match match, List<UUID> disconnectedPlayerUUIDs, int minSize) {
    queueManager.removeDisconnectedPlayers(match, disconnectedPlayerUUIDs);

    if (queueState.getQueueSize() < minSize) {
      cancelCountdown(match);
      return;
    }

    String table = plugin.config().databaseTables().getRankedDefaultTable();
    plugin.config().databaseTables().setTempTable(table);

    matchStarter.startMatch(match, table);

    queueState.getCountdownTask().cancel();
    queueState.setCountdownActive(false);
    queueState.setCountdownTask(null);
    queueState.setRanked(true);
  }

  private void sendCountdownMessage(Match match, int timeLeft, List<UUID> disconnectedPlayers) {
    Component message = Queue.RANKED_PREFIX
        .append(Component.space())
        .append(Component.text(SendMessage.formatTime(timeLeft)).color(NamedTextColor.AQUA));

    if (!disconnectedPlayers.isEmpty()) {
      List<String> disconnectedNames = disconnectedPlayers.stream()
          .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
          .filter(name -> name != null)
          .collect(Collectors.toList());

      List<Component> disconnectedNameComponents = disconnectedNames.stream()
          .map(name -> Component.text(name, NamedTextColor.DARK_AQUA))
          .collect(Collectors.toList());

      Component disconnectedNamesList =
          TextFormatter.list(disconnectedNameComponents, NamedTextColor.DARK_GRAY);

      Component waiting = Component.space()
          .append(Component.text("|").color(NamedTextColor.DARK_GRAY))
          .append(Component.space())
          .append(Component.translatable("ranked.waitingFor", disconnectedNamesList)
              .color(NamedTextColor.AQUA));
      message = message.append(waiting);
    }

    match.sendActionBar(message.decorate(TextDecoration.ITALIC));
    match.playSound(Sounds.INVENTORY_CLICK);
  }

  private int getCountdownTime(Match match) {
    int minSize = plugin.config().ranked().getRankedMinSize();
    int TimerWaiting = plugin.config().ranked().getOnTimerWaiting();
    int TimerMinReached = plugin.config().ranked().getOnTimerMinReached();
    int TimerFull = plugin.config().ranked().getOnTimerFull();

    if (!queueManager.getDisconnectedPlayersInQueue().isEmpty()) {
      return TimerWaiting;
    }

    if (queueState.getQueueSize() >= queueManager.getValidRankedSize(match)) {
      return TimerFull;
    }

    int playerCount = match.getPlayers().size();
    return (playerCount == minSize || playerCount == minSize + 1) ? TimerFull : TimerMinReached;
  }
}
