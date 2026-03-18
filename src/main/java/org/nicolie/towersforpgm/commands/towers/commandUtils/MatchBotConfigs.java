package org.nicolie.towersforpgm.commands.towers.commandUtils;

import java.util.List;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.rankeds.VoiceChannelManager;

public class MatchBotConfigs {

  public void toggle(Audience audience, Boolean enabled) {
    boolean state = enabled != null ? enabled : MatchBotConfig.isCommandsEnabled();

    if (enabled != null) {
      MatchBotConfig.setCommandsEnabled(enabled);
    }
    String key = state ? "matchbot.commands.enabled" : "matchbot.commands.disabled";
    audience.sendMessage(Component.translatable(key));
  }

  public void statPoints(Audience audience, Boolean enabled) {
    boolean state = enabled != null ? enabled : MatchBotConfig.isStatsPointsEnabled();

    if (enabled != null) {
      MatchBotConfig.setStatsPointsEnabled(enabled);
    }
    String key = state ? "matchbot.stats.points.enabled" : "matchbot.stats.points.disabled";
    audience.sendMessage(Component.translatable(key));
  }

  public void discordChannel(Audience audience, String channel) {
    String state = channel != null ? channel : MatchBotConfig.getDiscordChannel();
    if (channel != null) {
      MatchBotConfig.setDiscordChannel(channel);
    }
    String key =
        channel != null ? "matchbot.discord.channel.set" : "matchbot.discord.channel.current";
    audience.sendMessage(Component.translatable(key, Component.text(state)));
  }

  public void rankedRole(Audience audience, String roleId) {
    String state = roleId != null ? roleId : MatchBotConfig.getRankedRoleId();
    if (roleId != null) {
      MatchBotConfig.setRankedRoleId(roleId);
    }
    String key = roleId != null ? "matchbot.ranked.role.set" : "matchbot.ranked.role.current";
    audience.sendMessage(Component.translatable(key, Component.text(state)));
  }

  public void accounts(Audience audience, String table) {
    String state = table != null ? table : MatchBotConfig.getAccountsTable();
    if (table != null) {
      MatchBotConfig.setAccountsTable(table);
    }
    String key = table != null ? "matchbot.accounts.table.set" : "matchbot.accounts.table.current";
    audience.sendMessage(Component.translatable(key, Component.text(state)));
  }

  public void voiceChat(Audience audience, Boolean enabled) {
    boolean state = enabled != null ? enabled : MatchBotConfig.isVoiceChatEnabled();

    if (enabled != null) {
      MatchBotConfig.setVoiceChatEnabled(enabled);
    }
    String key = state ? "matchbot.voicechat.enabled" : "matchbot.voicechat.disabled";
    audience.sendMessage(Component.translatable(key));
  }

  public void privateChannels(Audience audience, Boolean enabled) {
    boolean state = enabled != null ? enabled : MatchBotConfig.isPrivateChannels();

    if (enabled != null) {
      MatchBotConfig.setPrivateChannels(enabled);
    }
    String key =
        state ? "matchbot.voicechat.private.enabled" : "matchbot.voicechat.private.disabled";
    audience.sendMessage(Component.translatable(key));
  }

  public void voiceChannelInactive(Audience audience, String channelId) {
    String state = channelId != null ? channelId : MatchBotConfig.getInactiveID();
    if (channelId != null) {
      MatchBotConfig.setInactiveID(channelId);
    }
    String key = channelId != null
        ? "matchbot.voicechat.inactive.set"
        : "matchbot.voicechat.inactive.current";
    audience.sendMessage(Component.translatable(key, Component.text(state)));
  }

  public void voiceChannelQueue(Audience audience, String channelId) {
    String state = channelId != null ? channelId : MatchBotConfig.getQueueID();
    if (channelId != null) {
      MatchBotConfig.setQueueID(channelId);
    }
    String key =
        channelId != null ? "matchbot.voicechat.queue.set" : "matchbot.voicechat.queue.current";
    audience.sendMessage(Component.translatable(key, Component.text(state)));
  }

  public void registeredRole(Audience audience, String roleId) {
    String state = roleId != null ? roleId : MatchBotConfig.getRegisteredRoleId();
    if (roleId != null) {
      MatchBotConfig.setRegisteredRoleId(roleId);
    }
    String key =
        roleId != null ? "matchbot.roles.registered.set" : "matchbot.roles.registered.current";
    audience.sendMessage(Component.translatable(key, Component.text(state)));
  }

  public void bronzeRole(Audience audience, String roleId) {
    String state = roleId != null ? roleId : MatchBotConfig.getBronzeRoleId();
    if (roleId != null) {
      MatchBotConfig.setBronzeRoleId(roleId);
    }
    String key = roleId != null ? "matchbot.roles.bronze.set" : "matchbot.roles.bronze.current";
    audience.sendMessage(Component.translatable(key, Component.text(state)));
  }

