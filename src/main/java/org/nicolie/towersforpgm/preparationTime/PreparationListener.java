package org.nicolie.towersforpgm.preparationTime;

import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
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
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.spawns.events.ParticipantKitApplyEvent;
import tc.oc.pgm.util.bukkit.Sounds;

// Clase para manejar la protección de la preparación de partidas
// Aunque PGM no soporta varias partidas simultáneas, todos los métodos están diseñados para ser
// usados **por mundo**
// Sin embargo por el momento al llamar a los métodos se debe especificar el nombre del mundo, estos
// se sacan de PGM
// (MatchLoadEvent, MatchUnloadEvent, MatchStartEvent, MatchEndEvent, etc.)
// y esto hace que se asuma que solo hay una partida en ejecución en todo el servidor.

public class PreparationListener implements Listener {
  private final TowersForPGM plugin = TowersForPGM.getInstance(); // Instancia del plugin
  private Map<String, Long> protectionStartTimes =
      new HashMap<>(); // Mapa para almacenar el tiempo de inicio de la protección por nombre de
  // mundo
  private Map<String, BukkitTask> activeTimers =
      new HashMap<>(); // Mapa para almacenar temporizadores activos por nombre de mundo

  public boolean isMapInConfig(String mapName) {
    if (plugin.getRegions().containsKey(mapName)) {
      return true;
    }
    return false;
  }

  // Método para iniciar la protección de una partida (con matchName y worldName)
  public void startProtection(Player player, Match match) {
    // Acceder a las regiones cargadas a través del plugin
    String matchName = match.getMap().getName(); // Obtener el nombre del partido
    String worldName = match.getWorld().getName(); // Obtener el nombre del mundo
    Map<String, Region> regions = plugin.getRegions();

    // Buscar la región por el nombre del partido
    Region region = regions.get(matchName);

    if (region != null) {
      // Comprobar si ya existe una configuración para el mundo
      if (plugin.getMatchConfig(worldName) != null) {
        // Si la configuración ya existe, no sobrescribir y dar un aviso
        SendMessage.sendToPlayer(player, LanguageManager.langMessage("preparation.alreadyStarted"));
        return; // Salir del método para evitar sobreescribir la configuración
      }

      // Acciones a realizar si la región fue encontrada
      // Obtener las coordenadas de la región
      Location p1 = region.getP1();
      Location p2 = region.getP2();
      int timer = region.getTimer() * 60; // Convertir minutos a segundos
      int haste = region.getHaste() * 60; // Convertir minutos a segundos
      haste = Math.min(timer, haste); // Limitar el tiempo de Haste al tiempo de protección
      Long timeStart = System.currentTimeMillis(); // Tiempo de inicio de la protección
      // Crear la instancia de MatchConfig con las coordenadas obtenidas
      MatchConfig matchConfig = new MatchConfig(p1, p2, timer, haste, timeStart);
      // Almacenar la configuración usando el método de la clase principal
      plugin.storeMatchConfig(
          worldName, matchConfig); // Guarda la configuración asociada al worldName
      startProtectionTimer(
          player, timer, haste, region, match); // Iniciar el temporizador de protección
      protectionStartTimes.put(worldName, System.currentTimeMillis());
    } else {
      SendMessage.sendToPlayer(player, LanguageManager.langMessage("region.mapError"));
    }
  }

