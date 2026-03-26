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
import org.nicolie.towersforpgm.draft.team.Teams;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.rankeds.NicknameManager;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.session.MatchSession;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.match.Match;

public class QueueJoinListener extends ListenerAdapter {

  private static final TowersForPGM plugin = TowersForPGM.getInstance();

  public static void register() {
    JDA jda = DiscordBot.getJDA();
    if (jda != null) jda.addEventListener(new QueueJoinListener());
  }

  @Override
  public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
    if (!MatchBotConfig.isVoiceChatEnabled()) return;
    if (event.getChannelJoined() != null) handleVoiceJoin(event);
    if (event.getChannelLeft() != null) handleVoiceLeave(event);
  }

  // TODO: hacerlo sin bukkit para que no dependa de si el servidor está activo.
  private void handleVoiceJoin(GuildVoiceUpdateEvent event) {
    String channelJoinedId = event.getChannelJoined().getId();
    String queueChannelId = MatchBotConfig.getQueueID();
    String inactiveChannelId = MatchBotConfig.getInactiveID();

    if (!channelJoinedId.equals(queueChannelId) && !channelJoinedId.equals(inactiveChannelId))
      return;

    String discordId = event.getMember().getId();

    DiscordManager.getDiscordPlayer(discordId)
        .thenAccept(discordPlayer -> {
          if (discordPlayer == null) return;

          NicknameManager.updateNicknameToMinecraftUsername(discordPlayer.getPlayerUuid());

          Bukkit.getScheduler().runTask(plugin, () -> {
            Match match = MatchManager.getMatch();
            if (match == null) return;
            MatchSession session = MatchSessionRegistry.get(match);
            Teams teams = session != null ? session.teams() : null;

            String playerName =
                Bukkit.getOfflinePlayer(discordPlayer.getPlayerUuid()).getName();

            if (playerName != null
                && match.isRunning()
                && teams != null
                && teams.isPlayerInAnyTeam(playerName)
                && Queue.isRanked()) {
              if (teams.isPlayerInTeam(playerName, 1)) {
                RankedListener.movePlayerToTeam1(discordPlayer.getPlayerUuid());
              } else {
                RankedListener.movePlayerToTeam2(discordPlayer.getPlayerUuid());
              }
            } else if (channelJoinedId.equals(queueChannelId)) {
              Queue.getQueue().addPlayer(discordPlayer.getPlayerUuid(), match);
            }
          });
        })
        .exceptionally(throwable -> null);
  }

  private void handleVoiceLeave(GuildVoiceUpdateEvent event) {
    if (!event.getChannelLeft().getId().equals(MatchBotConfig.getQueueID())) return;

    DiscordManager.getDiscordPlayer(event.getMember().getId())
        .thenAccept(discordPlayer -> {
          if (discordPlayer != null) {
            Queue.getQueue().removePlayer(discordPlayer.getPlayerUuid());
          }
        })
        .exceptionally(throwable -> null);
  }

  public static void reloadQueueFromVoice(Match match) {
    if (!MatchBotConfig.isVoiceChatEnabled()) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;

    String queueChannelId = MatchBotConfig.getQueueID();
    if (queueChannelId == null || queueChannelId.isEmpty()) return;

    VoiceChannel channel = jda.getVoiceChannelById(queueChannelId);
    if (channel == null) return;

    Queue queue = Queue.getQueue();

    for (Member member : channel.getMembers()) {
      DiscordManager.getDiscordPlayer(member.getId())
          .thenAccept(discordPlayer -> {
            if (discordPlayer != null) {
              queue.addPlayer(discordPlayer.getPlayerUuid(), match);
            }
          })
          .exceptionally(e -> null);
    }
  }
}
