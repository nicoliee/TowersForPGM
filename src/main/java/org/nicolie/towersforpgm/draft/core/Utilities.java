package org.nicolie.towersforpgm.draft.core;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.nicolie.towersforpgm.TowersForPGM;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.Sounds;

public class Utilities {
  private final TowersForPGM plugin = TowersForPGM.getInstance();
  private final org.nicolie.towersforpgm.configs.ConfigManager configManager;
  private final AvailablePlayers availablePlayers;
  private final Captains captains;

  public Utilities(
      org.nicolie.towersforpgm.configs.ConfigManager configManager,
      AvailablePlayers availablePlayers,
      Captains captains) {
    this.configManager = configManager;
    this.availablePlayers = availablePlayers;
    this.captains = captains;
  }

  public void suggestPicksForCaptains() {
    if (!configManager.draft().isDraftSuggestions()) {
      return;
    }
    if (!TowersForPGM.getInstance().getIsDatabaseActivated()) {
      return;
    } // No sugerencias si la base de datos no está activada
    MatchPlayer currentCaptain =
        PGM.get().getMatchManager().getPlayer(captains.getCurrentCaptain());
    int size = availablePlayers.getAllAvailablePlayers().size();
    List<String> topPlayers = availablePlayers.getTopPlayers();
    if (topPlayers.isEmpty() || size <= 1) {
      return;
    }
    if (size > 6) {

      topPlayers = topPlayers.subList(
          0, Math.min(topPlayers.size(), 3)); // Limitar a los 3 mejores jugadores
      topPlayers = new ArrayList<>(topPlayers.subList(0, 3)); // Limitar a los 3 mejores jugadores
      java.util.Collections.shuffle(topPlayers); // Ordenar aleatoriamente las sugerencias

    } else if (size > 3) {

      topPlayers = topPlayers.subList(
          0, Math.min(topPlayers.size(), 2)); // Limitar a los 2 mejores jugadores
      java.util.Collections.shuffle(topPlayers); // Ordenar aleatoriamente las sugerencias

    } else if (size > 1) {

      topPlayers = topPlayers.subList(
          0, Math.min(topPlayers.size(), 1)); // Limitar a los 1 mejores jugadores
    }
    Component suggestionsComponent = buildLists(topPlayers, NamedTextColor.AQUA, true);
    if (currentCaptain != null) {

      Component suggestions = Component.translatable(
              "draft.captains.suggestion", suggestionsComponent)
          .color(NamedTextColor.GRAY);
      currentCaptain.playSound(Sounds.ITEM_PICKUP);
      currentCaptain.sendMessage(suggestions);
    }
  }

  public String randomPick() {
    int size = availablePlayers.getAllAvailablePlayers().size();
    // Si la base de datos está desactivada, devolver un usuario aleatorio de los disponibles
    if (!TowersForPGM.getInstance().getIsDatabaseActivated()) {
      List<String> allPlayers = new ArrayList<>(availablePlayers.getAllAvailablePlayers());
      if (allPlayers.isEmpty()) {
        return null;
      }
      java.util.Collections.shuffle(allPlayers);
      return allPlayers.get(0);
    }
    List<String> topPlayers = availablePlayers.getTopPlayers();
    if (topPlayers.isEmpty() || size == 0) {
      return null;
    }
    if (size > 6) {
      topPlayers = new ArrayList<>(topPlayers.subList(0, 3)); // Limitar a los 3 mejores jugadores
    } else if (size > 3) {
      topPlayers = new ArrayList<>(topPlayers.subList(0, 2)); // Limitar a los 2 mejores jugadores
    } else if (size <= 3) {
      topPlayers = new ArrayList<>(topPlayers.subList(0, 1)); // Limitar a los 1 mejores jugadores
    }
    java.util.Collections.shuffle(topPlayers); // Ordenar aleatoriamente las sugerencias
    return topPlayers.get(0); // Devolver el primer jugador de la lista aleatoria
  }

  public Component buildLists(List<String> players, NamedTextColor color, boolean useOr) {
    if (players.isEmpty()) {
      return Component.empty();
    }

    if (players.size() == 1) {
      return Component.text(players.get(0), color);
    }

    if (players.size() == 2) {
      String key = useOr ? "misc.or" : "misc.list.pair";
      return Component.translatable(
          key, Component.text(players.get(0), color), Component.text(players.get(1), color));
    }

    // Para 3 o más elementos
    Component result = Component.text(players.get(0), color);
    for (int i = 1; i < players.size(); i++) {
      if (i == players.size() - 1) {
        // Último elemento
        String key = useOr ? "misc.or" : "misc.list.end";
        result = Component.translatable(key, result, Component.text(players.get(i), color));
      } else {
        // Elementos del medio
        result = Component.translatable(
            "misc.list.middle", result, Component.text(players.get(i), color));
      }
    }
    return result;
  }

  private static final int DEFAULT_DURATION = 20;
  private static final int[][] DURATION_TABLE = {
    {14, 50},
    {8, 40},
    {4, 30},
    {2, 20},
    {1, 0}
  };

  public int timerDuration() {
    int size = availablePlayers.getAllAvailablePlayers().size();
    for (int[] entry : DURATION_TABLE) {
      if (size >= entry[0]) return entry[1];
    }
    return DEFAULT_DURATION;
  }

  private static BukkitRunnable readyReminderTask;

  public void readyReminder(int delay, int period) {
    Component readyMessage = Component.translatable("draft.ready.tip", Component.text("/ready"))
        .color(NamedTextColor.GOLD)
        .color(NamedTextColor.AQUA);
    MatchPlayer captain1 = PGM.get().getMatchManager().getPlayer(captains.getCaptain1());
    MatchPlayer captain2 = PGM.get().getMatchManager().getPlayer(captains.getCaptain2());
    if (captain1 != null) {
      captain1.sendActionBar(readyMessage);
    }
    if (captain2 != null) {
      captain2.sendActionBar(readyMessage);
    }
    readyReminderTask = new BukkitRunnable() {
      @Override
      public void run() {
        if (!captains.isReadyActive()) {
          this.cancel();
          return;
        }
        MatchPlayer currentCaptain1 = PGM.get().getMatchManager().getPlayer(captains.getCaptain1());
        MatchPlayer currentCaptain2 = PGM.get().getMatchManager().getPlayer(captains.getCaptain2());
        if (!captains.isReady1() && currentCaptain1 != null) {
          currentCaptain1.sendMessage(readyMessage);
          currentCaptain1.playSound(Sounds.DIRECT_MESSAGE);
        }
        if (!captains.isReady2() && currentCaptain2 != null) {
          currentCaptain2.sendMessage(readyMessage);
          currentCaptain2.playSound(Sounds.DIRECT_MESSAGE);
        }
      }
    };
    readyReminderTask.runTaskTimer(TowersForPGM.getInstance(), delay * 20, period * 20);
  }

  public static void cancelReadyReminder() {
    readyReminderTask.cancel();
    readyReminderTask = null;
  }
}
