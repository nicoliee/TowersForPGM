package org.nicolie.towersforpgm.preparationTime;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.spawns.events.ParticipantKitApplyEvent;
import tc.oc.pgm.util.bukkit.Sounds;

// Clase para manejar la protección de la preparación de partidas
// Aunque PGM no soporta varias partidas simultáneas, todos los métodos están diseñados para ser
// usados **por partida**

public class PreparationListener implements Listener {
  private final TowersForPGM plugin = TowersForPGM.getInstance();
  private final Map<String, Long> protectionStartTimes = new HashMap<>();
  private final Map<String, BukkitTask> activeTimers = new HashMap<>();

  public boolean isMapInConfig(String mapName) {
    if (plugin.config().preparationTime().getRegions().containsKey(mapName)) {
      return true;
    }
    return false;
  }

  public void startProtection(Player player, Match match) {
    String matchName = match.getMap().getName();
    String worldName = match.getWorld().getName();
    Map<String, Region> regions = plugin.config().preparationTime().getRegions();
    Region region = regions.get(matchName);

    if (region != null) {
      if (plugin.config().preparationTime().getMatchConfig(worldName) != null && player != null) {
        MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
        matchPlayer.sendWarning(Component.translatable("preparation.alreadyStarted"));
        return;
      }

      Location p1 = region.getP1();
      Location p2 = region.getP2();
      int timer = region.getTimer() * 60; // Convertir minutos a segundos
      int haste = region.getHaste() * 60; // Convertir minutos a segundos
      haste = Math.min(timer, haste);
      Long timeStart = System.currentTimeMillis();
      MatchConfig matchConfig = new MatchConfig(p1, p2, timer, haste, timeStart);
      plugin.config().preparationTime().storeMatchConfig(worldName, matchConfig);
      startProtectionTimer(player, timer, haste, region, match);
      protectionStartTimes.put(worldName, System.currentTimeMillis());
    } else {
      if (player != null) {
        MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
        matchPlayer.sendWarning(Component.translatable("region.mapError"));
      }
    }
  }

