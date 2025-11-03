package org.nicolie.towersforpgm.matchbot.rankeds;

import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.nicolie.towersforpgm.database.DiscordManager;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.rankeds.Queue;

public class QueueLeaveListener extends ListenerAdapter {

  public static void register() {
    JDA jda = DiscordBot.getJDA();
    if (jda != null) {
      jda.addEventListener(new QueueLeaveListener());
    }
  }

  @Override
  public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
    String queueChannelId = MatchBotConfig.getQueueID();

    // Verificar si el queue ID está configurado
    if (queueChannelId == null || queueChannelId.isEmpty()) {
      return;
    }

    // Verificar si el usuario salió del canal de queue
    if (event.getChannelLeft() != null && event.getChannelLeft().getId().equals(queueChannelId)) {

      String discordId = event.getMember().getId();

      // Obtener el UUID de Minecraft asociado
      DiscordManager.getMinecraftUuid(discordId)
          .thenAccept(minecraftUuid -> {
            if (minecraftUuid != null) {
              Queue.getQueue().removePlayer(minecraftUuid, null);
            }
          })
          .exceptionally(throwable -> {
            return null;
          });
    }
  }
}
