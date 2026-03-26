package org.nicolie.towersforpgm.commands.ranked;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.database.models.top.Top;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.rankeds.Rank;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.suggestion.Suggestions;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.bukkit.Sounds;
import tc.oc.pgm.util.text.TextFormatter;

public class EloCommand {
  private final TowersForPGM plugin = TowersForPGM.getInstance();
  private static final int PAGE_SIZE = 5;

  @Command("elo")
  @CommandDescription("Show your ELO")
  public void eloSelf(Audience audience, Player sender) {
    sendEloMessage(audience, sender, sender.getName());
  }

  @Command("elo <player>")
  @CommandDescription("Show a player's ELO")
  public void eloPlayer(
      Audience audience,
      CommandSender sender,
      @Argument(value = "player", suggestions = "onlinePlayers") String targetName) {
    if ("lb".equalsIgnoreCase(targetName)) {
      sendEloTop(audience, sender, 1);
      return;
    }

    sendEloMessage(audience, sender, targetName);
  }

  @Suggestions("onlinePlayers")
  public List<String> onlinePlayersSuggestions(CommandSender sender) {
    if (!(sender instanceof Player)) {
      return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    Player senderPlayer = (Player) sender;
    return Bukkit.getOnlinePlayers().stream()
        .filter(player -> player.getWorld().equals(senderPlayer.getWorld()))
        .map(Player::getName)
        .toList();
  }

  @Command("elo lb [page]")
  @CommandDescription("Show ELO leaderboard")
  public void eloLeaderboard(
      Audience audience, CommandSender sender, @Argument("page") Integer pageArg) {
    int page = pageArg == null ? 1 : Math.max(1, pageArg);
    sendEloTop(audience, sender, page);
  }

  private void sendEloMessage(Audience audience, CommandSender sender, String targetName) {
    String table = plugin.config().databaseTables().getRankedDefaultTable();
    audience.playSound(Sounds.INVENTORY_CLICK);

    StatsManager.getStats(table, targetName).thenAccept(stats -> {
      if (stats == null) {
        audience.sendWarning(Queue.RANKED_PREFIX
            .append(Component.space())
            .append(Component.translatable("command.playerNotFound")));
        return;
      }

      Rank rank = Rank.getRankByElo(stats.getElo());
      Rank maxRank = Rank.getRankByElo(stats.getMaxElo());
      org.bukkit.entity.Player bukkitPlayerHeading =
          org.bukkit.Bukkit.getPlayerExact(stats.getUsername());
      Component headingName = MatchManager.getPrefixedName(targetName);

      Component eloMessage = rank.getNameComponent(true)
          .append(Component.space())
          .append(headingName)
          .append(Component.text(":").color(NamedTextColor.DARK_GRAY))
          .append(Component.space())
          .append(Component.text(stats.getElo()).color(rank.getColor()))
          .append(Component.space())
          .append(Component.text("["))
          .color(NamedTextColor.DARK_GRAY)
          .append(Component.text(stats.getMaxElo()).color(maxRank.getColor()))
          .append(Component.text("]").color(NamedTextColor.DARK_GRAY));
      audience.sendMessage(eloMessage);
    });
  }

  private void sendEloTop(Audience audience, CommandSender sender, int page) {

    String table = plugin.config().databaseTables().getRankedDefaultTable();
    audience.playSound(Sounds.INVENTORY_CLICK);

    StatsManager.getTop(table, "elo", PAGE_SIZE, page).thenAccept(topResult -> {
      if (topResult == null
          || topResult.getData() == null
          || topResult.getData().isEmpty()) {
        audience.sendWarning(Queue.RANKED_PREFIX
            .append(Component.space())
            .append(Component.translatable("command.emptyResult")));
        return;
      }

      int totalPages = topResult.getTotalPages(PAGE_SIZE);
      audience.sendMessage(TextFormatter.horizontalLineHeading(
          sender,
          Component.text("Top Elo")
              .append(Component.space())
              .append(Component.text("(").color(NamedTextColor.DARK_AQUA))
              .append(Component.translatable(
                      "command.simplePageHeader",
                      Component.text(page).color(NamedTextColor.AQUA),
                      Component.text(totalPages).color(NamedTextColor.AQUA))
                  .color(NamedTextColor.DARK_AQUA))
              .append(Component.text(")").color(NamedTextColor.DARK_AQUA)),
          NamedTextColor.WHITE,
          TextFormatter.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH * 3));

      int startIndex = (page - 1) * PAGE_SIZE;
      int index = 0;
      for (Top entry : topResult.getData()) {
        Rank rank = Rank.getRankByElo(entry.getValueAsInt());
        Component nameDisplay = MatchManager.getPrefixedName(entry.getUsername());
        Component user = rank.getNameComponent(true).append(Component.space()).append(nameDisplay);
        int position = startIndex + (++index);
        Component line = Component.text(position + ". ")
            .append(user)
            .append(Component.text(":").color(NamedTextColor.DARK_GRAY))
            .append(Component.space())
            .append(Component.text(entry.getValueAsInt()).color(rank.getColor()));
        audience.sendMessage(line);
      }

      audience.sendMessage(TextFormatter.horizontalLine(
          NamedTextColor.WHITE, TextFormatter.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH * 3));
    });
  }
}
