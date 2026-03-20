package org.nicolie.towersforpgm.rankeds.queue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
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

  private static final ScheduledThreadPoolExecutor EXECUTOR =
      new ScheduledThreadPoolExecutor(1, r -> {
        Thread t = new Thread(r, "ranked-countdown");
        t.setDaemon(true);
        return t;
      });

  static {
    EXECUTOR.setRemoveOnCancelPolicy(true);
  }

  public CountdownManager(QueueManager queueManager) {
    this.plugin = TowersForPGM.getInstance();
    this.queueState = QueueState.getInstance();
    this.queueManager = queueManager;
  }

  public void setMatchStarter(MatchStarter matchStarter) {
    this.matchStarter = matchStarter;
  }

  public boolean startRankedCountdown(Match match) {
    if (!canStartCountdown(match)) return false;

    queueState.setRanked(false);
    queueState.setCountdownActive(true);

    final AtomicInteger countdown = new AtomicInteger(getCountdownTime(match));

    sendInitialMessage(match, countdown.get());

    ScheduledFuture<?> future = EXECUTOR.scheduleAtFixedRate(
        () -> handleCountdownTick(match, countdown), 1, 1, TimeUnit.SECONDS);

    queueState.setCountdownFuture(future);
    return true;
  }

  public boolean cancelCountdown(Match match) {
    if (!queueState.isCountdownActive() || queueState.getCountdownFuture() == null) return false;

    queueState.cancelCountdown();

    Bukkit.getScheduler().runTask(plugin, () -> match.playSound(Sounds.WARNING));
    return true;
  }

  private boolean canStartCountdown(Match match) {
    return !(match.getPhase() == MatchPhase.RUNNING
        || match.getPhase() == MatchPhase.FINISHED
        || queueState.isCountdownActive()
        || queueState.isRanked());
  }

  private void handleCountdownTick(Match match, AtomicInteger countdown) {
    // Este método corre en el thread del executor, no en el thread principal de Bukkit.
    // Toda operación sobre el servidor se despacha con runTask().

    int timeLeft = countdown.decrementAndGet();

    List<UUID> disconnected = queueManager.getDisconnectedPlayersInQueue();

    if (timeLeft <= 0) {
      Bukkit.getScheduler().runTask(plugin, () -> handleCountdownEnd(match, disconnected));
      return;
    }

    // Ajustar si cambió el tiempo objetivo (más jugadores entraron)
    int targetTime = getCountdownTime(match);
    if (targetTime < countdown.get()) {
      countdown.set(targetTime);
    }

    if (queueState.getQueueSize() < plugin.config().ranked().getRankedMinSize()) {
      Bukkit.getScheduler().runTask(plugin, () -> cancelCountdown(match));
      return;
    }

    Bukkit.getScheduler()
        .runTask(plugin, () -> sendCountdownMessage(match, timeLeft, disconnected));
  }

  private void handleCountdownEnd(Match match, List<UUID> disconnected) {
    queueManager.removeDisconnectedPlayers(match, disconnected);

    if (queueState.getQueueSize() < plugin.config().ranked().getRankedMinSize()) {
      cancelCountdown(match);
      return;
    }

    String table = plugin.config().databaseTables().getRankedDefaultTable();
    plugin.config().databaseTables().setTempTable(table);

    queueState.cancelCountdown();
    queueState.setRanked(true);

    matchStarter.startMatch(match, table);
  }

  private void sendInitialMessage(Match match, int seconds) {
    Bukkit.getScheduler().runTask(plugin, () -> {
      Component message = Queue.RANKED_PREFIX
          .append(Component.space())
          .append(Component.translatable(
                  "ranked.countdown", Component.text(seconds).color(NamedTextColor.AQUA))
              .color(NamedTextColor.DARK_AQUA));
      match.sendMessage(message);
    });
  }

  private void sendCountdownMessage(Match match, int timeLeft, List<UUID> disconnectedPlayers) {
    Component message = Queue.RANKED_PREFIX
        .append(Component.space())
        .append(Component.text(SendMessage.formatTime(timeLeft)).color(NamedTextColor.AQUA));

    if (!disconnectedPlayers.isEmpty()) {
      List<String> names = disconnectedPlayers.stream()
          .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
          .filter(name -> name != null)
          .collect(Collectors.toList());

      List<Component> nameComponents = names.stream()
          .map(name -> Component.text(name, NamedTextColor.DARK_AQUA))
          .collect(Collectors.toList());

      message = message
          .append(Component.space())
          .append(Component.text("|").color(NamedTextColor.DARK_GRAY))
          .append(Component.space())
          .append(Component.translatable(
                  "ranked.waitingFor", TextFormatter.list(nameComponents, NamedTextColor.DARK_GRAY))
              .color(NamedTextColor.AQUA));
    }

    match.sendActionBar(message.decorate(TextDecoration.ITALIC));
    match.playSound(Sounds.INVENTORY_CLICK);
  }

  private int getCountdownTime(Match match) {
    int minSize = plugin.config().ranked().getRankedMinSize();
    int timerWaiting = plugin.config().ranked().getOnTimerWaiting();
    int timerMinReached = plugin.config().ranked().getOnTimerMinReached();
    int timerFull = plugin.config().ranked().getOnTimerFull();

    if (!queueManager.getDisconnectedPlayersInQueue().isEmpty()) return timerWaiting;

    if (queueState.getQueueSize() >= queueManager.getValidRankedSize(match)) return timerFull;

    int playerCount = match.getPlayers().size();
    return (playerCount == minSize || playerCount == minSize + 1) ? timerFull : timerMinReached;
  }
}