  public void stopProtection(Player player, Match match) {
    String worldName = match.getWorld().getName();
    MatchConfig matchConfig = plugin.config().preparationTime().getMatchConfig(worldName);
    if (matchConfig != null) {
      Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
        plugin.config().preparationTime().removeMatchConfig(worldName);
        cancelTimer(worldName);
        protectionStartTimes.remove(worldName);
      });
    } else {
      if (player != null) {
        MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
        matchPlayer.sendWarning(Component.translatable("preparation.notStarted"));
      }
    }
  }

  private void cancelTimer(String worldName) {
    BukkitTask activeTask = activeTimers.remove(worldName);
    if (activeTask != null) {
      activeTask.cancel();
    }
  }

  private void startTimer(String worldName, int duration, IntConsumer onTick, Runnable onComplete) {
    cancelTimer(worldName);

    int[] timeLeft = {duration};
    BukkitTask task = new BukkitRunnable() {
      @Override
      public void run() {
        if (timeLeft[0] <= 0) {
          if (onComplete != null) {
            onComplete.run();
          }
          cancel();
          return;
        }

        if (onTick != null) {
          onTick.accept(timeLeft[0]);
        }

        timeLeft[0]--;
      }
    }.runTaskTimer(plugin, 0L, 20L);

    activeTimers.put(worldName, task);
  }

  private void startProtectionTimer(
      Player player, int timer, int haste, Region region, Match match) {
    String worldName = match.getWorld().getName();
    match.sendMessage(Component.translatable(
            "preparation.timeRemaining",
            Component.text(SendMessage.formatTime(timer)).color(NamedTextColor.GREEN))
        .color(NamedTextColor.AQUA));

    startTimer(
        worldName,
        timer,
        timeRemaining -> {
          MatchConfig currentMatchConfig =
              plugin.config().preparationTime().getMatchConfig(worldName);
          if (currentMatchConfig == null) {
            cancelTimer(worldName);
            return;
          }

          Component message = Component.translatable(
                  "preparation.actionBar",
                  Component.text(SendMessage.formatTime(timeRemaining)).color(NamedTextColor.GREEN))
              .color(NamedTextColor.AQUA);
          match.sendActionBar(message.decorate(TextDecoration.ITALIC));

          if (timeRemaining != timer
              && (timeRemaining == 60
                  || timeRemaining == 30
                  || timeRemaining == 10
                  || (timeRemaining <= 5 && timeRemaining >= 1))) {
            match.sendMessage(Component.translatable(
                    "preparation.timeRemaining",
                    Component.text(SendMessage.formatTime(timeRemaining))
                        .color(NamedTextColor.GREEN))
                .color(NamedTextColor.AQUA));
          }

          if (timeRemaining != timer
              && (timeRemaining == 60 || timeRemaining <= 30 && timeRemaining > 3)) {
            match.playSound(Sounds.INVENTORY_CLICK);
          }
          if (timeRemaining <= 3 && timeRemaining > 0) {
            match.playSound(Sounds.MATCH_COUNTDOWN);
          }

          /* Remover la protección 1 segundo antes para evitar que los
          jugadores intenten colocar o romper bloques milisegundos antes
          de avisar que el tiempo acabó  */
          if (timeRemaining == 1) {
            stopProtection(player, match);
          }
        },
        () -> {
          match.sendMessage(Component.translatable("preparation.end").color(NamedTextColor.GREEN));
          match.playSound(Sounds.MATCH_START);
          cancelTimer(worldName);
        });
  }

  private void applyEffect(Player player, PotionEffectType effect, int duration, int amplifier) {
    if (player != null) {
      player.addPotionEffect(new PotionEffect(effect, duration * 20, amplifier));
    }
  }

  @EventHandler
  public void onParticipantKitApply(ParticipantKitApplyEvent event) {
    Player player = event.getPlayer().getBukkit();

    /* Ejecutar un tick después debido a que el kit se aplica después del evento,
    esto asegura que los efectos de regeneración y haste se apliquen correctamente */
    Bukkit.getScheduler()
        .runTaskLater(
            plugin,
            () -> {
              String worldName = player.getWorld().getName();
              MatchConfig matchConfig = plugin.config().preparationTime().getMatchConfig(worldName);

              if (matchConfig != null) {
                int totalProtectionTime = matchConfig.getTime();
                int hasteTime = matchConfig.getHaste();
                int timeElapsed =
                    (int) ((System.currentTimeMillis() - matchConfig.getTimeStart()) / 1000);

                int remainingRegenerationTime = totalProtectionTime - timeElapsed;
                int remainingHasteTime = hasteTime - timeElapsed;

                player.removePotionEffect(PotionEffectType.REGENERATION);
                player.removePotionEffect(PotionEffectType.FAST_DIGGING);

                applyEffect(
                    player, PotionEffectType.REGENERATION, remainingRegenerationTime + 1, 0);
                applyEffect(player, PotionEffectType.FAST_DIGGING, remainingHasteTime + 1, 1);
              }
            },
            5L);
  }

  // Manejar el evento de colocar bloques
  @EventHandler
  public void onBlockPlace(BlockPlaceEvent event) {
    Location location = event.getBlock().getLocation();
    String worldName = location.getWorld().getName();
    MatchConfig matchConfig = plugin.config().preparationTime().getMatchConfig(worldName);

    if (matchConfig != null) {
      if (matchConfig.isInside(location)) {
        Player player = event.getPlayer();
        event.setCancelled(true);
        Component message = Component.translatable("preparation.blockPlace");
        PGM.get().getMatchManager().getPlayer(player).sendWarning(message);
      }
    }
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent event) {
    Location location = event.getBlock().getLocation();
    String worldName = location.getWorld().getName();
    MatchConfig matchConfig = plugin.config().preparationTime().getMatchConfig(worldName);

    if (matchConfig != null) {
      if (matchConfig.isInside(location)) {
        Player player = event.getPlayer();
        event.setCancelled(true);
        Component message = Component.translatable("preparation.blockBreak");
        PGM.get().getMatchManager().getPlayer(player).sendWarning(message);
      }
    }
  }
}
