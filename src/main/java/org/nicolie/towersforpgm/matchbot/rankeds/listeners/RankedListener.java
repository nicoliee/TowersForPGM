package org.nicolie.towersforpgm.matchbot.rankeds.listeners;

import java.util.UUID;
import me.tbg.match.bot.configs.BotConfig;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
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

public class RankedListener implements Listener {
  private static final TowersForPGM plugin = TowersForPGM.getInstance();

  private static final String QUEUE_ID = MatchBotConfig.getQueueID();
  private static final String CHANNEL1_ID = MatchBotConfig.getTeam1ID();
  private static final String CHANNEL2_ID = MatchBotConfig.getTeam2ID();
  private static final String INACTIVE_ID = MatchBotConfig.getInactiveID();
  private static final Boolean RANKED_ENABLED = MatchBotConfig.isVoiceChatEnabled();

  private static Guild getBotGuild(JDA jda) {
    if (jda == null) return null;
    String guildId = BotConfig.getServerId();
    if (guildId == null || guildId.isEmpty()) return null;
    return jda.getGuildById(guildId);
  }

  private void movePlayersToChannel(java.util.List<UUID> uuids, VoiceChannel targetChannel) {
    if (!RANKED_ENABLED || uuids == null || uuids.isEmpty() || targetChannel == null) return;

    JDA jda = DiscordBot.getJDA();
    Guild guild = getBotGuild(jda);
    if (guild == null) return;

    var discordPlayerFutures =
        uuids.stream().map(DiscordManager::getDiscordPlayer).toList();

    java.util.concurrent.CompletableFuture.allOf(
            discordPlayerFutures.toArray(new java.util.concurrent.CompletableFuture[0]))
        .thenRun(() -> {
          var moveActions =
              new java.util.ArrayList<net.dv8tion.jda.api.requests.RestAction<Void>>();

          for (var future : discordPlayerFutures) {
            try {
              var discordPlayer = future.getNow(null);
              if (discordPlayer == null) continue;

              Member member = guild.getMemberById(discordPlayer.getDiscordId());
              if (member == null || member.getVoiceState() == null) continue;

              var currentChannel = member.getVoiceState().getChannel();
              if (currentChannel == null) continue;

              moveActions.add(guild.moveVoiceMember(member, targetChannel));
            } catch (Exception e) {
            }
          }

          if (!moveActions.isEmpty()) {
            net.dv8tion.jda.api.requests.RestAction.allOf(moveActions).queue();
          }
        });
  }

  public static void movePlayerToInactive(UUID minecraftUUID) {
    if (!RANKED_ENABLED) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;

    Guild guild = getBotGuild(jda);
    if (guild == null) return;

    if (INACTIVE_ID == null || INACTIVE_ID.isEmpty()) return;

    VoiceChannel inactiveChannel = jda.getVoiceChannelById(INACTIVE_ID);
    if (inactiveChannel == null) return;

    DiscordManager.getDiscordPlayer(minecraftUUID)
        .thenAccept(discordPlayer -> {
          if (discordPlayer == null) return;

          String discordId = discordPlayer.getDiscordId();
          Member member = guild.getMemberById(discordId);
          if (member == null || member.getVoiceState() == null) return;

          var currentChannel = member.getVoiceState().getChannel();
          if (currentChannel == null) return;

          guild.moveVoiceMember(member, inactiveChannel).queue();
        })
        .exceptionally(ex -> null);
  }

  public static void movePlayerToQueue(UUID minecraftUUID) {
    if (!RANKED_ENABLED) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;

    Guild guild = getBotGuild(jda);
    if (guild == null) return;

    if (QUEUE_ID == null || QUEUE_ID.isEmpty()) return;

    VoiceChannel queueChannel = jda.getVoiceChannelById(QUEUE_ID);
    if (queueChannel == null) return;

    DiscordManager.getDiscordPlayer(minecraftUUID)
        .thenAccept(discordPlayer -> {
          if (discordPlayer == null) return;

          String discordId = discordPlayer.getDiscordId();
          Member member = guild.getMemberById(discordId);
          if (member == null || member.getVoiceState() == null) return;

          var currentChannel = member.getVoiceState().getChannel();
          if (currentChannel == null) return;

          guild.moveVoiceMember(member, queueChannel).queue();
        })
        .exceptionally(ex -> null);
  }

