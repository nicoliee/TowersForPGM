package org.nicolie.towersforpgm.draft;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;

import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.Sounds;

import java.util.ArrayList;
import java.util.List;


public class Utilities {
    private final AvailablePlayers availablePlayers;
    private final Captains captains;
    private final LanguageManager languageManager;

    public Utilities(AvailablePlayers availablePlayers, Captains captains, LanguageManager languageManager) {
        this.availablePlayers = availablePlayers;
        this.captains = captains;
        this.languageManager = languageManager;
    }

    public void suggestPicksForCaptains() {
        if (!ConfigManager.isDraftSuggestions()) {
            return;
        }
        MatchPlayer currentCaptain = PGM.get().getMatchManager().getPlayer(captains.getCurrentCaptain());
        int size = availablePlayers.getAllAvailablePlayers().size();
        List<String> topPlayers = availablePlayers.getTopPlayers();
        if (topPlayers.isEmpty() || size <= 1) {
            return;
        }
        if (size > 6){

            topPlayers = topPlayers.subList(0, Math.min(topPlayers.size(), 3)); // Limitar a los 3 mejores jugadores
            topPlayers = new ArrayList<>(topPlayers.subList(0, 3)); // Limitar a los 3 mejores jugadores
            java.util.Collections.shuffle(topPlayers); // Ordenar aleatoriamente las sugerencias

        }else if (size > 3){

            topPlayers = topPlayers.subList(0, Math.min(topPlayers.size(), 2)); // Limitar a los 2 mejores jugadores
            java.util.Collections.shuffle(topPlayers); // Ordenar aleatoriamente las sugerencias

        } else if (size > 1){

            topPlayers = topPlayers.subList(0, Math.min(topPlayers.size(), 1)); // Limitar a los 1 mejores jugadores

        }
        StringBuilder suggestionsBuilder = buildLists(topPlayers, "§b", true);
        String suggestions = languageManager.getConfigurableMessage("captains.suggestions")
            .replace("{suggestions}", suggestionsBuilder.toString());
        currentCaptain.playSound(Sounds.RAINDROPS);
        currentCaptain.sendMessage(Component.text(suggestions));
    }

    public String randomPick(){
        List<String> topPlayers = availablePlayers.getTopPlayers();
        int size = availablePlayers.getAllAvailablePlayers().size();
        if (topPlayers.isEmpty() || size == 0) {
            return null;
        }
        if (size > 6){
            topPlayers = new ArrayList<>(topPlayers.subList(0, 3)); // Limitar a los 3 mejores jugadores
        }else if (size > 3){
            topPlayers = new ArrayList<>(topPlayers.subList(0, 2)); // Limitar a los 2 mejores jugadores
        }else if (size > 1){
            topPlayers = new ArrayList<>(topPlayers.subList(0, 1)); // Limitar a los 1 mejores jugadores
        }
        java.util.Collections.shuffle(topPlayers); // Ordenar aleatoriamente las sugerencias
        return topPlayers.get(0); // Devolver el primer jugador de la lista aleatoria
    }

    public StringBuilder buildLists(List<String> players, String color, boolean useOr) {
        StringBuilder suggestionsBuilder = new StringBuilder();
        for (int i = 0; i < players.size(); i++) {
            suggestionsBuilder.append(color).append(players.get(i));
            if (i < players.size() - 2) {
                suggestionsBuilder.append("§8, ");
            } else if (i == players.size() - 2) {
                suggestionsBuilder.append(" §8");
                if (useOr) {
                    suggestionsBuilder.append(languageManager.getPluginMessage("TowersForPGM.or"));
                } else {
                    suggestionsBuilder.append(languageManager.getPluginMessage("TowersForPGM.and"));
                }
                suggestionsBuilder.append(" §b");
            }
        }
        return suggestionsBuilder;
    }

    public int timerDuration() {
        int size = availablePlayers.getAllAvailablePlayers().size();
        if (size >= 22) {
            return 180; // 3 minutos
        } else if (size >= 14) {
            return 120; // 2 minutos
        } else if (size >= 7) {
            return 90; // 1 minuto y 30 segundos
        } else if (size >= 4) {
            return 60; // 1 minuto
        } else if (size >= 2 ) {
            return 35; // 35 segundos
        } else if (size == 1){
            return 0; // 1 segundo
        } else {
            return 30; // 30 segundos
        }
    }
}
