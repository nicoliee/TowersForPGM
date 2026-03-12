package org.nicolie.towersforpgm.preparationTime.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;

public class PreparationMatchLoadListener implements Listener {
  private final TowersForPGM plugin = TowersForPGM.getInstance();
  private final PreparationListener preparationListener;

  public PreparationMatchLoadListener(PreparationListener preparationListener) {
    this.preparationListener = preparationListener;
  }

  @EventHandler
  public void onMatchLoad(MatchLoadEvent event) {
    String map = event.getMatch().getMap().getName();

    if (preparationListener.isMapInConfig(map)
        && plugin.config().preparationTime().isPreparationEnabled()) {
      sendMessageToAdmins(event.getMatch(), "preparation.isAvailable", map);
    } else if (preparationListener.isMapInConfig(map)
        && !plugin.config().preparationTime().isPreparationEnabled()) {
      sendMessageToAdmins(event.getMatch(), "preparation.isAvailableButDisabled", map);
    }
  }

  private void sendMessageToAdmins(Match match, String key, String map) {
    for (MatchPlayer player : match.getPlayers()) {
      if (player.getBukkit().hasPermission("towers.admin")) {
        player.sendMessage(Component.translatable(key)
            .append(Component.space())
            .append(Component.text(map).color(NamedTextColor.GREEN))
            .color(NamedTextColor.YELLOW));
      }
    }
  }
}
