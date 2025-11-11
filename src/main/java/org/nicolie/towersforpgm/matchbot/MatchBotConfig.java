package org.nicolie.towersforpgm.matchbot;

import java.util.List;
import me.tbg.match.bot.configs.BotConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.nicolie.towersforpgm.utils.ConfigManager;

public class MatchBotConfig {
  private static List<String> tables;
  private static String discordChannel;
  private static String rankedRoleId;
  private static String accountsTable;
  private static boolean rankedEnabled;
  private static String registeredRoleId;
  private static String inactiveID;
  private static String queueID;
  private static String team1ID;
  private static String team2ID;
  private static String bronzeRoleId;
  private static String silverRoleId;
  private static String goldRoleId;
  private static String emeraldRoleId;
  private static String diamondRoleId;

  private static boolean statsCommandEnabled;
  private static boolean topCommandEnabled;
  private static boolean historyCommandEnabled;
  private static boolean linkCommandEnabled;

  public static void loadConfig(FileConfiguration config) {
    tables = config.getStringList("tables");
    discordChannel = config.getString("ranked.discordChannel", "");
    rankedRoleId = config.getString("ranked.rankedRoleId", "");
    rankedEnabled = config.getBoolean("ranked.enabled", false);
    accountsTable = config.getString("ranked.table", "DCAccounts");
    registeredRoleId = config.getString("ranked.registeredRoleId", "");

    inactiveID = config.getString("ranked.inactiveID", "");
    queueID = config.getString("ranked.queueID", "");
    team1ID = config.getString("ranked.team1ID", "");
    team2ID = config.getString("ranked.team2ID", "");

    bronzeRoleId = config.getString("roles.bronzeRoleId", "");
    silverRoleId = config.getString("roles.silverRoleId", "");
    goldRoleId = config.getString("roles.goldRoleId", "");
    emeraldRoleId = config.getString("roles.emeraldRoleId", "");
    diamondRoleId = config.getString("roles.diamondRoleId", "");

    statsCommandEnabled = config.getBoolean("commands.stats", true);
    topCommandEnabled = config.getBoolean("commands.top", true);
    historyCommandEnabled = config.getBoolean("commands.history", true);
    linkCommandEnabled = config.getBoolean("commands.link", true);

    BotConfig.addBlacklist(ConfigManager.getRankedMaps());
  }

  public static List<String> getTables() {
    return tables;
  }

  public static String getDiscordChannel() {
    return discordChannel;
  }

  public static String getRankedRoleId() {
    return rankedRoleId;
  }

  public static String getAccountsTable() {
    return accountsTable;
  }

  public static boolean isRankedEnabled() {
    return rankedEnabled;
  }

  public static String getRegisteredRoleId() {
    return registeredRoleId;
  }

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
}
