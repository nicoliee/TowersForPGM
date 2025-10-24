package org.nicolie.towersforpgm.utils;

import org.bukkit.plugin.PluginManager;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.AvailablePlayers;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.PicksGUI;
import org.nicolie.towersforpgm.draft.Teams;
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
import org.nicolie.towersforpgm.rankeds.ItemListener;
import org.nicolie.towersforpgm.rankeds.Queue;
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
        new MatchFinishListener(plugin, preparationListener, refillManager, draft), plugin);
    pluginManager.registerEvents(new ObserversKitApplyListener(pickInventory), plugin);
    pluginManager.registerEvents(
        new PlayerJoinListener(plugin, availablePlayers, teams, captains), plugin);
    pluginManager.registerEvents(new PlayerParticipationListener(teams, captains), plugin);
    pluginManager.registerEvents(new PlayerQuitListener(plugin, queue), plugin);
    // pluginManager.registerEvents(new CompetitorScoreChangeListener(), plugin);
    pluginManager.registerEvents(new MatchStatsListener(), plugin);
    pluginManager.registerEvents(new ItemListener(queue), plugin);
  }
}
