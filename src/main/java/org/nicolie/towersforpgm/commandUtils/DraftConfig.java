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

    public void handleDraftSuggestionsCommand(CommandSender sender, Boolean isEnabled) {
        if (isEnabled == null){
            boolean isDraftSuggestions = ConfigManager.isDraftSuggestions();
            String message = isDraftSuggestions ? "Draft suggestions are enabled." : "Draft suggestions are disabled.";
            SendMessage.sendToPlayer(sender, message);
            return;
        }
        ConfigManager.setDraftSuggestions(isEnabled);
        String message = isEnabled ? "Draft suggestions enabled." : "Draft suggestions disabled.";
        SendMessage.sendToPlayer(sender, message);
    }

    public void handleDraftTimerCommand(CommandSender sender, Boolean isEnabled) {
        if (isEnabled == null) {
            boolean isDraftTimer = ConfigManager.isDraftTimer();
            String message = isDraftTimer ? "Draft timer is enabled." : "Draft timer is disabled.";
            SendMessage.sendToPlayer(sender, message);
            return;
        }
        ConfigManager.setDraftTimer(isEnabled);
        String message = isEnabled ? "Draft timer enabled." : "Draft timer disabled.";
        SendMessage.sendToPlayer(sender, message);
        
    }

    public void handleSecondGetsExtraPlayerCommand(CommandSender sender, Boolean isEnabled) {
        if (isEnabled == null) {
            boolean secondGetsExtraPlayer = ConfigManager.isSecondPickBalance();
            String message = secondGetsExtraPlayer ? "Second team gets an extra player." : "Second team does not get an extra player.";
            SendMessage.sendToPlayer(sender, message);
            return;
        }
        ConfigManager.setSecondPickBalance(isEnabled);
        String message = isEnabled ? "Second team will get an extra player." : "Second team will not get an extra player.";
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
        if (order == null || order.isEmpty()) {
            String currentOrder = ConfigManager.getDraftOrder();
            String message = languageManager.getPluginMessage("draft.currentOrder")
                    .replace("{order}", currentOrder);
            SendMessage.sendToPlayer(sender, message);
            return;
        }
        if (!order.matches("A[AB]+")) { 
            String errorMessage = languageManager.getPluginMessage("draft.invalidOrder");
            SendMessage.sendToPlayer(sender, errorMessage);
            return;
        }
        ConfigManager.setDraftOrder(order);
        String success = languageManager.getPluginMessage("draft.orderSet")
                .replace("{order}", order);
        SendMessage.sendToPlayer(sender, success);
    }

    public void setMinDraftOrder(CommandSender sender, int size) {
        if (size < 1) {
            int currentSize = ConfigManager.getMinDraftOrder();
            String message = languageManager.getPluginMessage("draft.currentSize")
                    .replace("{size}", String.valueOf(currentSize));
            SendMessage.sendToPlayer(sender, message);
            return;
        }
        ConfigManager.setMinDraftOrder(size);
        String message = languageManager.getPluginMessage("draft.sizeSet")
                .replace("{size}", String.valueOf(size));
        SendMessage.sendToPlayer(sender, message);
    }
}