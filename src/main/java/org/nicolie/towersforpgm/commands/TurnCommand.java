package org.nicolie.towersforpgm.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;

public class TurnCommand implements CommandExecutor{
    private final Draft draft;
    private final Captains captains;
    private final LanguageManager languageManager;

    public TurnCommand(Draft draft, Captains captains, LanguageManager languageManager) {
        this.captains = captains;
        this.draft = draft;
        this.languageManager = languageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            SendMessage.sendToConsole(languageManager.getPluginMessage("errors.noPlayer"));
            return true;
        }

        if (!draft.isDraftActive()) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("picks.noDraft"));
            return true;
        }
        draft.removeBossbar();
        captains.toggleTurn();
        draft.startDraftTimer();
        String message = languageManager.getConfigurableMessage("captains.turn")
            .replace("{teamcolor}", captains.isCaptain1Turn() ? "&4" : "&9")
            .replace("{captain}", Bukkit.getPlayer(captains.isCaptain1Turn() ? captains.getCaptain1() : captains.getCaptain2()).getName());
        SendMessage.broadcast(message);
        return true;
    }
}
