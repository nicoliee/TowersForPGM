package org.nicolie.towersforpgm.draft.pick.gui.items;

import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.draft.state.DraftPhase;
import org.nicolie.towersforpgm.draft.state.PickResult;
import org.nicolie.towersforpgm.session.draft.DraftContext;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.MenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public class PlayerSkullItem implements MenuItem {

  public enum ClickResult {
    TOGGLE_EXPAND,
    ACTION_TAKEN,
    NONE
  }

  private final String targetName;
  private final Stats stats;
  private final DraftContext ctx;
  private final boolean expanded;

  public PlayerSkullItem(String targetName, Stats stats, DraftContext ctx, boolean expanded) {
    this.targetName = targetName;
    this.stats = stats;
    this.ctx = ctx;
    this.expanded = expanded;
  }

  @Override
  public Component getDisplayName() {
    return MatchManager.getPrefixedName(targetName);
  }

  @Override
  public List<String> getLore(Player player) {
    List<Component> lore = (stats != null && expanded)
        ? stats.getDetailedLore()
        : stats != null ? stats.getLore() : new java.util.ArrayList<>();

    if (stats != null) {
      lore.add(Component.space());
      lore.add(Component.translatable(
              expanded ? "draft.gui.rightClick.back" : "draft.gui.rightClick.details")
          .color(GRAY));
    }

    int captainNumber = ctx.getCaptainNumber(player.getUniqueId());
    boolean isCaptain = captainNumber != -1;
    boolean isCaptainTurn = (captainNumber == 1 && ctx.captains().isCaptain1Turn())
        || (captainNumber == 2 && !ctx.captains().isCaptain1Turn());

    int teamNumber = ctx.teams().getTeamNumber(player.getName());
    boolean isSuggestionTeamTurn = (teamNumber == 1 && ctx.captains().isCaptain1Turn())
        || (teamNumber == 2 && !ctx.captains().isCaptain1Turn());

    if (isCaptain && isCaptainTurn) {
      lore.add(Component.translatable("draft.gui.clickToPick").color(GOLD));
    } else if (ctx.captains().isPlayerSuggestions()
        && !ctx.availablePlayers().hasAlreadySuggested(player.getName())
        && isSuggestionTeamTurn) {
      lore.add(Component.translatable("draft.gui.clickToSuggest").color(GOLD));
    }

    return Lists.transform(lore, c -> TextTranslations.translateLegacy(c, player));
  }

  @Override
  public Material getMaterial(Player player) {
    return Material.SKULL_ITEM;
  }

  @Override
  public short getData() {
    return 3;
  }

  @Override
  public ItemMeta modifyMeta(ItemMeta meta) {
    ((SkullMeta) meta).setOwner(targetName);
    return meta;
  }

  public ClickResult handleClick(Player player, ClickType type) {
    if (type.isRightClick() && stats != null) return ClickResult.TOGGLE_EXPAND;
    return handleActionClick(player);
  }

  @Override
  public void onClick(Player player, ClickType type) {
    handleClick(player, type);
  }

  private ClickResult handleActionClick(Player player) {
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);

    if (ctx.getCaptainNumber(player.getUniqueId()) == -1) {
      boolean success = ctx.suggestPlayer(matchPlayer, targetName);
      if (success) player.closeInventory();
      return success ? ClickResult.ACTION_TAKEN : ClickResult.NONE;
    }

    return handleCaptainPick(player, matchPlayer);
  }

  private ClickResult handleCaptainPick(Player player, MatchPlayer matchPlayer) {
    DraftPhase phase = ctx.phase();
    if (phase == DraftPhase.CAPTAINS || phase == DraftPhase.REROLL) return ClickResult.NONE;

    PickResult result = ctx.validatePick(player.getUniqueId(), targetName);

    switch (result) {
      case NOT_A_CAPTAIN:
      case DRAFT_NOT_ACTIVE:
        return ClickResult.NONE;

      case NOT_YOUR_TURN:
        return ClickResult.NONE;

      case NOT_IN_DRAFT:
        matchPlayer.sendWarning(Component.translatable(
            "draft.picks.notInList", MatchManager.getPrefixedName(targetName)));
        player.closeInventory();
        return ClickResult.NONE;

      case ALREADY_PICKED:
        matchPlayer.sendWarning(Component.translatable(
            "draft.picks.alreadyPicked", MatchManager.getPrefixedName(targetName)));
        player.closeInventory();
        return ClickResult.NONE;

      case OK:
        ctx.pickPlayer(targetName);
        player.closeInventory();
        return ClickResult.ACTION_TAKEN;

      default:
        return ClickResult.NONE;
    }
  }
}
