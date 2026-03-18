package org.nicolie.towersforpgm.commands.towers.commandUtils;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.MatchManager;

public class DraftConfig {

  private final TowersForPGM plugin = TowersForPGM.getInstance();

  public void suggestions(Audience audience, Boolean enabled) {

    boolean state = enabled != null ? enabled : plugin.config().draft().isDraftSuggestions();

    if (enabled != null) {
      plugin.config().draft().setDraftSuggestions(enabled);
    }

    audience.sendMessage(Component.translatable(
        state ? "draft.config.suggestion.enabled" : "draft.config.suggestion.disabled"));
  }

  public void reroll(Audience audience, Boolean enabled) {

    boolean state = enabled != null ? enabled : plugin.config().draft().isReroll();

    if (enabled != null) {
      plugin.config().draft().setReroll(enabled);
    }

    audience.sendMessage(Component.translatable(
        state ? "draft.config.reroll.enabled" : "draft.config.reroll.disabled"));
  }

  public void timer(Audience audience, Boolean enabled) {

    boolean state = enabled != null ? enabled : plugin.config().draft().isDraftTimer();

    if (enabled != null) {
      plugin.config().draft().setDraftTimer(enabled);
    }

    audience.sendMessage(Component.translatable(
        state ? "draft.config.timer.enabled" : "draft.config.timer.disabled"));
  }

  public void secondPick(Audience audience, Boolean enabled) {

    boolean state = enabled != null ? enabled : plugin.config().draft().isSecondPickBalance();

    if (enabled != null) {
      plugin.config().draft().setSecondPickBalance(enabled);
    }

    audience.sendMessage(Component.translatable(
        state
            ? "draft.config.secondGetsExtraPlayer.enabled"
            : "draft.config.secondGetsExtraPlayer.disabled"));
  }

  public void privateMatch(Audience audience, Boolean enabled) {

    String map = MatchManager.getMatch().getMap().getName();

    boolean state = enabled != null ? enabled : plugin.config().privateMatch().isPrivateMatch(map);

    if (enabled != null) {
      plugin.config().privateMatch().setPrivateMatch(map, enabled);
    }

    audience.sendMessage(Component.translatable(
        state ? "privateMatch.true" : "privateMatch.false", Component.text(map)));
  }

  public void order(Audience audience, String order) {

    String state = plugin.config().draft().getOrder();

    if (order != null) {

      if (!order.matches("A[AB]+")) {
        audience.sendMessage(Component.translatable("draft.config.invalidOrder"));
        return;
      }

      plugin.config().draft().setOrder(order);
      state = order;
    }

    audience.sendMessage(Component.translatable(
        order != null ? "draft.config.orderSet" : "draft.config.currentOrder",
        Component.text(state)));
  }

  public void minOrder(Audience audience, int size) {

    int state = plugin.config().draft().getMinOrder();

    if (size != -1) {

      plugin.config().draft().setMinOrder(size);
      state = size;
    }

    audience.sendMessage(Component.translatable(
        size != -1 ? "draft.config.sizeSet" : "draft.config.currentMinOrder",
        Component.text(state)));
  }
}
