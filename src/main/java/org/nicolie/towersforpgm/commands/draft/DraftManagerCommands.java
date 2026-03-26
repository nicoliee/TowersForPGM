package org.nicolie.towersforpgm.commands.draft;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.draft.state.DraftPhase;
import org.nicolie.towersforpgm.draft.state.SubstituteResult;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import org.nicolie.towersforpgm.session.draft.AddResult;
import org.nicolie.towersforpgm.session.draft.DraftContext;
import org.nicolie.towersforpgm.session.draft.RemoveResult;
import org.nicolie.towersforpgm.utils.MatchManager;
import org.nicolie.towersforpgm.utils.Permissions;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.suggestion.Suggestions;
import tc.oc.pgm.lib.org.incendo.cloud.context.CommandContext;
import tc.oc.pgm.lib.org.incendo.cloud.context.CommandInput;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.bukkit.Sounds;

public class DraftManagerCommands {

  @Command("add <player>")
  @CommandDescription("Add a player to the draft in progress")
  @Permission(Permissions.ADMIN)
  public void addPlayerToDraft(
      Audience audience,
      Player sender,
      @Argument(value = "player", suggestions = "playersNotInDraft") String playerName) {

    Match match = PGM.get().getMatchManager().getMatch(sender);
    DraftContext ctx = getActiveDraft(audience, match);
    if (ctx == null) return;

    if (Queue.isRanked()) {
      audience.sendWarning(Component.translatable("ranked.notAllowed"));
      return;
    }

    switch (ctx.validateAdd(playerName)) {
      case IS_CAPTAIN:
        audience.sendWarning(Component.translatable("draft.add.captain"));
        break;
      case ALREADY_PICKED:
        audience.sendWarning(Component.translatable(
            "draft.alreadyInTeam", MatchManager.getPrefixedName(playerName)));
        break;
      case ALREADY_IN_DRAFT:
        audience.sendWarning(Component.translatable(
            "draft.alreadyInDraft", MatchManager.getPrefixedName(playerName)));
        break;
      case OK:
        ctx.addToPool(playerName);
        break;
    }
  }

  @Command("remove <player>")
  @CommandDescription("Remove a player from the draft in progress")
  @Permission(Permissions.ADMIN)
  public void removePlayerFromDraft(
      Audience audience,
      Player sender,
      @Argument(value = "player", suggestions = "playersInDraft") String playerName) {

    Match match = PGM.get().getMatchManager().getMatch(sender);
    DraftContext ctx = getActiveDraft(audience, match);
    if (ctx == null) return;

    if (Queue.isRanked()) {
      audience.sendWarning(Component.translatable("ranked.notAllowed"));
      return;
    }

    switch (ctx.validateRemove(playerName)) {
      case NOT_IN_DRAFT:
        audience.sendWarning(
            Component.translatable("draft.notInDraft", MatchManager.getPrefixedName(playerName)));
        break;
      case ALREADY_PICKED:
        audience.sendWarning(Component.translatable(
            "draft.captains.alreadyInTeam", MatchManager.getPrefixedName(playerName)));
        break;
      case OK:
        ctx.removeFromPool(playerName);
        match.sendMessage(
            Component.translatable("draft.remove", MatchManager.getPrefixedName(playerName))
                .color(NamedTextColor.GRAY));
        match.playSound(Sounds.WARNING);
        break;
    }
  }

  @Command("substitute|sub <team> <player>")
  @CommandDescription("Substitute a captain in an ongoing draft")
  @Permission(Permissions.ADMIN)
  public void substituteCaptain(
      Audience audience,
      Player sender,
      @Argument("team") Party team,
      @Argument("player") Player newCaptain) {
    Match match = PGM.get().getMatchManager().getMatch(sender);
    DraftContext ctx = getActiveDraft(audience, match);
    if (ctx == null) return;

    int teamNumber = ctx.teams().getTeamNumber(team);
    if (teamNumber == -1) {
      audience.sendWarning(Component.translatable("draft.substitute.invalidTeam", team.getName()));
      return;
    }

    SubstituteResult result = ctx.substituteCaptainByTeam(teamNumber, newCaptain.getUniqueId());

    switch (result) {
      case SUCCESS:
        match.sendMessage(Component.translatable(
            "draft.substitute.success",
            Component.text(newCaptain.getName()),
            ctx.teams().getTeam(teamNumber).getName()));
        match.playSound(Sounds.TIP);
        break;
      case NOT_CAPTAIN:
        audience.sendWarning(Component.translatable("draft.notCaptain"));
        break;
      case ENEMY_TEAM:
        audience.sendWarning(Component.translatable("draft.substitute.enemy"));
        break;
      case NOT_AVAILABLE:
        audience.sendWarning(Component.translatable("draft.substitute.notAvailable"));
        break;
    }
  }

  @Suggestions("playersNotInDraft")
  public List<String> suggestPlayersForAdd(
      Player sender, CommandContext<CommandSender> context, CommandInput input) {
    Match match = PGM.get().getMatchManager().getMatch(sender);
    DraftContext ctx = getContextSilently(match);
    if (ctx == null) return List.of();

    return Bukkit.getOnlinePlayers().stream()
        .filter(p -> ctx.validateAdd(p.getName()) == AddResult.OK)
        .map(Player::getName)
        .collect(Collectors.toList());
  }

  @Suggestions("playersInDraft")
  public List<String> suggestPlayersForRemove(
      Player sender, CommandContext<CommandSender> context, CommandInput input) {
    Match match = PGM.get().getMatchManager().getMatch(sender);
    DraftContext ctx = getContextSilently(match);
    if (ctx == null) return List.of();

    return ctx.availablePlayers().getAllAvailablePlayers().stream()
        .filter(name -> {
          UUID uuid = resolveOfflineUUID(name, ctx);
          return uuid == null || ctx.validateRemove(name) == RemoveResult.OK;
        })
        .collect(Collectors.toList());
  }

  private DraftContext getActiveDraft(Audience audience, Match match) {
    DraftContext ctx = getContextSilently(match);
    if (ctx == null) audience.sendWarning(Component.translatable("draft.inactive"));
    return ctx;
  }

  private DraftContext getContextSilently(Match match) {
    if (match == null) return null;
    var session = MatchSessionRegistry.get(match);
    if (session == null) return null;
    DraftContext ctx = session.getDraft();
    if (ctx == null || ctx.phase() == DraftPhase.IDLE || ctx.phase() == DraftPhase.ENDED) {
      return null;
    }
    return ctx;
  }

  private UUID resolveOfflineUUID(String name, DraftContext ctx) {
    Player online = Bukkit.getPlayerExact(name);
    if (online != null) return online.getUniqueId();
    return ctx.match().getPlayers().stream()
        .filter(mp -> mp.getNameLegacy().equalsIgnoreCase(name))
        .map(tc.oc.pgm.api.player.MatchPlayer::getId)
        .findFirst()
        .orElse(null);
  }
}
