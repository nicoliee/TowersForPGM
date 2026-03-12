package org.nicolie.towersforpgm.draft.components;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.ConfigManager;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.draft.core.Utilities;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.Sounds;

public class BossbarTimer {
  private final ConfigManager configManager;
  private final Captains captains;
  private final Teams teams;
  private final Utilities utilities;
  private final TowersForPGM plugin;
  private DraftPickManager pickManager;

  private BossBar currentBar;
  private BukkitTask currentTimer;

  public BossbarTimer(
      TowersForPGM plugin,
      ConfigManager configManager,
      Captains captains,
      Teams teams,
      Utilities utilities) {
    this.plugin = plugin;
    this.configManager = configManager;
    this.captains = captains;
    this.teams = teams;
    this.utilities = utilities;
  }

  public void setPickManager(DraftPickManager pickManager) {
    this.pickManager = pickManager;
  }

  public static String formatTime(int totalSeconds) {
    int minutes = Math.max(0, totalSeconds) / 60;
    int seconds = Math.max(0, totalSeconds) % 60;
    return String.format("%02d:%02d", minutes, seconds);
  }

  public void startTimer(
      int duration, Component message, BossBar.Color color, Runnable onTick, Runnable onComplete) {
    cancelTimer();

    currentBar = BossBar.bossBar(message, 1f, color, BossBar.Overlay.PROGRESS);
    MatchManager.getMatch().showBossBar(currentBar);

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
      MatchManager.getMatch().hideBossBar(currentBar);
      currentBar = null;
    }
  }

  public void startDraftTimer() {
    if (!configManager.draft().isDraftTimer()) return;

    int duration = utilities.timerDuration();
    int[] timeLeft = {duration};

    startTimer(
        duration,
        Component.translatable(
            "draft.picks.bossbar",
            captainName(),
            Component.text(formatTime(duration)).color(NamedTextColor.GREEN)),
        BossBar.Color.YELLOW,
        () -> {
          timeLeft[0]--;
          updateBarMessage(Component.translatable("draft.picks.bossbar", captainName())
              .append(Component.space())
              .append(Component.translatable(
                      "misc.timeRemaining",
                      Component.text(formatTime(timeLeft[0])).color(NamedTextColor.GREEN))
                  .color(NamedTextColor.AQUA)));

          if (timeLeft[0] == duration - 5) utilities.suggestPicksForCaptains();

          MatchPlayer captain = PGM.get().getMatchManager().getPlayer(captains.getCurrentCaptain());
          if (captain != null) {
            if (timeLeft[0] <= 30 && timeLeft[0] > 3) captain.playSound(Sounds.INVENTORY_CLICK);
            else if (timeLeft[0] <= 3 && timeLeft[0] >= 1) captain.playSound(Sounds.WARNING);
          }
        },
        () -> {
          if (pickManager == null) return;
          String pick = utilities.randomPick();
          if (pick == null) pickManager.endDraft();
          else pickManager.pickPlayer(pick);
        });
  }

  public void removeBossbar() {
    cancelTimer();
  }

  private Component captainName() {
    int currentCaptainNumber = captains.isCaptain1Turn() ? 1 : 2;
    Party team = teams.getTeam(currentCaptainNumber);
    MatchPlayer currentCaptain =
        PGM.get().getMatchManager().getPlayer(captains.getCurrentCaptain());
    return currentCaptain != null
        ? MatchManager.getPrefixedName(
            captains.isCaptain1Turn() ? captains.getCaptain1Name() : captains.getCaptain2Name())
        : team.getName();
  }
}
