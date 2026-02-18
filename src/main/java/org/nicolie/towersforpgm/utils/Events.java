package org.nicolie.towersforpgm.utils;

import org.bukkit.plugin.PluginManager;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.components.PicksGUI;
import org.nicolie.towersforpgm.draft.core.AvailablePlayers;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Draft;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.draft.listeners.DraftMatchFinishListener;
import org.nicolie.towersforpgm.draft.listeners.DraftMatchLoadListener;
import org.nicolie.towersforpgm.draft.listeners.DraftMatchStartListener;
import org.nicolie.towersforpgm.draft.listeners.DraftObserverKitListener;
import org.nicolie.towersforpgm.draft.listeners.DraftPlayerJoinListener;
import org.nicolie.towersforpgm.draft.listeners.PlayerParticipationStartListener;
import org.nicolie.towersforpgm.draft.listeners.PlayerPartyChangeListener;
import org.nicolie.towersforpgm.listeners.CommandListener;
import org.nicolie.towersforpgm.listeners.MatchFinishListener;
import org.nicolie.towersforpgm.listeners.MatchLoadListener;
import org.nicolie.towersforpgm.listeners.PlayerQuitListener;
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
      AvailablePlayers availablePlayers,
      Captains captains,
      Draft draft,
      Queue queue,
      PicksGUI pickInventory,
      RefillManager refillManager,
      Teams teams,
      PreparationListener preparationListener) {
    PluginManager pluginManager = plugin.getServer().getPluginManager();

    pluginManager.registerEvents(new CommandListener(captains, availablePlayers, teams), plugin);
    pluginManager.registerEvents(new PreparationListener(), plugin);
    pluginManager.registerEvents(new MatchLoadListener(), plugin);
    pluginManager.registerEvents(new PrivateMatchListener(queue), plugin);
    pluginManager.registerEvents(new MatchFinishListener(), plugin);
    pluginManager.registerEvents(new PlayerQuitListener(plugin), plugin);
    pluginManager.registerEvents(new RankedItem(queue), plugin);

    pluginManager.registerEvents(new DraftMatchLoadListener(draft), plugin);
    pluginManager.registerEvents(new DraftMatchStartListener(captains), plugin);
    pluginManager.registerEvents(new DraftMatchFinishListener(draft), plugin);
    pluginManager.registerEvents(
        new DraftPlayerJoinListener(availablePlayers, teams, captains), plugin);
    pluginManager.registerEvents(new PlayerPartyChangeListener(teams, captains), plugin);
    pluginManager.registerEvents(new PlayerParticipationStartListener(teams, captains), plugin);
    pluginManager.registerEvents(new DraftObserverKitListener(pickInventory), plugin);

    pluginManager.registerEvents(new RankedMatchLoadListener(), plugin);
    pluginManager.registerEvents(new RankedMatchStartListener(), plugin);
    pluginManager.registerEvents(new RankedPlayerJoinListener(plugin), plugin);
    pluginManager.registerEvents(new RankedPlayerParticipationListener(), plugin);
    pluginManager.registerEvents(new RankedPlayerQuitListener(queue), plugin);
    pluginManager.registerEvents(new MatchStatsListener(), plugin);
    pluginManager.registerEvents(new RankedObserverKitListener(), plugin);

    pluginManager.registerEvents(new RefillMatchLoadListener(refillManager), plugin);
    pluginManager.registerEvents(new RefillMatchStartListener(refillManager), plugin);
    pluginManager.registerEvents(new RefillMatchFinishListener(refillManager), plugin);

    pluginManager.registerEvents(new PreparationMatchLoadListener(preparationListener), plugin);
    pluginManager.registerEvents(new PreparationMatchStartListener(preparationListener), plugin);
    pluginManager.registerEvents(new PreparationMatchFinishListener(preparationListener), plugin);
  }
}
