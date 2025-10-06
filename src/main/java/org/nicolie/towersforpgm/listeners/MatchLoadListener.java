package org.nicolie.towersforpgm.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.MatchManager;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.commands.ForfeitCommand;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchLoadEvent;

public class MatchLoadListener implements Listener {
  private final RefillManager refillManager;
  private final PreparationListener preparationListener;
  private final Draft draft;

  public MatchLoadListener(
      RefillManager refillManager, PreparationListener preparationListener, Draft draft) {
    this.refillManager = refillManager;
    this.preparationListener = preparationListener;
    this.draft = draft;
  }

  @EventHandler
  public void onMatchLoad(MatchLoadEvent event) {
    Match match = event.getMatch();
    String map = match.getMap().getName();
    String world = match.getWorld().getName();
    TowersForPGM plugin = TowersForPGM.getInstance();
    draft.cleanLists();
    Queue.setRanked(false);
    MatchManager.setCurrentMatch(
        match); // Toma en cuenta que solo hay un mundo en el plugin como lo hace actualmente PGM
    // (10/03/2025)
    refillManager.loadChests(map, world);
    if (preparationListener.isMapInConfig(map)
        && TowersForPGM.getInstance().isPreparationEnabled()) {
      SendMessage.sendToAdmins(
          LanguageManager.langMessage("preparation.isAvailable").replace("{map}", map));
    } else if (preparationListener.isMapInConfig(map)
        && !TowersForPGM.getInstance().isPreparationEnabled()) {
      SendMessage.sendToAdmins(
          LanguageManager.langMessage("preparation.isAvailableButDisabled").replace("{map}", map));
    }
    plugin.getDisconnectedPlayers().clear();
    ForfeitCommand.forfeitedPlayers.clear();
    if (plugin.isStatsCancel()) {
      plugin.setStatsCancel(false);
    }
    if (!ConfigManager.getRankedMaps().contains(map) && Queue.getQueueSize() > 0) {
      Queue.clearQueue();
    }
    if (ConfigManager.getTempTable() != null) {
      ConfigManager.removeTempTable();
    }
  }
}
