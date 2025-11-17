package org.nicolie.towersforpgm.matchbot;

import java.util.List;
import me.tbg.match.bot.configs.BotConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.nicolie.towersforpgm.utils.ConfigManager;

public class MatchBotConfig {
  // tables
  private static List<String> tables;

  // commands
  private static boolean statsCommandEnabled;
  private static boolean topCommandEnabled;
  private static boolean historyCommandEnabled;
  private static boolean linkCommandEnabled;

  // stats
  private static boolean statsPointsEnabled;

  // ranked
  private static String discordChannel;
  private static String rankedRoleId;
  private static boolean rankedEnabled;
  private static String accountsTable;

  // ranked.voice-chat
  private static String inactiveID;
  private static String queueID;
  private static String team1ID;
  private static String team2ID;

  // ranked.roles
  private static String registeredRoleId;
  private static String bronzeRoleId;
  private static String silverRoleId;
  private static String goldRoleId;
  private static String emeraldRoleId;
  private static String diamondRoleId;

  public static void loadConfig(FileConfiguration config) {
    tables = config.getStringList("tables");

    // commands
    statsCommandEnabled = config.getBoolean("commands.stats", true);
    topCommandEnabled = config.getBoolean("commands.top", true);
    historyCommandEnabled = config.getBoolean("commands.history", true);
    linkCommandEnabled = config.getBoolean("commands.link", true);

    // stats
    statsPointsEnabled = config.getBoolean("stats.points", true);

    // ranked
    discordChannel = config.getString("ranked.discordChannel", "");
    rankedRoleId = config.getString("ranked.rankedRoleId", "");
    rankedEnabled = config.getBoolean("ranked.enabled", false);
    accountsTable = config.getString("ranked.table", "DCAccounts");

    // voice-chat
    inactiveID = config.getString("ranked.voice-chat.inactive", "");
    queueID = config.getString("ranked.voice-chat.queue", "");
    team1ID = config.getString("ranked.voice-chat.team1", "");
    team2ID = config.getString("ranked.voice-chat.team2", "");

    // roles under ranked
    registeredRoleId = config.getString("ranked.roles.registered", "");
    bronzeRoleId = config.getString("ranked.roles.bronze", "");
    silverRoleId = config.getString("ranked.roles.silver", "");
    goldRoleId = config.getString("ranked.roles.gold", "");
    emeraldRoleId = config.getString("ranked.roles.emerald", "");
    diamondRoleId = config.getString("ranked.roles.diamond", "");

    BotConfig.addBlacklist(ConfigManager.getRankedMaps());
  }

  // tables
  public static List<String> getTables() {
    return tables;
  }

  // commands
  public static boolean isStatsCommandEnabled() {
    return statsCommandEnabled;
  }

  public static boolean isTopCommandEnabled() {
    return topCommandEnabled;
  }

  public static boolean isHistoryCommandEnabled() {
    return historyCommandEnabled;
  }

  public static boolean isLinkCommandEnabled() {
    return linkCommandEnabled;
  }

  // stats
  public static boolean isStatsPointsEnabled() {
    return statsPointsEnabled;
  }

  // ranked
  public static String getDiscordChannel() {
    return discordChannel;
  }

  public static String getRankedRoleId() {
    return rankedRoleId;
  }

  public static boolean isRankedEnabled() {
    return rankedEnabled;
  }

  public static String getAccountsTable() {
    return accountsTable;
  }

  // voice-chat
  public static String getInactiveID() {
    return inactiveID;
  }

  public static String getQueueID() {
    return queueID;
  }

  public static String getTeam1ID() {
    return team1ID;
  }

  public static String getTeam2ID() {
    return team2ID;
  }

  // roles
  public static String getRegisteredRoleId() {
    return registeredRoleId;
  }

  public static String getBronzeRoleId() {
    return bronzeRoleId;
  }

  public static String getSilverRoleId() {
    return silverRoleId;
  }

  public static String getGoldRoleId() {
    return goldRoleId;
  }

  public static String getEmeraldRoleId() {
    return emeraldRoleId;
  }

  public static String getDiamondRoleId() {
    return diamondRoleId;
  }
}
