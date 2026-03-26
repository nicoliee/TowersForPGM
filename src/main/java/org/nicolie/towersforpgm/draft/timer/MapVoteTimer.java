package org.nicolie.towersforpgm.draft.timer;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.nicolie.towersforpgm.draft.map.MapVoteConfig;
import org.nicolie.towersforpgm.utils.SendMessage;

public final class MapVoteTimer {

  private final BossbarTimer bossbarTimer;
  private final MapVoteConfig config;
  private int timeLeft;

  public MapVoteTimer(BossbarTimer bossbarTimer, MapVoteConfig config) {
    this.bossbarTimer = bossbarTimer;
    this.config = config;
  }

  public void start(Runnable onComplete) {
    int duration = config.getDuration();
    this.timeLeft = duration;

    bossbarTimer.startTimer(
        duration,
        buildBossbarMsg(),
        BossBar.Color.GREEN,
        () -> {
          timeLeft--;
          bossbarTimer.updateBarMessage(buildBossbarMsg());
        },
        onComplete);
  }

  public void cancel() {
    bossbarTimer.cancelTimer();
  }

  public int getTimeLeft() {
    return Math.max(timeLeft, 0);
  }

  private Component buildBossbarMsg() {
    String key;
    switch (config.getVoteMode()) {
      case VETO:
        key = "draft.map.veto.title";
        break;
      case PLURALITY:
      default:
        key = "draft.map.vote.title";
        break;
    }

    return Component.translatable(key)
        .append(Component.space())
        .append(Component.translatable(
                "misc.timeRemaining",
                Component.text(SendMessage.formatTime(getTimeLeft())).color(NamedTextColor.GREEN))
            .color(NamedTextColor.AQUA));
  }
}
