package org.nicolie.towersforpgm;

import org.nicolie.towersforpgm.draft.AvailablePlayers;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.Teams;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.listeners.CompetitorScoreChangeListener;
import org.nicolie.towersforpgm.listeners.MatchAfterLoadListener;
import org.nicolie.towersforpgm.listeners.MatchFinishListener;
import org.nicolie.towersforpgm.listeners.MatchLoadListener;
import org.nicolie.towersforpgm.listeners.MatchStartListener;
import org.nicolie.towersforpgm.listeners.MatchStatsListener;
import org.nicolie.towersforpgm.listeners.PlayerJoinListener;
import org.nicolie.towersforpgm.listeners.PlayerParticipationStartListener;
import org.nicolie.towersforpgm.listeners.PlayerQuitListener;
import org.nicolie.towersforpgm.preparationTime.TorneoListener;

import org.bukkit.plugin.PluginManager;

public class Events {

    private final TowersForPGM plugin;

    public Events(TowersForPGM plugin) {
        this.plugin = plugin;
    }

    public void registerEvents(AvailablePlayers availablePlayers, Captains captains, Draft draft, MatchManager matchManager, RefillManager refillManager, Teams teams, TorneoListener torneoListener) {
        PluginManager pluginManager = plugin.getServer().getPluginManager();

        pluginManager.registerEvents(new TorneoListener(plugin), plugin);
        pluginManager.registerEvents(new MatchLoadListener(refillManager, torneoListener, matchManager), plugin);
        pluginManager.registerEvents(new MatchAfterLoadListener(), plugin);
        pluginManager.registerEvents(new MatchStartListener(torneoListener, refillManager, captains), plugin);
        pluginManager.registerEvents(new MatchFinishListener(plugin, torneoListener, refillManager, draft), plugin);
        pluginManager.registerEvents(new PlayerJoinListener(plugin, draft, availablePlayers, teams, captains), plugin);
        pluginManager.registerEvents(new PlayerParticipationStartListener(teams, captains), plugin);
        pluginManager.registerEvents(new PlayerQuitListener(availablePlayers, draft, matchManager, plugin, teams), plugin);
        pluginManager.registerEvents(new CompetitorScoreChangeListener(), plugin);
        pluginManager.registerEvents(new MatchStatsListener(), plugin);
    }
}