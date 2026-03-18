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
import tc.oc.pgm.api.PGM;
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
      String headingName = bukkitPlayerHeading != null
          ? PGM.get().getMatchManager().getPlayer(bukkitPlayerHeading).getPrefixedName()
          : "§3" + stats.getUsername();

      String eloMessage = rank.getPrefixedRank(true) + " " + headingName + "§8: "
          + rank.getColor() + stats.getElo() + " §8[" + maxRank.getColor() + stats.getMaxElo()
          + "§8]";
      audience.sendMessage(Component.text(eloMessage));
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
        org.bukkit.entity.Player bukkitPlayer =
            org.bukkit.Bukkit.getPlayerExact(entry.getUsername());
        String nameDisplay = bukkitPlayer != null
            ? PGM.get().getMatchManager().getPlayer(bukkitPlayer).getPrefixedName()
            : "§3" + entry.getUsername();
        String user = rank.getPrefixedRank(true) + " " + nameDisplay;
        int position = startIndex + (++index);
        String line = String.format(
            "%d. %s§8: " + rank.getColor() + "%d", position, user, entry.getValueAsInt());
        audience.sendMessage(Component.text(line));
      }

      audience.sendMessage(TextFormatter.horizontalLine(
          NamedTextColor.WHITE, TextFormatter.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH * 3));
    });
  }
}
