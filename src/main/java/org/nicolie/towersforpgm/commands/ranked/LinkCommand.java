package org.nicolie.towersforpgm.commands.ranked;

import java.util.Random;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.DiscordManager;
import org.nicolie.towersforpgm.utils.RegisterCodeManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.bukkit.Sounds;

public class LinkCommand implements CommandExecutor {

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    Audience audience = Audience.get(sender);
    if (!(sender instanceof Player)) {
      audience.sendWarning(Component.translatable("command.onlyPlayers"));
      return true;
    }
    boolean matchbotActive = TowersForPGM.getInstance().isMatchBotEnabled();

    if (!matchbotActive) {
      audience.sendWarning(Component.translatable("matchbot.register.systemDisabled"));
      return true;
    }

    Player player = (Player) sender;
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);

    // Verificar si la cuenta ya está vinculada
    DiscordManager.getDiscordPlayer(player.getUniqueId()).thenAccept(discordPlayer -> {
      if (discordPlayer != null) {
        matchPlayer.sendWarning(Component.translatable("matchbot.register.alreadyLinked"));
        return;
      }

      // Verificar si ya tiene un código activo
      if (RegisterCodeManager.hasActiveCode(player.getUniqueId())) {
        String existingCode = RegisterCodeManager.getActiveCode(player.getUniqueId());
        sendCode(matchPlayer, existingCode, false);
        return;
      }
      String code = generateCode();
      RegisterCodeManager.storeCode(player.getUniqueId(), code);
      sendCode(matchPlayer, code, true);
    });

    return true;
  }

  private String generateCode() {
    Random random = new Random();
    return String.format("%06d", random.nextInt(1000000));
  }

  private void sendCode(MatchPlayer matchPlayer, String code, boolean newCode) {
    String translationKey =
        newCode ? "matchbot.register.codeGenerated" : "matchbot.register.activeCode";

    Component message = Component.translatable(translationKey, code(code))
        .color(NamedTextColor.GREEN)
        .clickEvent(ClickEvent.copyToClipboard(code))
        .hoverEvent(hoverText(code));

    matchPlayer.sendMessage(message);
    matchPlayer.playSound(Sounds.ITEM_PICKUP);
  }

  private Component hoverText(String code) {
    Component hoverText = Component.translatable(
            "matchbot.register.hover.useCode", Component.text(code))
        .append(Component.newline())
        .append(Component.translatable("matchbot.register.hover.clickToCopy"))
        .append(Component.newline())
        .append(Component.translatable(
                "matchbot.register.hover.codeExpires",
                Component.text(RegisterCodeManager.CODE_EXPIRY_MINUTES).color(NamedTextColor.WHITE))
            .color(NamedTextColor.GRAY)
            .decorate(TextDecoration.ITALIC));
    return hoverText;
  }

  private Component code(String code) {
    Component value = Component.text("[")
        .color(NamedTextColor.DARK_GRAY)
        .append(Component.text(code).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
        .append(Component.text("]").color(NamedTextColor.DARK_GRAY));
    return value;
  }
}
