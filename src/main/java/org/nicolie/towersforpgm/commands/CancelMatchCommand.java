package org.nicolie.towersforpgm.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.commands.commandUtils.StatsConfig;
import org.nicolie.towersforpgm.matchbot.listeners.RankedListener;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;

public class CancelMatchCommand implements CommandExecutor {
  private final StatsConfig statsConfig;

  public CancelMatchCommand() {
    this.statsConfig = new StatsConfig();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    // Check if test parameter is provided
    if (args.length > 0 && args[0].equalsIgnoreCase("test")) {
      // TODO: Placeholder para funciones de prueba
      // Aquí puedes llamar las funciones que quieras probar
      sender.sendMessage("§a[DEBUG] Modo de prueba activado - Agrega aquí tus funciones de test");

      // Ejemplo de como agregar funciones de prueba:
      if (sender instanceof Player) {
        Player player = (Player) sender;
        RankedListener.movePlayerToInactive(player.getUniqueId());
      }

      return true;
    }

    // Comportamiento normal del comando
    statsConfig.toggleStats(sender);
    Match match = PGM.get().getMatchManager().getMatch(sender);
    match.sendWarning(Component.text(
        org.nicolie.towersforpgm.utils.LanguageManager.langMessage("system.matchCancelled")));
    if (match.isRunning()) {
      sender.getServer().dispatchCommand(sender.getServer().getConsoleSender(), "end");
    } else {
      sender.getServer().dispatchCommand(sender.getServer().getConsoleSender(), "cycle 5");
    }
    return true;
  }
}
