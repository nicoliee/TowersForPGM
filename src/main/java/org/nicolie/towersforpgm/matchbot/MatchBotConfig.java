package org.nicolie.towersforpgm.matchbot;

import java.util.List;
import me.tbg.match.bot.configs.BotConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.nicolie.towersforpgm.utils.ConfigManager;

public class MatchBotConfig {
  private static List<String> tables;
  private static String discordChannel;
  private static String rankedRoleId;

  public static void loadConfig(FileConfiguration config) {
    tables = config.getStringList("tables");
    discordChannel = config.getString("discordChannel", "");
    rankedRoleId = config.getString("rankedRoleId", "");
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
}
