package org.nicolie.towersforpgm.commands.draft;

import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import org.nicolie.towersforpgm.session.draft.DraftOptions;
import org.nicolie.towersforpgm.utils.Permissions;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.util.Audience;

public class DraftStartCommands {
  @Command("captains|capitanes <captain1> <captain2>")
  @CommandDescription("Start the draft by selecting the captains")
  @Permission(Permissions.CAPTAINS)
  public void startDraft(
      Audience audience,
      Player sender,
      @Argument("captain1") Player captain1,
      @Argument("captain2") Player captain2) {
    Match match = PGM.get().getMatchManager().getMatch(sender);
    MatchSessionRegistry.of(match)
        .startDraft(
            captain1.getUniqueId(),
            captain2.getUniqueId(),
            getPlayers(match, captain1, captain2),
            getDefaultDraftOptions());
  }

  @Command("balance <captain1> <captain2>")
  @CommandDescription("Balance the teams based on MMR")
  @Permission(Permissions.CAPTAINS)
  public void balanceTeams(
      Audience audience,
      Player sender,
      @Argument("captain1") Player captain1,
      @Argument("captain2") Player captain2) {
    Match match = PGM.get().getMatchManager().getMatch(sender);
    MatchSessionRegistry.of(match)
        .startMatchmaking(
            captain1.getUniqueId(), captain2.getUniqueId(), getPlayers(match, captain1, captain2));
  }

  private DraftOptions getDefaultDraftOptions() {
    return DraftOptions.builder()
        .orderPattern(TowersForPGM.getInstance().config().draft().getOrder())
        .minOrder(TowersForPGM.getInstance().config().draft().getMinOrder())
        .randomizeOrder(true)
        .allowReroll(TowersForPGM.getInstance().config().draft().isReroll())
        .build();
  }

  private List<MatchPlayer> getPlayers(Match match, Player captain1, Player captain2) {
    return match.getPlayers().stream()
        .filter(player -> !player.getId().equals(captain1.getUniqueId())
            && !player.getId().equals(captain2.getUniqueId()))
        .collect(Collectors.toList());
  }
}
