package org.nicolie.towersforpgm.session.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.pick.gui.PicksGUIManager;
import org.nicolie.towersforpgm.draft.state.DraftPhase;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.rankeds.queue.QueueState;
import org.nicolie.towersforpgm.rankeds.queue.RankedQueue;
import org.nicolie.towersforpgm.session.MatchSession;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import org.nicolie.towersforpgm.session.draft.DraftContext;
import org.nicolie.towersforpgm.session.draft.DraftOptions;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;

public final class CrossMatchBridge implements Listener {

  private static final CrossMatchBridge INSTANCE = new CrossMatchBridge();

  @Nullable
  private CrossMatchSnapshot pending = null;

  private TowersForPGM plugin;

  public static CrossMatchBridge getInstance() {
    return INSTANCE;
  }

  public void init(TowersForPGM plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    if (pending == null) return;

    // Espera un tick para asegurar que el match esté completamente cargado
    Bukkit.getScheduler().runTaskLater(plugin, () -> restoreIntoMatch(event.getMatch()), 1L);
  }

  public CaptureResult capture(Match match) {
    MatchSession session = MatchSessionRegistry.get(match);
    if (session == null) return CaptureResult.NO_SESSION;

    DraftContext ctx = session.getDraft();
    boolean isRanked = Queue.isRanked();
    String tempTable = plugin.config().databaseTables().getTempTable();
    if (ctx == null && !isRanked) return CaptureResult.NOTHING_TO_SAVE;

    CrossMatchSnapshot.Builder builder = CrossMatchSnapshot.builder();

    if (ctx != null) {
      DraftPhase phase = ctx.phase();

      if (phase != DraftPhase.IDLE) {
        builder.draftPhase(phase);
        builder.captain1(ctx.captains().getCaptain1());
        builder.captain2(ctx.captains().getCaptain2());
        builder.captain1Turn(ctx.captains().isCaptain1Turn());
        builder.firstCaptainTurn(ctx.state().isFirstCaptainTurn());

        builder.addPickHistory(ctx.availablePlayers().getPickHistory());

        if (phase == DraftPhase.RUNNING) {
          builder.addRemainingPlayers(ctx.availablePlayers().getAllAvailablePlayers());
        }

        builder.orderPattern(ctx.getOrderPattern());
        builder.orderMinPlayers(ctx.getOrderMinPlayers());
        builder.patternIndex(ctx.getPatternIndex());
        builder.usingCustomPattern(ctx.isUsingCustomPattern());

        builder.allowReroll(false);
        builder.tempTable(tempTable);
      } else if (!isRanked) {
        return CaptureResult.NOTHING_TO_SAVE;
      }
    }

    builder.ranked(isRanked);

    if (isRanked) {
      if (ctx != null) {
        ctx.teams().getAllTeam(1).forEach(name -> addUUIDByName(builder, name));
        ctx.teams().getAllTeam(2).forEach(name -> addUUIDByName(builder, name));
        ctx.availablePlayers()
            .getAllAvailablePlayers()
            .forEach(name -> addUUIDByName(builder, name));
      } else {
        builder.addRankedPlayers(RankedQueue.getInstance().snapshot());
      }

      Map<String, CompletableFuture<List<PlayerEloChange>>> cache =
          QueueState.getInstance().snapshotEloCache();

      if (cache != null && !cache.isEmpty()) {
        builder.eloCache(cache);
      }
    }

    CrossMatchSnapshot snapshot = builder.build();

    if (!snapshot.hasDraftState() && !snapshot.hasRankedPlayers()) {
      return CaptureResult.NOTHING_TO_SAVE;
    }

    pending = snapshot;

    log("Snapshot capturado: phase=" + snapshot.getDraftPhase()
        + ", ranked=" + snapshot.isRanked()
        + ", picks=" + snapshot.getPickHistory().size()
        + ", pool=" + snapshot.getRemainingPool().size()
        + ", jugadoresRanked=" + snapshot.getRankedPlayers().size());

    return CaptureResult.OK;
  }

  public boolean hasPending() {
    return pending != null;
  }

  @Nullable
  public CrossMatchSnapshot getPending() {
    return pending;
  }

  public void clear() {
    pending = null;
  }

  private void restoreIntoMatch(Match match) {
    if (pending == null) return;

    CrossMatchSnapshot snapshot = pending;
    pending = null;

    log("Restaurando snapshot en mapa: " + match.getMap().getName());
    plugin.config().databaseTables().setTempTable(snapshot.getTempTable());
    if (snapshot.isRanked()) restoreRanked(snapshot);
    if (snapshot.hasDraftState()) restoreDraft(match, snapshot);
  }

