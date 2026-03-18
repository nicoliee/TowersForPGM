package org.nicolie.towersforpgm.utils;

import org.bukkit.plugin.PluginManager;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.listeners.DraftMatchStartListener;
import org.nicolie.towersforpgm.draft.listeners.DraftObserverKitListener;
import org.nicolie.towersforpgm.draft.listeners.DraftPlayerJoinListener;
import org.nicolie.towersforpgm.draft.listeners.DraftPlayerParticipationStartListener;
import org.nicolie.towersforpgm.draft.listeners.DraftPlayerParticipationStopListener;
import org.nicolie.towersforpgm.draft.listeners.PlayerPartyChangeListener;
import org.nicolie.towersforpgm.draft.pick.gui.PicksGUIManager;
import org.nicolie.towersforpgm.listeners.CommandListener;
import org.nicolie.towersforpgm.listeners.MatchFinishListener;
import org.nicolie.towersforpgm.listeners.MatchLoadListener;
import org.nicolie.towersforpgm.listeners.PrivateMatchListener;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;
import org.nicolie.towersforpgm.preparationTime.listeners.PreparationMatchFinishListener;
import org.nicolie.towersforpgm.preparationTime.listeners.PreparationMatchLoadListener;
import org.nicolie.towersforpgm.preparationTime.listeners.PreparationMatchStartListener;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.rankeds.RankedItem;
import org.nicolie.towersforpgm.rankeds.listeners.MatchStatsListener;
import org.nicolie.towersforpgm.rankeds.listeners.RankedMatchLoadListener;
import org.nicolie.towersforpgm.rankeds.listeners.RankedMatchStartListener;
import org.nicolie.towersforpgm.rankeds.listeners.RankedObserverKitListener;
import org.nicolie.towersforpgm.rankeds.listeners.RankedPlayerJoinListener;
import org.nicolie.towersforpgm.rankeds.listeners.RankedPlayerParticipationListener;
import org.nicolie.towersforpgm.rankeds.listeners.RankedPlayerQuitListener;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.refill.listeners.RefillMatchFinishListener;
import org.nicolie.towersforpgm.refill.listeners.RefillMatchLoadListener;
import org.nicolie.towersforpgm.refill.listeners.RefillMatchStartListener;

public class Events {

  private final TowersForPGM plugin;

  public Events(TowersForPGM plugin) {
    this.plugin = plugin;
  }

  public void registerEvents(
      Queue queue, RefillManager refillManager, PreparationListener preparationListener) {
    PluginManager pluginManager = plugin.getServer().getPluginManager();

    // General
    pluginManager.registerEvents(new CommandListener(), plugin);
    pluginManager.registerEvents(new PreparationListener(), plugin);
    pluginManager.registerEvents(new MatchLoadListener(), plugin);
    org.nicolie.towersforpgm.listeners.MatchStatsListener matchStatsListener =
        new org.nicolie.towersforpgm.listeners.MatchStatsListener();
    pluginManager.registerEvents(matchStatsListener, plugin);
    pluginManager.registerEvents(new MatchFinishListener(matchStatsListener), plugin);
    pluginManager.registerEvents(new PrivateMatchListener(queue), plugin);
    pluginManager.registerEvents(new RankedItem(queue), plugin);

    // Draft
    pluginManager.registerEvents(new DraftMatchStartListener(), plugin);
    pluginManager.registerEvents(new DraftPlayerJoinListener(), plugin);
    pluginManager.registerEvents(new PlayerPartyChangeListener(), plugin);
    pluginManager.registerEvents(new DraftPlayerParticipationStartListener(), plugin);
    pluginManager.registerEvents(new DraftPlayerParticipationStopListener(), plugin);
    pluginManager.registerEvents(new PlayerPartyChangeListener(), plugin);
    pluginManager.registerEvents(new DraftObserverKitListener(), plugin);
    pluginManager.registerEvents(new PicksGUIManager(plugin), plugin);

    // Ranked
    pluginManager.registerEvents(new RankedMatchLoadListener(), plugin);
    pluginManager.registerEvents(new RankedMatchStartListener(), plugin);
    pluginManager.registerEvents(new RankedPlayerJoinListener(plugin), plugin);
    pluginManager.registerEvents(new RankedPlayerParticipationListener(), plugin);
    pluginManager.registerEvents(new RankedPlayerQuitListener(queue), plugin);
    pluginManager.registerEvents(new MatchStatsListener(), plugin);
    pluginManager.registerEvents(new RankedObserverKitListener(), plugin);

    // Refill
    pluginManager.registerEvents(new RefillMatchLoadListener(refillManager), plugin);
    pluginManager.registerEvents(new RefillMatchStartListener(refillManager), plugin);
    pluginManager.registerEvents(new RefillMatchFinishListener(refillManager), plugin);

    // Preparation Time
    pluginManager.registerEvents(new PreparationMatchLoadListener(preparationListener), plugin);
    pluginManager.registerEvents(new PreparationMatchStartListener(preparationListener), plugin);
    pluginManager.registerEvents(new PreparationMatchFinishListener(preparationListener), plugin);
  }
}
