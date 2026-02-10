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

  private static Guild getBotGuild(JDA jda) {
    if (jda == null) return null;
    String guildId = BotConfig.getServerId();
    if (guildId == null || guildId.isEmpty()) return null;
    return jda.getGuildById(guildId);
  }

  private void movePlayersToChannel(java.util.List<UUID> uuids, VoiceChannel targetChannel) {
    if (!MatchBotConfig.isVoiceChatEnabled()
        || uuids == null
        || uuids.isEmpty()
        || targetChannel == null) return;

    JDA jda = DiscordBot.getJDA();
    Guild guild = getBotGuild(jda);
    if (guild == null) return;

    var discordPlayerFutures =
        uuids.stream().map(DiscordManager::getDiscordPlayer).toList();

    java.util.concurrent.CompletableFuture.allOf(
            discordPlayerFutures.toArray(new java.util.concurrent.CompletableFuture[0]))
        .thenRun(() -> discordPlayerFutures.stream()
            .map(future -> future.getNow(null))
            .filter(discordPlayer -> discordPlayer != null)
            .map(discordPlayer -> guild.getMemberById(discordPlayer.getDiscordId()))
            .filter(member -> member != null && member.getVoiceState() != null)
            .filter(member -> member.getVoiceState().getChannel() != null)
            .forEach(member -> guild.moveVoiceMember(member, targetChannel).queue()));
  }

  private static void movePlayerToChannel(UUID minecraftUUID, String channelId) {
    if (!MatchBotConfig.isVoiceChatEnabled() || channelId == null || channelId.isEmpty()) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;

    Guild guild = getBotGuild(jda);
    VoiceChannel targetChannel = jda.getVoiceChannelById(channelId);
    if (guild == null || targetChannel == null) return;

    DiscordManager.getDiscordPlayer(minecraftUUID)
        .thenAccept(discordPlayer -> {
          if (discordPlayer == null) return;

          Member member = guild.getMemberById(discordPlayer.getDiscordId());
          if (member == null
              || member.getVoiceState() == null
              || member.getVoiceState().getChannel() == null) return;

          guild.moveVoiceMember(member, targetChannel).queue();
        })
        .exceptionally(ex -> null);
  }

  public static void movePlayerToInactive(UUID minecraftUUID) {
    movePlayerToChannel(minecraftUUID, INACTIVE_ID);
  }

  public static void movePlayerToQueue(UUID minecraftUUID) {
    movePlayerToChannel(minecraftUUID, QUEUE_ID);
  }

  public static void movePlayerToTeam1(UUID minecraftUUID) {
    movePlayerToChannel(minecraftUUID, CHANNEL1_ID);
  }

  public static void movePlayerToTeam2(UUID minecraftUUID) {
    movePlayerToChannel(minecraftUUID, CHANNEL2_ID);
  }

  @EventHandler
  public void onDraftStart(DraftStartEvent event) {
    if (!shouldProcessEvent()) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;

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
    if (!shouldProcessEvent()) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;
    DiscordBot.setBlacklistCurrentMap(true);
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
    if (!shouldProcessEvent()) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;
    DiscordBot.setBlacklistCurrentMap(true);
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

  private boolean shouldProcessEvent() {
    if (!MatchBotConfig.isVoiceChatEnabled()) return false;

    Boolean ranked = Queue.isRanked();
    boolean isRankedTable = plugin.config().databaseTables().currentTableIsRanked();

    return (ranked && isRankedTable);
  }
}
