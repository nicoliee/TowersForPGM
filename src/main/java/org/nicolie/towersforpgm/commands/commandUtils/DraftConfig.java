package org.nicolie.towersforpgm.commands.commandUtils;

import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;

public class DraftConfig {
  private final TowersForPGM plugin = TowersForPGM.getInstance();

  public void handleDraftSuggestionsCommand(CommandSender sender, Boolean isEnabled) {
    if (isEnabled == null) {
      boolean isDraftSuggestions = plugin.config().draft().isDraftSuggestions();
      String status = isDraftSuggestions
          ? LanguageManager.message("draft.config.enabled")
          : LanguageManager.message("draft.config.disabled");
      String message =
          LanguageManager.message("draft.config.suggestionsStatus").replace("{status}", status);
      SendMessage.sendToPlayer(sender, message);
      return;
    }
    plugin.config().draft().setDraftSuggestions(isEnabled);
    String message = isEnabled
        ? LanguageManager.message("draft.config.suggestions")
        : LanguageManager.message("draft.config.noSuggestions");
    SendMessage.sendToPlayer(sender, message);
  }

  public void handleDraftTimerCommand(CommandSender sender, Boolean isEnabled) {
    if (isEnabled == null) {
      boolean isDraftTimer = plugin.config().draft().isDraftTimer();
      String status = isDraftTimer
          ? LanguageManager.message("draft.config.enabled")
          : LanguageManager.message("draft.config.disabled");
      String message =
          LanguageManager.message("draft.config.timerStatus").replace("{status}", status);
      SendMessage.sendToPlayer(sender, message);
      return;
    }
    plugin.config().draft().setDraftTimer(isEnabled);
    String message = isEnabled
        ? LanguageManager.message("draft.config.timer")
        : LanguageManager.message("draft.config.noTimer");
    SendMessage.sendToPlayer(sender, message);
  }

  public void handleSecondGetsExtraPlayerCommand(CommandSender sender, Boolean isEnabled) {
    if (isEnabled == null) {
      boolean secondGetsExtraPlayer = plugin.config().draft().isSecondPickBalance();
      String status = secondGetsExtraPlayer
          ? LanguageManager.message("draft.config.gets")
          : LanguageManager.message("draft.config.doesNotGet");
      String message = LanguageManager.message("draft.config.secondTeamBalanceStatus")
          .replace("{status}", status);
      SendMessage.sendToPlayer(sender, message);
      return;
    }
    plugin.config().draft().setSecondPickBalance(isEnabled);
    String message = isEnabled
        ? LanguageManager.message("draft.config.secondTeamBalance")
        : LanguageManager.message("draft.config.noSecondTeamBalance");
    SendMessage.sendToPlayer(sender, message);
  }

  public void setPrivateMatch(CommandSender sender, boolean isPrivateMatch) {
    String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
    plugin.config().privateMatch().setPrivateMatch(mapName, isPrivateMatch);
    String message = isPrivateMatch
        ? LanguageManager.message("privateMatch.true").replace("{map}", mapName)
        : LanguageManager.message("privateMatch.false").replace("{map}", mapName);
    SendMessage.sendToPlayer(sender, message);
  }

  public void setDraftOrder(CommandSender sender, String order) {
    if (order == null || order.isEmpty()) {
      String currentOrder = plugin.config().draft().getOrder();
      String message =
          LanguageManager.message("draft.config.currentOrder").replace("{order}", currentOrder);
      SendMessage.sendToPlayer(sender, message);
      return;
    }
    if (!order.matches("A[AB]+")) {
      String errorMessage = LanguageManager.message("draft.config.invalidOrder");
      SendMessage.sendToPlayer(sender, errorMessage);
      return;
    }
    plugin.config().draft().setOrder(order);
    String success = LanguageManager.message("draft.config.orderSet").replace("{order}", order);
    SendMessage.sendToPlayer(sender, success);
  }

  public void setMinDraftOrder(CommandSender sender, int size) {
    if (size < 1) {
      int currentSize = plugin.config().draft().getMinOrder();
      String message = LanguageManager.message("draft.config.currentSize")
          .replace("{size}", String.valueOf(currentSize));
      SendMessage.sendToPlayer(sender, message);
      return;
    }
    plugin.config().draft().setMinOrder(size);
    String message =
        LanguageManager.message("draft.config.sizeSet").replace("{size}", String.valueOf(size));
    SendMessage.sendToPlayer(sender, message);
  }
}