  // Método para detener la protección de una partida (con worldName)
  public void stopProtection(Player player, Match match) {
    String worldName = match.getWorld().getName(); // Obtener el nombre del mundo
    // Verificar si la configuración existe para el mundo
    MatchConfig matchConfig = plugin.getMatchConfig(worldName);
    if (matchConfig != null) {
      Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
        plugin.removeMatchConfig(worldName); // Eliminar la configuración del mundo
        activeTimers.get(worldName).cancel();
        activeTimers.remove(worldName); // Eliminar el temporizador del mapa
        protectionStartTimes.remove(worldName); // Eliminar el tiempo de inicio de la protección
        Bukkit.getScheduler().runTask(plugin, () -> {
          // Enviar mensajes al mundo
          SendMessage.sendToWorld(worldName, LanguageManager.message("preparation.end"));
        });
      });
    } else {
      // Si la configuración no existe, enviamos un mensaje sincrónicamente
      SendMessage.sendToPlayer(player, LanguageManager.langMessage("preparation.notStarted"));
    }
  }

  private void startProtectionTimer(
      Player player, int timer, int haste, Region region, Match match) {
    String worldName = match.getWorld().getName(); // Obtener el nombre del mundo
    Component message = Component.text(LanguageManager.langMessage("preparation.actionBar"));
    SendMessage.sendToWorld(
        worldName,
        LanguageManager.message("preparation.timeRemaining")
            .replace("{time}", SendMessage.formatTime(timer)));
    match.playSound(Sounds.INVENTORY_CLICK);

    // Iniciar el temporizador para detener la protección después de 'timer' segundos
    BukkitTask task = new BukkitRunnable() {
      @Override
      public void run() {
        // Obtener el tiempo actual y calcular el tiempo restante en función de la diferencia con el
        // tiempo de inicio
        long currentTime = System.currentTimeMillis();
        long timeElapsed = (currentTime - plugin.getMatchConfig(worldName).getTimeStart())
            / 1000; // Tiempo transcurrido en segundos
        int timeRemaining =
            (int) (timer - timeElapsed); // Tiempo restante de protección en segundos
        match.sendActionBar(message.append(Component.text(SendMessage.formatTime(timeRemaining))));
        if (timeRemaining > 0 && timeRemaining <= timer) {
          // Enviar mensajes a intervalos específicos
          if (timeRemaining != timer
              && (timeRemaining == 60
                  || timeRemaining == 30
                  || timeRemaining == 10
                  || (timeRemaining <= 5 && timeRemaining >= 1))) {
            // Si el tiempo restante son 30, 10, 5, 4, 3, 2, 1 segundos
            SendMessage.sendToWorld(
                worldName,
                LanguageManager.message("preparation.timeRemaining")
                    .replace("{time}", SendMessage.formatTime(timeRemaining)));
          }

          // Reproducir sonidos a intervalos específicos
          if (timeRemaining != timer
              && (timeRemaining == 60 || timeRemaining <= 30 && timeRemaining > 3)) {
            match.playSound(Sounds.INVENTORY_CLICK);
          }
          if (timeRemaining <= 3 && timeRemaining > 0) {
            match.playSound(Sounds.MATCH_COUNTDOWN);
          }
        } else {
          // Al finalizar el temporizador, detener la protección
          match.playSound(Sounds.MATCH_START);
          stopProtection(player, match); // Detener la protección
          this.cancel(); // Cancelar el temporizador
        }
      }
    }.runTaskTimer(plugin, 0L, 20L); // Ejecutar cada segundo (20 ticks = 1 segundo)
    // Guardar el temporizador en el mapa de temporizadores activos
    activeTimers.put(worldName, task);
  }

  private void applyEffect(Player player, PotionEffectType effect, int duration, int amplifier) {
    if (player != null) {
      player.addPotionEffect(new PotionEffect(effect, duration * 20, amplifier));
    }
  }

  @EventHandler
  public void onParticipantKitApply(ParticipantKitApplyEvent event) {
    Player player = event.getPlayer().getBukkit();

    // Ejecutar un tick después
    Bukkit.getScheduler()
        .runTaskLater(
            plugin,
            () -> {
              String worldName = player.getWorld().getName();
              MatchConfig matchConfig = plugin.getMatchConfig(worldName);

              if (matchConfig != null) {
                // Obtener tiempos de protección y Haste
                int totalProtectionTime = matchConfig.getTime();
                int hasteTime = matchConfig.getHaste();
                int timeElapsed =
                    (int) ((System.currentTimeMillis() - matchConfig.getTimeStart()) / 1000);

                int remainingRegenerationTime = totalProtectionTime - timeElapsed;
                int remainingHasteTime = hasteTime - timeElapsed;

                // Remover efectos y aplicar los nuevos
                player.removePotionEffect(PotionEffectType.REGENERATION);
                player.removePotionEffect(PotionEffectType.FAST_DIGGING);

                applyEffect(
                    player, PotionEffectType.REGENERATION, remainingRegenerationTime + 1, 0);
                applyEffect(player, PotionEffectType.FAST_DIGGING, remainingHasteTime + 1, 1);
              }
            },
            5L); // 1 tick después
  }

  // Manejar el evento de colocar bloques
  @EventHandler
  public void onBlockPlace(BlockPlaceEvent event) {
    // Obtén la ubicación donde el jugador intenta colocar el bloque
    Location location = event.getBlock().getLocation();
    String worldName = location.getWorld().getName();

    // Obtener la configuración de la región para ese mundo
    MatchConfig matchConfig = plugin.getMatchConfig(worldName);

    if (matchConfig != null) {
      // Verifica si la ubicación está dentro de la región
      if (matchConfig.isInside(location)) {
        Player player = event.getPlayer();
        // Cancela el evento si la ubicación está dentro de la región
        event.setCancelled(true);
        Component message = Component.text(LanguageManager.langMessage("preparation.blockPlace"));
        PGM.get().getMatchManager().getPlayer(player).sendWarning(message);
      }
    }
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent event) {
    // Obtén la ubicación donde el jugador intenta romper el bloque
    Location location = event.getBlock().getLocation();
    String worldName = location.getWorld().getName();

    // Obtener la configuración de la región para ese mundo
    MatchConfig matchConfig = plugin.getMatchConfig(worldName);

    if (matchConfig != null) {
      // Verifica si la ubicación está dentro de la región
      if (matchConfig.isInside(location)) {
        // Cancela el evento si la ubicación está dentro de la región
        Player player = event.getPlayer();
        event.setCancelled(true);
        Component message = Component.text(LanguageManager.langMessage("preparation.blockBreak"));
        PGM.get().getMatchManager().getPlayer(player).sendWarning(message);
      }
    }
  }
}
