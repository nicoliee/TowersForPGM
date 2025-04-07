package org.nicolie.towersforpgm.commands;

import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.MatchManager;
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
    private final TowersForPGM plugin;
    private final MatchManager matchManager;

    public StatsCommand(TowersForPGM plugin, MatchManager matchManager) {
        this.plugin = plugin;
        this.matchManager = matchManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String targetPlayer;
        String tableName;
        
        if (!plugin.getIsDatabaseActivated()){
            sender.sendMessage(plugin.getPluginMessage("stats.disabled"));
            return true;
        }
        // Verifica si no se proporciona ningún argumento
        if (args.length == 0) {
            // Si no hay argumentos, se usa el nombre del jugador que ejecutó el comando
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getPluginMessage("stats.usage"));
                return true;
            }
            targetPlayer = sender.getName();  // Usa al jugador que ejecuta el comando
            tableName = ConfigManager.getTableForMap(matchManager.getMatch().getMap().getName()); // Usa la tabla por defecto
        } else if (args.length == 1) {
            // Si solo hay un argumento, se usa como tabla y el jugador será el que ejecuta el comando
            tableName = args[0].trim();  // Este es el nombre de la tabla
            targetPlayer = sender.getName();  // Usa al jugador que ejecuta el comando
        } else {
            // Si hay dos argumentos, se usan ambos como tabla y jugador
            tableName = args[0].trim();  // Este es el nombre de la tabla
            targetPlayer = args[1].trim();  // Este es el nombre del jugador
        }

        // Verifica si la tabla especificada existe en la configuración
        if (!ConfigManager.getTables().contains(tableName)) {
            sender.sendMessage(plugin.getPluginMessage("stats.tableNotFound")
                    .replace("{table}", tableName));
            return true;
        }

        // Muestra las estadísticas del jugador especificado
        StatsManager.showStats(sender, tableName, targetPlayer, this.plugin);

        return true;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Autocompletar con las tablas disponibles
            completions = ConfigManager.getTables(); 
        } else if (args.length == 2) {
            // Autocompletar con los nombres de jugadores en línea
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        
        // Filtrar las opciones basadas en lo que el usuario ha escrito
        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(option -> option.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
    }
}