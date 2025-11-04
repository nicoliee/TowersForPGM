package org.nicolie.towersforpgm.matchbot.listeners;

import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.DiscordManager;
import org.nicolie.towersforpgm.draft.events.DraftEndEvent;
import org.nicolie.towersforpgm.draft.events.DraftStartEvent;
import org.nicolie.towersforpgm.draft.events.MatchmakingEndEvent;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.ConfigManager;

public class Ranked implements Listener {

  private static boolean RANKED = MatchBotConfig.isRankedEnabled();
  private static String CHANNEL1 = MatchBotConfig.getTeam1ID();
  private static String CHANNEL2 = MatchBotConfig.getTeam2ID();

  @EventHandler
  public void onDraftStart(DraftStartEvent event) {
    if (!shouldProcessEvent(event.getMatch().getMap().getName())) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null || CHANNEL1 == null || CHANNEL1.isEmpty()) return;

    VoiceChannel channel1 = jda.getVoiceChannelById(CHANNEL1);
    if (channel1 == null) return;

    // Mover a todos los jugadores al CHANNEL1
    event.getPlayers().forEach(matchPlayer -> {
      Player player = matchPlayer.getBukkit();
      if (player != null) {
        movePlayerToChannel(player, channel1);
      }
    });
  }

  @EventHandler
  public void onDraftEnd(DraftEndEvent event) {
    if (!shouldProcessEvent(event.getMatch().getMap().getName())) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null || CHANNEL2 == null || CHANNEL2.isEmpty()) return;

    VoiceChannel channel2 = jda.getVoiceChannelById(CHANNEL2);
    if (channel2 == null) return;

    // Mover solo los jugadores del team 2 al CHANNEL2
    event.getTeam2().forEach(playerName -> {
      Player player = Bukkit.getPlayer(playerName);
      if (player != null) {
        movePlayerToChannel(player, channel2);
      }
    });
  }

  @EventHandler
  public void onMatchmakingEnd(MatchmakingEndEvent event) {
    if (!shouldProcessEvent(event.getMatch().getMap().getName())) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null
        || CHANNEL1 == null
        || CHANNEL1.isEmpty()
        || CHANNEL2 == null
        || CHANNEL2.isEmpty()) return;

    VoiceChannel channel1 = jda.getVoiceChannelById(CHANNEL1);
    VoiceChannel channel2 = jda.getVoiceChannelById(CHANNEL2);
    if (channel1 == null || channel2 == null) return;

    // Mover team 1 al CHANNEL1
    event.getTeam1().forEach(playerName -> {
      Player player = Bukkit.getPlayer(playerName);
      if (player != null) {
        movePlayerToChannel(player, channel1);
      }
    });

    // Mover team 2 al CHANNEL2
    event.getTeam2().forEach(playerName -> {
      Player player = Bukkit.getPlayer(playerName);
      if (player != null) {
        movePlayerToChannel(player, channel2);
      }
    });
  }

  private boolean shouldProcessEvent(String mapName) {
    if (!RANKED || !MatchBotConfig.isRankedEnabled()) return false;

    String table = ConfigManager.getActiveTable(mapName);
    Boolean ranked = Queue.isRanked();
    Boolean rankedTable = ConfigManager.isRankedTable(table);

    return ranked != null && ranked && rankedTable != null && rankedTable;
  }

  private void movePlayerToChannel(Player player, VoiceChannel targetChannel) {
    DiscordManager.getDiscordPlayer(player.getUniqueId())
        .thenAccept(discordPlayer -> {
          if (discordPlayer != null) {
            try {
              Member member = targetChannel.getGuild().getMemberById(discordPlayer.getDiscordId());
              if (member != null
                  && member.getVoiceState() != null
                  && member.getVoiceState().inAudioChannel()) {
                targetChannel
                    .getGuild()
                    .moveVoiceMember(member, targetChannel)
                    .queue(success -> {}, error -> TowersForPGM.getInstance()
                        .getLogger()
                        .warning("No se pudo mover a " + player.getName() + " al canal de voz: "
                            + error.getMessage()));
              }
            } catch (Exception e) {
              TowersForPGM.getInstance()
                  .getLogger()
                  .warning("Error al intentar mover a " + player.getName() + ": " + e.getMessage());
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
