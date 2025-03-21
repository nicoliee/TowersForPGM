package org.nicolie.towersforpgm.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.utils.SendMessage;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class PlayerQuitListener implements Listener {
    private final TowersForPGM plugin;
    private final Draft draft;

    public PlayerQuitListener(TowersForPGM plugin, Draft draft) {
        this.plugin = plugin;
        this.draft = draft;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (draft.isDraftActive()){
            // Si el jugador está en getAvailablePlayers, pasarlo a getAvailableOfflinePlayers
            if (draft.getAvailablePlayerNames().contains(player.getName())) {
                draft.getAvailablePlayers().remove(player);
                draft.getAvailableOfflinePlayers().add(player.getName());
            }
        }

        // Si el jugador está en un equipo añadirlo a una lista de jugadores que se han desconectado
        Match match = plugin.getCurrentMatch();
        MatchPlayer matchPlayer = match.getPlayer(player);
        Boolean isOnTeam = match.getPlayer(player).isParticipating();
        if (isOnTeam) {
            SendMessage.sendToDevelopers("El jugador " + player.getName() + " se ha desconectado, pero sus estadísticas se mantendrán.");
            plugin.addDisconnectedPlayer(player.getName(), matchPlayer);
        }
    }
}