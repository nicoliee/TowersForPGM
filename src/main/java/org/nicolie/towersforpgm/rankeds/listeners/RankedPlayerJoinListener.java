package org.nicolie.towersforpgm.rankeds.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.rankeds.DisconnectManager;
import org.nicolie.towersforpgm.rankeds.Queue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class RankedPlayerJoinListener implements Listener {
  private final TowersForPGM plugin;

  public RankedPlayerJoinListener(TowersForPGM plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    if (plugin.getDisconnectedPlayers().get(player.getName()) != null) {
      MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
      plugin.getDisconnectedPlayers().remove(player.getName());

      Match match = PGM.get().getMatchManager().getMatch(player);

      if (match != null
          && !match.isFinished()
          && Queue.isRanked()
          && !DisconnectManager.isSanctionActive(match)) {
        DisconnectManager.cancelDisconnectTimer(player.getName());
        Component message =
            Component.translatable("ranked.disconnect.reconnected", matchPlayer.getName());
        match.sendMessage(message);
      }
    }
  }
}
