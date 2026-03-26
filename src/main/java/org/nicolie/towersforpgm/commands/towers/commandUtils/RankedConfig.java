package org.nicolie.towersforpgm.commands.towers.commandUtils;

import java.util.List;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.tables.TableInfo;
import org.nicolie.towersforpgm.database.TableManager;
import org.nicolie.towersforpgm.draft.map.MapVoteConfig.VoteMode;
import org.nicolie.towersforpgm.rankeds.RankedProfile;

public class RankedConfig {

  private final TowersForPGM plugin = TowersForPGM.getInstance();

  public void minSize(Audience audience, String size) {

    int state = plugin.config().ranked().getRankedMinSize();

    if (size != null) {

      int newSize = Integer.parseInt(size);

      if (newSize < 2 || newSize % 2 != 0) {
        audience.sendMessage(Component.translatable("ranked.config.minSize.invalid"));
        return;
      }

      int max = plugin.config().ranked().getRankedMaxSize();

      if (max > 0 && newSize > max) {
        audience.sendMessage(Component.translatable("ranked.config.minSize.greaterThanMax"));
        return;
      }

      plugin.config().ranked().setRankedMinSize(newSize);
      state = newSize;
    }

    audience.sendMessage(Component.translatable(
        size != null ? "ranked.config.minSize.set" : "ranked.config.minSize.current",
        Component.text(state)));
  }

  public void maxSize(Audience audience, String size) {

    int state = plugin.config().ranked().getRankedMaxSize();

    if (size != null) {

      int newSize = Integer.parseInt(size);

      if (newSize < 2 || newSize % 2 != 0) {
        audience.sendMessage(Component.translatable("ranked.config.maxSize.invalid"));
        return;
      }

      int min = plugin.config().ranked().getRankedMinSize();

      if (min > 0 && newSize < min) {
        audience.sendMessage(Component.translatable("ranked.config.maxSize.lessThanMin"));
        return;
      }

      plugin.config().ranked().setRankedMaxSize(newSize);
      state = newSize;
    }

    audience.sendMessage(Component.translatable(
        size != null ? "ranked.config.maxSize.set" : "ranked.config.maxSize.current",
        Component.text(state)));
  }

  public void reroll(Audience audience, Boolean enabled) {

    boolean state = enabled != null ? enabled : plugin.config().ranked().isReroll();

    if (enabled != null) {
      plugin.config().ranked().setReroll(enabled);
    }

    audience.sendMessage(Component.translatable(
        state ? "ranked.config.rerollEnabled" : "ranked.config.rerollDisabled"));
  }

  public void mapVote(Audience audience, String mode) {
    String state = plugin.config().ranked().getMapVote();

    if (mode != null) {
      VoteMode voteMode;
      try {
        voteMode = VoteMode.valueOf(mode.trim().toUpperCase());
      } catch (IllegalArgumentException | NullPointerException e) {
        audience.sendMessage(Component.translatable("ranked.config.mapVote.invalid"));
        return;
      }
      plugin.config().ranked().setMapVote(voteMode.name());
      state = voteMode.name();
    }

    audience.sendMessage(Component.translatable(
        mode != null ? "ranked.config.mapVote.set" : "ranked.config.mapVote.current",
        Component.text(state)));
  }

  public void order(Audience audience, String order) {

    String state = plugin.config().ranked().getRankedOrder();

    if (order != null) {

      if (!order.matches("A[AB]+")) {
        audience.sendMessage(Component.translatable("ranked.config.order.invalid"));
        return;
      }

      plugin.config().ranked().setRankedOrder(order);
      state = order;
    }

    audience.sendMessage(Component.translatable(
        order != null ? "ranked.config.order.set" : "ranked.config.order.current",
        Component.text(state)));
  }

  public void pool(Audience audience, String poolName) {

    String state = plugin.config().ranked().getMapPool();

    if (poolName != null) {

      boolean success = plugin.config().ranked().setPool(poolName);

      if (!success) {
        audience.sendMessage(
            Component.translatable("ranked.config.pool.notFound", Component.text(poolName)));
        return;
      }

      state = poolName;
    }

    List<String> maps = plugin.config().ranked().getMapsFromPool(state);

    audience.sendMessage(Component.translatable(
        poolName != null ? "ranked.config.pool.set" : "ranked.config.pool.current",
        Component.text(state),
        Component.text(maps.size())));

    for (String map : maps) {
      audience.sendMessage(Component.text("§8- §f" + map));
    }
  }

  public void matchmaking(Audience audience, Boolean enabled) {

    boolean state = enabled != null ? enabled : plugin.config().ranked().isRankedMatchmaking();

    if (enabled != null) {
      plugin.config().ranked().setRankedMatchmaking(enabled);
    }

    audience.sendMessage(Component.translatable(
        state ? "ranked.config.matchmaking.enabled" : "ranked.config.matchmaking.disabled"));
  }

  public void tableAdd(Audience audience, String table) {

    TableInfo info = plugin.config().databaseTables().getTableInfo(table);

    if (info != null && info.isRanked()) {
      audience.sendMessage(Component.translatable("ranked.config.table.exists"));
      return;
    }

    plugin.config().databaseTables().addTable(table, true);
    TableManager.createTable(table);

    audience.sendMessage(
        Component.translatable("ranked.config.table.added", Component.text(table)));
  }

  public void tableRemove(Audience audience, String table) {

    if (plugin.config().databaseTables().getTableInfo(table) == null) {
      audience.sendMessage(
          Component.translatable("ranked.config.table.notFound", Component.text(table)));
      return;
    }

    plugin.config().databaseTables().removeTable(table);

    audience.sendMessage(
        Component.translatable("ranked.config.table.deleted", Component.text(table)));
  }

  public void profile(Audience audience, String profileName) {

    RankedProfile profile = profileName == null
        ? plugin.config().ranked().getActiveProfile()
        : plugin.config().ranked().getProfile(profileName);

    if (profile == null) {
      audience.sendMessage(Component.translatable(
          profileName == null ? "ranked.noProfile" : "ranked.profileNotFound",
          Component.text(profileName)));
      return;
    }

    audience.sendMessage(profile.getFormattedInfo());

    List<String> maps = plugin.config().ranked().getMapsFromPool(profile.getMapPool());

    for (String map : maps) {
      audience.sendMessage(Component.text("§8- §f" + map));
    }
  }

  public void setProfile(Audience audience, String profileName) {

    plugin.config().ranked().setActiveProfile(profileName);

    RankedProfile profile = plugin.config().ranked().getActiveProfile();

    audience.sendMessage(Component.translatable("ranked.profileSet", Component.text(profileName)));

    audience.sendMessage(profile.getFormattedInfo());
  }
}
