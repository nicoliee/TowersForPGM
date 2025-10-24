package org.nicolie.towersforpgm.draft;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.nicolie.towersforpgm.database.Stats;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.MatchManager;
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
    match.sendMessage(Component.text("§4" + Bukkit.getPlayer(captain1).getName() + " §l§bvs. "
        + "§9" + Bukkit.getPlayer(captain2).getName()));
    match.sendMessage(Component.text("§m---------------------------------"));
    match.sendMessage(Component.text(LanguageManager.message("picks.choosing")));

    // Realizar balance de equipos
    balanceTeams(match);
  }

  private void balanceTeams(Match match) {
    List<String> allPlayers = new ArrayList<>(availablePlayers.getAllAvailablePlayers());

    String captain1Name = captains.getCaptain1Name();
    String captain2Name = captains.getCaptain2Name();
    allPlayers.remove(captain1Name);
    allPlayers.remove(captain2Name);

    List<PlayerRating> playerRatings = new ArrayList<>();
    for (String playerName : allPlayers) {
      Stats stats = availablePlayers.getStatsForPlayer(playerName);
      int rating = calculateRating(stats);
      playerRatings.add(new PlayerRating(playerName, rating));
    }

    TeamPartition partition = findBestPartition(playerRatings);

    List<String> team1 = new ArrayList<>();
    List<String> team2 = new ArrayList<>();

    team1.add(captain1Name);
    team2.add(captain2Name);
    team1.addAll(partition.team1);
    team2.addAll(partition.team2);

    // Registrar equipos
    for (String player : team1) {
      teams.addPlayerToTeam(player, 1);
      if (Bukkit.getPlayer(player) != null) {
        teams.assignTeam(Bukkit.getPlayer(player), 1);
      }
    }
    for (String player : team2) {
      teams.addPlayerToTeam(player, 2);
      if (Bukkit.getPlayer(player) != null) {
        teams.assignTeam(Bukkit.getPlayer(player), 2);
      }
    }
    availablePlayers.clear();
    // Mostrar equipos
    displayTeams(team1, team2);

    // Iniciar countdown y preparar ready
    prepareMatchStart(match);
  }

  private int calculateRating(Stats stats) {
    // Si no hay estadísticas, 0
    if (stats == null) return 0;

    // Base: usar elo si existe, si no 0
    double base = (stats.getElo() == -9999) ? 0 : stats.getElo();

    // Convertir a tasas por juego (evitan sesgos por número de partidas)
    double games = Math.max(1, stats.getGames());
    double killsPerGame = stats.getKills() / games;
    double deathsPerGame = stats.getDeaths() / games;
    double assistsPerGame = stats.getAssists() / games;
    double pointsPerGame = stats.getPoints() / games;
    double damageDonePerGame = stats.getDamageDone() / games;
    double damageTakenPerGame = stats.getDamageTaken() / games;

    // Ratios útiles
    double winRate = stats.getGames() > 0 ? (stats.getWins() / (double) stats.getGames()) : 0.0;

    // Inferir rol del jugador: atacante, defensor o híbrido
    // Heurística simple:
    // - Si tiene muchos puntos por partida -> atacante
    // - Si tiene mucho daño tomado y pocas muertes relativas -> defensor (sobrevive y aguanta)
    // - Si tiene balance entre ambos -> híbrido
    double attackScore = pointsPerGame * 1.5 + damageDonePerGame * 0.2 + killsPerGame * 0.8;
    double defendScore = damageTakenPerGame * 0.6
        + (1.0 / Math.max(0.1, deathsPerGame)) * 0.5
        + (stats.getWins() * 1.0 / games) * 1.0;

    String inferredRole;
    if (attackScore > defendScore * 1.15) {
      inferredRole = "ATTACKER";
    } else if (defendScore > attackScore * 1.15) {
      inferredRole = "DEFENDER";
    } else {
      inferredRole = "HYBRID";
    }

    // Pesos por rol. Ajustar para favorecer la intención del rol:
    // - Attacker: puntos, kills, winrate
    // - Defender: supervivencia, damageTaken (soporte), assists
    // - Hybrid: balance entre kills/assists y points
    double score = 0.0;
    switch (inferredRole) {
      case "ATTACKER":
        score += pointsPerGame * 4.0; // puntos son muy importantes
        score += killsPerGame * 3.0;
        score += assistsPerGame * 1.5;
        score += winRate * 40.0;
        score += damageDonePerGame * 0.1;
        // penalizar muertes altas
        score -= deathsPerGame * 1.5;
        break;
      case "DEFENDER":
        score += damageTakenPerGame * 0.8; // aguantar daño es relevante
        score += assistsPerGame * 2.0;
        score += (1.0 / Math.max(0.1, deathsPerGame)) * 2.0; // sobrevivir más suma
        score += winRate * 30.0;
        score += killsPerGame * 1.0;
        break;
      default: // HYBRID
        score += pointsPerGame * 2.5;
        score += killsPerGame * 2.0;
        score += assistsPerGame * 1.8;
        score += winRate * 35.0;
        score += (damageDonePerGame + damageTakenPerGame) * 0.15;
        score -= deathsPerGame * 1.0;
        break;
    }

    // Normalizar y combinar con base Elo
    // Normalizar score a una escala aproximada: dividir por un factor y convertir a entero
    double normalized = score;

    // Combinar: dar 60% al elo/base y 40% al score normalizado
    double combined = base * 0.6 + normalized * 0.4;

    // Asegurar límites razonables
    int finalRating = (int) Math.round(Math.max(0, combined));

    return finalRating;
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

  private void displayTeams(List<String> team1, List<String> team2) {
    StringBuilder team1Display = utilities.buildLists(team1, "§4", false);
    StringBuilder team2Display = utilities.buildLists(team2, "§9", false);

    SendMessage.broadcast(LanguageManager.langMessage("draft.captains.teamsHeader"));
    SendMessage.broadcast(team1Display.toString());
    SendMessage.broadcast("§8[§4" + team1.size() + "§8] §l§bvs. §8[§9" + team2.size() + "§8]");
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
    if (captain1 != null) {
      captain1.sendActionBar(Component.text(readyMessage));
    }
    if (captain2 != null) {
      captain2.sendActionBar(Component.text(readyMessage));
    }

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
