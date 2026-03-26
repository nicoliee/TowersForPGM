package org.nicolie.towersforpgm.commands.draft;

import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.draft.pick.gui.PicksGUIManager;
import org.nicolie.towersforpgm.draft.state.DraftPhase;
import org.nicolie.towersforpgm.draft.state.PickResult;
import org.nicolie.towersforpgm.draft.state.ReadyResult;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import org.nicolie.towersforpgm.session.draft.DraftContext;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.suggestion.Suggestions;
import tc.oc.pgm.util.Audience;

public class DraftCaptainCommands {

  @Command("ready")
  @CommandDescription("Captains mark themselves as ready for the match to start")
  public void readyCommand(Audience audience, Player sender) {
    Match match = PGM.get().getMatchManager().getMatch(sender);
    DraftContext ctx = getContextSilently(match);

    if (ctx == null) {
      audience.sendWarning(Component.translatable("draft.inactive"));
      return;
    }

    UUID uuid = sender.getUniqueId();
    ReadyResult result = ctx.validateReady(uuid);

    switch (result) {
      case NOT_AVAILABLE:
        audience.sendWarning(Component.translatable("draft.ready.notAvailable"));
        break;
      case NOT_A_CAPTAIN:
        audience.sendWarning(Component.translatable("draft.notCaptain"));
        break;
      case ALREADY_READY:
        audience.sendWarning(Component.translatable("draft.ready.alreadyReady"));
        break;
      case OK:
        ctx.markReady(uuid, match);
        // markReady already triggers the countdown if both are ready.
        // Broadcast which team is ready.
        int captainNumber = ctx.getCaptainNumber(uuid);
        Party team = ctx.teams().getTeam(captainNumber);
        match.sendMessage(Component.translatable("draft.ready", team.getName()));
        break;
    }
  }

  @Command("pick <player>")
  @CommandDescription("Pick a player for your team")
  public void pickCommand(
      Audience audience,
      Player sender,
      @Argument(value = "player", suggestions = "availableDraftPlayers") String selectedPlayer) {

    Match match = PGM.get().getMatchManager().getMatch(sender);
    DraftContext ctx = getActiveDraft(audience, match);
    if (ctx == null) return;

    UUID captainUUID = sender.getUniqueId();
    PickResult result = ctx.validatePick(captainUUID, selectedPlayer);

    switch (result) {
      case DRAFT_NOT_ACTIVE:
        audience.sendWarning(Component.translatable("draft.inactive"));
        break;
      case NOT_A_CAPTAIN:
        audience.sendWarning(Component.translatable("draft.notCaptain"));
        break;
      case NOT_YOUR_TURN:
        audience.sendWarning(Component.translatable("draft.notTurn"));
        break;
      case NOT_IN_DRAFT:
        audience.sendWarning(Component.translatable(
            "draft.picks.notInDraft", MatchManager.getPrefixedName(selectedPlayer)));
        break;
      case ALREADY_PICKED:
        audience.sendWarning(Component.translatable(
            "draft.alreadyInTeam", MatchManager.getPrefixedName(selectedPlayer)));
        break;
      case OK:
        ctx.pickPlayer(selectedPlayer);
        break;
    }
  }

  @Command("pick")
  @CommandDescription("Opens the pick menu for the draft")
  public void openPickMenu(Audience audience, Player sender) {
    Match match = PGM.get().getMatchManager().getMatch(sender);
    DraftContext ctx = getActiveDraft(audience, match);
    if (ctx == null) return;

    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(sender);
    new PicksGUIManager(org.nicolie.towersforpgm.TowersForPGM.getInstance()).openMenu(matchPlayer);
    PicksGUIManager.giveItem(sender);
  }

  @Suggestions("availableDraftPlayers")
  public List<String> getAvailablePlayers(Player sender) {
    Match match = PGM.get().getMatchManager().getMatch(sender);
    DraftContext ctx = getContextSilently(match);
    if (ctx == null) return List.of();

    // Solo sugerir si el jugador es capitán y es su turno de pickear
    int captainNumber = ctx.getCaptainNumber(sender.getUniqueId());
    if (captainNumber == -1) return List.of();

    boolean myTurn = (captainNumber == 1 && ctx.captains().isCaptain1Turn())
        || (captainNumber == 2 && !ctx.captains().isCaptain1Turn());
    if (!myTurn) return List.of();

    return ctx.availablePlayers().getAllAvailablePlayers();
  }

  private DraftContext getActiveDraft(Audience audience, Match match) {
    if (match == null) {
      audience.sendWarning(Component.translatable("draft.inactive"));
      return null;
    }
    var session = MatchSessionRegistry.get(match);
    if (session == null) {
      audience.sendWarning(Component.translatable("draft.inactive"));
      return null;
    }
    DraftContext ctx = session.getDraft();
    if (ctx == null || ctx.phase() != DraftPhase.RUNNING) {
      audience.sendWarning(Component.translatable("draft.inactive"));
      return null;
    }
    return ctx;
  }

  private DraftContext getContextSilently(Match match) {
    if (match == null) return null;
    var session = MatchSessionRegistry.get(match);
    if (session == null) return null;
    DraftContext ctx = session.getDraft();
    if (ctx == null || ctx.phase() == DraftPhase.IDLE) return null;
    return ctx;
  }
}
