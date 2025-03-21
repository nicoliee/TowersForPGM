package org.nicolie.towersforpgm.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.utils.SendMessage;
import org.bukkit.entity.Player;

public class PlayerJoinListener implements Listener {
    private final TowersForPGM plugin;
    private final Draft draft;

    public PlayerJoinListener(TowersForPGM plugin, Draft draft) {
        this.plugin = plugin;
        this.draft = draft;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Verificamos si el jugador está en team1Offline, team1 o es capitán 1
        if (draft.getTeam1Offline().contains(player.getName()) ||
            draft.getTeam1Names().contains(player.getName()) ||
            (draft.getCaptain1() != null && draft.getCaptain1().getName().equals(player.getName()))) {
                draft.assignPlayerToTeam(player, 1);
        } 
        
        // Verificamos si el jugador está en team2 o es capitán 2
        else if (draft.getTeam2Offline().contains(player.getName()) ||
                 draft.getTeam2Names().contains(player.getName()) ||
                 (draft.getCaptain2() != null && draft.getCaptain2().getName().equals(player.getName()))) {
                    draft.assignPlayerToTeam(player, 2);
        }

        // Si el draft está activo
        if(draft.isDraftActive()){
            // Si el jugador está en getAvailableOfflinePlayers, pasarlo a getAvailablePlayers
            if (draft.getAvailableOfflinePlayers().contains(player.getName())) {
                draft.getAvailableOfflinePlayers().remove(player.getName());
                draft.getAvailablePlayers().add(player);
            }
        }

        if (plugin.getDisconnectedPlayers().get(player.getName()) != null){
            SendMessage.sendToDevelopers("El jugador " + player.getName() + " se ha reconectado, eliminando sus estadísticas de desconexión.");
            plugin.getDisconnectedPlayers().remove(player.getName());
        }
    }
}
