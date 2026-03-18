package org.nicolie.towersforpgm.utils;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.translatable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.rotation.MapPoolManager;

// Toma en cuenta que solo hay un mundo en el plugin como lo hace actualmente PGM (10/03/2025)

public class MatchManager {
  private static Match currentMatch; // Match actual

  // Getter y Setter para currentMatch
  public static Match getMatch() {
    return currentMatch;
  }

  public static void setCurrentMatch(Match match) {
    currentMatch = match;
  }

  public static String getMapPool() {
    if (PGM.get().getMapOrder() instanceof MapPoolManager) {
      MapPoolManager mapPoolManager = (MapPoolManager) PGM.get().getMapOrder();
      mapPoolManager.getActiveMapPool().getMaps();
      return mapPoolManager.getActiveMapPool().getName();
    }
    return null;
  }

  public static MapInfo getMap(String mapName) {
    return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                PGM.get().getMapLibrary().getMaps(), Spliterator.ORDERED),
            false)
        .filter(map -> map.getName().equalsIgnoreCase(mapName))
        .findFirst()
        .orElse(null);
  }

  public static Component getPrefixedName(String username) {
    if (username == null) return null;
    Component prefixed = Component.text("§3" + username);
    Player player = Bukkit.getPlayerExact(username);
    if (player != null && player.isOnline()) {
      MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
      prefixed = matchPlayer.getName();
    }
    return prefixed;
  }

  public static String getPrefixedNameAString(String username) {
    if (username == null) return null;
    Player player = Bukkit.getPlayerExact(username);
    if (player != null && player.isOnline()) {
      MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
      return matchPlayer.getPrefixedName();
    }
    return "§3" + username;
  }

  public static Component orList(Collection<? extends ComponentLike> texts, TextColor color) {
    var list =
        texts instanceof List ? (List<? extends ComponentLike>) texts : new ArrayList<>(texts);
    return switch (list.size()) {
      case 0 -> empty();
      case 1 -> empty().color(color).append(list.getFirst());
      case 2 -> translatable("misc.or", color, list.get(0), list.get(1));
      default -> {
        var it = list.iterator();
        var a = translatable("misc.list.start", color, it.next(), it.next());
        var b = it.next();
        while (it.hasNext()) {
          a = translatable("misc.list.middle", color, a, b);
          b = it.next();
        }
        yield translatable("misc.or", color, a, b);
      }
    };
  }

  public static Collection<Component> convert(List<String> texts, TextColor color) {
    return texts.stream()
        .map(text -> (Component) Component.text(text).color(color))
        .toList();
  }
}
