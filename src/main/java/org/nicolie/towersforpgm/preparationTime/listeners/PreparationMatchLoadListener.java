package org.nicolie.towersforpgm.preparationTime.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.match.event.MatchLoadEvent;

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
      SendMessage.sendToDevelopers(
          Component.translatable("preparation.isAvailable", Component.text(map))
              .color(NamedTextColor.GREEN));
    } else if (preparationListener.isMapInConfig(map)
        && !plugin.config().preparationTime().isPreparationEnabled()) {
      SendMessage.sendToDevelopers(
          Component.translatable("preparation.isAvailableButDisabled", Component.text(map))
              .color(NamedTextColor.YELLOW));
    }
  }
}
