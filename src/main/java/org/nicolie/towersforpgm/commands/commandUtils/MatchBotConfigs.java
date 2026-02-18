package org.nicolie.towersforpgm.commands.commandUtils;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.utils.LanguageManager;

public class MatchBotConfigs {

  public void toggleCommands(CommandSender sender) {
    boolean current = MatchBotConfig.isCommandsEnabled();
    MatchBotConfig.setCommandsEnabled(!current);
    sender.sendMessage(LanguageManager.message("matchbot.commands.toggle")
        .replace("{status}", !current ? "enabled" : "disabled"));
  }

  public void toggleStatsPoints(CommandSender sender) {
    boolean current = MatchBotConfig.isStatsPointsEnabled();
    MatchBotConfig.setStatsPointsEnabled(!current);
    sender.sendMessage(LanguageManager.message("matchbot.stats.points.toggle")
        .replace("{status}", !current ? "enabled" : "disabled"));
  }

  public void setDiscordChannel(CommandSender sender, String channel) {
    MatchBotConfig.setDiscordChannel(channel);
    sender.sendMessage(
        LanguageManager.message("matchbot.discord.channel.set").replace("{channel}", channel));
  }

  public void setRankedRole(CommandSender sender, String roleId) {
    MatchBotConfig.setRankedRoleId(roleId);
    sender.sendMessage(
        LanguageManager.message("matchbot.ranked.role.set").replace("{role}", roleId));
  }

  public void setAccountsTable(CommandSender sender, String table) {
    MatchBotConfig.setAccountsTable(table);
    sender.sendMessage(
        LanguageManager.message("matchbot.accounts.table.set").replace("{table}", table));
  }

  public void toggleVoiceChat(CommandSender sender) {
    boolean current = MatchBotConfig.isVoiceChatEnabled();
    MatchBotConfig.setVoiceChatEnabled(!current);
    sender.sendMessage(LanguageManager.message("matchbot.voicechat.toggle")
        .replace("{status}", !current ? "enabled" : "disabled"));
  }

  public void togglePrivateChannels(CommandSender sender) {
    boolean current = MatchBotConfig.isPrivateChannels();
    MatchBotConfig.setPrivateChannels(!current);
    sender.sendMessage(LanguageManager.message("matchbot.voicechat.private.toggle")
        .replace("{status}", !current ? "enabled" : "disabled"));
  }

  public void setVoiceChannelInactive(CommandSender sender, String channelId) {
    MatchBotConfig.setInactiveID(channelId);
    sender.sendMessage(
        LanguageManager.message("matchbot.voicechat.inactive.set").replace("{channel}", channelId));
  }

  public void setVoiceChannelQueue(CommandSender sender, String channelId) {
    MatchBotConfig.setQueueID(channelId);
    sender.sendMessage(
        LanguageManager.message("matchbot.voicechat.queue.set").replace("{channel}", channelId));
  }

  public void setVoiceChannelTeam1(CommandSender sender, String channelId) {
    MatchBotConfig.setTeam1ID(channelId);
    sender.sendMessage(
        LanguageManager.message("matchbot.voicechat.team1.set").replace("{channel}", channelId));
  }

  public void setVoiceChannelTeam2(CommandSender sender, String channelId) {
    MatchBotConfig.setTeam2ID(channelId);
    sender.sendMessage(
        LanguageManager.message("matchbot.voicechat.team2.set").replace("{channel}", channelId));
  }

  public void setRegisteredRole(CommandSender sender, String roleId) {
    MatchBotConfig.setRegisteredRoleId(roleId);
    sender.sendMessage(
        LanguageManager.message("matchbot.roles.registered.set").replace("{role}", roleId));
  }

  public void setBronzeRole(CommandSender sender, String roleId) {
    MatchBotConfig.setBronzeRoleId(roleId);
    sender.sendMessage(
        LanguageManager.message("matchbot.roles.bronze.set").replace("{role}", roleId));
  }

