package org.nicolie.towersforpgm.matchbot.rankeds;

import me.tbg.match.bot.configs.BotConfig;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.rankeds.Rank;

public class RoleManager {

  public static void changeRole(String discordId, Rank previousRank, Rank newRank) {
    if (!MatchBotConfig.isRankedEnabled()) return;

    String newRoleId = newRank.getRoleID();
    if (newRoleId == null || newRoleId.isEmpty()) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;
    String serverID = BotConfig.getServerId();
    Guild guild = jda.getGuildById(serverID);
    if (guild == null) return;

    guild.retrieveMemberById(discordId).queue(member -> {
      if (member == null) return;

      Role newRole = guild.getRoleById(newRoleId);
      if (newRole == null) return;

      if (member.getRoles().contains(newRole)) return;

      String[] allRoleIds = new String[] {
        MatchBotConfig.getBronzeRoleId(),
        MatchBotConfig.getSilverRoleId(),
        MatchBotConfig.getGoldRoleId(),
        MatchBotConfig.getEmeraldRoleId(),
        MatchBotConfig.getDiamondRoleId()
      };

      for (String rid : allRoleIds) {
        if (rid == null || rid.isEmpty()) continue;
        Role r = guild.getRoleById(rid);
        if (r == null) continue;
        if (member.getRoles().contains(r)) {
          guild.removeRoleFromMember(member, r).queue(null, error -> {});
        }
      }

      guild.addRoleToMember(member, newRole).queue(null, error -> {});
    });
  }
}
