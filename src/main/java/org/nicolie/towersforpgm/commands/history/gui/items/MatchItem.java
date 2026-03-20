package org.nicolie.towersforpgm.commands.history.gui.items;

import com.google.common.collect.Lists;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.nicolie.towersforpgm.database.models.history.MatchHistory;
import org.nicolie.towersforpgm.database.models.history.PlayerHistory;
import org.nicolie.towersforpgm.rankeds.Rank;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.menu.MenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public class MatchItem implements MenuItem {

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm").withZone(ZoneId.systemDefault());

  private final MatchHistory matchHistory;
  private final String viewerUsername;

  public MatchItem(MatchHistory matchHistory, String viewerUsername) {
    this.matchHistory = matchHistory;
    this.viewerUsername = viewerUsername;
  }

  @Override
  public Component getDisplayName() {
    PlayerHistory ph = getViewerHistory();
    NamedTextColor color =
        (ph != null && ph.getWin() == 1) ? NamedTextColor.GREEN : NamedTextColor.RED;

    return Component.text(matchHistory.getMapName())
        .color(color)
        .decoration(TextDecoration.ITALIC, false);
  }

  @Override
  @SuppressWarnings("deprecation")
  public List<String> getLore(Player player) {
    List<Component> lore = buildLore(player);
    return Lists.transform(lore, c -> TextTranslations.translateLegacy(c, player));
  }

  private List<Component> buildLore(Player player) {
    List<Component> lore = new ArrayList<>();

    String dateStr = DATE_FMT.format(Instant.ofEpochMilli(matchHistory.getFinishedAt()));
    Component dateComponent = Component.translatable(
            "history.date", Component.text(dateStr).color(NamedTextColor.DARK_GRAY))
        .color(NamedTextColor.GRAY)
        .decoration(TextDecoration.ITALIC, false);
    lore.add(dateComponent);

    Component durationComponent = Component.translatable(
            "history.duration",
            Component.text(SendMessage.formatTime(matchHistory.getDurationSeconds()))
                .color(NamedTextColor.DARK_GRAY))
        .color(NamedTextColor.GRAY)
        .decoration(TextDecoration.ITALIC, false);
    lore.add(durationComponent);

    PlayerHistory ph = getViewerHistory();
    if (ph != null) {
      lore.add(Component.empty());
    }

    List<PlayerHistory> players = matchHistory.getPlayers();
    if (players != null && !players.isEmpty() && hasTeamInfo(players)) {
      lore.add(Component.empty());

      Map<String, List<PlayerHistory>> byTeam = new LinkedHashMap<>();
      Map<String, String> teamColorMap = new LinkedHashMap<>();

      for (PlayerHistory p : players) {
        String teamName = p.getTeamName();
        if (teamName == null || teamName.isBlank()) continue;
        byTeam.computeIfAbsent(teamName, k -> new ArrayList<>()).add(p);
        teamColorMap.putIfAbsent(teamName, p.getTeamColorHex());
      }

      for (Map.Entry<String, List<PlayerHistory>> entry : byTeam.entrySet()) {
        String teamName = entry.getKey();
        List<PlayerHistory> members = entry.getValue();
        TextColor teamColor = resolveColor(teamColorMap.get(teamName));

        lore.add(
            Component.text(teamName).color(teamColor).decoration(TextDecoration.ITALIC, false));

        for (PlayerHistory member : members) {
          boolean isViewer = member.getUsername().equalsIgnoreCase(viewerUsername);

          Component playerComponent;
          if (matchHistory.isRanked()) {
            playerComponent = formatRanked(
                Component.text("  " + member.getUsername())
                    .color(isViewer ? NamedTextColor.WHITE : NamedTextColor.GRAY),
                member.getEloAfter(),
                member.getEloDelta());
          } else {
            playerComponent = Component.text("  " + member.getUsername())
                .color(isViewer ? NamedTextColor.WHITE : NamedTextColor.GRAY);
          }

          playerComponent = playerComponent
              .decoration(TextDecoration.ITALIC, isViewer)
              .decoration(TextDecoration.BOLD, isViewer);

          lore.add(playerComponent);
        }
      }
    }

    // Se agregará esto después
    // lore.add(Component.empty());
    // lore.add(Component.text("Click para ver detalles")
    //     .color(NamedTextColor.DARK_GRAY)
    //     .decoration(TextDecoration.ITALIC, true));

    return lore;
  }

  @Override
  public Material getMaterial(Player player) {
    PlayerHistory ph = getViewerHistory();
    if (ph != null) {
      return ph.getWin() == 1 ? Material.EMERALD : Material.REDSTONE;
    }
    return Material.COAL;
  }

  @Override
  public void onClick(Player player, ClickType type) {
    // La lógica de navegación al detalle se maneja en el menú padre (HistoryMenu)
  }

  public PlayerHistory getViewerHistory() {
    if (matchHistory.getPlayers() == null) return null;
    return matchHistory.getPlayers().stream()
        .filter(p -> p.getUsername().equalsIgnoreCase(viewerUsername))
        .findFirst()
        .orElse(null);
  }

  public MatchHistory getMatchHistory() {
    return matchHistory;
  }

  private TextColor resolveColor(String hex) {
    if (hex != null && !hex.isBlank()) {
      TextColor parsed = TextColor.fromHexString(hex);
      if (parsed != null) return parsed;
    }
    return NamedTextColor.DARK_GRAY;
  }

  private boolean hasTeamInfo(List<PlayerHistory> players) {
    return players.stream()
        .anyMatch(p -> p.getTeamName() != null && !p.getTeamName().isBlank());
  }

  private Component formatRanked(Component name, Integer elo, Integer eloDelta) {
    Rank rank = Rank.getRankByElo(elo);
    boolean positive = eloDelta != null && eloDelta > 0;
    Component eloComponent = Component.text(rank.getPrefixedRank(true))
        .append(Component.space())
        .append(name)
        .append(Component.space())
        .append(Component.text(elo))
        .append(Component.space())
        .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
        .append(Component.text((positive ? "+" : "") + eloDelta)
            .color(positive ? NamedTextColor.GREEN : NamedTextColor.RED))
        .append(Component.text(")").color(NamedTextColor.DARK_GRAY));
    return eloComponent;
  }
}
