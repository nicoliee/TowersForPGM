package org.nicolie.towersforpgm.draft.map.gui.items;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.nicolie.towersforpgm.draft.map.MapVoteConfig;
import org.nicolie.towersforpgm.draft.map.MapVoteManager;
import tc.oc.pgm.menu.MenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public final class MapItem implements MenuItem {

  private static final boolean SHOW_VOTES = true;

  private static final Material[] SLOT_MATERIALS = {
    Material.EMERALD, Material.DIAMOND, Material.GOLD_INGOT, Material.IRON_INGOT, Material.PAPER
  };

  private final String displayedName;
  private final int slotIndex;
  private final MapVoteManager voteManager;
  private final boolean isSecret;
  private final boolean isVetoed;
  private final boolean isSelected;

  public MapItem(
      String displayedName, int slotIndex, MapVoteManager voteManager, boolean isSelected) {
    this.displayedName = displayedName;
    this.slotIndex = slotIndex;
    this.voteManager = voteManager;
    this.isSelected = isSelected;
    this.isSecret = MapVoteManager.SECRET_KEY.equals(displayedName);
    this.isVetoed = !isSecret
        && voteManager.getConfig().getVoteMode() == MapVoteConfig.VoteMode.VETO
        && !voteManager.getRemainingMaps().contains(displayedName);
  }

  @Override
  public Component getDisplayName() {
    if (isSecret) {
      return Component.translatable("draft.map.secret")
          .color(NamedTextColor.DARK_PURPLE)
          .decorate(TextDecoration.BOLD);
    }
    if (isVetoed) {
      return Component.text(displayedName)
          .color(NamedTextColor.RED)
          .decorate(TextDecoration.STRIKETHROUGH);
    }

    switch (voteManager.getConfig().getVoteMode()) {
      case VETO:
        return buildVetoAliveDisplayName();
      case PLURALITY:
      default:
        return buildPluralityDisplayName();
    }
  }

  @Override
  public List<String> getLore(Player player) {
    List<Component> lore = new ArrayList<>();

    switch (voteManager.getConfig().getVoteMode()) {
      case VETO:
        buildVetoAliveLore(player, lore);
        break;
      case PLURALITY:
      default:
        buildPluralityLore(player, lore);
        break;
    }

    return translateLore(player, lore);
  }

  private Component buildPluralityDisplayName() {
    return Component.text(displayedName).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD);
  }

  private Component buildVetoAliveDisplayName() {
    return Component.text(displayedName).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD);
  }

  @SuppressWarnings("deprecation")
  private List<String> translateLore(Player player, List<Component> lore) {
    return lore.stream()
        .map(c -> TextTranslations.translateLegacy(c, player))
        .collect(Collectors.toList());
  }

  private void buildPluralityLore(Player player, List<Component> lore) {
    int votes = voteManager.getVoteCounts().getOrDefault(displayedName, 0);
    lore.add(
        Component.translatable("draft.map.votes", Component.text(votes).color(NamedTextColor.WHITE))
            .color(NamedTextColor.GRAY));

    if (SHOW_VOTES) appendVoterList(lore, "draft.map.voters", NamedTextColor.WHITE);

    if (voteManager.isEligible(player.getUniqueId())) {
      lore.add(Component.empty());
      lore.add(Component.translatable("draft.map.clickToVote").color(NamedTextColor.YELLOW));
    }
  }

  private void buildVetoAliveLore(Player player, List<Component> lore) {
    lore.add(Component.translatable("draft.map.veto.alive").color(NamedTextColor.GREEN));
    if (SHOW_VOTES) appendVoterList(lore, "draft.map.vetoedBy", NamedTextColor.RED);
    if (voteManager.isEligible(player.getUniqueId())) {
      lore.add(Component.empty());
      lore.add(Component.translatable("draft.map.veto.clickToVeto").color(NamedTextColor.YELLOW));
    }
  }

  private void appendVoterList(List<Component> lore, String headerKey, NamedTextColor color) {
    List<String> voters = voteManager.getVotersForMap(displayedName);
    if (!voters.isEmpty()) {
      lore.add(Component.empty());
      lore.add(Component.translatable(headerKey).color(NamedTextColor.GRAY));
      voters.forEach(name -> lore.add(Component.text("  » " + name).color(color)));
    }
  }

  @Override
  public Material getMaterial(Player player) {
    if (isSelected) return Material.NETHER_STAR;
    if (isSecret) return Material.PAPER;
    if (isVetoed) return Material.STAINED_GLASS_PANE;
    return SLOT_MATERIALS[slotIndex % SLOT_MATERIALS.length];
  }

  @Override
  public short getData() {
    if (isSelected) return 0;
    if (isVetoed) return 14;
    return 0;
  }

  @Override
  public ItemMeta modifyMeta(ItemMeta meta) {
    if (isSelected) {
      meta.addEnchant(Enchantment.DURABILITY, 1, true);
      meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }
    return meta;
  }

  @Override
  public void onClick(Player player, ClickType type) {
    if (isVetoed) return;
    if (!voteManager.isEligible(player.getUniqueId())) return;

    switch (voteManager.getConfig().getVoteMode()) {
      case VETO:
        handleVetoClick(player);
        return;
      case PLURALITY:
      default:
        handlePluralityClick(player);
        return;
    }
  }

  private void handlePluralityClick(Player player) {
    if (isSecret) {
      voteManager.castVote(player.getUniqueId(), MapVoteManager.SECRET_KEY);
      return;
    }
    voteManager.castVote(player.getUniqueId(), displayedName);
  }

  private void handleVetoClick(Player player) {
    if (isSecret) return;
    voteManager.castVote(player.getUniqueId(), displayedName);
  }
}
