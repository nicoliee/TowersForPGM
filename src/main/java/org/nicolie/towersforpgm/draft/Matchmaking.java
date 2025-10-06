package org.nicolie.towersforpgm.draft;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.nicolie.towersforpgm.MatchManager;
import org.nicolie.towersforpgm.database.Stats;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.util.bukkit.Sounds;

public class Matchmaking {
  private final Captains captains;
  private final Utilities utilities;
  private final AvailablePlayers availablePlayers;
  private final Teams teams;
  private static boolean isMatchmakingActive = false;

  public Matchmaking(
      AvailablePlayers availablePlayers, Captains captains, Teams teams, Utilities utilities) {
    this.captains = captains;
    this.availablePlayers = availablePlayers;
    this.teams = teams;
    this.utilities = utilities;
  }

  public void startMatchmaking(
      UUID captain1, UUID captain2, List<MatchPlayer> players, Match match) {
    if (MatchManager.getMatch() == null) {
      MatchManager.setCurrentMatch(match);
    }

    // Inicializar estado
    captains.clear();
    availablePlayers.clear();
    teams.clear();
    isMatchmakingActive = true;

    // Registrar capitanes
    captains.setCaptain1(captain1);
    captains.setCaptain2(captain2);

    // Limpiar equipos y agregar capitanes
    teams.removeFromTeams(match);
    teams.addPlayerToTeam(Bukkit.getPlayer(captain1).getName(), 1);
    teams.addPlayerToTeam(Bukkit.getPlayer(captain2).getName(), 2);

    // Registrar jugadores disponibles
    for (MatchPlayer player : players) {
      availablePlayers.addPlayer(player.getNameLegacy());
    }

    // Mensajes iniciales
    match.playSound(Sounds.RAINDROPS);
    match.sendMessage(Component.text(LanguageManager.langMessage("captains.captainsHeader")));
    match.sendMessage(Component.text("&4" + Bukkit.getPlayer(captain1).getName() + " &l&bvs. "
        + "&9" + Bukkit.getPlayer(captain2).getName()));
    match.sendMessage(Component.text("§m---------------------------------"));
    match.sendMessage(Component.text(LanguageManager.message("picks.choosing")));

    // Realizar balance de equipos
    balanceTeams(match);
  }

  private void balanceTeams(Match match) {
    List<String> allPlayers = new ArrayList<>(availablePlayers.getAllAvailablePlayers());

    List<PlayerRating> playerRatings = new ArrayList<>();
    for (String playerName : allPlayers) {
      Stats stats = availablePlayers.getStatsForPlayer(playerName);
      int rating = calculateRating(stats);
      playerRatings.add(new PlayerRating(playerName, rating));
    }

    TeamPartition partition = findBestPartition(playerRatings);

    List<String> team1 = new ArrayList<>();
    List<String> team2 = new ArrayList<>();

    team1.add(captains.getCaptain1Name());
    team2.add(captains.getCaptain2Name());
    team1.addAll(partition.team1);
    team2.addAll(partition.team2);

    // Registrar equipos
    for (String player : team1) {
      teams.addPlayerToTeam(player, 1);
    }
    for (String player : team2) {
      teams.addPlayerToTeam(player, 2);
    }

    // Asignar jugadores a equipos en PGM
    assignPlayersToTeams();

    // Mostrar equipos
    displayTeams(team1, team2);

    // Iniciar countdown y preparar ready
    prepareMatchStart(match);
  }

  private int calculateRating(Stats stats) {
    if (stats == null) return 0;

    int rating = (stats.getElo() == -9999) ? 0 : stats.getElo();

    // KDA
    if (stats.getDeaths() > 0) {
      double kda = (stats.getKills() + stats.getAssists()) / (double) stats.getDeaths();
      rating += (int) (kda * 10);
    }

    // Win rate
    if (stats.getGames() > 0) {
      double winRate = stats.getWins() / (double) stats.getGames();
      rating += (int) (winRate * 100);
    }

    // Puntos
    if (stats.getPoints() > 0) {
      rating += stats.getPoints() / 10;
    }

    // Ratio de daño
    if (stats.getGames() > 0 && stats.getDamageDone() > 0 && stats.getDamageTaken() > 0) {
      double avgDamageDone = stats.getDamageDone() / (double) stats.getGames();
      double avgDamageTaken = stats.getDamageTaken() / (double) stats.getGames();
      double damageRatio = avgDamageDone / avgDamageTaken;
      rating += (int) (damageRatio * 10);
    }

    return rating;
  }

