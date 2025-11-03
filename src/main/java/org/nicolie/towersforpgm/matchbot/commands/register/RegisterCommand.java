package org.nicolie.towersforpgm.matchbot.commands.register;

import java.awt.Color;
import java.time.Instant;
import java.util.UUID;
import me.tbg.match.bot.configs.DiscordBot;
import me.tbg.match.bot.configs.MessagesConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.DiscordManager;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.RegisterCodeManager;

public class RegisterCommand extends ListenerAdapter {

  public static final String NAME = "register";
  private static final String OPTION_CODE = "code";

  public static void register() {
    JDA jda = DiscordBot.getJDA();
    if (jda != null) {
      jda.addEventListener(new RegisterCommand());

      jda.upsertCommand(NAME, LanguageManager.langMessage("matchbot.register.description"))
          .addOption(
              OptionType.STRING,
              OPTION_CODE,
              "Código de 6 dígitos obtenido en Minecraft con /register",
              true)
          .queue();
    }
  }

  @Override
  public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
    if (!event.getName().equals(NAME)) return;

    if (!MatchBotConfig.isRankedEnabled()) {
      event
          .reply("❌ " + LanguageManager.langMessage("matchbot.register.system-disabled"))
          .setEphemeral(true)
          .queue();
      return;
    }

    OptionMapping codeOption = event.getOption(OPTION_CODE);
    if (codeOption == null) {
      event.reply("❌ Debes proporcionar un código.").setEphemeral(true).queue();
      return;
    }

    String code = codeOption.getAsString().trim();
    String discordId = event.getUser().getId();

    if (!code.matches("\\d{6}")) {
      event
          .reply("❌ " + LanguageManager.langMessage("matchbot.register.invalid-code-format"))
          .setEphemeral(true)
          .queue();
      return;
    }

    event.deferReply(true).queue(hook -> {
      DiscordManager.getMinecraftUuid(discordId)
          .thenAccept(linkedUuid -> {
            if (linkedUuid != null) {
              hook.editOriginal("❌ "
                      + LanguageManager.langMessage("matchbot.register.discord-already-linked"))
                  .queue();
              return;
            }

            UUID playerUuid = RegisterCodeManager.validateAndConsumeCode(code);
            if (playerUuid == null) {
              hook.editOriginal(
                      "❌ " + LanguageManager.langMessage("matchbot.register.invalid-or-expired"))
                  .queue();
              return;
            }

            DiscordManager.getDiscordId(playerUuid)
                .thenAccept(existingDiscordId -> {
                  if (existingDiscordId != null) {
                    hook.editOriginal("❌ "
                            + LanguageManager.langMessage(
                                "matchbot.register.minecraft-already-linked"))
                        .queue();
                    return;
                  }

                  DiscordManager.registerDCAccount(playerUuid, discordId)
                      .thenAccept(success -> {
                        if (success) {
                          OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
                          String playerName =
                              offlinePlayer.getName() != null ? offlinePlayer.getName() : "Jugador";

                          String registeredRoleId = MatchBotConfig.getRegisteredRoleId();
                          if (registeredRoleId != null && !registeredRoleId.isEmpty()) {
                            event
                                .getGuild()
                                .addRoleToMember(
                                    event.getUser(), event.getGuild().getRoleById(registeredRoleId))
                                .queue();
                          }

                          EmbedBuilder embed = new EmbedBuilder()
                              .setColor(Color.GREEN)
                              .setTitle(
                                  "✅ " + LanguageManager.langMessage("matchbot.register.success"))
                              .setTimestamp(Instant.now())
                              .setThumbnail(
                                  "https://mc-heads.net/avatar/" + playerName.toLowerCase())
                              .setAuthor(
                                  MessagesConfig.message("author.name"),
                                  null,
                                  MessagesConfig.message("author.icon_url"))
                              .addField(
                                  LanguageManager.langMessage("matchbot.register.minecraft-label"),
                                  playerName,
                                  true)
                              .addField(
                                  LanguageManager.langMessage("matchbot.register.discord-label"),
                                  "<@" + discordId + ">",
                                  true);
                          hook.editOriginalEmbeds(embed.build()).queue();
                        } else {
                          hook.editOriginal("❌ "
                                  + LanguageManager.langMessage("matchbot.register.link-error"))
                              .queue();
                        }
                      })
                      .exceptionally(throwable -> {
                        TowersForPGM.getInstance()
                            .getLogger()
                            .severe("Error registrando vinculación: " + throwable.getMessage());
                        hook.editOriginal("❌ "
                                + LanguageManager.langMessage("matchbot.register.internal-error"))
                            .queue();
                        return null;
                      });
                })
                .exceptionally(throwable -> {
                  TowersForPGM.getInstance()
                      .getLogger()
                      .severe("Error verificando cuenta Minecraft: " + throwable.getMessage());
                  hook.editOriginal(
                          "❌ " + LanguageManager.langMessage("matchbot.register.internal-error"))
                      .queue();
                  return null;
                });
          })
          .exceptionally(throwable -> {
            TowersForPGM.getInstance()
                .getLogger()
                .severe("Error verificando cuenta Discord: " + throwable.getMessage());
            hook.editOriginal(
                    "❌ " + LanguageManager.langMessage("matchbot.register.internal-error"))
                .queue();
            return null;
          });
    });
  }
}
