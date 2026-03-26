package org.nicolie.towersforpgm.commands.towers;

import java.util.List;
import java.util.Set;
import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.commands.towers.commandUtils.MatchBotConfigs;
import org.nicolie.towersforpgm.configs.tables.TableType;
import org.nicolie.towersforpgm.utils.Permissions;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.*;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.suggestion.Suggestions;
import tc.oc.pgm.lib.org.incendo.cloud.context.CommandContext;
import tc.oc.pgm.lib.org.incendo.cloud.context.CommandInput;
import tc.oc.pgm.util.Audience;

public class MatchBotCommand {
  private final MatchBotConfigs matchBotConfig = new MatchBotConfigs();

  private boolean isEnabled(Audience audience) {
    if (!TowersForPGM.getInstance().isMatchBotEnabled()) {
      audience.sendWarning(net.kyori.adventure.text.Component.translatable("matchbot.notEnabled"));
      return false;
    }
    return true;
  }

  @Command("towers matchbot accounts [table]")
  // @CommandDescription("Set matchbot accounts table")
  @Permission(Permissions.ADMIN)
  public void matchBotAccounts(
      Audience audience,
      CommandSender sender,
      @Argument(value = "table", suggestions = "allTables") String table) {
    if (!isEnabled(audience)) return;
    matchBotConfig.accounts(audience, table);
  }

  @Command("towers matchbot channel [value]")
  // @CommandDescription("Set matchbot Discord channel")
  @Permission(Permissions.ADMIN)
  public void matchBotChannel(
      Audience audience, CommandSender sender, @Argument("value") String value) {
    if (!isEnabled(audience)) return;
    matchBotConfig.discordChannel(audience, value);
  }

  @Command("towers matchbot config")
  // @CommandDescription("Show matchbot config")
  @Permission(Permissions.ADMIN)
  public void matchBotConfig(Audience audience, CommandSender sender) {
    if (!isEnabled(audience)) return;
    matchBotConfig.showConfig(audience);
  }

  @Command("towers matchbot rankedrole [roleId]")
  // @CommandDescription("Set matchbot ranked role")
  @Permission(Permissions.ADMIN)
  public void matchBotRankedRole(
      Audience audience, CommandSender sender, @Argument("roleId") String roleId) {
    if (!isEnabled(audience)) return;
    matchBotConfig.rankedRole(audience, roleId);
  }

  @Command("towers matchbot roles bronze [roleId]")
  @Permission(Permissions.ADMIN)
  public void matchBotRolesBronze(
      Audience audience, CommandSender sender, @Argument("roleId") String roleId) {
    if (!isEnabled(audience)) return;
    matchBotConfig.bronzeRole(audience, roleId);
  }

  @Command("towers matchbot roles diamond [roleId]")
  @Permission(Permissions.ADMIN)
  public void matchBotRolesDiamond(
      Audience audience, CommandSender sender, @Argument("roleId") String roleId) {
    if (!isEnabled(audience)) return;
    matchBotConfig.diamondRole(audience, roleId);
  }

  @Command("towers matchbot roles emerald [roleId]")
  @Permission(Permissions.ADMIN)
  public void matchBotRolesEmerald(
      Audience audience, CommandSender sender, @Argument("roleId") String roleId) {
    if (!isEnabled(audience)) return;
    matchBotConfig.emeraldRole(audience, roleId);
  }

  @Command("towers matchbot roles gold [roleId]")
  @Permission(Permissions.ADMIN)
  public void matchBotRolesGold(
      Audience audience, CommandSender sender, @Argument("roleId") String roleId) {
    if (!isEnabled(audience)) return;
    matchBotConfig.goldRole(audience, roleId);
  }

  @Command("towers matchbot roles registered [roleId]")
  @Permission(Permissions.ADMIN)
  public void matchBotRolesRegistered(
      Audience audience, CommandSender sender, @Argument("roleId") String roleId) {
    if (!isEnabled(audience)) return;
    matchBotConfig.registeredRole(audience, roleId);
  }

  @Command("towers matchbot roles silver [roleId]")
  @Permission(Permissions.ADMIN)
  public void matchBotRolesSilver(
      Audience audience, CommandSender sender, @Argument("roleId") String roleId) {
    if (!isEnabled(audience)) return;
    matchBotConfig.silverRole(audience, roleId);
  }

  @Command("towers matchbot stats [value]")
  // @CommandDescription("Toggle matchbot stat points")
  @Permission(Permissions.ADMIN)
  public void matchBotStats(
      Audience audience, CommandSender sender, @Argument("value") Boolean value) {
    if (!isEnabled(audience)) return;
    matchBotConfig.statPoints(audience, value);
  }

  @Command("towers matchbot tables add <table>")
  // @CommandDescription("Add a matchbot table")
  @Permission(Permissions.ADMIN)
  public void matchBotTablesAdd(
      Audience audience,
      CommandSender sender,
      @Argument(value = "table", suggestions = "allTables") String table) {
    if (!isEnabled(audience)) return;
    matchBotConfig.addTable(audience, table);
  }

  @Command("towers matchbot tables list")
  // @CommandDescription("List matchbot tables")
  @Permission(Permissions.ADMIN)
  public void matchBotTablesList(Audience audience, CommandSender sender) {
    if (!isEnabled(audience)) return;
    matchBotConfig.listTables(audience);
  }

  @Command("towers matchbot tables remove <table>")
  // @CommandDescription("Remove a matchbot table")
  @Permission(Permissions.ADMIN)
  public void matchBotTablesRemove(
      Audience audience,
      CommandSender sender,
      @Argument(value = "table", suggestions = "allTables") String table) {
    if (!isEnabled(audience)) return;
    matchBotConfig.removeTable(audience, table);
  }

  @Command("towers matchbot toggle [value]")
  // @CommandDescription("Toggle matchbot")
  public void matchBotToggle(
      Audience audience, CommandSender sender, @Argument("value") Boolean value) {
    if (!isEnabled(audience)) return;
    matchBotConfig.toggle(audience, value);
  }

  @Command("towers matchbot voice inactive [channelId]")
  public void matchBotVoiceInactive(
      Audience audience, CommandSender sender, @Argument("channelId") String channelId) {
    if (!isEnabled(audience)) return;
    matchBotConfig.voiceChannelInactive(audience, channelId);
  }

  @Command("towers matchbot voice private [value]")
  public void matchBotVoicePrivate(
      Audience audience, CommandSender sender, @Argument("value") Boolean value) {
    if (!isEnabled(audience)) return;
    matchBotConfig.privateChannels(audience, value);
  }

  @Command("towers matchbot voice queue [channelId]")
  public void matchBotVoiceQueue(
      Audience audience, CommandSender sender, @Argument("channelId") String channelId) {
    if (!isEnabled(audience)) return;
    matchBotConfig.voiceChannelQueue(audience, channelId);
  }

  @Command("towers matchbot voice reset")
  public void matchBotVoiceReset(Audience audience, CommandSender sender) {
    if (!isEnabled(audience)) return;
    matchBotConfig.channelsRemove(audience);
  }

  @Command("towers matchbot voice toggle [value]")
  public void matchBotVoiceToggle(
      Audience audience, CommandSender sender, @Argument("value") Boolean value) {
    if (!isEnabled(audience)) return;
    matchBotConfig.voiceChat(audience, value);
  }

  @Suggestions("allTables")
  public List<String> allTablesSuggestions(
      CommandContext<CommandSender> context, CommandInput input) {
    Set<String> tables =
        TowersForPGM.getInstance().config().databaseTables().getTables(TableType.ALL);
    return List.copyOf(tables);
  }
}
