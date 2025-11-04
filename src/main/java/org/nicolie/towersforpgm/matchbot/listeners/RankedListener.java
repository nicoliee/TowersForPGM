package org.nicolie.towersforpgm.matchbot.listeners;

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
import org.nicolie.towersforpgm.database.DiscordManager;
import org.nicolie.towersforpgm.draft.events.DraftEndEvent;
import org.nicolie.towersforpgm.draft.events.DraftStartEvent;
import org.nicolie.towersforpgm.draft.events.MatchmakingEndEvent;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.ConfigManager;

public class RankedListener implements Listener {

  private static final String CHANNEL1_ID = MatchBotConfig.getTeam1ID();
  private static final String CHANNEL2_ID = MatchBotConfig.getTeam2ID();
  private static final String INACTIVE_ID = MatchBotConfig.getInactiveID();
  private static final Boolean RANKED_ENABLED = MatchBotConfig.isRankedEnabled();

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

    // Obtener todos los DiscordPlayers de manera asíncrona
    var discordPlayerFutures = uuids.stream()
        .map(DiscordManager::getDiscordPlayer)
        .toList();

    // Cuando todos los DiscordPlayers estén listos, procesarlos en lote
    java.util.concurrent.CompletableFuture.allOf(
        discordPlayerFutures.toArray(new java.util.concurrent.CompletableFuture[0]))
        .thenRun(() -> {
          var moveActions = new java.util.ArrayList<net.dv8tion.jda.api.requests.RestAction<Void>>();

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
              // Continuar con el siguiente miembro si hay error
            }
          }

          // Ejecutar todas las acciones de mover en paralelo
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

  @EventHandler
  public void onDraftStart(DraftStartEvent event) {
    if (!shouldProcessEvent(event.getMatch().getMap().getName())) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null || CHANNEL1_ID == null || CHANNEL1_ID.isEmpty()) return;

    VoiceChannel channel1 = jda.getVoiceChannelById(CHANNEL1_ID);
    if (channel1 == null) return;

    // Recolectar todos los UUIDs a mover
    var uuidsToMove = new java.util.ArrayList<UUID>();
    uuidsToMove.add(event.getCaptain1());
    uuidsToMove.add(event.getCaptain2());
    event.getPlayers().forEach(matchPlayer -> uuidsToMove.add(matchPlayer.getId()));

    // Mover todos los jugadores en lote
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

    // Recolectar UUIDs del equipo 1
    var team1UUIDs = new java.util.ArrayList<UUID>();
    event.getTeam1().forEach(playerName -> {
      Player player = Bukkit.getPlayer(playerName);
      if (player != null) team1UUIDs.add(player.getUniqueId());
    });

    // Recolectar UUIDs del equipo 2
    var team2UUIDs = new java.util.ArrayList<UUID>();
    event.getTeam2().forEach(playerName -> {
      Player player = Bukkit.getPlayer(playerName);
      if (player != null) team2UUIDs.add(player.getUniqueId());
    });

    // Mover ambos equipos en lotes
    movePlayersToChannel(team1UUIDs, channel1);
    movePlayersToChannel(team2UUIDs, channel2);
  }

  private boolean shouldProcessEvent(String mapName) {
    if (!RANKED_ENABLED) return false;

    String table = ConfigManager.getActiveTable(mapName);
    Boolean ranked = Queue.isRanked();
    Boolean rankedTable = ConfigManager.isRankedTable(table);

    return (ranked && rankedTable);
  }
}
