package org.nicolie.towersforpgm.utils;

import org.bukkit.plugin.PluginManager;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.components.PicksGUI;
import org.nicolie.towersforpgm.draft.core.AvailablePlayers;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Draft;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.listeners.CommandListener;
import org.nicolie.towersforpgm.listeners.MatchAfterLoadListener;
import org.nicolie.towersforpgm.listeners.MatchFinishListener;
import org.nicolie.towersforpgm.listeners.MatchLoadListener;
import org.nicolie.towersforpgm.listeners.MatchStartListener;
import org.nicolie.towersforpgm.listeners.MatchStatsListener;
import org.nicolie.towersforpgm.listeners.ObserversKitApplyListener;
import org.nicolie.towersforpgm.listeners.PlayerJoinListener;
import org.nicolie.towersforpgm.listeners.PlayerParticipationListener;
import org.nicolie.towersforpgm.listeners.PlayerQuitListener;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.rankeds.RankedItem;
import org.nicolie.towersforpgm.refill.RefillManager;

public class Events {

  private final TowersForPGM plugin;

  public Events(TowersForPGM plugin) {
    this.plugin = plugin;
  }

  public void registerEvents(
      AvailablePlayers availablePlayers,
      Captains captains,
      Draft draft,
      Queue queue,
      PicksGUI pickInventory,
      RefillManager refillManager,
      Teams teams,
      PreparationListener preparationListener) {
    PluginManager pluginManager = plugin.getServer().getPluginManager();

    pluginManager.registerEvents(new CommandListener(captains), plugin);
    pluginManager.registerEvents(new PreparationListener(), plugin);
    pluginManager.registerEvents(
        new MatchLoadListener(refillManager, preparationListener, draft), plugin);
    pluginManager.registerEvents(new MatchAfterLoadListener(queue), plugin);
    pluginManager.registerEvents(
        new MatchStartListener(preparationListener, refillManager, captains), plugin);
    pluginManager.registerEvents(
        new MatchFinishListener(preparationListener, refillManager, draft), plugin);
    pluginManager.registerEvents(new ObserversKitApplyListener(pickInventory), plugin);
    pluginManager.registerEvents(
        new PlayerJoinListener(plugin, availablePlayers, teams, captains), plugin);
    pluginManager.registerEvents(new PlayerParticipationListener(teams, captains), plugin);
    pluginManager.registerEvents(new PlayerQuitListener(plugin, queue), plugin);
    // pluginManager.registerEvents(new CompetitorScoreChangeListener(), plugin);
    pluginManager.registerEvents(new MatchStatsListener(), plugin);
    pluginManager.registerEvents(new RankedItem(queue), plugin);
  }
}
