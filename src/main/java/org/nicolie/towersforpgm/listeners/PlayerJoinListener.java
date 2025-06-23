package org.nicolie.towersforpgm.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.AvailablePlayers;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.PickInventory;
import org.nicolie.towersforpgm.draft.Teams;
import org.nicolie.towersforpgm.rankeds.ItemListener;
import org.nicolie.towersforpgm.utils.ConfigManager;

import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;

import org.bukkit.entity.Player;

public class PlayerJoinListener implements Listener {
    private final AvailablePlayers availablePlayers;
    private final Captains captains;
    private final Teams teams;
    private final TowersForPGM plugin;
    private final PickInventory pickInventory;

    public PlayerJoinListener(TowersForPGM plugin, AvailablePlayers availablePlayers, Teams teams, Captains captains, PickInventory pickInventory) {
        this.availablePlayers = availablePlayers;
        this.captains = captains;
        this.teams = teams;
        this.plugin = plugin;
        this.pickInventory = pickInventory;

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String username = player.getName();
        Match match = PGM.get().getMatchManager().getMatch(player);
        String map = match.getMap().getName();
        // Si el draft está activo
        if(Draft.isDraftActive() && !PGM.get().getMatchManager().getMatch(player).isRunning()) {
            // Si el jugador está en getAvailableOfflinePlayers, pasarlo a getAvailablePlayers
            availablePlayers.handleReconnect(player);
            pickInventory.giveItemToPlayer(player);
            // Si el jugador está en algún equipo, añadirlo a la lista de jugadores desconectados
            if (teams.isPlayerInAnyTeam(username)) {
                teams.handleReconnect(player);
            }
        }

        // Verificamos si el jugador está en team1
        if (teams.isPlayerInTeam(player.getName(), 1) ||
            (captains.getCaptain1() != null && captains.getCaptain1().equals(player.getUniqueId()))) {
                teams.assignTeam(player, 1);
        } 
        
        // Verificamos si el jugador está en team2
        else if (teams.isPlayerInTeam(player.getName(), 2) ||
                 (captains.getCaptain2() != null && captains.getCaptain2().equals(player.getUniqueId()))) {
                    teams.assignTeam(player, 2);
        }

        if (plugin.getDisconnectedPlayers().get(player.getName()) != null){
            plugin.getDisconnectedPlayers().remove(player.getName());
        }

        if (ConfigManager.getRankedMaps().contains(map) && !match.isRunning()){
            ItemListener.giveItem(player);
        }
    }
}