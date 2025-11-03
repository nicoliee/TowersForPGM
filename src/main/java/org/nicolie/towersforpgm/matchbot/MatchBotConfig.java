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

  public static void loadConfig(FileConfiguration config) {
    tables = config.getStringList("tables");
    discordChannel = config.getString("discordChannel", "");
    rankedRoleId = config.getString("rankedRoleId", "");
    accountsTable = config.getString("ranked.table", "DCAccounts");
    rankedEnabled = config.getBoolean("ranked.enabled", false);
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
}
