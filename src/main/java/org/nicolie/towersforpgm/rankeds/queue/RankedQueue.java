package org.nicolie.towersforpgm.rankeds.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RankedQueue {

  private static final RankedQueue INSTANCE = new RankedQueue();

  private final List<UUID> players = new ArrayList<>();

  private RankedQueue() {}

  public static RankedQueue getInstance() {
    return INSTANCE;
  }

  public boolean contains(UUID uuid) {
    return players.contains(uuid);
  }

  public boolean add(UUID uuid) {
    if (players.contains(uuid)) return false;
    players.add(uuid);
    return true;
  }

  public boolean remove(UUID uuid) {
    return players.remove(uuid);
  }

  public int size() {
    return players.size();
  }

  /** Devuelve una copia defensiva para iterar sin riesgo de ConcurrentModification. */
  public List<UUID> snapshot() {
    return new ArrayList<>(players);
  }

  /** Primeros {@code count} jugadores de la cola. */
  public List<UUID> take(int count) {
    return new ArrayList<>(players.subList(0, Math.min(count, players.size())));
  }

  public void clear() {
    players.clear();
  }
}
