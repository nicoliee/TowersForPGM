package org.nicolie.towersforpgm.commands;

import java.util.Random;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.database.DiscordManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.RegisterCodeManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

public class LinkCommand implements CommandExecutor {

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      SendMessage.sendToPlayer(sender, LanguageManager.message("errors.noPlayer"));
      return true;
    }

    Player player = (Player) sender;
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);

    // // Verificar si el sistema de ranked está habilitado
    // if (!MatchBotConfig.isVoiceChatEnabled()) {
    //   matchPlayer.sendWarning(
    //       Component.text(LanguageManager.message("matchbot.register.system-disabled")));
    //   return true;
    // }

    // Verificar si la cuenta ya está vinculada
    DiscordManager.getDiscordPlayer(player.getUniqueId()).thenAccept(discordPlayer -> {
      if (discordPlayer != null) {
        Component message =
            Component.text(LanguageManager.message("matchbot.register.already-linked")
                .replace("{discord_id}", discordPlayer.getDiscordId()));
        matchPlayer.sendWarning(message);
        return;
      }

      // Verificar si ya tiene un código activo
      if (RegisterCodeManager.hasActiveCode(player.getUniqueId())) {
        String existingCode = RegisterCodeManager.getActiveCode(player.getUniqueId());

        Component hoverText = Component.text(
                LanguageManager.message("matchbot.register.click-copy-tooltip"))
            .color(NamedTextColor.AQUA)
            .decorate(TextDecoration.BOLD)
            .append(Component.text("\n"
                    + LanguageManager.message("matchbot.register.use-code-discord")
                        .replace("{code}", existingCode))
                .color(NamedTextColor.YELLOW))
            .append(Component.text("\n" + LanguageManager.message("matchbot.register.code-expires"))
                .color(NamedTextColor.GRAY)
                .decorate(TextDecoration.ITALIC));

        Component message = Component.text(
                LanguageManager.message("matchbot.register.active-code-exists"))
            .color(NamedTextColor.YELLOW)
            .append(Component.text("[" + existingCode + "]")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.copyToClipboard(existingCode))
                .hoverEvent(hoverText));

        matchPlayer.sendMessage(message);
        return;
      }

      // Generar código de 6 dígitos
      String code = generateCode();

      // Almacenar código temporalmente (10 minutos)
      RegisterCodeManager.storeCode(player.getUniqueId(), code);

      Component hoverText = Component.text(
              LanguageManager.message("matchbot.register.click-copy-tooltip"))
          .color(NamedTextColor.AQUA)
          .decorate(TextDecoration.BOLD)
          .append(Component.text("\n"
                  + LanguageManager.message("matchbot.register.use-code-discord")
                      .replace("{code}", code))
              .color(NamedTextColor.YELLOW))
          .append(Component.text("\n" + LanguageManager.message("matchbot.register.code-expires"))
              .color(NamedTextColor.GRAY)
              .decorate(TextDecoration.ITALIC));

      Component message = Component.text(
              LanguageManager.message("matchbot.register.code-generated") + " ")
          .color(NamedTextColor.GREEN)
          .append(Component.text(LanguageManager.message("matchbot.register.click-to-copy") + " ")
              .color(NamedTextColor.YELLOW))
          .append(Component.text("[" + code + "]")
              .color(NamedTextColor.GOLD)
              .decorate(TextDecoration.BOLD)
              .clickEvent(ClickEvent.copyToClipboard(code))
              .hoverEvent(hoverText));

      matchPlayer.sendMessage(message);
    });

    return true;
  }

  private String generateCode() {
    Random random = new Random();
    return String.format("%06d", random.nextInt(1000000));
  }
}
