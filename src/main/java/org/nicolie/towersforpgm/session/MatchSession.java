package org.nicolie.towersforpgm.session;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.team.AvailablePlayers;
import org.nicolie.towersforpgm.draft.team.Captains;
import org.nicolie.towersforpgm.draft.team.Teams;
import org.nicolie.towersforpgm.session.draft.DraftContext;
import org.nicolie.towersforpgm.session.draft.DraftContextFactory;
import org.nicolie.towersforpgm.session.draft.DraftOptions;
import org.nicolie.towersforpgm.session.ranked.RankedSession;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public final class MatchSession {

  private final Match match;
  private final TowersForPGM plugin;
  private final DraftContextFactory draftFactory;

  @Nullable
  private DraftContext draftContext;

  @Nullable
  private RankedSession rankedSession;

  MatchSession(Match match, TowersForPGM plugin) {
    this.match = match;
    this.plugin = plugin;
    this.draftFactory = new DraftContextFactory(match, plugin);
  }

  public DraftContext startDraft(
      UUID captain1,
      UUID captain2,
      List<MatchPlayer> players,
      DraftOptions options,
      boolean snapshot) {
    if (draftContext != null) draftFactory.cleanup(draftContext);

    draftContext = draftFactory.createDraft(captain1, captain2, players, options, snapshot);
    return draftContext;
  }

  public DraftContext startMatchmaking(UUID captain1, UUID captain2, List<MatchPlayer> players) {
    if (draftContext != null) draftFactory.cleanup(draftContext);

    draftContext = draftFactory.createMatchmaking(captain1, captain2, players);
    return draftContext;
  }

  public boolean hasDraft() {
    return draftContext != null && draftContext.isActive();
  }

  public void endDraft() {
    if (draftContext != null) {
      draftFactory.cleanup(draftContext);
      draftContext = null;
    }
  }

  @Nullable
  public DraftContext getDraft() {
    return draftContext;
  }

  public RankedSession startRanked() {
    if (rankedSession != null) rankedSession.destroy();

    Teams sessionTeams = teams();
    if (sessionTeams == null) {
      throw new IllegalStateException("No hay Teams disponibles para crear RankedSession");
    }

    rankedSession = new RankedSession(match, plugin, sessionTeams);
    rankedSession.activate();
    return rankedSession;
  }

  @Nullable
  public RankedSession getRanked() {
    return rankedSession;
  }

  public boolean hasRanked() {
    return rankedSession != null && rankedSession.isActive();
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

  void destroy() {
    endDraft();
    if (rankedSession != null) {
      rankedSession.destroy();
      rankedSession = null;
    }
  }
}
