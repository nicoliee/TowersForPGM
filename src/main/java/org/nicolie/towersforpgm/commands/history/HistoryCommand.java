package org.nicolie.towersforpgm.commands.history;

import java.util.List;

import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.tables.TableType;

import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.*;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.suggestion.Suggestions;
import tc.oc.pgm.util.Audience;

public class HistoryCommand {
    private final TowersForPGM plugin = TowersForPGM.getInstance();
    @Command("history [player] [table]")
    @CommandDescription("View match history for a player")
    public void history(Audience audience, MatchPlayer sender, @Argument("player") String target, @Argument("table") String table) {
        //String map = sender.getMatch().getMap().getName();
        //TODO Si no se especifica un jugador, mostrar el historial del jugador que ejecuta el comando
        //TODO Si no se especifica una tabla, se usará plugin.config().databaseTables().getTable(map)
    }

    @Suggestions("onlinePlayers")
    public List<String> suggestOnlinePlayers(Player sender) {
        return sender.getWorld().getPlayers().stream()
                .map(Player::getName)
                .toList();
    }

    @Suggestions("tables")
    public List<String> suggestTables(Player sender) {
        return List.copyOf(plugin.config().databaseTables().getTables(TableType.ALL));
    }
}
