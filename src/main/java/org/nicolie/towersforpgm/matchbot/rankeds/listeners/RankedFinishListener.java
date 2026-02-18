package org.nicolie.towersforpgm.matchbot.rankeds.listeners;

import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.JDA;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.rankeds.Queue;
import tc.oc.pgm.api.match.event.MatchFinishEvent;

public class RankedFinishListener implements Listener {
  private static final TowersForPGM plugin = TowersForPGM.getInstance();

  @EventHandler
  public void onMatchFinish(MatchFinishEvent event) {
    if (!shouldProcessEvent()) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;

    // Dar tiempo para que los jugadores hagan click al item antes de mover a AFK y borrar canales
    // El item ya se da automáticamente por ObserverKitApplyEvent
    org.bukkit.Bukkit.getScheduler()
        .runTaskLaterAsynchronously(
            plugin,
            () -> {
              // Borrar los canales dinámicos (esto también mueve a los jugadores a AFK)
              org.nicolie.towersforpgm.matchbot.rankeds.VoiceChannelManager.deleteTeamChannels();
            },
            100L);
  }

  private boolean shouldProcessEvent() {
    if (!MatchBotConfig.isVoiceChatEnabled()) return false;

    Boolean ranked = Queue.isRanked();
    boolean isRankedTable = plugin.config().databaseTables().currentTableIsRanked();

    return (ranked && isRankedTable);
  }
}
