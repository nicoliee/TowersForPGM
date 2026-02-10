package org.nicolie.towersforpgm.commands.commandUtils;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.tables.TableInfo;
import org.nicolie.towersforpgm.database.TableManager;
import org.nicolie.towersforpgm.rankeds.RankedProfile;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;

public class RankedConfig {
  private final TowersForPGM plugin = TowersForPGM.getInstance();

  public void minSize(CommandSender sender, String size) {
    if (size == null || size.isEmpty()) {
      String message = LanguageManager.message("ranked.config.minSize.current")
          .replace("{size}", String.valueOf(plugin.config().ranked().getRankedMinSize()));
      SendMessage.sendToPlayer(sender, message);
      return;
    }

    try {
      int newSize = Integer.parseInt(size);
      if (newSize < 2 || newSize % 2 != 0) {
        SendMessage.sendToPlayer(sender, LanguageManager.message("ranked.config.minSize.invalid"));
        return;
      }
      int maxSize = plugin.config().ranked().getRankedMaxSize();
      if (maxSize > 0 && newSize > maxSize) {
        SendMessage.sendToPlayer(
            sender, LanguageManager.message("ranked.config.minSize.greaterThanMax"));
        return;
      }
      plugin.config().ranked().setRankedMinSize(newSize);
      String message = LanguageManager.message("ranked.config.minSize.set")
          .replace("{size}", String.valueOf(newSize));
      SendMessage.sendToPlayer(sender, message);
    } catch (NumberFormatException e) {
      SendMessage.sendToPlayer(
          sender, LanguageManager.message("ranked.config.minSize.invalidFormat"));
    }
  }

  public void setRerollOption(CommandSender sender, boolean isEnabled) {
    plugin.config().ranked().setReroll(isEnabled);
    String message = isEnabled
        ? LanguageManager.message("ranked.config.rerollEnabled")
        : LanguageManager.message("ranked.config.rerollDisabled");
    SendMessage.sendToPlayer(sender, message);
  }

  public void maxSize(CommandSender sender, String size) {
    if (size == null || size.isEmpty()) {
      String message = LanguageManager.message("ranked.config.maxSize.current")
          .replace("{size}", String.valueOf(plugin.config().ranked().getRankedMaxSize()));
      SendMessage.sendToPlayer(sender, message);
      return;
    }

    try {
      int newSize = Integer.parseInt(size);
      if (newSize < 2 || newSize % 2 != 0) {
        SendMessage.sendToPlayer(sender, LanguageManager.message("ranked.config.maxSize.invalid"));
        return;
      }
      int minSize = plugin.config().ranked().getRankedMinSize();
      if (minSize > 0 && newSize < minSize) {
        SendMessage.sendToPlayer(
            sender, LanguageManager.message("ranked.config.maxSize.lessThanMin"));
        return;
      }
      plugin.config().ranked().setRankedMaxSize(newSize);
      String message = LanguageManager.message("ranked.config.maxSize.set")
          .replace("{size}", String.valueOf(newSize));
      SendMessage.sendToPlayer(sender, message);
    } catch (NumberFormatException e) {
      SendMessage.sendToPlayer(
          sender, LanguageManager.message("ranked.config.maxSize.invalidFormat"));
    }
  }

  public void draftOrder(CommandSender sender, String order) {
    if (order == null || order.isEmpty()) {
      String message = LanguageManager.message("ranked.config.order.current")
          .replace("{order}", plugin.config().ranked().getRankedOrder());
      SendMessage.sendToPlayer(sender, message);
      return;
    }

    if (!order.matches("A[AB]+")) {
      String errorMessage = LanguageManager.message("ranked.config.order.invalid");
      SendMessage.sendToPlayer(sender, errorMessage);
      return;
    }

    plugin.config().ranked().setRankedOrder(order);
    String message = LanguageManager.message("ranked.config.order.set").replace("{order}", order);
    SendMessage.sendToPlayer(sender, message);
  }

  public void addTable(CommandSender sender, String table) {
    TableInfo tableInfo = TowersForPGM.getInstance().config().databaseTables().getTableInfo(table);
    boolean isRankedTable = tableInfo != null && tableInfo.isRanked();
    if (isRankedTable) {
      SendMessage.sendToPlayer(sender, LanguageManager.message("ranked.config.table.exists"));
      return;
    }

    plugin.config().databaseTables().addTable(table, true);
    TableManager.createTable(table);
    String message = LanguageManager.message("ranked.config.table.added").replace("{table}", table);
    SendMessage.sendToPlayer(sender, message);
  }

