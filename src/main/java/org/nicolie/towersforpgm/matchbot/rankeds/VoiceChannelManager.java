package org.nicolie.towersforpgm.matchbot.rankeds;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import me.tbg.match.bot.configs.BotConfig;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;

public class VoiceChannelManager {
  private static final String RANKED_PREFIX = "[Ranked] ";
  private static VoiceChannel team1Channel = null;
  private static VoiceChannel team2Channel = null;

  public static void cleanupRankedChannelsOnStartup() {
    JDA jda = DiscordBot.getJDA();
    if (jda == null || !MatchBotConfig.isVoiceChatEnabled()) return;

    Guild guild = getGuild(jda);
    if (guild == null) return;

    Category category = getVoiceCategory(guild);
    if (category == null) return;

    VoiceChannel inactiveChannel = jda.getVoiceChannelById(MatchBotConfig.getInactiveID());

    List<VoiceChannel> channelsToDelete = new ArrayList<>();
    for (VoiceChannel channel : category.getVoiceChannels()) {
      if (channel.getName().startsWith(RANKED_PREFIX)) {
        channelsToDelete.add(channel);
      }
    }

    // Mover usuarios a AFK y eliminar canales
    for (VoiceChannel channel : channelsToDelete) {
      if (inactiveChannel != null) {
        for (Member member : new ArrayList<>(channel.getMembers())) {
          guild.moveVoiceMember(member, inactiveChannel).queue(success -> {}, error -> {});
        }
      }
      final VoiceChannel channelToDelete = channel;
      new java.util.Timer()
          .schedule(
              new java.util.TimerTask() {
                @Override
                public void run() {
                  channelToDelete.delete().queue(success -> {}, error -> {});
                }
              },
              2000);
    }
  }

  public static CompletableFuture<Void> createTeamChannels(
      String team1Name, String team2Name, int team1Size, int team2Size) {
    JDA jda = DiscordBot.getJDA();
    if (jda == null || !MatchBotConfig.isVoiceChatEnabled()) {
      return CompletableFuture.completedFuture(null);
    }

    Guild guild = getGuild(jda);
    if (guild == null) return CompletableFuture.completedFuture(null);

    Category category = getVoiceCategory(guild);
    if (category == null) return CompletableFuture.completedFuture(null);

    CompletableFuture<VoiceChannel> team1Future = new CompletableFuture<>();
    CompletableFuture<VoiceChannel> team2Future = new CompletableFuture<>();

    boolean privateChannels = MatchBotConfig.isPrivateChannels();

    // Crear canal del equipo 1
    category
        .createVoiceChannel(RANKED_PREFIX + team1Name)
        .queue(
            channel -> {
              if (privateChannels && team1Size > 0) {
                channel
                    .getManager()
                    .setUserLimit(team1Size)
                    .queue(
                        success -> {
                          team1Channel = channel;
                          team1Future.complete(channel);
                        },
                        error -> {
                          team1Channel = channel;
                          team1Future.complete(channel);
                        });
              } else {
                team1Channel = channel;
                team1Future.complete(channel);
              }
            },
            error -> team1Future.completeExceptionally(error));

    // Crear canal del equipo 2
    category
        .createVoiceChannel(RANKED_PREFIX + team2Name)
        .queue(
            channel -> {
              if (privateChannels && team2Size > 0) {
                channel
                    .getManager()
                    .setUserLimit(team2Size)
                    .queue(
                        success -> {
                          team2Channel = channel;
                          team2Future.complete(channel);
                        },
                        error -> {
                          team2Channel = channel;
                          team2Future.complete(channel);
                        });
              } else {
                team2Channel = channel;
                team2Future.complete(channel);
              }
            },
            error -> team2Future.completeExceptionally(error));

    return CompletableFuture.allOf(team1Future, team2Future);
  }

  public static void deleteTeamChannels() {
    if (!MatchBotConfig.isVoiceChatEnabled()) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;

    Guild guild = getGuild(jda);
    if (guild == null) return;

    VoiceChannel inactiveChannel = jda.getVoiceChannelById(MatchBotConfig.getInactiveID());
    if (inactiveChannel == null) return;

    if (team1Channel != null) {
      for (Member member : new ArrayList<>(team1Channel.getMembers())) {
        guild.moveVoiceMember(member, inactiveChannel).queue(success -> {}, error -> {});
      }
      final VoiceChannel channelToDelete = team1Channel;
      new java.util.Timer()
          .schedule(
              new java.util.TimerTask() {
                @Override
                public void run() {
                  channelToDelete.delete().queue(success -> {}, error -> {});
                }
              },
              2000);
      team1Channel = null;
    }

    if (team2Channel != null) {
      for (Member member : new ArrayList<>(team2Channel.getMembers())) {
        guild.moveVoiceMember(member, inactiveChannel).queue(success -> {}, error -> {});
      }
      final VoiceChannel channelToDelete = team2Channel;
      new java.util.Timer()
          .schedule(
              new java.util.TimerTask() {
                @Override
                public void run() {
                  channelToDelete.delete().queue(success -> {}, error -> {});
                }
              },
              2000);
      team2Channel = null;
    }
  }

  public static VoiceChannel getTeam1Channel() {
    return team1Channel;
  }

  public static VoiceChannel getTeam2Channel() {
    return team2Channel;
  }

  private static Category getVoiceCategory(Guild guild) {
    String queueId = MatchBotConfig.getQueueID();
    VoiceChannel queueChannel = guild.getVoiceChannelById(queueId);

    if (queueChannel != null && queueChannel.getParentCategory() != null) {
      return queueChannel.getParentCategory();
    }

    String inactiveId = MatchBotConfig.getInactiveID();
    VoiceChannel inactiveChannel = guild.getVoiceChannelById(inactiveId);

    if (inactiveChannel != null && inactiveChannel.getParentCategory() != null) {
      return inactiveChannel.getParentCategory();
    }

    return null;
  }

  private static Guild getGuild(JDA jda) {
    String guildId = BotConfig.getServerId();
    if (guildId == null || guildId.isEmpty()) return null;
    return jda.getGuildById(guildId);
  }
}
