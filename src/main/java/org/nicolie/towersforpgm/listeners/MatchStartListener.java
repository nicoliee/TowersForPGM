package org.nicolie.towersforpgm.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Utilities;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.embeds.RankedStart;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.utils.ConfigManager;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.player.MatchPlayer;

public class MatchStartListener implements Listener {
  private final PreparationListener preparationListener;
  private final RefillManager refillManager;
  private final TowersForPGM plugin;
  private final Captains captains;

  public MatchStartListener(
      PreparationListener preparationListener, RefillManager refillManager, Captains captains) {
    this.captains = captains;
    this.preparationListener = preparationListener;
    this.refillManager = refillManager;
    this.plugin = TowersForPGM.getInstance();
  }

  @EventHandler
  public void onMatchStart(MatchStartEvent event) {
    String worldName = event.getMatch().getWorld().getName();
    if (captains.isReadyActive()) {
      Utilities.cancelReadyReminder();
      captains.resetReady();
    }
    refillManager.startRefillTask(worldName);
    if (plugin.isPreparationEnabled()) {
      preparationListener.startProtection(null, event.getMatch());
    }
    sendRankedStartEmbed(event);
  }

  private void sendRankedStartEmbed(MatchStartEvent event) {
    boolean ranked = Queue.isRanked();
    boolean matchbot = plugin.isMatchBotEnabled();
    if (matchbot && ranked) {
      Collection<MatchPlayer> players = event.getMatch().getPlayers();
      List<String> usernames = new ArrayList<>();
      for (MatchPlayer player : players) {
        usernames.add(player.getNameLegacy());
      }
      StatsManager.getEloForUsernames(
              ConfigManager.getActiveTable(event.getMatch().getMap().getName()), usernames)
          .thenAccept(eloChanges -> {
            EmbedBuilder embed = RankedStart.create(event.getMatch(), eloChanges);
            DiscordBot.setEmbedThumbnail(event.getMatch().getMap(), embed);
            DiscordBot.sendMatchEmbed(
                embed, event.getMatch(), MatchBotConfig.getDiscordChannel(), null);
          })
          .exceptionally(throwable -> {
            plugin
                .getLogger()
                .severe("Error al obtener ELO para match start: " + throwable.getMessage());
            return null;
          });
    }
  }
}
