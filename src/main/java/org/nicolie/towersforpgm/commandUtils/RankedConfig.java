package org.nicolie.towersforpgm.commandUtils;

import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.database.TableManager;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;

public class RankedConfig {
  private final LanguageManager languageManager;

  public RankedConfig(LanguageManager languageManager) {
    this.languageManager = languageManager;
  }

  public void size(CommandSender sender, String size) {
    if (size == null || size.isEmpty()) {
      String message = "Current ranked match size: " + ConfigManager.getRankedSize();
      SendMessage.sendToPlayer(sender, message);
      return;
    }

    try {
      int newSize = Integer.parseInt(size);
      if (newSize < 2 || newSize > 12) {
        SendMessage.sendToPlayer(sender, "Ranked match size must be between 2 and 12.");
        return;
      }
      ConfigManager.setRankedSize(newSize);
      String message = "Ranked match size set to: " + newSize;
      SendMessage.sendToPlayer(sender, message);
    } catch (NumberFormatException e) {
      SendMessage.sendToPlayer(
          sender, "Invalid size format. Please enter a number between 2 and 12.");
    }
  }

  public void draftOrder(CommandSender sender, String order) {
    if (order == null || order.isEmpty()) {
      String message = "Current ranked match order: " + ConfigManager.getRankedOrder();
      SendMessage.sendToPlayer(sender, message);
      return;
    }

    if (!order.matches("A[AB]+")) {
      String errorMessage = languageManager.getPluginMessage("draft.invalidOrder");
      SendMessage.sendToPlayer(sender, errorMessage);
      return;
    }

    ConfigManager.setRankedOrder(order);
    String message = "Ranked match order set to: " + order;
    SendMessage.sendToPlayer(sender, message);
  }

  public void addTable(CommandSender sender, String table) {
    if (ConfigManager.getTables().contains(table)) {
      SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("table.exists"));
      return;
    }

    ConfigManager.addRankedTable(table);
    TableManager.createTable(table);
    String message = languageManager.getPluginMessage("table.created").replace("{table}", table);
    SendMessage.sendToPlayer(sender, message);
  }

  public void deleteTable(CommandSender sender, String table) {
    if (!ConfigManager.getRankedTables().contains(table)) {
      SendMessage.sendToPlayer(
          sender, languageManager.getPluginMessage("table.notFound").replace("{table}", table));
      return;
    }

    ConfigManager.removeRankedTable(table);
    String message = languageManager.getPluginMessage("table.deleted").replace("{table}", table);
    SendMessage.sendToPlayer(sender, message);
  }

  public void addMap(CommandSender sender) {
    Match match = PGM.get().getMatchManager().getMatch(sender);
    String map = match.getMap().getName();
    if (ConfigManager.getRankedMaps().contains(map)) {
      SendMessage.sendToPlayer(
          sender, languageManager.getPluginMessage("map.exists").replace("{map}", map));
      return;
    }
    ConfigManager.addRankedMap(map);
    String message = languageManager.getPluginMessage("map.added").replace("{map}", map);
    SendMessage.sendToPlayer(sender, message);
  }

  public void removeMap(CommandSender sender) {
    Match match = PGM.get().getMatchManager().getMatch(sender);
    String map = match.getMap().getName();
    if (!ConfigManager.getRankedMaps().contains(map)) {
      SendMessage.sendToPlayer(
          sender, languageManager.getPluginMessage("map.notFound").replace("{map}", map));
      return;
    }
    ConfigManager.removeRankedMap(map);
    String message = languageManager.getPluginMessage("map.removed").replace("{map}", map);
    SendMessage.sendToPlayer(sender, message);
  }

  public void matchmaking(CommandSender sender, Boolean isEnabled) {
    if (isEnabled == null) {
      boolean isRankedMatchmaking = ConfigManager.isRankedMatchmaking();
      String message = isRankedMatchmaking
          ? "Ranked matchmaking is enabled."
          : "Ranked matchmaking is disabled.";
      SendMessage.sendToPlayer(sender, message);
      return;
    }
    ConfigManager.setRankedMatchmaking(isEnabled);
    String message = isEnabled ? "Ranked matchmaking enabled." : "Ranked matchmaking disabled.";
    SendMessage.sendToPlayer(sender, message);
  }

  public void setDefaultTable(CommandSender sender, String table) {
    if (!ConfigManager.getRankedTables().contains(table)) {
      SendMessage.sendToPlayer(
          sender, languageManager.getPluginMessage("table.notFound").replace("{table}", table));
      return;
    }
    ConfigManager.setRankedDefaultTable(table);
    String message = languageManager.getPluginMessage("table.success").replace("{table}", table);
    SendMessage.sendToPlayer(sender, message);
  }
}