  public static void movePlayerToTeam1(UUID minecraftUUID) {
    if (!RANKED_ENABLED) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;

    Guild guild = getBotGuild(jda);
    if (guild == null) return;

    if (CHANNEL1_ID == null || CHANNEL1_ID.isEmpty()) return;

    VoiceChannel team1Channel = jda.getVoiceChannelById(CHANNEL1_ID);
    if (team1Channel == null) return;

    DiscordManager.getDiscordPlayer(minecraftUUID)
        .thenAccept(discordPlayer -> {
          if (discordPlayer == null) return;

          String discordId = discordPlayer.getDiscordId();
          Member member = guild.getMemberById(discordId);
          if (member == null || member.getVoiceState() == null) return;

          var currentChannel = member.getVoiceState().getChannel();
          if (currentChannel == null) return;

          guild.moveVoiceMember(member, team1Channel).queue();
        })
        .exceptionally(ex -> null);
  }

  public static void movePlayerToTeam2(UUID minecraftUUID) {
    if (!RANKED_ENABLED) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;

    Guild guild = getBotGuild(jda);
    if (guild == null) return;

    if (CHANNEL2_ID == null || CHANNEL2_ID.isEmpty()) return;

    VoiceChannel team2Channel = jda.getVoiceChannelById(CHANNEL2_ID);
    if (team2Channel == null) return;

    DiscordManager.getDiscordPlayer(minecraftUUID)
        .thenAccept(discordPlayer -> {
          if (discordPlayer == null) return;

          String discordId = discordPlayer.getDiscordId();
          Member member = guild.getMemberById(discordId);
          if (member == null || member.getVoiceState() == null) return;

          var currentChannel = member.getVoiceState().getChannel();
          if (currentChannel == null) return;

          guild.moveVoiceMember(member, team2Channel).queue();
        })
        .exceptionally(ex -> null);
  }

  @EventHandler
  public void onDraftStart(DraftStartEvent event) {
    if (!shouldProcessEvent(event.getMatch().getMap().getName())) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null || CHANNEL1_ID == null || CHANNEL1_ID.isEmpty()) return;

    VoiceChannel channel1 = jda.getVoiceChannelById(CHANNEL1_ID);
    if (channel1 == null) return;

    var uuidsToMove = new java.util.ArrayList<UUID>();
    uuidsToMove.add(event.getCaptain1());
    uuidsToMove.add(event.getCaptain2());
    event.getPlayers().forEach(matchPlayer -> uuidsToMove.add(matchPlayer.getId()));

    movePlayersToChannel(uuidsToMove, channel1);
  }

  @EventHandler
  public void onDraftEnd(DraftEndEvent event) {
    if (!shouldProcessEvent(event.getMatch().getMap().getName())) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null || CHANNEL2_ID == null || CHANNEL2_ID.isEmpty()) return;

    VoiceChannel channel2 = jda.getVoiceChannelById(CHANNEL2_ID);
    if (channel2 == null) return;

    // Recolectar todos los UUIDs del equipo 2
    var uuidsToMove = new java.util.ArrayList<UUID>();
    event.getTeam2().forEach(playerName -> {
      Player player = Bukkit.getPlayer(playerName);
      if (player != null) uuidsToMove.add(player.getUniqueId());
    });

    // Mover todos los jugadores en lote
    movePlayersToChannel(uuidsToMove, channel2);
  }

  @EventHandler
  public void onMatchmakingEnd(MatchmakingEndEvent event) {
    if (!shouldProcessEvent(event.getMatch().getMap().getName())) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;

    VoiceChannel channel1 = jda.getVoiceChannelById(CHANNEL1_ID);
    VoiceChannel channel2 = jda.getVoiceChannelById(CHANNEL2_ID);
    if (channel1 == null || channel2 == null) return;

    var team1UUIDs = new java.util.ArrayList<UUID>();
    event.getTeam1().forEach(playerName -> {
      Player player = Bukkit.getPlayer(playerName);
      if (player != null) team1UUIDs.add(player.getUniqueId());
    });

    var team2UUIDs = new java.util.ArrayList<UUID>();
    event.getTeam2().forEach(playerName -> {
      Player player = Bukkit.getPlayer(playerName);
      if (player != null) team2UUIDs.add(player.getUniqueId());
    });

    movePlayersToChannel(team1UUIDs, channel1);
    movePlayersToChannel(team2UUIDs, channel2);
  }

  private boolean shouldProcessEvent(String mapName) {
    if (!RANKED_ENABLED) return false;

    Boolean ranked = Queue.isRanked();
    boolean isRankedTable = plugin.config().databaseTables().currentTableIsRanked();

    return (ranked && isRankedTable);
  }
}
