package org.nicolie.towersforpgm.session;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.team.AvailablePlayers;
import org.nicolie.towersforpgm.draft.team.Captains;
import org.nicolie.towersforpgm.draft.team.Teams;
import org.nicolie.towersforpgm.session.draft.DraftContext;
import org.nicolie.towersforpgm.session.draft.DraftOptions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public final class MatchSession {

  private final Match match;
  private final TowersForPGM plugin;

  @Nullable
  private DraftContext draftContext;

  MatchSession(Match match, TowersForPGM plugin) {
    this.match = match;
    this.plugin = plugin;
  }

  public DraftContext startDraft(
      UUID captain1, UUID captain2, List<MatchPlayer> players, DraftOptions options) {

    if (draftContext != null) cleanupDraftContext(draftContext);

    draftContext = createDraftContext();

    invokeDraftMethod(
        draftContext,
        "startDraft",
        new Class<?>[] {UUID.class, UUID.class, List.class, DraftOptions.class},
        captain1,
        captain2,
        players,
        options);

    return draftContext;
  }

  public DraftContext startMatchmaking(UUID captain1, UUID captain2, List<MatchPlayer> players) {

    if (draftContext != null) cleanupDraftContext(draftContext);

    draftContext = createDraftContext();

    invokeDraftMethod(
        draftContext,
        "startMatchmaking",
        new Class<?>[] {UUID.class, UUID.class, List.class},
        captain1,
        captain2,
        players);

    return draftContext;
  }

  public boolean hasDraft() {
    return draftContext != null && draftContext.isActive();
  }

  public void endDraft() {
    if (draftContext != null) {
      cleanupDraftContext(draftContext);
      draftContext = null;
    }
  }

  @Nullable
  public DraftContext getDraft() {
    return draftContext;
  }

  @Nullable
  public Teams teams() {
    return draftContext != null ? draftContext.teams() : null;
  }

  @Nullable
  public Captains captains() {
    return draftContext != null ? draftContext.captains() : null;
  }

  @Nullable
  public AvailablePlayers availablePlayers() {
    return draftContext != null ? draftContext.availablePlayers() : null;
  }

  public Match getMatch() {
    return match;
  }

  private DraftContext createDraftContext() {
    try {
      Constructor<DraftContext> ctor =
          DraftContext.class.getDeclaredConstructor(Match.class, TowersForPGM.class);

      ctor.setAccessible(true);
      return ctor.newInstance(match, plugin);

    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("No se pudo crear DraftContext", e);
    }
  }

  private void cleanupDraftContext(DraftContext context) {
    invokeDraftMethod(context, "cleanup", new Class<?>[0]);
  }

  /**
   * Invoca métodos privados de DraftContext usando reflexión. Se usa para evitar exponer métodos
   * internamente.
   */
  private void invokeDraftMethod(
      DraftContext context, String methodName, Class<?>[] parameterTypes, Object... args) {

    try {
      Method method = DraftContext.class.getDeclaredMethod(methodName, parameterTypes);
      method.setAccessible(true);
      method.invoke(context, args);

    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("No se pudo invocar DraftContext." + methodName + "()", e);
    }
  }

  void destroy() {
    endDraft();
  }
}
