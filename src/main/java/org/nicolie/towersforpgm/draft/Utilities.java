package org.nicolie.towersforpgm.draft;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.scheduler.BukkitRunnable;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.Sounds;

public class Utilities {
  private final TowersForPGM plugin = TowersForPGM.getInstance();
  private final AvailablePlayers availablePlayers;
  private final Captains captains;

  public Utilities(AvailablePlayers availablePlayers, Captains captains) {
    this.availablePlayers = availablePlayers;
    this.captains = captains;
  }

  public void suggestPicksForCaptains() {
    if (!plugin.config().draft().isDraftSuggestions()) {
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
    StringBuilder suggestionsBuilder = buildLists(topPlayers, "§b", true);
    if (currentCaptain != null) {
      String suggestions = LanguageManager.message("draft.captains.suggestions")
          .replace("{suggestions}", suggestionsBuilder.toString());
      currentCaptain.playSound(Sounds.RAINDROPS);
      currentCaptain.sendMessage(Component.text(suggestions));
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

  public StringBuilder buildLists(List<String> players, String color, boolean useOr) {
    StringBuilder suggestionsBuilder = new StringBuilder();
    for (int i = 0; i < players.size(); i++) {
      suggestionsBuilder.append(color).append(players.get(i));
      if (i < players.size() - 2) {
        suggestionsBuilder.append("§8, ");
      } else if (i == players.size() - 2) {
        suggestionsBuilder.append(" §8");
        if (useOr) {
          suggestionsBuilder.append(LanguageManager.message("system.or"));
        } else {
          suggestionsBuilder.append(LanguageManager.message("system.and"));
        }
        suggestionsBuilder.append(" §b");
      }
    }
    return suggestionsBuilder;
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
    String readyMessage = LanguageManager.message("draft.captains.ready");
    MatchPlayer captain1 = PGM.get().getMatchManager().getPlayer(captains.getCaptain1());
    MatchPlayer captain2 = PGM.get().getMatchManager().getPlayer(captains.getCaptain2());
    if (captain1 != null) {
      captain1.sendActionBar(Component.text(readyMessage));
    }
    if (captain2 != null) {
      captain2.sendActionBar(Component.text(readyMessage));
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
          currentCaptain1.sendMessage(Component.text(readyMessage));
          currentCaptain1.playSound(Sounds.DIRECT_MESSAGE);
        }
        if (!captains.isReady2() && currentCaptain2 != null) {
          currentCaptain2.sendMessage(Component.text(readyMessage));
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