  private TeamPartition findBestPartition(List<PlayerRating> players) {
    if (players.isEmpty()) {
      return new TeamPartition(new ArrayList<>(), new ArrayList<>());
    }

    // Si hay más de 14 jugadores, usar algoritmo híbrido
    if (players.size() > 14) {
      return findHybridPartition(players);
    }

    // Para 14 o menos jugadores, usar fuerza bruta
    return findBruteForcePartition(players);
  }

  private TeamPartition findBruteForcePartition(List<PlayerRating> players) {
    int totalPlayers = players.size();
    int team1Size = calculateTeamSize(totalPlayers, true);
    int team2Size = totalPlayers - team1Size;

    List<List<Integer>> combinations = generateCombinations(players.size(), team1Size);

    if (combinations.size() > 1000) {
      Collections.shuffle(combinations);
      combinations = combinations.subList(0, 1000);
    }

    List<TeamPartition> bestPartitions = new ArrayList<>();
    double bestBalance = Double.MAX_VALUE;

    for (List<Integer> team1Indices : combinations) {
      List<String> team1Names = new ArrayList<>();
      List<String> team2Names = new ArrayList<>();
      int team1Rating = 0;
      int team2Rating = 0;

      for (int i = 0; i < players.size(); i++) {
        PlayerRating player = players.get(i);
        if (team1Indices.contains(i)) {
          team1Names.add(player.name);
          team1Rating += player.rating;
        } else {
          team2Names.add(player.name);
          team2Rating += player.rating;
        }
      }

      if (team1Names.size() != team1Size || team2Names.size() != team2Size) {
        continue;
      }

      double balance = Math.abs(team1Rating - team2Rating);

      if (balance < bestBalance) {
        bestBalance = balance;
        bestPartitions.clear();
        bestPartitions.add(new TeamPartition(team1Names, team2Names));
      } else if (Math.abs(balance - bestBalance) < 0.1) {
        bestPartitions.add(new TeamPartition(team1Names, team2Names));
      }
    }

    if (bestPartitions.isEmpty()) {
      return createFallbackPartition(players);
    }

    // Variabilidad aleatoria: elegir entre las 3 mejores
    Random random = new Random();
    int index = random.nextInt(Math.min(bestPartitions.size(), 3));
    return bestPartitions.get(index);
  }

  private TeamPartition findHybridPartition(List<PlayerRating> allPlayers) {
    // Ordenar jugadores por rating
    List<PlayerRating> sorted = new ArrayList<>(allPlayers);
    sorted.sort((a, b) -> Integer.compare(b.rating, a.rating));

    // Tomar los 14 mejores
    List<PlayerRating> top14 = sorted.subList(0, 14);
    List<PlayerRating> remaining = sorted.subList(14, sorted.size());

    // Aplicar fuerza bruta a los 14 mejores
    TeamPartition top14Partition = findBruteForcePartition(top14);

    // Distribuir restantes secuencialmente
    List<String> team1 = new ArrayList<>(top14Partition.team1);
    List<String> team2 = new ArrayList<>(top14Partition.team2);

    distributeRemaining(remaining, team1, team2, allPlayers.size());

    return new TeamPartition(team1, team2);
  }

  private void distributeRemaining(
      List<PlayerRating> remaining, List<String> team1, List<String> team2, int totalPlayers) {
    int finalTeam1Size = calculateTeamSize(totalPlayers, true);
    int finalTeam2Size = totalPlayers - finalTeam1Size;

    boolean addToTeam1 = true;
    for (PlayerRating player : remaining) {
      if (addToTeam1 && team1.size() < finalTeam1Size) {
        team1.add(player.name);
      } else if (!addToTeam1 && team2.size() < finalTeam2Size) {
        team2.add(player.name);
      } else if (team1.size() < finalTeam1Size) {
        team1.add(player.name);
      } else if (team2.size() < finalTeam2Size) {
        team2.add(player.name);
      }
      addToTeam1 = !addToTeam1;
    }
  }

  private int calculateTeamSize(int totalPlayers, boolean isTeam1) {
    int team1Size = totalPlayers / 2;
    int team2Size = totalPlayers - team1Size;

    // Si es impar, dar el jugador extra al equipo más débil
    if (totalPlayers % 2 == 1) {
      Stats captain1Stats = availablePlayers.getStatsForPlayer(captains.getCaptain1Name());
      Stats captain2Stats = availablePlayers.getStatsForPlayer(captains.getCaptain2Name());

      int captain1Rating = calculateRating(captain1Stats);
      int captain2Rating = calculateRating(captain2Stats);

      if (captain1Rating < captain2Rating) {
        team1Size = (totalPlayers + 1) / 2;
      } else {
        team2Size = (totalPlayers + 1) / 2;
        team1Size = totalPlayers / 2;
      }
    }

    return isTeam1 ? team1Size : team2Size;
  }

