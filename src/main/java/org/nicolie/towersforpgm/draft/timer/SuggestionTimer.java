package org.nicolie.towersforpgm.draft.timer;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.ConfigManager;
import org.nicolie.towersforpgm.draft.team.AvailablePlayers;
import org.nicolie.towersforpgm.draft.team.Captains;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.Sounds;

public class SuggestionTimer {
  private final TowersForPGM plugin;
  private final ConfigManager configManager;
  private final Captains captains;
  private final AvailablePlayers availablePlayers;

  private final int SUGGESTION_TIME = 5;
  private BukkitTask suggestionTask;

  public SuggestionTimer(
      TowersForPGM plugin,
      ConfigManager configManager,
      Captains captains,
      AvailablePlayers availablePlayers) {
    this.plugin = plugin;
    this.configManager = configManager;
    this.captains = captains;
    this.availablePlayers = availablePlayers;
  }

  public void startTimer() {
    cancelTimer();
    int[] elapsedTime = {0};
    suggestionTask = new BukkitRunnable() {
      @Override
      public void run() {
        if (elapsedTime[0] == SUGGESTION_TIME) {
          suggestPicksForCaptains();
        }
        elapsedTime[0]++;
      }
    }.runTaskTimer(plugin, 20L, 20L);
  }

  public void cancelTimer() {
    if (suggestionTask != null) {
      suggestionTask.cancel();
      suggestionTask = null;
    }
  }

  private void suggestPicksForCaptains() {
    if (!configManager.draft().isDraftSuggestions()) return;
    // No sugerencias si la base de datos no está activada
    if (!TowersForPGM.getInstance().getIsDatabaseActivated()) return;
    MatchPlayer currentCaptain =
        PGM.get().getMatchManager().getPlayer(captains.getCurrentCaptain());
    int size = availablePlayers.getAllAvailablePlayers().size();
    List<String> topPlayers = availablePlayers.getTopPlayers();
    if (topPlayers.isEmpty() || size <= 1) {
      return;
    }
    // Limitar a 3 sugerencias como máximo, o menos si hay menos jugadores disponibles
    int limit = size > 6 ? 3 : (size > 3 ? 2 : 1);
    topPlayers = new ArrayList<>(topPlayers.subList(0, Math.min(topPlayers.size(), limit)));
    if (limit > 1) {
      java.util.Collections.shuffle(topPlayers);
    }
    Component suggestionsComponent = MatchManager.orList(
        MatchManager.convert(topPlayers, NamedTextColor.AQUA), NamedTextColor.DARK_GRAY);
    if (currentCaptain != null) {

      Component suggestions = Component.translatable(
              "draft.captains.suggestion", suggestionsComponent)
          .color(NamedTextColor.GRAY);
      currentCaptain.playSound(Sounds.ITEM_PICKUP);
      currentCaptain.sendMessage(suggestions);
    }
  }
}
