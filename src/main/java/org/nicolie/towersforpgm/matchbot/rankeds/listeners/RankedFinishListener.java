package org.nicolie.towersforpgm.matchbot.rankeds.listeners;

import java.util.Objects;
import java.util.stream.Stream;
import me.tbg.match.bot.configs.BotConfig;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.rankeds.RankedItem;
import tc.oc.pgm.api.match.event.MatchFinishEvent;

public class RankedFinishListener implements Listener {
  private static final TowersForPGM plugin = TowersForPGM.getInstance();
  private static final String INACTIVE_ID = MatchBotConfig.getInactiveID();
  private static final String CHANNEL1_ID = MatchBotConfig.getTeam1ID();
  private static final String CHANNEL2_ID = MatchBotConfig.getTeam2ID();
  private static final String GUILD_ID = BotConfig.getServerId();

  @EventHandler
  public void onMatchFinish(MatchFinishEvent event) {
    if (!shouldProcessEvent()) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;

    // Dar tiempo para que los jugadores hagan click al item antes de mover a AFK
    // El item ya se da automáticamente por ObserverKitApplyEvent
    org.bukkit.Bukkit.getScheduler()
        .runTaskLaterAsynchronously(plugin, this::movePlayersToInactiveChannel, 100L);
  }

  private boolean shouldProcessEvent() {
    if (!MatchBotConfig.isVoiceChatEnabled()) return false;

    Boolean ranked = Queue.isRanked();
    boolean isRankedTable = plugin.config().databaseTables().currentTableIsRanked();

    return (ranked && isRankedTable);
  }

  private void movePlayersToInactiveChannel() {
    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;
    var guild = jda.getGuildById(GUILD_ID);
    if (guild == null) return;

    VoiceChannel inactiveChannel = guild.getVoiceChannelById(INACTIVE_ID);
    if (inactiveChannel == null) return;

    Stream.of(guild.getVoiceChannelById(CHANNEL1_ID), guild.getVoiceChannelById(CHANNEL2_ID))
        .filter(Objects::nonNull)
        .flatMap(channel -> channel.getMembers().stream())
        .filter(member -> !RankedItem.isDiscordIdGoingToQueue(member.getId()))
        .forEach(member -> guild.moveVoiceMember(member, inactiveChannel).queue());
  }
}