  public void deleteTable(CommandSender sender, String table) {
    if (plugin.config().databaseTables().getTableInfo(table) == null) {
      SendMessage.sendToPlayer(
          sender,
          LanguageManager.message("ranked.config.table.notFound").replace("{table}", table));
      return;
    }

    plugin.config().databaseTables().removeTable(table);
    String message =
        LanguageManager.message("ranked.config.table.deleted").replace("{table}", table);
    SendMessage.sendToPlayer(sender, message);
  }

  public void addMap(CommandSender sender) {
    SendMessage.sendToPlayer(
        sender, "§cEste comando está obsoleto. Usa /towers ranked setpool <pool>");
  }

  public void removeMap(CommandSender sender) {
    SendMessage.sendToPlayer(
        sender, "§cEste comando está obsoleto. Usa /towers ranked setpool <pool>");
  }

  public void setPool(CommandSender sender, String poolName) {
    if (poolName == null || poolName.isEmpty()) {
      // Show current pool
      String currentPool = plugin.config().ranked().getMapPool();
      SendMessage.sendToPlayer(
          sender,
          LanguageManager.message("ranked.config.pool.current").replace("{pool}", currentPool));
      showPoolMaps(sender, currentPool);
      return;
    }

    // Set new pool
    boolean success = plugin.config().ranked().setPool(poolName);
    if (success) {
      List<String> maps = plugin.config().ranked().getMapsFromPool(poolName);
      String message = LanguageManager.message("ranked.config.pool.set")
          .replace("{pool}", poolName)
          .replace("{count}", String.valueOf(maps.size()));
      SendMessage.sendToPlayer(sender, message);
      showPoolMaps(sender, poolName);
    } else {
      SendMessage.sendToPlayer(
          sender,
          LanguageManager.message("ranked.config.pool.notFound").replace("{pool}", poolName));
    }
  }

  public void matchmaking(CommandSender sender, Boolean isEnabled) {
    if (isEnabled == null) {
      boolean isRankedMatchmaking = plugin.config().ranked().isRankedMatchmaking();
      String status = isRankedMatchmaking
          ? LanguageManager.message("ranked.config.matchmaking.statusEnabled")
          : LanguageManager.message("ranked.config.matchmaking.statusDisabled");
      String message =
          LanguageManager.message("ranked.config.matchmaking.status").replace("{status}", status);
      SendMessage.sendToPlayer(sender, message);
      return;
    }
    plugin.config().ranked().setRankedMatchmaking(isEnabled);
    String message = isEnabled
        ? LanguageManager.message("ranked.config.matchmaking.enabled")
        : LanguageManager.message("ranked.config.matchmaking.disabled");
    SendMessage.sendToPlayer(sender, message);
  }

  public void showProfile(CommandSender sender, String profileName) {
    RankedProfile profile = (profileName == null || profileName.isEmpty())
        ? plugin.config().ranked().getActiveProfile()
        : plugin.config().ranked().getProfile(profileName);

    if (profile == null) {
      String message = (profileName == null || profileName.isEmpty())
          ? LanguageManager.message("ranked.noProfile")
          : LanguageManager.message("ranked.profileNotFound").replace("{profile}", profileName);
      SendMessage.sendToPlayer(sender, message);
      return;
    }

    sender.sendMessage(profile.getFormattedInfo());
    showPoolMaps(sender, profile.getMapPool());
  }

  private void showPoolMaps(CommandSender sender, String poolName) {
    List<String> maps = plugin.config().ranked().getMapsFromPool(poolName);
    if (!maps.isEmpty()) {
      SendMessage.sendToPlayer(sender, LanguageManager.message("ranked.config.pool.maps"));
      for (String map : maps) {
        sender.sendMessage("  §8- §f" + map);
      }
    }
  }

  public void setProfile(CommandSender sender, String profileName) {
    if (!sender.hasPermission("towers.admin")) {
      SendMessage.sendToPlayer(sender, LanguageManager.message("errors.noPermission"));
      return;
    }

    plugin.config().ranked().setActiveProfile(profileName);
    RankedProfile profile = plugin.config().ranked().getActiveProfile();

    SendMessage.sendToPlayer(
        sender, LanguageManager.message("ranked.profileSet").replace("{profile}", profileName));
    sender.sendMessage(profile.getFormattedInfo());
    showPoolMaps(sender, profile.getMapPool());
  }
}
