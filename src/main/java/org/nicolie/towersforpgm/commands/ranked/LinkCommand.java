package org.nicolie.towersforpgm.commands.ranked;

import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.DiscordManager;
import org.nicolie.towersforpgm.utils.RegisterCodeManager;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.bukkit.Sounds;

public class LinkCommand {

  @Command("link")
  @CommandDescription("Generate a code to link your Discord account")
  public void linkCommand(Audience audience, Player sender) {
    boolean matchbotActive = TowersForPGM.getInstance().isMatchBotEnabled();
    if (!matchbotActive) {
      audience.sendMessage(Component.translatable("matchbot.register.systemDisabled"));
      return;
    }

    DiscordManager.getDiscordPlayer(sender.getUniqueId()).thenAccept(discordPlayer -> {
      if (discordPlayer != null) {
        audience.sendWarning(Component.translatable("matchbot.register.alreadyLinked"));
        return;
      }

      if (RegisterCodeManager.hasActiveCode(sender.getUniqueId())) {
        String existingCode = RegisterCodeManager.getActiveCode(sender.getUniqueId());
        sendCode(audience, existingCode, false);
        return;
      }

      String code = generateCode();
      RegisterCodeManager.storeCode(sender.getUniqueId(), code);
      sendCode(audience, code, true);
    });
  }

  private String generateCode() {
    return String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
  }

  private void sendCode(Audience audience, String code, boolean newCode) {
    String translationKey =
        newCode ? "matchbot.register.codeGenerated" : "matchbot.register.activeCode";

    Component message = Component.translatable(translationKey, codeComponent(code))
        .color(NamedTextColor.GREEN)
        .clickEvent(ClickEvent.copyToClipboard(code))
        .hoverEvent(hoverText(code));

    audience.sendMessage(message);
    audience.playSound(Sounds.ITEM_PICKUP);
  }

  private Component hoverText(String code) {
    return Component.translatable("matchbot.register.hover.useCode", Component.text(code))
        .append(Component.newline())
        .append(Component.translatable("matchbot.register.hover.clickToCopy"))
        .append(Component.newline())
        .append(Component.translatable(
                "matchbot.register.hover.codeExpires",
                Component.text(RegisterCodeManager.CODE_EXPIRY_MINUTES).color(NamedTextColor.WHITE))
            .color(NamedTextColor.GRAY)
            .decorate(TextDecoration.ITALIC));
  }

  private Component codeComponent(String code) {
    return Component.text("[")
        .color(NamedTextColor.DARK_GRAY)
        .append(Component.text(code).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
        .append(Component.text("]").color(NamedTextColor.DARK_GRAY));
  }
}
