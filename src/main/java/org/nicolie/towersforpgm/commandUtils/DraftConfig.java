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

    public void setPrivateMatch(CommandSender sender, boolean isPrivateMatch) {
        String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName(); // Obtener el mapa actual
        ConfigManager.setPrivateMatch(mapName, isPrivateMatch);
        String message = isPrivateMatch ? languageManager.getPluginMessage("privateMatch.true")
        .replace("{map}", mapName)
                : languageManager.getPluginMessage("privateMatch.false")
                .replace("{map}", mapName);
        SendMessage.sendToPlayer(sender, message);
    }
}