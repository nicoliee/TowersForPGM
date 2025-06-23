package org.nicolie.towersforpgm.commands;

import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;

import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.StatsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class StatsCommand implements CommandExecutor, TabCompleter {
    private final LanguageManager languageManager;
    private final TowersForPGM plugin = TowersForPGM.getInstance(); // Instancia del plugin

    public StatsCommand(LanguageManager languageManager) {
        this.languageManager = languageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!TowersForPGM.getInstance().getIsDatabaseActivated()){return true;}
        if (!(sender instanceof Player)) {
            SendMessage.sendToConsole(languageManager.getPluginMessage("errors.noPlayer"));
            return true;
        }
        String targetPlayer;
        String table;
        MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer((Player) sender);
        if (args.length == 0) {
            // Si no hay argumentos, se usa el nombre del jugador que ejecutó el comando
            if (!(sender instanceof Player)) {
                matchPlayer.sendWarning(Component.text(languageManager.getPluginMessage("stats.usage")));
                return true;
            }
            targetPlayer = sender.getName();  // Usa al jugador que ejecuta el comando
            if (ConfigManager.getTempTable() != null) {
                table = ConfigManager.getTempTable();
            } else {
                table = ConfigManager.getTableForMap(PGM.get().getMatchManager().getMatch(sender).getMap().getName());
            }
        } else if (args.length == 1) {
            // Si solo hay un argumento, se usa como jugador y la tabla será la del mapa actual
            targetPlayer = args[0].trim();  // Este es el nombre del jugador
            if (ConfigManager.getTempTable() != null) {
                table = ConfigManager.getTempTable();
            } else {
                table = ConfigManager.getTableForMap(PGM.get().getMatchManager().getMatch(sender).getMap().getName());
            }
        } else {
            // Si hay dos argumentos, se usan ambos como jugador y tabla
            targetPlayer = args[0].trim();  // Este es el nombre del jugador
            table = args[1].trim();  // Este es el nombre de la tabla
        }

        // Verifica si la tabla especificada existe en la configuración
        if (!ConfigManager.getTables().contains(table)) {
            matchPlayer.sendWarning(Component.text(languageManager.getPluginMessage("stats.tableNotFound")
                    .replace("{table}", table)));
            return true;
        }

        // Muestra las estadísticas del jugador especificado
        StatsManager.showStats(sender, table, targetPlayer, languageManager);

        return true;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Autocompletar con los nombres de jugadores en línea
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 2) {
            // Autocompletar con las tablas disponibles
            completions = ConfigManager.getTables();
        }

        // Filtrar las opciones basadas en lo que el usuario ha escrito
        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(option -> option.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
    }
}