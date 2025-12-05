package org.nicolie.towersforpgm.listeners;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.rankeds.DisconnectManager;
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
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    Match match = PGM.get().getMatchManager().getMatch(player);
    MatchPlayer matchPlayer = match.getPlayer(player);
    Boolean isOnTeam = match.getPlayer(player).isParticipating();
    if (isOnTeam) {
      plugin.addDisconnectedPlayer(player.getName(), matchPlayer);
      if (Queue.isRanked()) {
        DisconnectManager.startDisconnectTimer(match, matchPlayer);
      }
    }

    boolean isPlayerInQueue = queue.getQueuePlayers().contains(uuid);
    boolean isRankedEnabled = MatchBotConfig.isVoiceChatEnabled();

    if (isPlayerInQueue && !isRankedEnabled) {
      queue.removePlayer(matchPlayer);
    }
  }
}
