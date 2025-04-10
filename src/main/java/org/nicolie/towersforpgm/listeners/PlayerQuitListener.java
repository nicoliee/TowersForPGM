package org.nicolie.towersforpgm.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nicolie.towersforpgm.MatchManager;
import org.nicolie.towersforpgm.TowersForPGM;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class PlayerQuitListener implements Listener {
    private final MatchManager matchManager;
    private final TowersForPGM plugin;

    public PlayerQuitListener(MatchManager matchManager, TowersForPGM plugin) {
        this.matchManager = matchManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Si el jugador está en un equipo añadirlo a una lista de jugadores que se han desconectado
        Player player = event.getPlayer();
        Match match = matchManager.getMatch();
        MatchPlayer matchPlayer = match.getPlayer(player);
        Boolean isOnTeam = match.getPlayer(player).isParticipating();
        if (isOnTeam) {
            plugin.addDisconnectedPlayer(player.getName(), matchPlayer);
        }
    }
}