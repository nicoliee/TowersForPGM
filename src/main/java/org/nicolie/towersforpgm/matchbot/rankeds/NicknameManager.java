package org.nicolie.towersforpgm.matchbot.rankeds;

import java.util.UUID;
import me.tbg.match.bot.configs.BotConfig;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.bukkit.Bukkit;
import org.nicolie.towersforpgm.database.DiscordManager;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;

public class NicknameManager {

  public static void updateNicknameToMinecraftUsername(UUID minecraftUUID) {
    if (!MatchBotConfig.isVoiceChatEnabled()) return;

    JDA jda = DiscordBot.getJDA();
    if (jda == null) return;

    String minecraftUsername = Bukkit.getOfflinePlayer(minecraftUUID).getName();
    if (minecraftUsername == null) return;

    DiscordManager.getDiscordPlayer(minecraftUUID)
        .thenAccept(discordPlayer -> {
          if (discordPlayer == null) return;

          String discordId = discordPlayer.getDiscordId();
          Guild guild = getGuild(jda);
          if (guild == null) return;

          Member member = guild.getMemberById(discordId);
          if (member == null) return;

          String currentNickname = member.getEffectiveName();

          // Si el nickname no coincide con el username de Minecraft, actualizarlo
          if (!currentNickname.equals(minecraftUsername)) {
            member.modifyNickname(minecraftUsername).queue(success -> {}, error -> {});
          }
        })
        .exceptionally(error -> {
          return null;
        });
  }

  private static Guild getGuild(JDA jda) {
    String guildId = BotConfig.getServerId();
    if (guildId == null || guildId.isEmpty()) return null;
    return jda.getGuildById(guildId);
  }
}
