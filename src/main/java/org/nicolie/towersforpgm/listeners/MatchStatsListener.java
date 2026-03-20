package org.nicolie.towersforpgm.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import tc.oc.pgm.api.match.event.MatchStatsEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;

public class MatchStatsListener implements Listener {

  @EventHandler
  public void onMatchStats(MatchStatsEvent event) {
    String baseURL = TowersForPGM.getInstance().config().database().getMatchLink();
    String matchURL = TowersForPGM.getInstance().config().database().getStatsLink();
    if (matchURL != null && !matchURL.isEmpty() && baseURL != null && !baseURL.isEmpty()) {
      String urlCopy = matchURL;
      String fullURL = baseURL + urlCopy.replace("/", "");
      org.bukkit.Bukkit.getScheduler()
          .runTaskLater(
              TowersForPGM.getInstance(),
              () -> {
                for (MatchPlayer player : event.getMatch().getPlayers()) {
                  if (player.getSettings().getValue(SettingKey.STATS) == SettingValue.STATS_OFF)
                    continue;
                  player.sendMessage(Component.text("Stats: ")
                      .color(net.kyori.adventure.text.format.NamedTextColor.GOLD)
                      .append(Component.text(urlCopy)
                          .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)
                          .decorate(net.kyori.adventure.text.format.TextDecoration.UNDERLINED)
                          .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl(fullURL))));
                }
              },
              1L);
      TowersForPGM.getInstance().config().database().setStatsLink(null);
    }
  }
}
