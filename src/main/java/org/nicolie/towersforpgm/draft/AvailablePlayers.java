package org.nicolie.towersforpgm.draft;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class AvailablePlayers {
    private final List<Player> availablePlayers = new ArrayList<>();
    private final List<String> availableOfflinePlayers = new ArrayList<>();



    public void addPlayer(String playerName) {
        Player player = Bukkit.getPlayerExact(playerName); // nombre exacto para evitar ambigüedades
    
        if (player != null && player.isOnline()) {
            // Evita duplicados en la lista online
            if (availablePlayers.stream().noneMatch(p -> p.getName().equalsIgnoreCase(playerName))) {
                availablePlayers.add(player);
            }
    
            // Por si estaba en la lista offline, lo sacamos
            availableOfflinePlayers.removeIf(name -> name.equalsIgnoreCase(playerName));
    
        } else {
            // Evita duplicados en la lista offline
            if (availableOfflinePlayers.stream().noneMatch(name -> name.equalsIgnoreCase(playerName))) {
                availableOfflinePlayers.add(playerName);
            }
            // Por si estaba en la lista online, lo removemos
            availablePlayers.removeIf(p -> p.getName().equalsIgnoreCase(playerName));
        }
    }

    public void removePlayer(String playerName) {
        availablePlayers.removeIf(p -> p.getName().equalsIgnoreCase(playerName));
        availableOfflinePlayers.removeIf(p -> p.equalsIgnoreCase(playerName));
    }

    public List<Player> getAvailablePlayers() {
        return new ArrayList<>(availablePlayers);
    }

    public List<String> getAvailableOfflinePlayers() {
        return new ArrayList<>(availableOfflinePlayers);
    }

    public List<String> getAllAvailablePlayers() {
        List<String> allAvailablePlayers = new ArrayList<>();
        availablePlayers.forEach(player -> allAvailablePlayers.add(player.getName()));
        allAvailablePlayers.addAll(availableOfflinePlayers);
        return allAvailablePlayers;
    }

    public boolean isEmpty() {
        return availablePlayers.isEmpty() && availableOfflinePlayers.isEmpty();
    }
    
    public void clear() {
        if(availablePlayers == null || availableOfflinePlayers == null) return;
        if(availablePlayers.isEmpty() && availableOfflinePlayers.isEmpty()) return;
        availablePlayers.clear();
        availableOfflinePlayers.clear();
    }

    public void handleDisconnect(Player player) {
        // Remover de online si está
        if (availablePlayers.remove(player)) {
            // Agregar a offline por nombre
            String name = player.getName();
            availableOfflinePlayers.add(name);
        }
    }
    
    public void handleReconnect(Player player) {
        String name = player.getName();
        // Si estaba en la lista de offline, lo quitamos
        if (availableOfflinePlayers.removeIf(p -> p.equalsIgnoreCase(name))) {
            // Lo agregamos como Player online
            availablePlayers.add(player);
        }
    }
    
    
}