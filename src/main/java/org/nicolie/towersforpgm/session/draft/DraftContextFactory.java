package org.nicolie.towersforpgm.session.draft;

import java.util.List;
import java.util.UUID;
import org.nicolie.towersforpgm.TowersForPGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public final class DraftContextFactory {

  private final Match match;
  private final TowersForPGM plugin;

  public DraftContextFactory(Match match, TowersForPGM plugin) {
    this.match = match;
    this.plugin = plugin;
  }

  public DraftContext createDraft(
      UUID captain1,
      UUID captain2,
      List<MatchPlayer> players,
      DraftOptions options,
      boolean snapshot) {
    DraftContext ctx = new DraftContext(match, plugin);
    ctx.startDraft(captain1, captain2, players, options, snapshot);
    return ctx;
  }

  public DraftContext createMatchmaking(UUID captain1, UUID captain2, List<MatchPlayer> players) {
    DraftContext ctx = new DraftContext(match, plugin);
    ctx.startMatchmaking(captain1, captain2, players);
    return ctx;
  }

  public void cleanup(DraftContext ctx) {
    if (ctx != null) ctx.cleanup();
  }
}
