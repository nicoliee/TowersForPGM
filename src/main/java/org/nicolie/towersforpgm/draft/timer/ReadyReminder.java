package org.nicolie.towersforpgm.draft.timer;

import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.team.Captains;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.util.bukkit.Sounds;

public class ReadyReminder {
  private final TowersForPGM plugin;
  private final Captains captains;

  private final int REMINDER_INTERVAL = 15;
  private BukkitTask reminderTask;

  public ReadyReminder(TowersForPGM plugin, Captains captains) {
    this.plugin = plugin;
    this.captains = captains;
  }

  public void startTimer() {
    cancelTimer();
    sendReadyReminder(true);
    reminderTask = new BukkitRunnable() {
      @Override
      public void run() {
        sendReadyReminder(false);
      }
    }.runTaskTimer(plugin, 20L * REMINDER_INTERVAL, 20L * REMINDER_INTERVAL);
  }

  public void cancelTimer() {
    if (reminderTask != null) {
      reminderTask.cancel();
      reminderTask = null;
    }
  }

  private void sendReadyReminder(boolean isActionBar) {
    MatchPlayer captain1 = PGM.get().getMatchManager().getPlayer(captains.getCaptain1());
    MatchPlayer captain2 = PGM.get().getMatchManager().getPlayer(captains.getCaptain2());
    Component readyActionBarMessage = Component.translatable(
            "draft.ready.tip", Component.text("/ready"))
        .color(NamedTextColor.GOLD)
        .color(NamedTextColor.AQUA);
    if (isActionBar) {
      if (captain1 != null) {
        captain1.sendActionBar(readyActionBarMessage);
      }
      if (captain2 != null) {
        captain2.sendActionBar(readyActionBarMessage);
      }
    } else {
      if (captain1 != null) {
        captain1.sendMessage(readyActionBarMessage);
        captain1.playSound(Sounds.DIRECT_MESSAGE);
      }
      if (captain2 != null) {
        captain2.sendMessage(readyActionBarMessage);
        captain2.playSound(Sounds.DIRECT_MESSAGE);
      }
    }
  }

  public void setReady(int teamNumber, Match match) {
    if (teamNumber == 1) {
      captains.setReady1(true);
    } else if (teamNumber == 2) {
      captains.setReady2(true);
    }
    checkReady(match);
  }

  private void checkReady(Match match) {
    if (captains.isReady1() && captains.isReady2()) {
      cancelTimer();
      onCaptainsReady(match);
    }
  }

  private void onCaptainsReady(Match match) {
    match
        .needModule(StartMatchModule.class)
        .forceStartCountdown(Duration.ofSeconds(5), Duration.ZERO);
  }
}