  private TeamPartition createFallbackPartition(List<PlayerRating> players) {
    List<String> team1 = new ArrayList<>();
    List<String> team2 = new ArrayList<>();

    boolean addToTeam1 = true;
    for (PlayerRating player : players) {
      if (addToTeam1) {
        team1.add(player.name);
      } else {
        team2.add(player.name);
      }
      addToTeam1 = !addToTeam1;
    }

    return new TeamPartition(team1, team2);
  }

  private List<List<Integer>> generateCombinations(int n, int k) {
    List<List<Integer>> combinations = new ArrayList<>();
    generateCombinationsHelper(combinations, new ArrayList<>(), 0, n, k);
    return combinations;
  }

  private void generateCombinationsHelper(
      List<List<Integer>> combinations, List<Integer> current, int start, int n, int k) {
    if (current.size() == k) {
      combinations.add(new ArrayList<>(current));
      return;
    }

    for (int i = start; i < n; i++) {
      current.add(i);
      generateCombinationsHelper(combinations, current, i + 1, n, k);
      current.remove(current.size() - 1);
    }
  }

  private void assignPlayersToTeams() {
    int maxTeamSize = Math.max(teams.getAllTeam(1).size(), teams.getAllTeam(2).size());
    teams.setTeamsSize(maxTeamSize);

    for (MatchPlayer mp : availablePlayers.getAvailablePlayers()) {
      String playerName = mp.getBukkit().getName();
      if (teams.isPlayerInTeam(playerName, 1)) {
        teams.assignTeam(mp.getBukkit(), 1);
      } else if (teams.isPlayerInTeam(playerName, 2)) {
        teams.assignTeam(mp.getBukkit(), 2);
      }
    }
    availablePlayers.clear();
  }

  private void displayTeams(List<String> team1, List<String> team2) {
    StringBuilder team1Display = utilities.buildLists(team1, "§4", false);
    StringBuilder team2Display = utilities.buildLists(team2, "§9", false);

    SendMessage.broadcast(LanguageManager.langMessage("captains.teamsHeader"));
    SendMessage.broadcast(team1Display.toString());
    SendMessage.broadcast("&8[&4" + team1.size() + "&8] &l&bvs. " + "&8[&9" + team2.size() + "&8]");
    SendMessage.broadcast(team2Display.toString());
    SendMessage.broadcast("§m------------------------------");
  }

  private void prepareMatchStart(Match match) {
    teams.setTeamsSize(Math.max(teams.getAllTeam(1).size(), teams.getAllTeam(2).size()));

    MatchManager.getMatch().playSound(Sounds.MATCH_START);

    captains.setReady1(false, match);
    captains.setReady2(false, match);
    captains.setReadyActive(true);
    captains.setMatchWithCaptains(true);

    String readyMessage = LanguageManager.langMessage("captains.ready");
    MatchPlayer captain1 = PGM.get().getMatchManager().getPlayer(captains.getCaptain1());
    MatchPlayer captain2 = PGM.get().getMatchManager().getPlayer(captains.getCaptain2());
    captain1.sendActionBar(Component.text(readyMessage));
    captain2.sendActionBar(Component.text(readyMessage));

    Match currentMatch = MatchManager.getMatch();
    if (currentMatch != null) {
      StartMatchModule startModule = currentMatch.needModule(StartMatchModule.class);
      if (startModule != null) {
        startModule.forceStartCountdown(Duration.ofSeconds(45), Duration.ZERO);
      }
    }

    utilities.readyReminder(3, 15);
  }

  public boolean isMatchmakingActive() {
    return isMatchmakingActive;
  }

  public void setMatchmakingActive(boolean active) {
    isMatchmakingActive = active;
  }

  private static class TeamPartition {
    final List<String> team1;
    final List<String> team2;

    TeamPartition(List<String> team1, List<String> team2) {
      this.team1 = team1;
      this.team2 = team2;
    }
  }

  private static class PlayerRating {
    final String name;
    final int rating;

    PlayerRating(String name, int rating) {
      this.name = name;
      this.rating = rating;
    }
  }
}
