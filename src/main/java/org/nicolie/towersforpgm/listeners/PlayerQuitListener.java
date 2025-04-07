package org.nicolie.towersforpgm.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nicolie.towersforpgm.MatchManager;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.AvailablePlayers;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.Teams;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class PlayerQuitListener implements Listener {
    private final AvailablePlayers availablePlayers;
    private final Draft draft;
    private final MatchManager matchManager;
    private final TowersForPGM plugin;
    private final Teams teams;

    public PlayerQuitListener(AvailablePlayers availablePlayers, Draft draft, MatchManager matchManager, TowersForPGM plugin, Teams teams) {
        this.availablePlayers = availablePlayers;
        this.draft = draft;
        this.matchManager = matchManager;
        this.plugin = plugin;
        this.teams = teams;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String username = player.getName();
        if (draft.isDraftActive()){
            // Si el jugador está en getAvailablePlayers, pasarlo a getAvailableOfflinePlayers
            availablePlayers.handleDisconnect(player);
            // Si el jugador está en algún equipo, añadirlo a la lista de jugadores desconectados
            if (teams.isPlayerInAnyTeam(username)) {
                if (teams.isPlayerInTeam(username, 1)){
                    teams.removePlayerFromTeam(username, 1);
                    teams.addPlayerToTeam(username, 1);
                } else if (teams.isPlayerInTeam(username, 2)){
                    teams.removePlayerFromTeam(username, 2);
                    teams.addPlayerToTeam(username, 2);
                }
            }
        }

        // Si el jugador está en un equipo añadirlo a una lista de jugadores que se han desconectado
        Match match = matchManager.getMatch();
        MatchPlayer matchPlayer = match.getPlayer(player);
        Boolean isOnTeam = match.getPlayer(player).isParticipating();
        if (isOnTeam) {
            plugin.addDisconnectedPlayer(player.getName(), matchPlayer);
        }
    }
}