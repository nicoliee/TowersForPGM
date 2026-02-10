package org.nicolie.towersforpgm.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.nicolie.towersforpgm.draft.components.DraftPhase;
import org.nicolie.towersforpgm.draft.components.DraftReroll;
import org.nicolie.towersforpgm.draft.core.Draft;

public class RerollItemListener implements Listener {

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_AIR
        && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    Player player = event.getPlayer();
    ItemStack item = event.getItem();

    if (!DraftReroll.isRerollItem(item)) {
      return;
    }

    // Check if draft is active
    if (Draft.getPhase() == DraftPhase.IDLE) {
      return;
    }

    Draft draft = getDraftInstance();
    if (draft == null) {
      return;
    }

    // Cancel the event to prevent any default behavior
    event.setCancelled(true);

    DraftPhase currentPhase = draft.getState().getCurrentPhase();

    // CAPTAINS phase: Vote for reroll
    if (currentPhase == DraftPhase.CAPTAINS) {
      boolean success = draft.getRerollManager().onPlayerRequestReroll(player.getUniqueId());
      if (!success) {
        player.sendMessage("§cNo puedes solicitar un reroll en este momento.");
      }
      return;
    }

    if (currentPhase == DraftPhase.REROLL) {
      draft.getRerollOptionsGUI().reopenGUI(player);
      return;
    }
  }

  private Draft getDraftInstance() {
    try {
      java.lang.reflect.Field instanceField = Draft.class.getDeclaredField("instance");
      instanceField.setAccessible(true);
      return (Draft) instanceField.get(null);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      return null;
    }
  }
}
