package org.nicolie.towersforpgm.draft.team;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.nicolie.towersforpgm.database.models.MMR;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.draft.events.MatchmakingEndEvent;
import org.nicolie.towersforpgm.draft.state.DraftPhase;
import org.nicolie.towersforpgm.session.draft.DraftContext;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.util.bukkit.Sounds;
import tc.oc.pgm.util.text.TextFormatter;

public final class MatchmakingAssigner {

  public static void assign(DraftContext ctx) {
    Match match = ctx.match();

    List<String> allPlayers = new ArrayList<>(ctx.availablePlayers().getAllAvailablePlayers());

    List<PlayerRating> rated = new ArrayList<>();
    for (String name : allPlayers) {
      Stats stats = ctx.availablePlayers().getStatsForPlayer(name);
      rated.add(new PlayerRating(name, MMR.computeInt(stats)));
    }

    Partition partition = balance(rated, ctx);

    List<String> team1 = new ArrayList<>();
    List<String> team2 = new ArrayList<>();

    team1.add(ctx.captains().getCaptain1Name());
    team2.add(ctx.captains().getCaptain2Name());
    team1.addAll(partition.team1);
    team2.addAll(partition.team2);

    for (String name : partition.team1) {
      ctx.teams().addPlayerToTeam(name, 1);
      org.bukkit.entity.Player p = Bukkit.getPlayer(name);
      if (p != null) ctx.teams().assignTeam(p, 1);
    }
    for (String name : partition.team2) {
      ctx.teams().addPlayerToTeam(name, 2);
      org.bukkit.entity.Player p = Bukkit.getPlayer(name);
      if (p != null) ctx.teams().assignTeam(p, 2);
    }

    ctx.availablePlayers().clear();
    displayTeams(ctx, team1, team2);
    Bukkit.getPluginManager().callEvent(new MatchmakingEndEvent(team1, team2, match));
    prepareMatchStart(ctx);
    ctx.state().setCurrentPhase(DraftPhase.ENDED);
  }

  private static Partition balance(List<PlayerRating> players, DraftContext ctx) {
    if (players.isEmpty()) return new Partition(new ArrayList<>(), new ArrayList<>());
    return players.size() > 14 ? hybrid(players, ctx) : bruteForce(players, ctx);
  }

  private static Partition bruteForce(List<PlayerRating> players, DraftContext ctx) {
    int total = players.size();
    int team1Size = teamSize(total, true, ctx);
    int team2Size = total - team1Size;

    List<List<Integer>> combos = combinations(players.size(), team1Size);
    if (combos.size() > 1000) {
      Collections.shuffle(combos);
      combos = combos.subList(0, 1000);
    }

    List<Partition> best = new ArrayList<>();
    double bestDiff = Double.MAX_VALUE;

    for (List<Integer> t1Indices : combos) {
      List<String> t1 = new ArrayList<>(), t2 = new ArrayList<>();
      int r1 = 0, r2 = 0;
      for (int i = 0; i < players.size(); i++) {
        if (t1Indices.contains(i)) {
          t1.add(players.get(i).name);
          r1 += players.get(i).rating;
        } else {
          t2.add(players.get(i).name);
          r2 += players.get(i).rating;
        }
      }
      if (t1.size() != team1Size || t2.size() != team2Size) continue;
      double diff = Math.abs(r1 - r2);
      if (diff < bestDiff) {
        bestDiff = diff;
        best.clear();
        best.add(new Partition(t1, t2));
      } else if (Math.abs(diff - bestDiff) < 0.1) best.add(new Partition(t1, t2));
    }

    if (best.isEmpty()) return fallback(players);
    return best.get(new Random().nextInt(Math.min(best.size(), 3)));
  }

  private static Partition hybrid(List<PlayerRating> all, DraftContext ctx) {
    List<PlayerRating> sorted = new ArrayList<>(all);
    sorted.sort((a, b) -> b.rating - a.rating);

    Partition top = bruteForce(sorted.subList(0, 14), ctx);
    List<String> t1 = new ArrayList<>(top.team1);
    List<String> t2 = new ArrayList<>(top.team2);

    int finalT1 = teamSize(all.size(), true, ctx);
    int finalT2 = all.size() - finalT1;
    boolean addToT1 = true;
    for (PlayerRating p : sorted.subList(14, sorted.size())) {
      if (addToT1 && t1.size() < finalT1) t1.add(p.name);
      else if (!addToT1 && t2.size() < finalT2) t2.add(p.name);
      else if (t1.size() < finalT1) t1.add(p.name);
      else t2.add(p.name);
      addToT1 = !addToT1;
    }
    return new Partition(t1, t2);
  }

