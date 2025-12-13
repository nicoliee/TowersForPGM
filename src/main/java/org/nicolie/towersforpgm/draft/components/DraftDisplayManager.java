package org.nicolie.towersforpgm.draft.components;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.scheduler.BukkitRunnable;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.ConfigManager;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.draft.core.Utilities;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.MatchManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.Sounds;

public class DraftDisplayManager {
  private final ConfigManager configManager;
  private final DraftState state;
  private final Captains captains;
  private final Teams teams;
  private final Utilities utilities;
  private final TowersForPGM plugin;
  private DraftPickManager pickManager;

  public DraftDisplayManager(
      TowersForPGM plugin,
      ConfigManager configManager,
      DraftState state,
      Captains captains,
      Teams teams,
      Utilities utilities) {
    this.plugin = plugin;
    this.configManager = configManager;
    this.state = state;
    this.captains = captains;
    this.teams = teams;
    this.utilities = utilities;
  }

  public void setPickManager(DraftPickManager pickManager) {
    this.pickManager = pickManager;
  }

  public void startDraftTimer() {
    if (!configManager.draft().isDraftTimer()) {
      return;
    }

    if (state.getPickTimerBar() == null) {
      String currentCaptainName =
          captains.isCaptain1Turn() ? captains.getCaptain1Name() : captains.getCaptain2Name();
      int currentTeamNumber = captains.isCaptain1Turn() ? 1 : 2;
      String currentCaptainColor = teams.getTeamColor(currentTeamNumber);
      String bossbarMessage = LanguageManager.message("draft.captains.bossbar")
          .replace("{captain}", currentCaptainColor + currentCaptainName);

      BossBar bossBar = BossBar.bossBar(
          Component.text(
              bossbarMessage.replace("{time}", String.valueOf(utilities.timerDuration()))),
          1f,
          BossBar.Color.YELLOW,
          BossBar.Overlay.PROGRESS);
      state.setPickTimerBar(bossBar);
      MatchManager.getMatch().showBossBar(bossBar);
    }

    if (state.getDraftTimer() != null) {
      state.getDraftTimer().cancel();
    }

    int initialTime = utilities.timerDuration();
    int[] timeLeft = {initialTime};

    MatchPlayer currentCaptain =
        PGM.get().getMatchManager().getPlayer(captains.getCurrentCaptain());
    String currentCaptainName;
    if (currentCaptain != null) {
      currentCaptainName =
          captains.isCaptain1Turn() ? captains.getCaptain1Name() : captains.getCaptain2Name();
    } else {
      int teamNumber = captains.isCaptain1Turn() ? 1 : 2;
      currentCaptainName = teams.getTeamName(teamNumber);
    }
    int currentTeamNumber = captains.isCaptain1Turn() ? 1 : 2;
    String currentCaptainColor = teams.getTeamColor(currentTeamNumber);
    String bossbarMessage = LanguageManager.message("draft.captains.bossbar")
        .replace("{captain}", currentCaptainColor + currentCaptainName);
    if (currentCaptain != null) {
      currentCaptain.sendActionBar(Component.text(LanguageManager.message("draft.captains.tip")));
    }

    BukkitRunnable draftTimer = new BukkitRunnable() {
      @Override
      public void run() {
        float progress = Math.max(0f, (float) timeLeft[0] / initialTime);
        if (state.getPickTimerBar() != null) {
          state
              .getPickTimerBar()
              .name(Component.text(
                  bossbarMessage.replace("{time}", SendMessage.formatTime(timeLeft[0]))));
          state.getPickTimerBar().progress(progress);
        }

        if (timeLeft[0] == initialTime - 5) {
          utilities.suggestPicksForCaptains();
        }
        if (timeLeft[0] <= 30 && timeLeft[0] > 3) {
          MatchPlayer current = PGM.get().getMatchManager().getPlayer(captains.getCurrentCaptain());
          if (current != null) {
            current.playSound(Sounds.INVENTORY_CLICK);
          }
        } else if (timeLeft[0] <= 3 && timeLeft[0] >= 1) {
          MatchPlayer current = PGM.get().getMatchManager().getPlayer(captains.getCurrentCaptain());
          if (current != null) {
            current.playSound(Sounds.WARNING);
          }
        }
        if ((timeLeft[0] <= 5 && timeLeft[0] >= 1)) {
          MatchPlayer current = PGM.get().getMatchManager().getPlayer(captains.getCurrentCaptain());
          if (current != null) {
            SendMessage.sendToPlayer(
                current.getBukkit(),
                LanguageManager.message("draft.captains.timeRemaining")
                    .replace("{time}", SendMessage.formatTime(timeLeft[0])));
          }
        }

        if (timeLeft[0] == 0) {
          if (pickManager == null) {
            cancel();
            return;
          }
          String pick = utilities.randomPick();
          if (pick == null) {
            pickManager.endDraft();
          } else {
            pickManager.pickPlayer(pick);
          }
          cancel();
        }
        timeLeft[0]--;
      }
    };
    draftTimer.runTaskTimer(plugin, 0, 20);
    state.setDraftTimer(draftTimer);
  }

  public void removeBossbar() {
    if (state.getPickTimerBar() != null) {
      MatchManager.getMatch().hideBossBar(state.getPickTimerBar());
      state.setPickTimerBar(null);
    }
  }
}