  private void restoreRanked(CrossMatchSnapshot snapshot) {
    Queue.setRanked(true);
    // snapshot.getRankedPlayers().forEach(uuid -> RankedQueue.getInstance().add(uuid));

    Map<String, CompletableFuture<List<PlayerEloChange>>> cache = snapshot.getEloCache();

    if (cache != null) {
      cache.forEach((table, future) -> QueueState.getInstance().putEloCache(table, future));
    }

    log("Ranked restaurado con " + snapshot.getRankedPlayers().size() + " jugadores.");
  }

  private void restoreDraft(Match match, CrossMatchSnapshot snapshot) {
    UUID c1 = snapshot.getCaptain1();
    UUID c2 = snapshot.getCaptain2();

    if (c1 == null || c2 == null) {
      log("Capitanes nulos, no se puede restaurar el draft.");
      return;
    }

    List<MatchPlayer> onlinePool = new ArrayList<>();
    List<String> offlinePool = new ArrayList<>();

    for (String name : snapshot.getRemainingPool()) {
      var bukkit = Bukkit.getPlayerExact(name);
      if (bukkit != null) {
        MatchPlayer mp = PGM.get().getMatchManager().getPlayer(bukkit);
        if (mp != null) {
          onlinePool.add(mp);
          continue;
        }
      }
      offlinePool.add(name);
    }

    DraftOptions options = DraftOptions.builder()
        .orderPattern("")
        .minOrder(0)
        .randomizeOrder(false)
        .build();

    try {
      MatchSession session = MatchSessionRegistry.of(match);
      session.startDraft(c1, c2, onlinePool, options, true);

      DraftContext ctx = session.getDraft();
      if (ctx == null) {
        log("Error: DraftContext es null.");
        return;
      }

      // Añadir jugadores offline manualmente
      for (String name : offlinePool) {
        ctx.availablePlayers().addPlayer(name);
      }

      ctx.captains().setCaptain1Turn(snapshot.isCaptain1Turn());
      ctx.state().setFirstCaptainTurn(snapshot.isFirstCaptainTurn());

      ctx.state().setCustomOrderPattern(snapshot.getOrderPattern());
      ctx.state().setCustomOrderMinPlayers(snapshot.getOrderMinPlayers());
      ctx.state().setCurrentPatternIndex(snapshot.getPatternIndex());
      ctx.state().setUsingCustomPattern(snapshot.isUsingCustomPattern());

      replayPicks(ctx, snapshot);

      if (snapshot.getDraftPhase() == DraftPhase.ENDED) {
        ctx.state().setCurrentPhase(DraftPhase.ENDED);
        ctx.finalizeTeams();
        log("Draft restaurado como terminado.");
      } else {
        ctx.state().setCurrentPhase(DraftPhase.RUNNING);
        PicksGUIManager.giveItem(match);
        ctx.resumePickTimer();
        log("Draft restaurado en ejecución.");
      }

    } catch (Exception e) {
      log("Error al restaurar draft: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void replayPicks(DraftContext ctx, CrossMatchSnapshot snapshot) {
    int replayed = 0;

    for (Map.Entry<String, Integer> pick : snapshot.getPickHistory()) {
      String name = pick.getKey();
      int team = pick.getValue();

      if (ctx.captains().isCaptainByName(name)) continue;

      try {
        ctx.teams().addPlayerToTeam(name, team);
        ctx.availablePlayers().removePlayer(name);
        ctx.availablePlayers().recordPick(name, team);

        var bukkit = Bukkit.getPlayerExact(name);
        if (bukkit != null) ctx.teams().assignTeam(bukkit, team);

        replayed++;
      } catch (Exception e) {
        log("No se pudo restaurar pick de " + name);
      }
    }

    log("Picks restaurados: " + replayed + "/" + snapshot.getPickHistory().size());
  }

  private static void addUUIDByName(CrossMatchSnapshot.Builder builder, String name) {
    var p = Bukkit.getPlayerExact(name);
    if (p != null) builder.addRankedPlayers(List.of(p.getUniqueId()));
  }

  private void log(String msg) {
    if (plugin != null) {
      plugin.getLogger().info("[CrossMatchBridge] " + msg);
    }
  }

  public enum CaptureResult {
    OK,
    NO_SESSION,
    NOTHING_TO_SAVE
  }
}
