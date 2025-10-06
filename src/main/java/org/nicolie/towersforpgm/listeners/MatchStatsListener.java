package org.nicolie.towersforpgm.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.rankeds.Queue;
import tc.oc.pgm.api.match.event.MatchStatsEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.util.text.TextFormatter;

public class MatchStatsListener implements Listener {
  private final TowersForPGM plugin = TowersForPGM.getInstance();

  @EventHandler
  public void onMatchStatsEvent(MatchStatsEvent event) {
    if (Queue.isRanked() && plugin.isMatchBotEnabled()) {
      String messageEndMatch = plugin.getLanguageManager().getPluginMessage("ranked.end-match");
      org.bukkit.Bukkit.getScheduler()
          .runTaskLater(
              plugin,
              () -> {
                for (MatchPlayer viewer : event.getMatch().getPlayers()) {
                  if (viewer.getSettings().getValue(SettingKey.STATS) == SettingValue.STATS_OFF)
                    continue;
                  viewer.sendMessage(Component.text(messageEndMatch));
                  viewer.sendMessage(TextFormatter.horizontalLine(
                      NamedTextColor.WHITE, TextFormatter.MAX_CHAT_WIDTH));
                }
              },
              2L);
    }
  }
}
