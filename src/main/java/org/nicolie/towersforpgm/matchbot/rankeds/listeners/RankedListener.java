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
import org.nicolie.towersforpgm.matchbot.rankeds.VoiceChannelManager;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;

public class RankedListener implements Listener {
  private static final TowersForPGM plugin = TowersForPGM.getInstance();

  private static final String QUEUE_ID = MatchBotConfig.getQueueID();
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

  private static void movePlayerToChannel(UUID minecraftUUID, VoiceChannel targetChannel) {
    if (!MatchBotConfig.isVoiceChatEnabled() || targetChannel == null) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;

    Guild guild = getBotGuild(jda);
    if (guild == null) return;

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
    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;
    VoiceChannel channel = jda.getVoiceChannelById(INACTIVE_ID);
    if (channel != null) movePlayerToChannel(minecraftUUID, channel);
  }

  public static void movePlayerToQueue(UUID minecraftUUID) {
    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;
    VoiceChannel channel = jda.getVoiceChannelById(QUEUE_ID);
    if (channel != null) movePlayerToChannel(minecraftUUID, channel);
  }

  public static void movePlayerToTeam1(UUID minecraftUUID) {
    VoiceChannel channel = VoiceChannelManager.getTeam1Channel();
    if (channel != null) movePlayerToChannel(minecraftUUID, channel);
  }

  public static void movePlayerToTeam2(UUID minecraftUUID) {
    VoiceChannel channel = VoiceChannelManager.getTeam2Channel();
    if (channel != null) movePlayerToChannel(minecraftUUID, channel);
  }

  @EventHandler
  public void onDraftStart(DraftStartEvent event) {
    if (!shouldProcessEvent()) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;

    // Obtener nombres de los equipos de PGM
    Match match = MatchManager.getMatch();
    if (match == null) return;

    String team1Name = "Team 1";
    String team2Name = "Team 2";

    try {
      java.util.Collection<? extends Party> parties = match.getCompetitors();
      if (parties.size() >= 2) {
        java.util.Iterator<? extends Party> iterator = parties.iterator();
        Party party1 = iterator.next();
        Party party2 = iterator.next();
        team1Name =
            net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(party1.getName());
        team2Name =
            net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(party2.getName());
      }
    } catch (Exception e) {
      // Usar nombres por defecto
    }

    // Calcular tamaño de equipos (capitanes + jugadores disponibles / 2)
    int totalPlayers = 2 + event.getPlayers().size();
    int team1Size = (totalPlayers + 1) / 2; // Redondear hacia arriba
    int team2Size = totalPlayers / 2;

    // Crear canales dinámicos
    VoiceChannelManager.createTeamChannels(team1Name, team2Name, team1Size, team2Size)
        .thenRun(() -> {
          VoiceChannel channel1 = VoiceChannelManager.getTeam1Channel();
          if (channel1 == null) return;

          var uuidsToMove = new java.util.ArrayList<UUID>();
          uuidsToMove.add(event.getCaptain1());
          uuidsToMove.add(event.getCaptain2());
          event.getPlayers().forEach(matchPlayer -> uuidsToMove.add(matchPlayer.getId()));

          movePlayersToChannel(uuidsToMove, channel1);
        });
  }

  @EventHandler
  public void onDraftEnd(DraftEndEvent event) {
    if (!shouldProcessEvent()) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;
    DiscordBot.setBlacklistCurrentMap(true);
    VoiceChannel channel2 = VoiceChannelManager.getTeam2Channel();
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

    // Si los canales no existen (matchmaking sin draft), crearlos ahora
    if (VoiceChannelManager.getTeam1Channel() == null
        || VoiceChannelManager.getTeam2Channel() == null) {
      // Obtener nombres de los equipos de PGM
      String team1Name = "Team 1";
      String team2Name = "Team 2";

      try {
        java.util.List<String> team1List = event.getTeam1();
        if (!team1List.isEmpty()) {
          Player player = Bukkit.getPlayer(team1List.get(0));
          if (player != null) {
            tc.oc.pgm.api.match.Match match = PGM.get().getMatchManager().getMatch(player);
            if (match != null) {
              java.util.Collection<? extends Party> parties = match.getCompetitors();
              if (parties.size() >= 2) {
                java.util.Iterator<? extends Party> iterator = parties.iterator();
                Party party1 = iterator.next();
                Party party2 = iterator.next();
                team1Name =
                    net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                        .plainText()
                        .serialize(party1.getName());
                team2Name =
                    net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                        .plainText()
                        .serialize(party2.getName());
              }
            }
          }
        }
      } catch (Exception e) {
        // Usar nombres por defecto
      }

      int team1Size = event.getTeam1().size();
      int team2Size = event.getTeam2().size();

      VoiceChannelManager.createTeamChannels(team1Name, team2Name, team1Size, team2Size)
          .thenRun(() -> {
            VoiceChannel channel1 = VoiceChannelManager.getTeam1Channel();
            VoiceChannel channel2 = VoiceChannelManager.getTeam2Channel();
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
          });
    } else {
      // Los canales ya existen (draft terminado)
      VoiceChannel channel1 = VoiceChannelManager.getTeam1Channel();
      VoiceChannel channel2 = VoiceChannelManager.getTeam2Channel();
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
  }

  private boolean shouldProcessEvent() {
    if (!MatchBotConfig.isVoiceChatEnabled()) return false;

    Boolean ranked = Queue.isRanked();
    boolean isRankedTable = plugin.config().databaseTables().currentTableIsRanked();

    return (ranked && isRankedTable);
  }
}
