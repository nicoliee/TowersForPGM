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
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.RegisterCodeManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

public class RegisterCommand implements CommandExecutor {

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      SendMessage.sendToPlayer(sender, LanguageManager.langMessage("errors.noPlayer"));
      return true;
    }

    Player player = (Player) sender;
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);

    // Verificar si el sistema de ranked está habilitado
    if (!MatchBotConfig.isRankedEnabled()) {
      matchPlayer.sendMessage(
          Component.text(LanguageManager.langMessage("matchbot.register.system-disabled"))
              .color(NamedTextColor.RED));
      return true;
    }

    // Verificar si la cuenta ya está vinculada
    DiscordManager.getDiscordId(player.getUniqueId()).thenAccept(discordId -> {
      if (discordId != null) {
        Component message = Component.text(
                LanguageManager.langMessage("matchbot.register.already-linked")
                    .replace("{discord_id}", discordId))
            .color(NamedTextColor.YELLOW)
            .append(
                Component.text(discordId).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));

        matchPlayer.sendMessage(message);
        return;
      }

      // Verificar si ya tiene un código activo
      if (RegisterCodeManager.hasActiveCode(player.getUniqueId())) {
        String existingCode = RegisterCodeManager.getActiveCode(player.getUniqueId());

        Component message = Component.text(
                LanguageManager.langMessage("matchbot.register.active-code-exists")
                    .replace("{code}", existingCode))
            .color(NamedTextColor.YELLOW)
            .append(Component.text("[" + existingCode + "]")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.copyToClipboard(existingCode))
                .hoverEvent(Component.text(
                    LanguageManager.langMessage("matchbot.register.click-copy-tooltip"))))
            .append(Component.text("\n"
                    + LanguageManager.langMessage("matchbot.register.use-code-discord")
                        .replace("{code}", existingCode))
                .color(NamedTextColor.GRAY));

        matchPlayer.sendMessage(message);
        return;
      }

      // Generar código de 6 dígitos
      String code = generateCode();

      // Almacenar código temporalmente (10 minutos)
      RegisterCodeManager.storeCode(player.getUniqueId(), code);

      // Enviar mensaje al jugador con el código clickeable
      Component message = Component.text(
              LanguageManager.langMessage("matchbot.register.code-generated") + " ")
          .color(NamedTextColor.GREEN)
          .append(
              Component.text(LanguageManager.langMessage("matchbot.register.click-to-copy") + " ")
                  .color(NamedTextColor.YELLOW))
          .append(Component.text("[" + code + "]")
              .color(NamedTextColor.GOLD)
              .decorate(TextDecoration.BOLD)
              .clickEvent(ClickEvent.copyToClipboard(code))
              .hoverEvent(Component.text(
                  LanguageManager.langMessage("matchbot.register.click-copy-tooltip"))))
          .append(Component.text("\n"
                  + LanguageManager.langMessage("matchbot.register.use-code-discord")
                      .replace("{code}", code))
              .color(NamedTextColor.GRAY))
          .append(
              Component.text("\n" + LanguageManager.langMessage("matchbot.register.code-expires"))
                  .color(NamedTextColor.GRAY));

      matchPlayer.sendMessage(message);
    });

    return true;
  }

  private String generateCode() {
    Random random = new Random();
    return String.format("%06d", random.nextInt(1000000));
  }
}
