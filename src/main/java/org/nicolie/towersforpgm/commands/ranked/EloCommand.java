package org.nicolie.towersforpgm.commands.ranked;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.database.models.top.Top;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.rankeds.Rank;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.bukkit.Sounds;
import tc.oc.pgm.util.text.TextFormatter;

public class EloCommand implements CommandExecutor {
  private final TowersForPGM plugin = TowersForPGM.getInstance();

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    Audience audience = Audience.get(sender);

    if (!(sender instanceof Player) && args.length == 0) {
      audience.sendWarning(Queue.RANKED_PREFIX
          .append(Component.space())
          .append(Component.translatable("command.specifyPlayer")));
      return true;
    }

    Match match = MatchManager.getMatch();
    boolean isRankedMap =
        match != null && plugin.config().ranked().isMapRanked(match.getMap().getName());

    if (!isRankedMap) {
      audience.sendWarning(Queue.RANKED_PREFIX.append(
          Component.space().append(Component.translatable("command.emptyResult"))));
      return true;
    }
    String table = plugin.config().databaseTables().getRankedDefaultTable();
    if (args.length > 0 && "lb".equalsIgnoreCase(args[0])) {
      int page = 1;
      if (args.length > 1) {
        try {
          page = Math.max(1, Integer.parseInt(args[1]));
        } catch (NumberFormatException ignored) {
          page = 1;
        }
      }
      sendEloTop(sender, table, page);
    } else {
      sendEloMessage(audience, sender, table, args);
    }
    return true;
  }

  private void sendEloMessage(
      Audience audience, CommandSender sender, String table, String[] args) {
    MatchPlayer matchPlayer =
        sender instanceof Player ? PGM.get().getMatchManager().getPlayer((Player) sender) : null;

    audience.playSound(Sounds.INVENTORY_CLICK);
    String targetName = args.length > 0 ? args[0] : matchPlayer.getNameLegacy();
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
      String eloMessage = rank.getPrefixedRank(true) + " " + headingName + "§8: " + rank.getColor()
          + stats.getElo() + " §8[" + maxRank.getColor() + stats.getMaxElo() + "§8]";

      audience.sendMessage(Component.text(eloMessage));
    });
  }

  private void sendEloTop(CommandSender sender, String table, int page) {
    Audience audience = Audience.get(sender);
    audience.playSound(Sounds.INVENTORY_CLICK);
    final int PAGE_SIZE = 5;
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
