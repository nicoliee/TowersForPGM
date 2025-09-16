package org.nicolie.towersforpgm.listeners;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.rankeds.Queue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class PlayerQuitListener implements Listener {
  private final TowersForPGM plugin;
  private final Queue queue;

  public PlayerQuitListener(TowersForPGM plugin, Queue queue) {
    this.plugin = plugin;
    this.queue = queue;
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    // Si el jugador está en un equipo añadirlo a una lista de jugadores que se han desconectado
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    Match match = PGM.get().getMatchManager().getMatch(player);
    MatchPlayer matchPlayer = match.getPlayer(player);
    if (matchPlayer != null) {
      Queue.removePlayer(matchPlayer, plugin.getLanguageManager());
    }
    Boolean isOnTeam = match.getPlayer(player).isParticipating();
    if (isOnTeam) {
      plugin.addDisconnectedPlayer(player.getName(), matchPlayer);
    }
    if (queue.getQueuePlayers().contains(uuid)) {
      queue.removePlayer(matchPlayer);
    }
  }
}
