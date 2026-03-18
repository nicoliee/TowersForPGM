package org.nicolie.towersforpgm.draft.timer;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.ConfigManager;
import org.nicolie.towersforpgm.draft.team.AvailablePlayers;
import org.nicolie.towersforpgm.draft.team.Captains;
import org.nicolie.towersforpgm.draft.team.Teams;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.Sounds;

public class BossbarTimer {
  private final Match match;
  private final ConfigManager configManager;
  private final Captains captains;
  private final AvailablePlayers availablePlayers;
  private final Teams teams;
  private final TowersForPGM plugin;

  private BossBar currentBar;
  private BukkitTask currentTimer;

  private static final int DEFAULT_DURATION = 20;
  private static final int[][] DURATION_TABLE = {
    {14, 50},
    {8, 40},
    {4, 30},
    {2, 20},
    {1, 0}
  };

  public BossbarTimer(
      Match match,
      TowersForPGM plugin,
      ConfigManager configManager,
      Captains captains,
      AvailablePlayers availablePlayers,
      Teams teams) {
    this.match = match;
    this.plugin = plugin;
    this.configManager = configManager;
    this.captains = captains;
    this.availablePlayers = availablePlayers;
    this.teams = teams;
  }

  public void startTimer(
      int duration, Component message, BossBar.Color color, Runnable onTick, Runnable onComplete) {
    cancelTimer();

    currentBar = BossBar.bossBar(message, 1f, color, BossBar.Overlay.PROGRESS);
    match.showBossBar(currentBar);

    int[] timeLeft = {duration};
    currentTimer = new BukkitRunnable() {
      @Override
      public void run() {

        if (timeLeft[0] <= 0) {
          if (onComplete != null) onComplete.run();
          cancel();
          return;
        }

        float progress = Math.max(0f, (float) timeLeft[0] / duration);
        currentBar.progress(progress);

        if (onTick != null) onTick.run();
        timeLeft[0]--;
      }
    }.runTaskTimer(plugin, 0, 20);
  }

  public void updateBarMessage(Component message) {
    if (currentBar != null) {
      currentBar.name(message);
    }
  }

  public void cancelTimer() {
    if (currentTimer != null) {
      currentTimer.cancel();
      currentTimer = null;
    }
    if (currentBar != null) {
      match.hideBossBar(currentBar);
      currentBar = null;
    }
  }

  public void startDraftTimer(Runnable onTimeoutPick) {
    if (!configManager.draft().isDraftTimer()) return;

    int duration = timerDuration();
    int[] timeLeft = {duration};

    startTimer(
        duration,
        Component.translatable(
            "draft.picks.bossbar",
            captainName(captains.isCaptain1Turn() ? 1 : 2),
            Component.text(SendMessage.formatTime(duration)).color(NamedTextColor.GREEN)),
        BossBar.Color.YELLOW,
        () -> {
          timeLeft[0]--;
          updateBarMessage(Component.translatable(
                  "draft.picks.bossbar", captainName(captains.isCaptain1Turn() ? 1 : 2))
              .append(Component.space())
              .append(Component.translatable(
                      "misc.timeRemaining",
                      Component.text(SendMessage.formatTime(timeLeft[0]))
                          .color(NamedTextColor.GREEN))
                  .color(NamedTextColor.AQUA)));

          MatchPlayer captain = PGM.get().getMatchManager().getPlayer(captains.getCurrentCaptain());
          if (captain != null) {
            if (timeLeft[0] <= 30 && timeLeft[0] > 3) captain.playSound(Sounds.INVENTORY_CLICK);
            else if (timeLeft[0] <= 3 && timeLeft[0] >= 1) captain.playSound(Sounds.WARNING);
          }
        },
        () -> {
          if (onTimeoutPick != null) {
            onTimeoutPick.run();
          }
        });
  }

  public Component captainName(int teamNumber) {
    Party team = teams.getTeam(teamNumber);
    MatchPlayer captain = PGM.get().getMatchManager().getPlayer(captains.getCaptain(teamNumber));

    return captain != null ? captain.getName() : team.getName();
  }

  public int timerDuration() {
    int size = availablePlayers.getAllAvailablePlayers().size();
    for (int[] entry : DURATION_TABLE) {
      if (size >= entry[0]) return entry[1];
    }
    return DEFAULT_DURATION;
  }
}
