package org.nicolie.towersforpgm.commandUtils;

import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;

public class DraftConfig {

  public void handleDraftSuggestionsCommand(CommandSender sender, Boolean isEnabled) {
    if (isEnabled == null) {
      boolean isDraftSuggestions = ConfigManager.isDraftSuggestions();
      String status = isDraftSuggestions
          ? LanguageManager.langMessage("draft.config.enabled")
          : LanguageManager.langMessage("draft.config.disabled");
      String message =
          LanguageManager.langMessage("draft.config.suggestionsStatus").replace("{status}", status);
      SendMessage.sendToPlayer(sender, message);
      return;
    }
    ConfigManager.setDraftSuggestions(isEnabled);
    String message = isEnabled
        ? LanguageManager.langMessage("draft.config.suggestions")
        : LanguageManager.langMessage("draft.config.noSuggestions");
    SendMessage.sendToPlayer(sender, message);
  }

  public void handleDraftTimerCommand(CommandSender sender, Boolean isEnabled) {
    if (isEnabled == null) {
      boolean isDraftTimer = ConfigManager.isDraftTimer();
      String status = isDraftTimer
          ? LanguageManager.langMessage("draft.config.enabled")
          : LanguageManager.langMessage("draft.config.disabled");
      String message =
          LanguageManager.langMessage("draft.config.timerStatus").replace("{status}", status);
      SendMessage.sendToPlayer(sender, message);
      return;
    }
    ConfigManager.setDraftTimer(isEnabled);
    String message = isEnabled
        ? LanguageManager.langMessage("draft.config.timer")
        : LanguageManager.langMessage("draft.config.noTimer");
    SendMessage.sendToPlayer(sender, message);
  }

  public void handleSecondGetsExtraPlayerCommand(CommandSender sender, Boolean isEnabled) {
    if (isEnabled == null) {
      boolean secondGetsExtraPlayer = ConfigManager.isSecondPickBalance();
      String status = secondGetsExtraPlayer
          ? LanguageManager.langMessage("draft.config.gets")
          : LanguageManager.langMessage("draft.config.doesNotGet");
      String message = LanguageManager.langMessage("draft.config.secondTeamBalanceStatus")
          .replace("{status}", status);
      SendMessage.sendToPlayer(sender, message);
      return;
    }
    ConfigManager.setSecondPickBalance(isEnabled);
    String message = isEnabled
        ? LanguageManager.langMessage("draft.config.secondTeamBalance")
        : LanguageManager.langMessage("draft.config.noSecondTeamBalance");
    SendMessage.sendToPlayer(sender, message);
  }

  public void setPrivateMatch(CommandSender sender, boolean isPrivateMatch) {
    String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
    ConfigManager.setPrivateMatch(mapName, isPrivateMatch);
    String message = isPrivateMatch
        ? LanguageManager.langMessage("privateMatch.true").replace("{map}", mapName)
        : LanguageManager.langMessage("privateMatch.false").replace("{map}", mapName);
    SendMessage.sendToPlayer(sender, message);
  }

  public void setDraftOrder(CommandSender sender, String order) {
    if (order == null || order.isEmpty()) {
      String currentOrder = ConfigManager.getDraftOrder();
      String message =
          LanguageManager.langMessage("draft.config.currentOrder").replace("{order}", currentOrder);
      SendMessage.sendToPlayer(sender, message);
      return;
    }
    if (!order.matches("A[AB]+")) {
      String errorMessage = LanguageManager.langMessage("draft.config.invalidOrder");
      SendMessage.sendToPlayer(sender, errorMessage);
      return;
    }
    ConfigManager.setDraftOrder(order);
    String success = LanguageManager.langMessage("draft.config.orderSet").replace("{order}", order);
    SendMessage.sendToPlayer(sender, success);
  }

  public void setMinDraftOrder(CommandSender sender, int size) {
    if (size < 1) {
      int currentSize = ConfigManager.getMinDraftOrder();
      String message = LanguageManager.langMessage("draft.config.currentSize")
          .replace("{size}", String.valueOf(currentSize));
      SendMessage.sendToPlayer(sender, message);
      return;
    }
    ConfigManager.setMinDraftOrder(size);
    String message =
        LanguageManager.langMessage("draft.config.sizeSet").replace("{size}", String.valueOf(size));
    SendMessage.sendToPlayer(sender, message);
  }
}
