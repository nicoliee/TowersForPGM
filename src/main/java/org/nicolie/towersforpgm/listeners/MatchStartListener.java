package org.nicolie.towersforpgm.listeners;

import org.bukkit.event.Listener;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.utils.ConfigManager;

import me.tbg.match.bot.MatchBot;

import java.util.Collection;

import org.bukkit.event.EventHandler;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.player.MatchPlayer;

import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.matchbot.Embed;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;
import java.util.ArrayList;
import java.util.List;

public class MatchStartListener implements Listener{
    private final PreparationListener preparationListener;
    private final RefillManager refillManager;
    private final TowersForPGM plugin;
    private final Captains captains;

    public MatchStartListener(PreparationListener preparationListener, RefillManager refillManager, Captains captains) {
        this.captains = captains;
        this.preparationListener = preparationListener;
        this.refillManager = refillManager;
        this.plugin = TowersForPGM.getInstance();
        
    }

    @EventHandler
    public void onMatchStart(MatchStartEvent event) {
        String worldName = event.getMatch().getWorld().getName();
        if (captains.isReadyActive()){
            Draft.cancelReadyReminder();
            captains.setReadyActive(false);
            captains.setReady1(false, null);
            captains.setReady2(false, null);
        }
        refillManager.startRefillTask(worldName);
        if (plugin.isPreparationEnabled()) {
            preparationListener.startProtection(null, event.getMatch());
        }
        String table;
        if (ConfigManager.getTempTable() != null) {
            table = ConfigManager.getTempTable();
        } else {
            table = ConfigManager.getTableForMap(event.getMatch().getMap().getName());
        }
        String map = event.getMatch().getMap().getName();
        boolean isRanked = ConfigManager.getRankedTables().contains(table) && ConfigManager.getRankedMaps().contains(map);
        if(plugin.isMatchBotEnabled() && isRanked){
            Collection<MatchPlayer> players = event.getMatch().getPlayers();
            List<String> usernames = new ArrayList<>();
            for (MatchPlayer player : players) {
                usernames.add(player.getNameLegacy());
            }
            StatsManager.getEloForUsernames(table, usernames, eloChanges -> {
                EmbedBuilder embed = Embed.createMatchStartEmbed(event.getMatch(), eloChanges);
                MatchBot.getInstance().getBot().setEmbedThumbnail(event.getMatch().getMap(), embed, MatchBot.getInstance().getBot());
                MatchBot.getInstance().getBot().sendMatchEmbed(embed, event.getMatch(), ConfigManager.getRankedChannel(), null);
            });
        }
    }
}