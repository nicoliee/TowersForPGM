package org.nicolie.towersforpgm.listeners;

import java.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchStatsEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.stats.StatsMatchModule;

public class MatchStatsListener implements Listener {
  private final TowersForPGM plugin = TowersForPGM.getInstance();
  private final LanguageManager languageManager;

  public MatchStatsListener(LanguageManager languageManager) {
    this.languageManager = languageManager;
  }

  @EventHandler
  public void onMatchStatsEvent(MatchStatsEvent event) {
    Match match = event.getMatch();
    Collection<Gamemode> gamemodes = match.getMap().getGamemodes();
    if (!gamemodes.contains(Gamemode.SCOREBOX)) {
      return;
    }
    StatsMatchModule statsModule = match.getModule(StatsMatchModule.class);
    List<MatchPlayer> allPlayers = new ArrayList<>(match.getParticipants());
    allPlayers.addAll(plugin.getDisconnectedPlayers().values());
    if (statsModule != null) {
      double bestRatio = -1;
      String mvpName = null;
      for (MatchPlayer player : allPlayers) {
        PlayerStats stats = statsModule.getPlayerStat(player);
        double damageDone = stats != null ? stats.getDamageDone() : 0;
        double damageTaken = stats != null ? stats.getDamageDone() : 0;
        double bowDamage = stats != null ? stats.getBowDamage() : 0;
        double bowDamageTaken = stats != null ? stats.getBowDamageTaken() : 0;

        double totalDamageDone = damageDone + bowDamage;
        double totalDamageTaken = damageTaken + bowDamageTaken;

        double ratio = totalDamageTaken > 0
            ? totalDamageDone / totalDamageTaken
            : (totalDamageDone > 0 ? totalDamageDone : 0);

        if (ratio > bestRatio) {
          bestRatio = ratio;
          if (player.getParty() == null) {
            mvpName = "§3" + player.getNameLegacy();
          } else {
            mvpName = player.getPrefixedName();
          }
        }
      }
      if (mvpName != null) {
        Component mvpmessage = Component.text(
            languageManager.getPluginMessage("stats.mvp").replace("{player}", mvpName));
        for (MatchPlayer player : match.getParticipants()) {
          player.sendMessage(mvpmessage);
        }
      }
    }
  }
}
