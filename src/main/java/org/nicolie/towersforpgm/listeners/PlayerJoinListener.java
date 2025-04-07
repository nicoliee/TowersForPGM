package org.nicolie.towersforpgm.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.AvailablePlayers;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.Teams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlayerJoinListener implements Listener {
    private final AvailablePlayers availablePlayers;
    private final Captains captains;
    private final Draft draft;
    private final Teams teams;
    private final TowersForPGM plugin;

    public PlayerJoinListener(TowersForPGM plugin, Draft draft, AvailablePlayers availablePlayers, Teams teams, Captains captains) {
        this.availablePlayers = availablePlayers;
        this.captains = captains;
        this.draft = draft;
        this.teams = teams;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String username = player.getName();
        // Si el draft está activo
        if(draft.isDraftActive()){
            // Si el jugador está en getAvailableOfflinePlayers, pasarlo a getAvailablePlayers
            availablePlayers.handleReconnect(player);
            // Si el jugador está en algún equipo, añadirlo a la lista de jugadores desconectados
            if (teams.isPlayerInAnyTeam(username)) {
                if (teams.isPlayerInTeam(username, 1)){
                    teams.removePlayerFromTeam(username, 1);
                    
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        teams.addPlayerToTeam(username, 1);
                    }, 5L); // Esperar 5 ticks antes de asignar el equipo
                } else if (teams.isPlayerInTeam(username, 2)){
                    teams.removePlayerFromTeam(username, 2);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        teams.addPlayerToTeam(username, 2);
                    }, 5L); // Esperar 5 ticks antes de asignar el equipo
                }
            }
        }

        // Verificamos si el jugador está en team1
        if (teams.isPlayerInTeam(player.getName(), 1) ||
            (captains.getCaptain1() != null && captains.getCaptain1().equals(player.getUniqueId()))) {
                draft.assignPlayerToTeam(player, 1);
        } 
        
        // Verificamos si el jugador está en team2
        else if (teams.isPlayerInTeam(player.getName(), 2) ||
                 (captains.getCaptain2() != null && captains.getCaptain2().equals(player.getUniqueId()))) {
                    draft.assignPlayerToTeam(player, 2);
        }

        if (plugin.getDisconnectedPlayers().get(player.getName()) != null){
            plugin.getDisconnectedPlayers().remove(player.getName());
        }
    }
}