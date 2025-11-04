package org.nicolie.towersforpgm.matchbot.listeners;

import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.DiscordManager;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.ConfigManager;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;

public class MatchFinishListener implements Listener {

  @EventHandler
  public void onMatchFinish(MatchFinishEvent event) {
    if (!shouldProcessEvent(event.getMatch().getMap().getName())) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;

    // Desconectar a todos los jugadores de canales de voz
    event.getMatch().getPlayers().forEach(this::disconnectPlayerFromVoice);
  }

  private boolean shouldProcessEvent(String mapName) {
    boolean ranked = MatchBotConfig.isRankedEnabled();
    if (!ranked) return false;

    String table = ConfigManager.getActiveTable(mapName);
    Boolean queueRanked = Queue.isRanked();
    Boolean rankedTable = ConfigManager.isRankedTable(table);

    return queueRanked != null && queueRanked && rankedTable != null && rankedTable;
  }

  private void disconnectPlayerFromVoice(MatchPlayer matchPlayer) {
    Player player = matchPlayer.getBukkit();
    if (player == null) return;

    DiscordManager.getDiscordPlayer(player.getUniqueId())
        .thenAccept(discordPlayer -> {
          if (discordPlayer != null) {
            try {
              JDA jda = DiscordBot.getJDA();
              if (jda == null) return;

              // Buscar el miembro en todos los servidores donde esté el bot
              jda.getGuilds().forEach(guild -> {
                Member member = guild.getMemberById(discordPlayer.getDiscordId());
                if (member != null
                    && member.getVoiceState() != null
                    && member.getVoiceState().inAudioChannel()) {
                  guild
                      .kickVoiceMember(member)
                      .queue(success -> {}, error -> TowersForPGM.getInstance()
                          .getLogger()
                          .warning("No se pudo desconectar a " + player.getName()
                              + " del canal de voz: " + error.getMessage()));
                }
              });
            } catch (Exception e) {
              TowersForPGM.getInstance()
                  .getLogger()
                  .warning("Error al intentar desconectar a " + player.getName() + ": "
                      + e.getMessage());
            }
          }
        })
        .exceptionally(throwable -> {
          TowersForPGM.getInstance()
              .getLogger()
              .warning("Error obteniendo datos Discord para " + player.getName() + ": "
                  + throwable.getMessage());
          return null;
        });
  }
}
