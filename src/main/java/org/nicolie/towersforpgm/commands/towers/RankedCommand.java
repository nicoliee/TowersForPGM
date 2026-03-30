package org.nicolie.towersforpgm.commands.towers;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.commands.towers.commandUtils.RankedConfig;
import org.nicolie.towersforpgm.draft.map.MapVoteConfig;
import org.nicolie.towersforpgm.utils.Permissions;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.*;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.suggestion.Suggestions;
import tc.oc.pgm.lib.org.incendo.cloud.context.CommandContext;
import tc.oc.pgm.lib.org.incendo.cloud.context.CommandInput;
import tc.oc.pgm.rotation.pools.MapPool;
import tc.oc.pgm.util.Audience;

public class RankedCommand {
  private final RankedConfig rankedConfig = new RankedConfig();

  @Command("towers ranked addmap [table]")
  // @CommandDescription("Add a map to ranked pool")
  @Permission(Permissions.ADMIN)
  public void rankedAddMap(
      Audience audience, CommandSender sender, @Argument("table") String table) {
    rankedConfig.tableAdd(audience, table);
  }

  @Command("towers ranked matchmaking [value]")
  // @CommandDescription("Toggle ranked matchmaking")
  @Permission(Permissions.ADMIN)
  public void rankedMatchmaking(
      Audience audience, CommandSender sender, @Argument("value") Boolean value) {
    rankedConfig.matchmaking(audience, value);
  }

  @Command("towers ranked maxsize [size]")
  // @CommandDescription("Set ranked max size")
  @Permission(Permissions.ADMIN)
  public void rankedMaxSize(
      Audience audience, CommandSender sender, @Argument("size") String size) {
    rankedConfig.maxSize(audience, size);
  }

  @Command("towers ranked minsize [size]")
  // @CommandDescription("Set ranked min size")
  @Permission(Permissions.ADMIN)
  public void rankedMinSize(
      Audience audience, CommandSender sender, @Argument("size") String size) {
    rankedConfig.minSize(audience, size);
  }

  @Command("towers ranked order [order]")
  // @CommandDescription("Set ranked order")
  @Permission(Permissions.ADMIN)
  public void rankedOrder(
      Audience audience, CommandSender sender, @Argument("order") String order) {
    rankedConfig.order(audience, order);
  }

  @Command("towers ranked profile [profile]")
  // @CommandDescription("Set ranked profile")
  @Permission(Permissions.ADMIN)
  public void rankedProfile(
      Audience audience,
      CommandSender sender,
      @Argument(value = "profile", suggestions = "rankedProfiles") String profile) {
    rankedConfig.profile(audience, profile);
  }

  @Suggestions("rankedProfiles")
  public List<String> rankedProfileSuggestions(
      CommandContext<CommandSender> context, CommandInput input) {
    Set<String> profiles = TowersForPGM.getInstance().config().ranked().getProfileNames();
    return List.copyOf(profiles);
  }

  @Command("towers ranked removemap [table]")
  // @CommandDescription("Remove a map from ranked pool")
  @Permission(Permissions.ADMIN)
  public void rankedRemoveMap(
      Audience audience, CommandSender sender, @Argument("table") String table) {
    rankedConfig.tableRemove(audience, table);
  }

  @Command("towers ranked reroll [value]")
  // @CommandDescription("Toggle ranked reroll")
  @Permission(Permissions.ADMIN)
  public void rankedReroll(
      Audience audience, CommandSender sender, @Argument("value") Boolean value) {
    rankedConfig.reroll(audience, value);
  }

  @Command("towers ranked vote [value]")
  // @CommandDescription("Set ranked map vote mode")
  @Permission(Permissions.ADMIN)
  public void rankedMapVote(
      Audience audience,
      CommandSender sender,
      @Argument(value = "value", suggestions = "voteModes") String value) {
    rankedConfig.mapVote(audience, value);
  }

  @Suggestions("voteModes")
  public List<String> voteModeSuggestions(
      CommandContext<CommandSender> context, CommandInput input) {
    return Arrays.stream(MapVoteConfig.VoteMode.values()).map(Enum::name).toList();
  }

  @Command("towers ranked pool [pool]")
  // @CommandDescription("Set ranked pool")
  @Permission(Permissions.ADMIN)
  public void rankedSetPool(
      Audience audience, CommandSender sender, @Argument("pool") MapPool pool) {
    rankedConfig.pool(audience, pool);
  }
}
