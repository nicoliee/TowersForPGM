package org.nicolie.towersforpgm.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;

public class ReadyCommand implements CommandExecutor{
    private final Captains captains;
    private final LanguageManager languageManager;
    public ReadyCommand(Captains captains, LanguageManager languageManager) {
        this.captains = captains;
        this.languageManager = languageManager;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(languageManager.getPluginMessage("errors.noPlayer"));
            return true;
        }

        Player player = (Player) sender;

        // Comprobar si el draft está activo
        if (!captains.isReadyActive()) {
            SendMessage.sendToPlayer(player, languageManager.getConfigurableMessage("ready.notAvailable"));
            return true;
        }
        if (captains.isCaptain(player.getUniqueId())){
            if (captains.isCaptain1(player.getUniqueId())) {
                if (captains.isReady1()) {
                    SendMessage.sendToPlayer(player, languageManager.getConfigurableMessage("ready.alreadyReady"));
                    return true;
                } else {
                    captains.setReady1(true);
                    SendMessage.broadcast(languageManager.getConfigurableMessage("ready.ready")
                            .replace("{teamcolor}", languageManager.getConfigurableMessage("team.redColor"))
                            .replace("{team}", languageManager.getConfigurableMessage("team.red")));
                }
            } else if (captains.isCaptain2(player.getUniqueId())) {
                if (captains.isReady2()) {
                    SendMessage.sendToPlayer(player, languageManager.getConfigurableMessage("ready.alreadyReady"));
                    return true;
                } else {
                    captains.setReady2(true);
                    SendMessage.broadcast(languageManager.getConfigurableMessage("ready.ready")
                            .replace("{teamcolor}", languageManager.getConfigurableMessage("team.blueColor"))
                            .replace("{team}", languageManager.getConfigurableMessage("team.blue")));
                }
            }
        }else{
            SendMessage.sendToPlayer(player, languageManager.getConfigurableMessage("picks.notCaptain"));
            return true;
        }
        return true;
    }
}