  public void setSilverRole(CommandSender sender, String roleId) {
    MatchBotConfig.setSilverRoleId(roleId);
    sender.sendMessage(
        LanguageManager.message("matchbot.roles.silver.set").replace("{role}", roleId));
  }

  public void setGoldRole(CommandSender sender, String roleId) {
    MatchBotConfig.setGoldRoleId(roleId);
    sender.sendMessage(
        LanguageManager.message("matchbot.roles.gold.set").replace("{role}", roleId));
  }

  public void setEmeraldRole(CommandSender sender, String roleId) {
    MatchBotConfig.setEmeraldRoleId(roleId);
    sender.sendMessage(
        LanguageManager.message("matchbot.roles.emerald.set").replace("{role}", roleId));
  }

  public void setDiamondRole(CommandSender sender, String roleId) {
    MatchBotConfig.setDiamondRoleId(roleId);
    sender.sendMessage(
        LanguageManager.message("matchbot.roles.diamond.set").replace("{role}", roleId));
  }

  public void addTable(CommandSender sender, String table) {
    MatchBotConfig.addTable(table);
    sender.sendMessage(LanguageManager.message("matchbot.tables.add").replace("{table}", table));
  }

  public void removeTable(CommandSender sender, String table) {
    MatchBotConfig.removeTable(table);
    sender.sendMessage(LanguageManager.message("matchbot.tables.remove").replace("{table}", table));
  }

  public void listTables(CommandSender sender) {
    List<String> tables = MatchBotConfig.getTables();
    if (tables == null || tables.isEmpty()) {
      sender.sendMessage(LanguageManager.message("matchbot.tables.empty"));
      return;
    }
    sender.sendMessage(LanguageManager.message("matchbot.tables.list"));
    for (String table : tables) {
      sender.sendMessage("§7- §f" + table);
    }
  }

  public void showConfig(CommandSender sender) {
    sender.sendMessage("§6§l=== MatchBot Configuration ===");
    sender.sendMessage("§eCommands: §f" + MatchBotConfig.isCommandsEnabled());
    sender.sendMessage("§eStats Points: §f" + MatchBotConfig.isStatsPointsEnabled());
    sender.sendMessage("§eDiscord Channel: §f" + MatchBotConfig.getDiscordChannel());
    sender.sendMessage("§eRanked Role: §f" + MatchBotConfig.getRankedRoleId());
    sender.sendMessage("§eAccounts Table: §f" + MatchBotConfig.getAccountsTable());
    sender.sendMessage("§eVoice Chat: §f" + MatchBotConfig.isVoiceChatEnabled());
    if (MatchBotConfig.isVoiceChatEnabled()) {
      sender.sendMessage("  §7Private Channels: §f" + MatchBotConfig.isPrivateChannels());
      sender.sendMessage("  §7Inactive: §f" + MatchBotConfig.getInactiveID());
      sender.sendMessage("  §7Queue: §f" + MatchBotConfig.getQueueID());
      sender.sendMessage("  §7Team 1: §f" + MatchBotConfig.getTeam1ID());
      sender.sendMessage("  §7Team 2: §f" + MatchBotConfig.getTeam2ID());
    }
    sender.sendMessage("§6§lRoles:");
    sender.sendMessage("  §7Registered: §f" + MatchBotConfig.getRegisteredRoleId());
    sender.sendMessage("  §7Bronze: §f" + MatchBotConfig.getBronzeRoleId());
    sender.sendMessage("  §7Silver: §f" + MatchBotConfig.getSilverRoleId());
    sender.sendMessage("  §7Gold: §f" + MatchBotConfig.getGoldRoleId());
    sender.sendMessage("  §7Emerald: §f" + MatchBotConfig.getEmeraldRoleId());
    sender.sendMessage("  §7Diamond: §f" + MatchBotConfig.getDiamondRoleId());
  }
}
