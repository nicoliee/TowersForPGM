package org.nicolie.towersforpgm.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.commandUtils.StatsConfig;
import org.nicolie.towersforpgm.utils.LanguageManager;

import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;


public class CancelMatchCommand implements CommandExecutor{
    private final StatsConfig statsConfig;
    private String message = "§cEl juego ha sido cancelado por el administrador.";
    public CancelMatchCommand(LanguageManager languageManager) {
        this.statsConfig = new StatsConfig(languageManager);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        statsConfig.toggleStats(sender);
        Match match = PGM.get().getMatchManager().getMatch(sender);
        match.sendWarning(Component.text(message));
        if (match.isRunning()){
            sender.getServer().dispatchCommand(sender.getServer().getConsoleSender(), "end");
        }else{
            sender.getServer().dispatchCommand(sender.getServer().getConsoleSender(), "cycle 5");
        }
        return true;
    }
}