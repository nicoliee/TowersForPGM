package org.nicolie.towersforpgm.draft;

import java.time.Duration;
import java.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
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
  private final LanguageManager languageManager;

  public Matchmaking(
      AvailablePlayers availablePlayers,
      Captains captains,
      LanguageManager languageManager,
      Teams teams,
      Utilities utilities) {
    this.captains = captains;
    this.availablePlayers = availablePlayers;
    this.teams = teams;
    this.languageManager = languageManager;
    this.utilities = utilities;
  }

  public void startMatchmaking(List<MatchPlayer> players, Match match) {
    if (MatchManager.getMatch() == null) {
      MatchManager.setCurrentMatch(match);
    }

    if (players.size() < 4) {
      SendMessage.sendToConsole(languageManager.getPluginMessage("matchmaking.not-enough-players"));
      return;
    }

    captains.clear();
    availablePlayers.clear();
    teams.clear();
    teams.removeFromTeams(match);

    for (MatchPlayer player : players) {
      availablePlayers.addPlayer(player.getNameLegacy());
    }
    captainsBalance(players, match);
  }

  private void captainsBalance(List<MatchPlayer> players, Match match) {
    List<PlayerStatsPair> playerRatings = new ArrayList<>();

    for (MatchPlayer player : players) {
      Stats stats = availablePlayers.getStatsForPlayer(player.getNameLegacy());
      int rating = calculatePlayerRating(stats);
      playerRatings.add(new PlayerStatsPair(player.getNameLegacy(), rating));
    }

    // Usar algoritmo de partición por fuerza bruta para seleccionar capitanes
    PlayerStatsPair[] bestCaptains = findBestCaptainsPair(playerRatings);

    if (bestCaptains.length >= 2) {
      String captain1Name = bestCaptains[0].getPlayerName();
      String captain2Name = bestCaptains[1].getPlayerName();

      for (MatchPlayer player : players) {
        String name = player.getNameLegacy();
        if (name.equals(captain1Name) || name.equals(captain2Name)) {
          int team = name.equals(captain1Name) ? 1 : 2;
          if (team == 1) {
            captains.setCaptain1(player.getId());
          } else {
            captains.setCaptain2(player.getId());
          }
          teams.addPlayerToTeam(name, team);
          teams.assignTeam(player.getBukkit(), team);
        }
      }

      availablePlayers.removePlayer(captain1Name);
      availablePlayers.removePlayer(captain2Name);
      match.playSound(Sounds.MATCH_COUNTDOWN);
      SendMessage.broadcast(languageManager.getPluginMessage("captains.captainsHeader"));
      SendMessage.broadcast("&4" + captain1Name + " &l&bvs. " + "&9" + captain2Name);
      SendMessage.broadcast("§m---------------------------------");
      match.sendMessage(Component.text(languageManager.getConfigurableMessage("picks.choosing")));
      teamBalance(match);
    }
  }

  private void teamBalance(Match match) {
    List<String> allPlayers = new ArrayList<>();
    for (MatchPlayer mp : availablePlayers.getAvailablePlayers()) {
      allPlayers.add(mp.getNameLegacy());
    }
    allPlayers.addAll(availablePlayers.getAvailableOfflinePlayers());

    List<PlayerStatsPair> playerStats = new ArrayList<>();
    for (String playerName : allPlayers) {
      Stats stats = availablePlayers.getStatsForPlayer(playerName);
      int rating = calculatePlayerRating(stats);
      playerStats.add(new PlayerStatsPair(playerName, rating));
    }

    // Usar algoritmo de partición por fuerza bruta con variabilidad aleatoria
    TeamPartition bestPartition = findBestTeamPartition(playerStats);

    // Agregar los capitanes al inicio de cada equipo
    String captain1Name = captains.getCaptain1Name();
    String captain2Name = captains.getCaptain2Name();

    List<String> team1Names = new ArrayList<>();
    List<String> team2Names = new ArrayList<>();

    if (captain1Name != null) {
      team1Names.add(captain1Name);
    }
    if (captain2Name != null) {
      team2Names.add(captain2Name);
    }

    // Agregar el resto de los jugadores (sin los capitanes)
    for (String name : bestPartition.getTeam1()) {
      if (!name.equals(captain1Name) && !name.equals(captain2Name)) {
        team1Names.add(name);
      }
    }
    for (String name : bestPartition.getTeam2()) {
      if (!name.equals(captain1Name) && !name.equals(captain2Name)) {
        team2Names.add(name);
      }
    }

    // Finalizar el balance de equipos
    finalizaTeamBalance(team1Names, team2Names, match);
  }

  private void finalizaTeamBalance(List<String> team1Names, List<String> team2Names, Match match) {
    for (String player : team1Names) {
      teams.addPlayerToTeam(player, 1);
    }
    for (String player : team2Names) {
      teams.addPlayerToTeam(player, 2);
    }

    assignPlayersToTeams();

    StringBuilder team1 = utilities.buildLists(team1Names, "§4", false);
    StringBuilder team2 = utilities.buildLists(team2Names, "§9", false);
    int team1Size = team1Names.size();
    int team2Size = team2Names.size();
    int teamsize = Math.max(team1Size, team2Size);

    SendMessage.broadcast(languageManager.getPluginMessage("captains.teamsHeader"));
    SendMessage.broadcast(team1.toString());
    SendMessage.broadcast("&8[&4" + team1Size + "&8] &l&bvs. " + "&8[&9" + team2Size + "&8]");
    SendMessage.broadcast(team2.toString());
    SendMessage.broadcast("§m------------------------------");
    teams.setTeamsSize(teamsize);
    MatchManager.getMatch().playSound(Sounds.MATCH_START);
    captains.setReady1(false, match);
    captains.setReady2(false, match);
    captains.setReadyActive(true);
    captains.setMatchWithCaptains(true);
    String readyMessage = languageManager.getPluginMessage("captains.ready");
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

  private void assignPlayersToTeams() {
    int team1Size = teams.getAllTeam(1).size();
    int team2Size = teams.getAllTeam(2).size();
    int maxTeamSize = Math.max(team1Size, team2Size);

    teams.setTeamsSize(maxTeamSize);

    for (MatchPlayer mp : availablePlayers.getAvailablePlayers()) {
      Player player = mp.getBukkit();
      if (teams.isPlayerInTeam(player.getName(), 1)) {
        teams.assignTeam(player, 1);
      } else if (teams.isPlayerInTeam(player.getName(), 2)) {
        teams.assignTeam(player, 2);
      }
    }
  }

  private int calculatePlayerRating(Stats stats) {
    if (stats == null) {
      return 0;
    }

    int rating;
    if (stats.getElo() == -9999) {
      rating = 0;
    } else {
      rating = stats.getElo();
    }

    if (stats.getDeaths() > 0) {
      double kda = (stats.getKills() + stats.getAssists()) / (double) stats.getDeaths();
      rating += (int) (kda * 10);
    }

    if (stats.getGames() > 0) {
      double winRate = stats.getWins() / (double) stats.getGames();
      rating += (int) (winRate * 100);
    }

    if (stats.getPoints() > 0) {
      rating += stats.getPoints() / 10; // Ajuste por puntos obtenidos
    }

    if (stats.getGames() > 0 && stats.getDamageDone() > 0 && stats.getDamageTaken() > 0) {
      double avgDamageDone = stats.getDamageDone() / (double) stats.getGames();
      double avgDamageTaken = stats.getDamageTaken() / (double) stats.getGames();
      double damageRatio = avgDamageDone / avgDamageTaken;
      rating += (int) (damageRatio * 10); // Ajuste por ratio de daño por partida
    }

    return rating;
  }

  /** Encuentra la mejor pareja de capitanes usando fuerza bruta con variabilidad aleatoria */
  private PlayerStatsPair[] findBestCaptainsPair(List<PlayerStatsPair> players) {
    if (players.size() < 2) {
      return new PlayerStatsPair[0];
    }

    List<PlayerStatsPair[]> validPairs = new ArrayList<>();

    // Generar todas las combinaciones posibles de capitanes
    for (int i = 0; i < players.size(); i++) {
      for (int j = i + 1; j < players.size(); j++) {
        PlayerStatsPair captain1 = players.get(i);
        PlayerStatsPair captain2 = players.get(j);

        // Crear una copia de la lista sin los capitanes para evaluar el resto
        List<PlayerStatsPair> remainingPlayers = new ArrayList<>(players);
        remainingPlayers.removeIf(p -> p.getPlayerName().equals(captain1.getPlayerName())
            || p.getPlayerName().equals(captain2.getPlayerName()));

        // Evaluar si esta pareja de capitanes genera un buen balance
        if (isGoodCaptainPair(captain1, captain2, remainingPlayers)) {
          validPairs.add(new PlayerStatsPair[] {captain1, captain2});
        }
      }
    }

    // Si no hay pares válidos, usar los dos mejores jugadores
    if (validPairs.isEmpty()) {
      players.sort((a, b) -> Integer.compare(b.getRating(), a.getRating()));
      return new PlayerStatsPair[] {players.get(0), players.get(1)};
    }

    // Seleccionar aleatoriamente entre los mejores pares válidos (variabilidad aleatoria parcial)
    Random random = new Random();
    int randomIndex = random.nextInt(Math.min(validPairs.size(), 3)); // Tomar entre los 3 mejores
    return validPairs.get(randomIndex);
  }

  /** Evalúa si una pareja de capitanes puede generar un buen balance */
  private boolean isGoodCaptainPair(
      PlayerStatsPair captain1, PlayerStatsPair captain2, List<PlayerStatsPair> remainingPlayers) {
    int ratingDifference = Math.abs(captain1.getRating() - captain2.getRating());
    int totalRemainingRating =
        remainingPlayers.stream().mapToInt(PlayerStatsPair::getRating).sum();

    // Los capitanes no deben tener una diferencia muy grande
    if (ratingDifference > totalRemainingRating * 0.3) {
      return false;
    }

    return true;
  }

  /**
   * Encuentra la mejor partición de equipos usando fuerza bruta con variabilidad aleatoria Para más
   * de 14 jugadores, aplica fuerza bruta solo a los 14 mejores y distribuye el resto
   * secuencialmente
   */
  private TeamPartition findBestTeamPartition(List<PlayerStatsPair> players) {
    if (players.size() <= 2) {
      List<String> team1 = new ArrayList<>();
      List<String> team2 = new ArrayList<>();
      if (players.size() >= 1) team1.add(players.get(0).getPlayerName());
      if (players.size() >= 2) team2.add(players.get(1).getPlayerName());
      return new TeamPartition(team1, team2);
    }

    // Si hay más de 14 jugadores, usar algoritmo híbrido
    if (players.size() > 14) {
      return findHybridTeamPartition(players);
    }

    List<TeamPartition> bestPartitions = new ArrayList<>();
    double bestBalance = Double.MAX_VALUE;

    // Determinar tamaños de equipos
    int totalPlayers = players.size();
    int team1Size = totalPlayers / 2;
    int team2Size = totalPlayers - team1Size;

    // TODO: Asegurar que si hay números impares, el equipo más débil (determinado por team entero)
    // tenga el jugador extra
    String captain1Name = captains.getCaptain1Name();
    String captain2Name = captains.getCaptain2Name();

    if (totalPlayers % 2 == 1 && captain1Name != null && captain2Name != null) {
      PlayerStatsPair captain1Stats = players.stream()
          .filter(p -> p.getPlayerName().equals(captain1Name))
          .findFirst()
          .orElse(null);
      PlayerStatsPair captain2Stats = players.stream()
          .filter(p -> p.getPlayerName().equals(captain2Name))
          .findFirst()
          .orElse(null);

      if (captain1Stats != null && captain2Stats != null) {
        if (captain1Stats.getRating() < captain2Stats.getRating()) {
          team1Size = (totalPlayers + 1) / 2; // Team 1 (más débil) recibe el jugador extra
          team2Size = totalPlayers - team1Size;
        } else {
          team1Size = totalPlayers / 2;
          team2Size = (totalPlayers + 1) / 2; // Team 2 (más débil) recibe el jugador extra
        }
      }
    }

    // Generar combinaciones (limitadas para evitar explosión exponencial)
    List<List<Integer>> combinations = generateCombinations(players.size(), team1Size);

    // Limitar las combinaciones para evitar lag (máximo 1000 iteraciones)
    if (combinations.size() > 1000) {
      Collections.shuffle(combinations);
      combinations = combinations.subList(0, 1000);
    }

    for (List<Integer> team1Indices : combinations) {
      List<String> team1Names = new ArrayList<>();
      List<String> team2Names = new ArrayList<>();
      int team1Rating = 0;
      int team2Rating = 0;

      for (int i = 0; i < players.size(); i++) {
        PlayerStatsPair player = players.get(i);
        if (team1Indices.contains(i)) {
          team1Names.add(player.getPlayerName());
          team1Rating += player.getRating();
        } else {
          team2Names.add(player.getPlayerName());
          team2Rating += player.getRating();
        }
      }

      // Validar que los tamaños de equipo sean correctos
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

    // Variabilidad aleatoria parcial: elegir entre las mejores particiones
    if (bestPartitions.isEmpty()) {
      return generateFallbackPartition(players);
    }

    Random random = new Random();
    int randomIndex = random.nextInt(Math.min(bestPartitions.size(), 3));
    return bestPartitions.get(randomIndex);
  }

  /**
   * Algoritmo híbrido para más de 14 jugadores: - Aplica fuerza bruta a los 14 mejores jugadores -
   * Distribuye el resto secuencialmente manteniendo balance de tamaños
   */
  private TeamPartition findHybridTeamPartition(List<PlayerStatsPair> allPlayers) {
    // Ordenar jugadores por rating (mejor a peor)
    List<PlayerStatsPair> sortedPlayers = new ArrayList<>(allPlayers);
    sortedPlayers.sort((a, b) -> Integer.compare(b.getRating(), a.getRating()));

    // Tomar los 14 mejores para fuerza bruta
    List<PlayerStatsPair> top14Players = sortedPlayers.subList(0, 14);
    List<PlayerStatsPair> remainingPlayers = sortedPlayers.subList(14, sortedPlayers.size());

    // Aplicar fuerza bruta a los 14 mejores
    TeamPartition top14Partition = findBestTeamPartitionForTop14(top14Players);

    // Obtener las listas iniciales de los equipos
    List<String> team1Names = new ArrayList<>(top14Partition.getTeam1());
    List<String> team2Names = new ArrayList<>(top14Partition.getTeam2());

    // Distribuir jugadores restantes de manera secuencial
    distributeRemainingPlayers(remainingPlayers, team1Names, team2Names, allPlayers.size());

    return new TeamPartition(team1Names, team2Names);
  }

  /** Aplica el algoritmo de fuerza bruta específicamente a los 14 mejores jugadores */
  private TeamPartition findBestTeamPartitionForTop14(List<PlayerStatsPair> players) {
    List<TeamPartition> bestPartitions = new ArrayList<>();
    double bestBalance = Double.MAX_VALUE;

    // Determinar tamaños de equipos para los 14 mejores (7 por equipo)
    int team1Size = 7;
    int team2Size = 7;

    // Generar combinaciones (limitadas para evitar explosión exponencial)
    List<List<Integer>> combinations = generateCombinations(players.size(), team1Size);

    // Limitar las combinaciones para evitar lag (máximo 1000 iteraciones)
    if (combinations.size() > 1000) {
      Collections.shuffle(combinations);
      combinations = combinations.subList(0, 1000);
    }

    for (List<Integer> team1Indices : combinations) {
      List<String> team1Names = new ArrayList<>();
      List<String> team2Names = new ArrayList<>();
      int team1Rating = 0;
      int team2Rating = 0;

      for (int i = 0; i < players.size(); i++) {
        PlayerStatsPair player = players.get(i);
        if (team1Indices.contains(i)) {
          team1Names.add(player.getPlayerName());
          team1Rating += player.getRating();
        } else {
          team2Names.add(player.getPlayerName());
          team2Rating += player.getRating();
        }
      }

      // Validar que los tamaños de equipo sean correctos
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

    // Variabilidad aleatoria parcial: elegir entre las mejores particiones
    if (bestPartitions.isEmpty()) {
      return generateFallbackPartitionForTop14(players);
    }

    Random random = new Random();
    int randomIndex = random.nextInt(Math.min(bestPartitions.size(), 3));
    return bestPartitions.get(randomIndex);
  }

  /** Distribuye los jugadores restantes de manera secuencial cuidando el balance de tamaños */
  private void distributeRemainingPlayers(
      List<PlayerStatsPair> remainingPlayers,
      List<String> team1Names,
      List<String> team2Names,
      int totalPlayers) {

    // Calcular tamaños finales ideales
    int finalTeam1Size = totalPlayers / 2;
    int finalTeam2Size = totalPlayers - finalTeam1Size;

    // Si el número total es impar, determinar qué equipo debe tener el jugador extra
    if (totalPlayers % 2 == 1) {
      String captain1Name = captains.getCaptain1Name();
      String captain2Name = captains.getCaptain2Name();

      if (captain1Name != null && captain2Name != null) {
        // Calcular el rating actual de cada equipo (incluyendo solo los 14 mejores distribuidos)
        int team1Rating = calculateTeamRating(team1Names);
        int team2Rating = calculateTeamRating(team2Names);

        if (team1Rating < team2Rating) {
          finalTeam1Size = (totalPlayers + 1) / 2; // Team 1 (más débil) recibe el jugador extra
          finalTeam2Size = totalPlayers / 2;
        } else {
          finalTeam1Size = totalPlayers / 2;
          finalTeam2Size = (totalPlayers + 1) / 2; // Team 2 (más débil) recibe el jugador extra
        }
      }
    }

    // Distribuir jugadores restantes alternadamente, pero respetando los límites de tamaño
    boolean addToTeam1 = true;
    for (PlayerStatsPair player : remainingPlayers) {
      if (addToTeam1 && team1Names.size() < finalTeam1Size) {
        team1Names.add(player.getPlayerName());
      } else if (!addToTeam1 && team2Names.size() < finalTeam2Size) {
        team2Names.add(player.getPlayerName());
      } else if (team1Names.size() < finalTeam1Size) {
        // Si no podemos añadir al equipo preferido, añadir al otro si tiene espacio
        team1Names.add(player.getPlayerName());
      } else if (team2Names.size() < finalTeam2Size) {
        team2Names.add(player.getPlayerName());
      }

      addToTeam1 = !addToTeam1;
    }
  }

  /** Calcula el rating total de un equipo basado en los nombres de los jugadores */
  private int calculateTeamRating(List<String> teamNames) {
    int totalRating = 0;
    for (String playerName : teamNames) {
      Stats stats = availablePlayers.getStatsForPlayer(playerName);
      totalRating += calculatePlayerRating(stats);
    }
    return totalRating;
  }

  /** Genera una partición de respaldo para los 14 mejores jugadores */
  private TeamPartition generateFallbackPartitionForTop14(List<PlayerStatsPair> players) {
    List<String> team1 = new ArrayList<>();
    List<String> team2 = new ArrayList<>();

    boolean addToTeam1 = true;
    for (PlayerStatsPair player : players) {
      if (addToTeam1) {
        team1.add(player.getPlayerName());
      } else {
        team2.add(player.getPlayerName());
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

  /** Genera una partición de respaldo cuando no se encuentran buenas combinaciones */
  private TeamPartition generateFallbackPartition(List<PlayerStatsPair> players) {
    players.sort((a, b) -> Integer.compare(b.getRating(), a.getRating()));

    List<String> team1 = new ArrayList<>();
    List<String> team2 = new ArrayList<>();

    boolean addToTeam1 = true;
    for (PlayerStatsPair player : players) {
      if (addToTeam1) {
        team1.add(player.getPlayerName());
      } else {
        team2.add(player.getPlayerName());
      }
      addToTeam1 = !addToTeam1;
    }

    return new TeamPartition(team1, team2);
  }

  /** Clase para representar una partición de equipos */
  private static class TeamPartition {
    private final List<String> team1;
    private final List<String> team2;

    public TeamPartition(List<String> team1, List<String> team2) {
      this.team1 = team1;
      this.team2 = team2;
    }

    public List<String> getTeam1() {
      return team1;
    }

    public List<String> getTeam2() {
      return team2;
    }
  }

  private static class PlayerStatsPair {
    private final String playerName;
    private final int rating;

    public PlayerStatsPair(String playerName, int rating) {
      this.playerName = playerName;
      this.rating = rating;
    }

    public String getPlayerName() {
      return playerName;
    }

    public int getRating() {
      return rating;
    }
  }
}