  public void silverRole(Audience audience, String roleId) {
    String state = roleId != null ? roleId : MatchBotConfig.getSilverRoleId();
    if (roleId != null) {
      MatchBotConfig.setSilverRoleId(roleId);
    }
    String key = roleId != null ? "matchbot.roles.silver.set" : "matchbot.roles.silver.current";
    audience.sendMessage(Component.translatable(key, Component.text(state)));
  }

  public void goldRole(Audience audience, String roleId) {
    String state = roleId != null ? roleId : MatchBotConfig.getGoldRoleId();
    if (roleId != null) {
      MatchBotConfig.setGoldRoleId(roleId);
    }
    String key = roleId != null ? "matchbot.roles.gold.set" : "matchbot.roles.gold.current";
    audience.sendMessage(Component.translatable(key, Component.text(state)));
  }

  public void emeraldRole(Audience audience, String roleId) {
    String state = roleId != null ? roleId : MatchBotConfig.getEmeraldRoleId();
    if (roleId != null) {
      MatchBotConfig.setEmeraldRoleId(roleId);
    }
    String key = roleId != null ? "matchbot.roles.emerald.set" : "matchbot.roles.emerald.current";
    audience.sendMessage(Component.translatable(key, Component.text(state)));
  }

  public void diamondRole(Audience audience, String roleId) {
    String state = roleId != null ? roleId : MatchBotConfig.getDiamondRoleId();
    if (roleId != null) {
      MatchBotConfig.setDiamondRoleId(roleId);
    }
    String key = roleId != null ? "matchbot.roles.diamond.set" : "matchbot.roles.diamond.current";
    audience.sendMessage(Component.translatable(key, Component.text(state)));
  }

  public void addTable(Audience audience, String table) {
    if (MatchBotConfig.getTables().contains(table)) {
      audience.sendMessage(
          Component.translatable("matchbot.tables.alreadyexists", Component.text(table)));
      return;
    }
    MatchBotConfig.addTable(table);
    audience.sendMessage(Component.translatable("matchbot.tables.add", Component.text(table)));
  }

  public void removeTable(Audience audience, String table) {
    MatchBotConfig.removeTable(table);
    audience.sendMessage(Component.translatable("matchbot.tables.remove", Component.text(table)));
  }

  public void channelsRemove(Audience audience) {
    VoiceChannelManager.cleanupRankedChannelsOnStartup();
    audience.sendMessage(
        Component.translatable("matchbot.voicechat.channels.cleanup").color(NamedTextColor.GRAY));
  }

  public void listTables(Audience audience) {
    List<String> tables = MatchBotConfig.getTables();
    if (tables == null || tables.isEmpty()) {
      audience.sendMessage(Component.translatable("matchbot.tables.empty"));
      return;
    }
    audience.sendMessage(Component.translatable("matchbot.tables.list"));
    for (String table : tables) {
      audience.sendMessage(Component.text("§7- §f" + table));
    }
  }

  public void showConfig(Audience audience) {
    audience.sendMessage(Component.text("§6§l=== MatchBot Configuration ==="));
    audience.sendMessage(Component.text("§eCommands: §f" + MatchBotConfig.isCommandsEnabled()));
    audience.sendMessage(
        Component.text("§eStats Points: §f" + MatchBotConfig.isStatsPointsEnabled()));
    audience.sendMessage(
        Component.text("§eDiscord Channel: §f" + MatchBotConfig.getDiscordChannel()));
    audience.sendMessage(Component.text("§eRanked Role: §f" + MatchBotConfig.getRankedRoleId()));
    audience.sendMessage(
        Component.text("§eAccounts Table: §f" + MatchBotConfig.getAccountsTable()));
    audience.sendMessage(Component.text("§eVoice Chat: §f" + MatchBotConfig.isVoiceChatEnabled()));
    if (MatchBotConfig.isVoiceChatEnabled()) {
      audience.sendMessage(
          Component.text("  §7Private Channels: §f" + MatchBotConfig.isPrivateChannels()));
      audience.sendMessage(Component.text("  §7Inactive: §f" + MatchBotConfig.getInactiveID()));
      audience.sendMessage(Component.text("  §7Queue: §f" + MatchBotConfig.getQueueID()));
    }
    audience.sendMessage(Component.text("§6§lRoles:"));
    audience.sendMessage(
        Component.text("  §7Registered: §f" + MatchBotConfig.getRegisteredRoleId()));
    audience.sendMessage(Component.text("  §7Bronze: §f" + MatchBotConfig.getBronzeRoleId()));
    audience.sendMessage(Component.text("  §7Silver: §f" + MatchBotConfig.getSilverRoleId()));
    audience.sendMessage(Component.text("  §7Gold: §f" + MatchBotConfig.getGoldRoleId()));
    audience.sendMessage(Component.text("  §7Emerald: §f" + MatchBotConfig.getEmeraldRoleId()));
    audience.sendMessage(Component.text("  §7Diamond: §f" + MatchBotConfig.getDiamondRoleId()));
  }
}
