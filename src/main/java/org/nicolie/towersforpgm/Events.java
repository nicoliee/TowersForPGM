package org.nicolie.towersforpgm;

import org.nicolie.towersforpgm.draft.AvailablePlayers;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.PickInventory;
import org.nicolie.towersforpgm.draft.Teams;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.listeners.CompetitorScoreChangeListener;
import org.nicolie.towersforpgm.listeners.MatchAfterLoadListener;
import org.nicolie.towersforpgm.listeners.MatchFinishListener;
import org.nicolie.towersforpgm.listeners.MatchLoadListener;
import org.nicolie.towersforpgm.listeners.MatchStartListener;
import org.nicolie.towersforpgm.listeners.MatchStatsListener;
import org.nicolie.towersforpgm.listeners.PlayerJoinListener;
import org.nicolie.towersforpgm.listeners.PlayerParticipationListener;
import org.nicolie.towersforpgm.listeners.PlayerQuitListener;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;

import org.bukkit.plugin.PluginManager;

public class Events {

    private final TowersForPGM plugin;

    public Events(TowersForPGM plugin) {
        this.plugin = plugin;
    }

    public void registerEvents(AvailablePlayers availablePlayers, Captains captains, Draft draft, MatchManager matchManager, LanguageManager languageManager, PickInventory pickInventory, RefillManager refillManager, Teams teams, PreparationListener preparationListener) {
        PluginManager pluginManager = plugin.getServer().getPluginManager();

        pluginManager.registerEvents(new PreparationListener(languageManager), plugin);
        pluginManager.registerEvents(new MatchLoadListener(refillManager, preparationListener, matchManager, draft, languageManager), plugin);
        pluginManager.registerEvents(new MatchAfterLoadListener(), plugin);
        pluginManager.registerEvents(new MatchStartListener(preparationListener, refillManager, captains), plugin);
        pluginManager.registerEvents(new MatchFinishListener(plugin, preparationListener, refillManager, draft, languageManager), plugin);
        pluginManager.registerEvents(new PlayerJoinListener(plugin, availablePlayers, teams, captains, pickInventory), plugin);
        pluginManager.registerEvents(new PlayerParticipationListener(teams, captains, languageManager), plugin);
        pluginManager.registerEvents(new PlayerQuitListener(plugin), plugin);
        pluginManager.registerEvents(new CompetitorScoreChangeListener(), plugin);
        pluginManager.registerEvents(new MatchStatsListener(languageManager), plugin);
    }
}