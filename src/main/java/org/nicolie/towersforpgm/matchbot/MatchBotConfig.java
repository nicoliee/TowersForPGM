package org.nicolie.towersforpgm.matchbot;

import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.nicolie.towersforpgm.TowersForPGM;

public class MatchBotConfig {
  private static boolean commands;
  // tables
  private static List<String> tables;

  // stats
  private static boolean statsPointsEnabled;

  // ranked
  private static String discordChannel;
  private static String rankedRoleId;
  private static String accountsTable;

  // ranked.voice-chat
  private static boolean voiceChatEnabled;
  private static boolean privateChannels;
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
    commands = config.getBoolean("commands", true);
    tables = config.getStringList("tables");
    // stats
    statsPointsEnabled = config.getBoolean("stats.points", true);

    // ranked
    discordChannel = config.getString("ranked.discordChannel", "");
    rankedRoleId = config.getString("ranked.rankedRoleId", "");
    accountsTable = config.getString("ranked.table", "DCAccounts");

    // voice-chat
    voiceChatEnabled = config.getBoolean("ranked.voice-chat.enabled", false);
    privateChannels = config.getBoolean("ranked.voice-chat.privateChannels", false);
    inactiveID = config.getString("ranked.voice-chat.inactive", "");
    queueID = config.getString("ranked.voice-chat.queue", "");
    team1ID = config.getString("ranked.voice-chat.team1", "");
    team2ID = config.getString("ranked.voice-chat.team2", "");

    // roles
    registeredRoleId = config.getString("ranked.roles.registered", "");
    bronzeRoleId = config.getString("ranked.roles.bronze", "");
    silverRoleId = config.getString("ranked.roles.silver", "");
    goldRoleId = config.getString("ranked.roles.gold", "");
    emeraldRoleId = config.getString("ranked.roles.emerald", "");
    diamondRoleId = config.getString("ranked.roles.diamond", "");

    // BotConfig.addBlacklist(plugin.config().ranked().getRankedMaps());
  }

  private static void saveToFile(String path, Object value) {
    try {
      TowersForPGM plugin = TowersForPGM.getInstance();
      if (plugin == null) return;
      FileConfiguration cfg = plugin.getMatchBotConfig();
      if (cfg == null) return;
      cfg.set(path, value);
      plugin.saveMatchBotConfig();
      // reload in-memory values after saving
      loadConfig(cfg);
    } catch (Exception ignored) {
    }
  }

  public static boolean isCommandsEnabled() {
    return commands;
  }

  public static void setCommandsEnabled(boolean enabled) {
    commands = enabled;
    saveToFile("commands", enabled);
  }

  // tables
  public static void addTable(String table) {
    if (tables == null) tables = new java.util.ArrayList<>();
    if (!tables.contains(table)) {
      tables.add(table);
      saveToFile("tables", tables);
    }
  }

  public static void removeTable(String table) {
    if (tables == null) return;
    if (tables.contains(table)) {
      tables.remove(table);
      saveToFile("tables", tables);
    }
  }

  // stats
  public static void setStatsPointsEnabled(boolean enabled) {
    statsPointsEnabled = enabled;
    saveToFile("stats.points", enabled);
  }

  // ranked
  public static void setDiscordChannel(String channel) {
    discordChannel = channel;
    saveToFile("ranked.discordChannel", channel);
  }

  public static void setRankedRoleId(String roleId) {
    rankedRoleId = roleId;
    saveToFile("ranked.rankedRoleId", roleId);
  }

  public static void setVoiceChatEnabled(boolean enabled) {
    voiceChatEnabled = enabled;
    saveToFile("ranked.voice-chat.enabled", enabled);
  }

  public static void setPrivateChannels(boolean enabled) {
    privateChannels = enabled;
    saveToFile("ranked.voice-chat.privateChannels", enabled);
  }

  public static void setAccountsTable(String table) {
    accountsTable = table;
    saveToFile("ranked.table", table);
  }

  // ranked.voice-chat
  public static void setInactiveID(String id) {
    inactiveID = id;
    saveToFile("ranked.voice-chat.inactive", id);
  }

  public static void setQueueID(String id) {
    queueID = id;
    saveToFile("ranked.voice-chat.queue", id);
  }

  public static void setTeam1ID(String id) {
    team1ID = id;
    saveToFile("ranked.voice-chat.team1", id);
  }

  public static void setTeam2ID(String id) {
    team2ID = id;
    saveToFile("ranked.voice-chat.team2", id);
  }

  // ranked.roles
  public static void setRegisteredRoleId(String id) {
    registeredRoleId = id;
    saveToFile("ranked.roles.registered", id);
  }

  public static void setBronzeRoleId(String id) {
    bronzeRoleId = id;
    saveToFile("ranked.roles.bronze", id);
  }

  public static void setSilverRoleId(String id) {
    silverRoleId = id;
    saveToFile("ranked.roles.silver", id);
  }

  public static void setGoldRoleId(String id) {
    goldRoleId = id;
    saveToFile("ranked.roles.gold", id);
  }

  public static void setEmeraldRoleId(String id) {
    emeraldRoleId = id;
    saveToFile("ranked.roles.emerald", id);
  }

  public static void setDiamondRoleId(String id) {
    diamondRoleId = id;
    saveToFile("ranked.roles.diamond", id);
  }

  // tables
  public static List<String> getTables() {
    return tables;
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

  public static boolean isVoiceChatEnabled() {
    return voiceChatEnabled;
  }

  public static boolean isPrivateChannels() {
    return privateChannels;
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
