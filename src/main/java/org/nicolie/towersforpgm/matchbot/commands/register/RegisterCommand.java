package org.nicolie.towersforpgm.matchbot.commands.register;

import java.util.UUID;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.DiscordManager;
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

    // Verificar si el sistema ranked está habilitado
    if (!isRankedSystemEnabled()) {
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

    // Validar formato del código (6 dígitos)
    if (!code.matches("\\d{6}")) {
      event
          .reply("❌ " + LanguageManager.langMessage("matchbot.register.invalid-code-format"))
          .setEphemeral(true)
          .queue();
      return;
    }

    event.deferReply(true).queue(hook -> {
      // Verificar si la cuenta de Discord ya está vinculada
      DiscordManager.getMinecraftUuid(discordId)
          .thenAccept(linkedUuid -> {
            if (linkedUuid != null) {
              hook.editOriginal("❌ "
                      + LanguageManager.langMessage("matchbot.register.discord-already-linked"))
                  .queue();
              return;
            }

            // Validar y consumir el código
            UUID playerUuid = RegisterCodeManager.validateAndConsumeCode(code);
            if (playerUuid == null) {
              hook.editOriginal(
                      "❌ " + LanguageManager.langMessage("matchbot.register.invalid-or-expired"))
                  .queue();
              return;
            }

            // Verificar si la cuenta de Minecraft ya está vinculada
            DiscordManager.getDiscordId(playerUuid)
                .thenAccept(existingDiscordId -> {
                  if (existingDiscordId != null) {
                    hook.editOriginal("❌ "
                            + LanguageManager.langMessage(
                                "matchbot.register.minecraft-already-linked"))
                        .queue();
                    return;
                  }

                  // Registrar la vinculación
                  DiscordManager.registerDCAccount(playerUuid, discordId)
                      .thenAccept(success -> {
                        if (success) {
                          // Obtener el nombre del jugador para el mensaje
                          OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
                          String playerName =
                              offlinePlayer.getName() != null ? offlinePlayer.getName() : "Jugador";

                          hook.editOriginal("✅ "
                                  + LanguageManager.langMessage("matchbot.register.success") + "\n"
                                  + "**"
                                  + LanguageManager.langMessage("matchbot.register.minecraft-label")
                                  + "** " + playerName + "\n" + "**"
                                  + LanguageManager.langMessage("matchbot.register.discord-label")
                                  + "** <@" + discordId + ">")
                              .queue();

                          // Log en consola
                          TowersForPGM.getInstance()
                              .getLogger()
                              .info("Cuenta vinculada: " + playerName + " (" + playerUuid + ") <-> "
                                  + event.getUser().getAsTag() + " (" + discordId + ")");

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

  /** Verifica si el sistema ranked está habilitado en matchbot.yml */
  private boolean isRankedSystemEnabled() {
    if (!TowersForPGM.getInstance().isMatchBotEnabled()) {
      return false;
    }

    return TowersForPGM.getInstance().getMatchBotConfig().getBoolean("ranked.enabled", false);
  }
}
