package org.nicolie.towersforpgm.commandUtils;

import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.SendMessage;

import tc.oc.pgm.api.PGM;

public class DraftConfig {
    private final LanguageManager languageManager;

    public DraftConfig(LanguageManager languageManager) {
        this.languageManager = languageManager;
    }

    public void handleDraftSuggestionsCommand(CommandSender sender) {
        boolean isDraftSuggestions = ConfigManager.isDraftSuggestions();
        ConfigManager.setDraftSuggestions(!isDraftSuggestions);
        String message = isDraftSuggestions ? "Draft suggestions disabled." : "Draft suggestions enabled.";
        SendMessage.sendToPlayer(sender, message);
    }

    public void handleDraftTimerCommand(CommandSender sender) {
        boolean isDraftTimer = ConfigManager.isDraftTimer();
        ConfigManager.setDraftTimer(!isDraftTimer);
        String message = isDraftTimer ? "Draft timer disabled." : "Draft timer enabled.";
        SendMessage.sendToPlayer(sender, message);
    }

    public void handleSecondGetsExtraPlayerCommand(CommandSender sender) {
        boolean isSecondPickBalance = ConfigManager.isSecondPickBalance();
        ConfigManager.setSecondPickBalance(!isSecondPickBalance);
        String message = isSecondPickBalance ? "Second player does not get an extra player." : "Second player gets an extra player.";
        SendMessage.sendToPlayer(sender, message);
    }

    public void setPrivateMatch(CommandSender sender, boolean isPrivateMatch) {
        String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
        ConfigManager.setPrivateMatch(mapName, isPrivateMatch);
        String message = isPrivateMatch ? languageManager.getPluginMessage("privateMatch.true")
        .replace("{map}", mapName)
                : languageManager.getPluginMessage("privateMatch.false")
                .replace("{map}", mapName);
        SendMessage.sendToPlayer(sender, message);
    }

    public void setDraftOrder(CommandSender sender, String order) {
        if (!order.matches("A[AB]+")) { 
            String errorMessage = languageManager.getPluginMessage("draft.invalidOrder");
            SendMessage.sendToPlayer(sender, errorMessage);
            return;
        }
        ConfigManager.setDraftOrder(order);
        String message = languageManager.getPluginMessage("draft.orderSet")
                .replace("{order}", order);
        SendMessage.sendToPlayer(sender, message);
    }

    public void setMinDraftOrder(CommandSender sender, int size) {
        ConfigManager.setMinDraftOrder(size);
        String message = languageManager.getPluginMessage("draft.sizeSet")
                .replace("{size}", String.valueOf(size));
        SendMessage.sendToPlayer(sender, message);
    }
}