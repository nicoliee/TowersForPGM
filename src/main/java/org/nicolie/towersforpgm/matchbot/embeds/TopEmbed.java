package org.nicolie.towersforpgm.matchbot.embeds;

import java.text.DecimalFormat;
import java.time.Instant;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.tables.TableInfo;
import org.nicolie.towersforpgm.database.models.top.Top;
import org.nicolie.towersforpgm.database.models.top.TopResult;
import org.nicolie.towersforpgm.matchbot.enums.Stat;
import org.nicolie.towersforpgm.rankeds.Rank;

public class TopEmbed {
  private static final TowersForPGM plugin = TowersForPGM.getInstance();

  private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

  public static EmbedBuilder createTopEmbed(
      Stat stat, String table, int page, TopResult topResult, boolean perGame) {

    TableInfo tableInfo = plugin.config().databaseTables().getTableInfo(table);
    boolean isRankedTable = tableInfo != null && tableInfo.isRanked();
    EmbedBuilder embed = new EmbedBuilder().setTimestamp(Instant.now());

    List<Top> topEntries = topResult.getData();

    if (topEntries.isEmpty()) {
      embed.addField(
          "Sin Datos",
          "Estadística " + stat.getDisplayName() + " no disponible para " + table + ".",
          false);
      return embed;
    } else {
      StringBuilder rankingText = new StringBuilder();
      int startRank = (page - 1) * 10 + 1;
      int endRank = Math.min(startRank + topEntries.size() - 1, startRank + 9);

      for (int i = 0; i < topEntries.size(); i++) {
        Top entry = topEntries.get(i);
        int rank = startRank + i;

        // Format value based on stat type
        String formattedValue = formatStatValue(stat, entry.getValue(), perGame);

        // Special formatting for ELO stats
        if ((stat == Stat.ELO || stat == Stat.MAX_ELO) && isRankedTable) {
          Rank playerRank = Rank.getRankByElo((int) entry.getValue());
          rankingText.append(String.format(
              "%d. %s %s: %s\n",
              rank, playerRank.getPrefixedRank(false), entry.getUsername(), formattedValue));
        } else {
          rankingText.append(
              String.format("%d. %s: %s\n", rank, entry.getUsername(), formattedValue));
        }
      }

      String statDisplayName;
      if (perGame) {
        statDisplayName = stat.getDisplayName() + " (Por Partida)";
      } else {
        statDisplayName = stat.getDisplayName();
      }
      String fieldName =
          String.format("Top %d-%d %s %s", startRank, endRank, statDisplayName, table);

      embed.addField(fieldName, rankingText.toString().trim(), true);
    }

    int totalPages = topResult.getTotalPages(10);

    embed.setFooter("Página " + page + "/" + totalPages);

    return embed;
  }

  private static String formatStatValue(Stat stat, double value, boolean perGame) {
    if (stat.isPercentage()) {
      return DECIMAL_FORMAT.format(value) + "%";
    } else if (stat.isInteger() && !perGame) {
      return String.valueOf((int) value);
    } else {
      return DECIMAL_FORMAT.format(value);
    }
  }
}
