package org.nicolie.towersforpgm.commandUtils;

import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.database.TableManager;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;

public class RankedConfig {

  public void size(CommandSender sender, String size) {
    if (size == null || size.isEmpty()) {
      String message = LanguageManager.langMessage("ranked.config.size.current")
          .replace("{size}", String.valueOf(ConfigManager.getRankedSize()));
      SendMessage.sendToPlayer(sender, message);
      return;
    }

    try {
      int newSize = Integer.parseInt(size);
      if (newSize < 2 || newSize > 12) {
        SendMessage.sendToPlayer(sender, LanguageManager.langMessage("ranked.config.size.invalid"));
        return;
      }
      ConfigManager.setRankedSize(newSize);
      String message = LanguageManager.langMessage("ranked.config.size.set")
          .replace("{size}", String.valueOf(newSize));
      SendMessage.sendToPlayer(sender, message);
    } catch (NumberFormatException e) {
      SendMessage.sendToPlayer(
          sender, LanguageManager.langMessage("ranked.config.size.invalidFormat"));
    }
  }

  public void draftOrder(CommandSender sender, String order) {
    if (order == null || order.isEmpty()) {
      String message = LanguageManager.langMessage("ranked.config.order.current")
          .replace("{order}", ConfigManager.getRankedOrder());
      SendMessage.sendToPlayer(sender, message);
      return;
    }

    if (!order.matches("A[AB]+")) {
      String errorMessage = LanguageManager.langMessage("ranked.config.order.invalid");
      SendMessage.sendToPlayer(sender, errorMessage);
      return;
    }

    ConfigManager.setRankedOrder(order);
    String message =
        LanguageManager.langMessage("ranked.config.order.set").replace("{order}", order);
    SendMessage.sendToPlayer(sender, message);
  }

  public void addTable(CommandSender sender, String table) {
    if (ConfigManager.getTables().contains(table)) {
      SendMessage.sendToPlayer(sender, LanguageManager.langMessage("ranked.config.table.exists"));
      return;
    }

    ConfigManager.addRankedTable(table);
    TableManager.createTable(table);
    String message =
        LanguageManager.langMessage("ranked.config.table.added").replace("{table}", table);
    SendMessage.sendToPlayer(sender, message);
  }

  public void deleteTable(CommandSender sender, String table) {
    if (!ConfigManager.getRankedTables().contains(table)) {
      SendMessage.sendToPlayer(
          sender,
          LanguageManager.langMessage("ranked.config.table.notFound").replace("{table}", table));
      return;
    }

    ConfigManager.removeRankedTable(table);
    String message =
        LanguageManager.langMessage("ranked.config.table.deleted").replace("{table}", table);
    SendMessage.sendToPlayer(sender, message);
  }

  public void addMap(CommandSender sender) {
    Match match = PGM.get().getMatchManager().getMatch(sender);
    String map = match.getMap().getName();
    if (ConfigManager.getRankedMaps().contains(map)) {
      SendMessage.sendToPlayer(
          sender, LanguageManager.langMessage("ranked.config.map.exists").replace("{map}", map));
      return;
    }
    ConfigManager.addRankedMap(map);
    String message = LanguageManager.langMessage("ranked.config.map.added").replace("{map}", map);
    SendMessage.sendToPlayer(sender, message);
  }

  public void removeMap(CommandSender sender) {
    Match match = PGM.get().getMatchManager().getMatch(sender);
    String map = match.getMap().getName();
    if (!ConfigManager.getRankedMaps().contains(map)) {
      SendMessage.sendToPlayer(
          sender, LanguageManager.langMessage("ranked.config.map.notFound").replace("{map}", map));
      return;
    }
    ConfigManager.removeRankedMap(map);
    String message = LanguageManager.langMessage("ranked.config.map.deleted").replace("{map}", map);
    SendMessage.sendToPlayer(sender, message);
  }

  public void matchmaking(CommandSender sender, Boolean isEnabled) {
    if (isEnabled == null) {
      boolean isRankedMatchmaking = ConfigManager.isRankedMatchmaking();
      String status = isRankedMatchmaking
          ? LanguageManager.langMessage("ranked.config.matchmaking.statusEnabled")
          : LanguageManager.langMessage("ranked.config.matchmaking.statusDisabled");
      String message = LanguageManager.langMessage("ranked.config.matchmaking.status")
          .replace("{status}", status);
      SendMessage.sendToPlayer(sender, message);
      return;
    }
    ConfigManager.setRankedMatchmaking(isEnabled);
    String message = isEnabled
        ? LanguageManager.langMessage("ranked.config.matchmaking.enabled")
        : LanguageManager.langMessage("ranked.config.matchmaking.disabled");
    SendMessage.sendToPlayer(sender, message);
  }

  public void setDefaultTable(CommandSender sender, String table) {
    if (!ConfigManager.getRankedTables().contains(table)) {
      SendMessage.sendToPlayer(
          sender,
          LanguageManager.langMessage("ranked.config.table.notFound").replace("{table}", table));
      return;
    }
    ConfigManager.setRankedDefaultTable(table);
    String message =
        LanguageManager.langMessage("ranked.config.table.success").replace("{table}", table);
    SendMessage.sendToPlayer(sender, message);
  }
}
