package org.nicolie.towersforpgm.rankeds;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HashMap;

import tc.oc.pgm.api.player.MatchPlayer;

public class RankedPlayers {
    private final UUID captain1;
    private final UUID captain2;
    private final List<MatchPlayer> remainingPlayers;
    private static final int MAX_HISTORY = 4;
    private static final List<UUID> captainHistory = new LinkedList<>();    
    private static final Map<UUID, Integer> captainCount = new HashMap<>();

    public RankedPlayers(UUID captain1, UUID captain2, List<MatchPlayer> remainingPlayers) {
        this.captain1 = captain1;
        this.captain2 = captain2;
        this.remainingPlayers = new LinkedList<>(remainingPlayers);
        updateHistory(captain1);
        updateHistory(captain2);
    }

    private static void updateHistory(UUID captain) {
        if (captainHistory.size() >= MAX_HISTORY) {
            captainHistory.remove(0);
        }
        captainHistory.add(captain);
        
        captainCount.put(captain, captainCount.getOrDefault(captain, 0) + 1);
    }

    public static RankedPlayers selectCaptains(List<Map.Entry<MatchPlayer, Integer>> players) {
        if (players.size() < 4) {
            return new RankedPlayers(
                players.get(0).getKey().getId(),
                players.get(1).getKey().getId(),
                players.subList(2, players.size()).stream().map(Map.Entry::getKey).collect(Collectors.toList())
            );
        }
        
        // Ordenar por ELO descendente
        players.sort((a, b) -> b.getValue() - a.getValue());
    
        final MatchPlayer[] cap1 = new MatchPlayer[1];
        final MatchPlayer[] cap2 = new MatchPlayer[1];
    
        java.util.Random random = new java.util.Random();
        boolean usarTop2 = random.nextBoolean();
        
        // Obtener jugadores que no han sido capitanes recientemente
        List<Map.Entry<MatchPlayer, Integer>> availablePlayers = players.stream()
            .filter(p -> !captainHistory.contains(p.getKey().getId()))
            .collect(Collectors.toList());
            
        // Si no hay suficientes jugadores disponibles, usar todos
        if (availablePlayers.size() < 2) {
            availablePlayers = new java.util.ArrayList<>(players);
        }
        
        if (usarTop2) {
            // Caso 1: elegir a los 2 con mayor ELO, priorizando jugadores que han sido capitanes menos veces
            availablePlayers.sort((a, b) -> {
                int countA = captainCount.getOrDefault(a.getKey().getId(), 0);
                int countB = captainCount.getOrDefault(b.getKey().getId(), 0);
                if (countA != countB) {
                    return countA - countB; // Priorizar los que han sido capitanes menos veces
                }
                // Si han sido capitanes el mismo número de veces, usar el ELO
                return b.getValue() - a.getValue();
            });
            
            cap1[0] = availablePlayers.get(0).getKey();
            cap2[0] = availablePlayers.get(1).getKey();
        } else {
            // Caso 2: elegir a los 2 más cercanos al promedio, priorizando jugadores que han sido capitanes menos veces
            double avg = players.stream().mapToInt(Map.Entry::getValue).average().orElse(0);
            availablePlayers.sort((a, b) -> {
                int countA = captainCount.getOrDefault(a.getKey().getId(), 0);
                int countB = captainCount.getOrDefault(b.getKey().getId(), 0);
                if (countA != countB) {
                    return countA - countB; // Priorizar los que han sido capitanes menos veces
                }
                // Si han sido capitanes el mismo número de veces, usar la cercanía al promedio
                return Double.compare(
                    Math.abs(a.getValue() - avg),
                    Math.abs(b.getValue() - avg)
                );
            });
            
            cap1[0] = availablePlayers.get(0).getKey();
            cap2[0] = availablePlayers.get(1).getKey();
        }
    
        UUID uuid1 = cap1[0].getId();
        UUID uuid2 = cap2[0].getId();
    
        // Eliminar capitanes de la lista de jugadores restantes
        List<MatchPlayer> remaining = players.stream()
            .map(Map.Entry::getKey)
            .filter(p -> !p.equals(cap1[0]) && !p.equals(cap2[0]))
            .collect(Collectors.toList());
    
        return new RankedPlayers(uuid1, uuid2, remaining);
    }

    public static void clearCaptainHistory() {
        captainHistory.clear();
        captainCount.clear();
    }

    public UUID getCaptain1() {return captain1;}
    public UUID getCaptain2() {return captain2;}
    public List<MatchPlayer> getRemainingPlayers() {return remainingPlayers;}
}
