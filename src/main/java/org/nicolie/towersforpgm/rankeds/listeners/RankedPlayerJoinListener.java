package org.nicolie.towersforpgm.rankeds.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.rankeds.DisconnectManager;
import org.nicolie.towersforpgm.rankeds.Queue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;

public class RankedPlayerJoinListener implements Listener {
  private final TowersForPGM plugin;

  public RankedPlayerJoinListener(TowersForPGM plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    if (plugin.getDisconnectedPlayers().get(player.getName()) != null) {
      plugin.getDisconnectedPlayers().remove(player.getName());

      Match match = PGM.get().getMatchManager().getMatch(player);

      if (match != null
          && !match.isFinished()
          && Queue.isRanked()
          && !DisconnectManager.isSanctionActive(match)) {
        DisconnectManager.cancelDisconnectTimer(player.getName());

        String msg = org.nicolie.towersforpgm.utils.LanguageManager.message("ranked.prefix")
            + org.nicolie.towersforpgm.utils.LanguageManager.message(
                    "ranked.disconnect.reconnected")
                .replace("{player}", player.getName());
        match.sendMessage(net.kyori.adventure.text.Component.text(msg));
      }
    }
  }
}
