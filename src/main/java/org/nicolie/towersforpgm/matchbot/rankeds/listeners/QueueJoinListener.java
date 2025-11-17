package org.nicolie.towersforpgm.matchbot.rankeds.listeners;

import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
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

    if (queueChannelId == null || queueChannelId.isEmpty()) {
      return;
    }

    if (event.getChannelJoined() != null
        && event.getChannelJoined().getId().equals(queueChannelId)) {

      String discordId = event.getMember().getId();

      DiscordManager.getDiscordPlayer(discordId)
          .thenAccept(discordPlayer -> {
            if (discordPlayer != null) {
              Queue.getQueue().processVoiceJoin(discordPlayer.getPlayerUuid(), null);
            }
          })
          .exceptionally(throwable -> {
            return null;
          });
    }
  }

  public static void reloadQueueFromVoice(Queue queue) {
    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;

    String queueChannelId = MatchBotConfig.getQueueID();
    if (queueChannelId == null || queueChannelId.isEmpty()) return;

    VoiceChannel channel = jda.getVoiceChannelById(queueChannelId);
    if (channel == null) return;

    for (Member member : channel.getMembers()) {
      DiscordManager.getDiscordPlayer(member.getId())
          .thenAccept(discordPlayer -> {
            if (discordPlayer != null) {
              queue.processVoiceJoin(discordPlayer.getPlayerUuid(), null);
            }
          })
          .exceptionally(e -> null);
    }
  }
}
