package org.nicolie.towersforpgm.matchbot.rankeds.listeners;

import me.tbg.match.bot.configs.BotConfig;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.rankeds.Queue;
import tc.oc.pgm.api.match.event.MatchFinishEvent;

public class RankedFinishListener implements Listener {
  private static final TowersForPGM plugin = TowersForPGM.getInstance();
  private static final String INACTIVE_ID = MatchBotConfig.getInactiveID();
  private static final String CHANNEL1_ID = MatchBotConfig.getTeam1ID();
  private static final String CHANNEL2_ID = MatchBotConfig.getTeam2ID();
  private static final String GUILD_ID = BotConfig.getServerId();

  @EventHandler
  public void onMatchFinish(MatchFinishEvent event) {
    if (!shouldProcessEvent(event.getMatch().getMap().getName())) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;
    movePlayersToInactiveChannel();
  }

  private boolean shouldProcessEvent(String mapName) {
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

    VoiceChannel channel1 = guild.getVoiceChannelById(CHANNEL1_ID);
    VoiceChannel channel2 = guild.getVoiceChannelById(CHANNEL2_ID);

    var membersToMove = new java.util.ArrayList<Member>();

    if (channel1 != null) {
      membersToMove.addAll(channel1.getMembers());
    }
    if (channel2 != null) {
      membersToMove.addAll(channel2.getMembers());
    }

    if (!membersToMove.isEmpty()) {
      var moveActions = membersToMove.stream()
          .map(member -> guild.moveVoiceMember(member, inactiveChannel))
          .toList();

      net.dv8tion.jda.api.requests.RestAction.allOf(moveActions).queue();
    }
  }
}