  private static Partition fallback(List<PlayerRating> players) {
    List<String> t1 = new ArrayList<>(), t2 = new ArrayList<>();
    boolean flip = true;
    for (PlayerRating p : players) {
      (flip ? t1 : t2).add(p.name);
      flip = !flip;
    }
    return new Partition(t1, t2);
  }

  private static int teamSize(int total, boolean isTeam1, DraftContext ctx) {
    int half = total / 2;
    if (total % 2 == 1) {
      Stats c1 = ctx.availablePlayers().getStatsForPlayer(ctx.captains().getCaptain1Name());
      Stats c2 = ctx.availablePlayers().getStatsForPlayer(ctx.captains().getCaptain2Name());
      return isTeam1
          ? (MMR.computeInt(c1) < MMR.computeInt(c2) ? half + 1 : half)
          : (MMR.computeInt(c2) < MMR.computeInt(c1) ? half + 1 : half);
    }
    return half;
  }

  private static List<List<Integer>> combinations(int n, int k) {
    List<List<Integer>> result = new ArrayList<>();
    combine(result, new ArrayList<>(), 0, n, k);
    return result;
  }

  private static void combine(
      List<List<Integer>> result, List<Integer> cur, int start, int n, int k) {
    if (cur.size() == k) {
      result.add(new ArrayList<>(cur));
      return;
    }
    for (int i = start; i < n; i++) {
      cur.add(i);
      combine(result, cur, i + 1, n, k);
      cur.remove(cur.size() - 1);
    }
  }

  private static void displayTeams(DraftContext ctx, List<String> team1, List<String> team2) {
    Match match = ctx.match();
    Component t1comp = TextFormatter.list(
        MatchManager.convert(team1, ctx.teams().getTeam(1).getTextColor()),
        NamedTextColor.DARK_GRAY);
    Component t2comp = TextFormatter.list(
        MatchManager.convert(team2, ctx.teams().getTeam(2).getTextColor()),
        NamedTextColor.DARK_GRAY);
    Component header =
        Component.translatable("draft.captains.teamsHeader").color(NamedTextColor.AQUA);

    for (MatchPlayer viewer : match.getPlayers()) {
      viewer.sendMessage(TextFormatter.horizontalLineHeading(
          viewer.getBukkit(), header, NamedTextColor.WHITE, 200));
      viewer.sendMessage(t1comp);
      viewer.sendMessage(
          Component.text("§8[" + ctx.teams().getTeamColor(1).replace("&", "§") + team1.size()
              + "§8] §l§bvs. "
              + "§8[" + ctx.teams().getTeamColor(2).replace("&", "§") + team2.size() + "§8]"));
      viewer.sendMessage(t2comp);
      viewer.sendMessage(TextFormatter.horizontalLine(NamedTextColor.WHITE, 200));
    }
  }

  private static void prepareMatchStart(DraftContext ctx) {
    Match match = ctx.match();
    ctx.teams()
        .setTeamsSize(
            Math.max(ctx.teams().getAllTeam(1).size(), ctx.teams().getAllTeam(2).size()));
    match.playSound(Sounds.MATCH_START);

    ctx.captains().resetReady();
    ctx.captains().setReadyActive(true);
    ctx.captains().setMatchWithCaptains(true);

    Component readyMsg = Component.translatable(
            "draft.ready.tip", Component.text("/ready").color(NamedTextColor.GOLD))
        .color(NamedTextColor.AQUA);

    sendToCaption(match, ctx.captains().getCaptain1(), readyMsg);
    sendToCaption(match, ctx.captains().getCaptain2(), readyMsg);

    StartMatchModule start = match.needModule(StartMatchModule.class);
    if (start != null) start.forceStartCountdown(Duration.ofSeconds(45), Duration.ZERO);

    ctx.readyReminder().startTimer();
  }

  private static void sendToCaption(Match match, java.util.UUID uuid, Component msg) {
    if (uuid == null) return;
    MatchPlayer p = match.getPlayer(uuid);
    if (p != null) p.sendActionBar(msg);
  }

  private static final class PlayerRating {
    final String name;
    final int rating;

    PlayerRating(String name, int rating) {
      this.name = name;
      this.rating = rating;
    }
  }

  private static final class Partition {
    final List<String> team1, team2;

    Partition(List<String> t1, List<String> t2) {
      this.team1 = t1;
      this.team2 = t2;
    }
  }
}
