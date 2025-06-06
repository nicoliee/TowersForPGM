package org.nicolie.towersforpgm.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.utils.LanguageManager;

import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

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
        MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
        // Comprobar si el draft está activo
        if (!captains.isReadyActive()) {
            matchPlayer.sendWarning(Component.text(languageManager.getPluginMessage("ready.notAvailable")));
            return true;
        }
        if (captains.isCaptain(player.getUniqueId())) {
            boolean isCaptain1 = captains.isCaptain1(player.getUniqueId());
            boolean alreadyReady = isCaptain1 ? captains.isReady1() : captains.isReady2();

            if (alreadyReady) {
                matchPlayer.sendWarning(Component.text(languageManager.getPluginMessage("ready.alreadyReady")));
                return true;
            }
            if (isCaptain1) {
                captains.setReady1(true, PGM.get().getMatchManager().getMatch(player));
            } else {
                captains.setReady2(true, PGM.get().getMatchManager().getMatch(player));
            }
            String readyMessage = getReadyMessage(matchPlayer);
            matchPlayer.getMatch().sendMessage(Component.text(readyMessage));
        } else {
            matchPlayer.sendWarning(Component.text(languageManager.getConfigurableMessage("picks.notCaptain")));
            return true;
        }
        return true;
    }

    public static String getReadyMessage(MatchPlayer matchPlayer) {
        String teamColor = matchPlayer.getParty().getColor().toString();
        String teamName = matchPlayer.getParty().getNameLegacy();
        return TowersForPGM.getInstance().getLanguageManager().getPluginMessage("ready.ready")
                .replace("{teamcolor}", teamColor)
                .replace("{team}", teamName);
    }
}