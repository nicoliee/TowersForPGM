package org.nicolie.towersforpgm.matchbot.rankeds.listeners;

import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.nicolie.towersforpgm.database.DiscordManager;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.rankeds.Queue;

public class QueueJoinListener extends ListenerAdapter {

  public static void register() {
    JDA jda = DiscordBot.getJDA();
    if (jda != null) {
      jda.addEventListener(new QueueJoinListener());
    }
  }

  @Override
  public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
    String queueChannelId = MatchBotConfig.getQueueID();

    // Verificar si el queue ID está configurado
    if (queueChannelId == null || queueChannelId.isEmpty()) {
      return;
    }

    // Verificar si el usuario se unió al canal de queue
    if (event.getChannelJoined() != null
        && event.getChannelJoined().getId().equals(queueChannelId)) {

      String discordId = event.getMember().getId();

      // Obtener el UUID de Minecraft asociado
      DiscordManager.getDiscordPlayer(discordId)
          .thenAccept(discordPlayer -> {
            if (discordPlayer != null) {
              Queue.getQueue().addPlayer(discordPlayer.getPlayerUuid(), null);
            }
          })
          .exceptionally(throwable -> {
            return null;
          });
    }
  }
}
