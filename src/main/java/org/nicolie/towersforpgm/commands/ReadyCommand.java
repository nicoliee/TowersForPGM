package org.nicolie.towersforpgm.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.utils.SendMessage;

public class ReadyCommand implements CommandExecutor{
    private final TowersForPGM plugin;
    private final Captains captains;
    public ReadyCommand(TowersForPGM plugin, Captains captains) {
        this.plugin = plugin;
        this.captains = captains;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPluginMessage("errors.noPlayer"));
            return true;
        }

        Player player = (Player) sender;

        // Comprobar si el draft está activo
        if (!captains.isReadyActive()) {
            SendMessage.sendToPlayer(player, plugin.getConfigurableMessage("ready.notAvailable"));
            return true;
        }
        if (captains.isCaptain(player.getUniqueId())){
            if (captains.isCaptain1(player.getUniqueId())) {
                if (captains.isReady1()) {
                    SendMessage.sendToPlayer(player, plugin.getConfigurableMessage("ready.alreadyReady"));
                    return true;
                } else {
                    captains.setReady1(true);
                    SendMessage.broadcast(plugin.getConfigurableMessage("ready.ready")
                            .replace("{teamcolor}", plugin.getConfigurableMessage("team.redColor"))
                            .replace("{team}", plugin.getConfigurableMessage("team.red")));
                }
            } else if (captains.isCaptain2(player.getUniqueId())) {
                if (captains.isReady2()) {
                    SendMessage.sendToPlayer(player, plugin.getConfigurableMessage("ready.alreadyReady"));
                    return true;
                } else {
                    captains.setReady2(true);
                    SendMessage.broadcast(plugin.getConfigurableMessage("ready.ready")
                            .replace("{teamcolor}", plugin.getConfigurableMessage("team.blueColor"))
                            .replace("{team}", plugin.getConfigurableMessage("team.blue")));
                }
            }
        }else{
            SendMessage.sendToPlayer(player, plugin.getConfigurableMessage("picks.notCaptain"));
            return true;
        }
        return true;
    }
}
