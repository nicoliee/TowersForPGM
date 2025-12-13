package org.nicolie.towersforpgm.matchbot.rankeds.listeners;

import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.DiscordManager;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.match.Match;

public class QueueJoinListener extends ListenerAdapter {
  private static final TowersForPGM plugin = TowersForPGM.getInstance();

  public static void register() {
    JDA jda = DiscordBot.getJDA();
    if (jda != null) {
      jda.addEventListener(new QueueJoinListener());
    }
  }

  @Override
  public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
    if (!MatchBotConfig.isVoiceChatEnabled()) {
      return;
    }

    String channelJoinedId = event.getChannelJoined().getId();
    String queueChannelId = MatchBotConfig.getQueueID();
    String team1ChannelId = MatchBotConfig.getTeam1ID();
    String team2ChannelId = MatchBotConfig.getTeam2ID();
    String inactiveChannelId = MatchBotConfig.getInactiveID();

    if (!channelJoinedId.equals(queueChannelId)
        && !channelJoinedId.equals(team1ChannelId)
        && !channelJoinedId.equals(team2ChannelId)
        && !channelJoinedId.equals(inactiveChannelId)) {
      return;
    }

    if (event.getChannelJoined() != null) {
      handleVoiceJoin(event);
    }

    if (event.getChannelLeft() != null) {
      handleVoiceLeave(event);
    }
  }

  private void handleVoiceJoin(GuildVoiceUpdateEvent event) {
    String discordId = event.getMember().getId();
    String channelJoinedId = event.getChannelJoined().getId();
    String queueChannelId = MatchBotConfig.getQueueID();

    DiscordManager.getDiscordPlayer(discordId)
        .thenAccept(discordPlayer -> {
          if (discordPlayer != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
              Match match = MatchManager.getMatch();
              Teams teams = plugin.getDraft().getTeams();
              String playerName =
                  Bukkit.getOfflinePlayer(discordPlayer.getPlayerUuid()).getName();

              if (match.isRunning() && teams.isPlayerInAnyTeam(playerName)) {
                if (teams.isPlayerInTeam(playerName, 1)) {
                  RankedListener.movePlayerToTeam1(discordPlayer.getPlayerUuid());
                } else {
                  RankedListener.movePlayerToTeam2(discordPlayer.getPlayerUuid());
                }
              } else {
                if (channelJoinedId.equals(queueChannelId)) {
                  Queue.getQueue().processVoiceJoin(discordPlayer.getPlayerUuid(), null);
                }
              }
            });
          }
        })
        .exceptionally(throwable -> {
          return null;
        });
  }

  private void handleVoiceLeave(GuildVoiceUpdateEvent event) {
    String channelLeftId = event.getChannelLeft().getId();
    String queueChannelId = MatchBotConfig.getQueueID();

    if (channelLeftId.equals(queueChannelId)) {
      String discordId = event.getMember().getId();

      DiscordManager.getDiscordPlayer(discordId)
          .thenAccept(discordPlayer -> {
            if (discordPlayer != null) {
              Queue.getQueue().removePlayer(discordPlayer.getPlayerUuid());
            }
          })
          .exceptionally(throwable -> {
            return null;
          });
    }
  }

  public static void reloadQueueFromVoice(Queue queue) {
    if (!MatchBotConfig.isVoiceChatEnabled()) {
      return;
    }

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
