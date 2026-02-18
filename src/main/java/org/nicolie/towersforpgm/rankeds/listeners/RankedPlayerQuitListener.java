package org.nicolie.towersforpgm.rankeds.listeners;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.rankeds.DisconnectManager;
import org.nicolie.towersforpgm.rankeds.Queue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class RankedPlayerQuitListener implements Listener {
  private final Queue queue;

  public RankedPlayerQuitListener(Queue queue) {
    this.queue = queue;
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    Match match = PGM.get().getMatchManager().getMatch(player);
    MatchPlayer matchPlayer = match.getPlayer(player);
    Boolean isOnTeam = matchPlayer.isParticipating();

    if (isOnTeam && Queue.isRanked()) {
      DisconnectManager.startDisconnectTimer(match, matchPlayer);
    }

    boolean isPlayerInQueue = queue.getQueuePlayers().contains(uuid);
    boolean isRankedEnabled = MatchBotConfig.isVoiceChatEnabled();

    if (isPlayerInQueue && !isRankedEnabled) {
      queue.removePlayer(matchPlayer);
    }
  }
}
