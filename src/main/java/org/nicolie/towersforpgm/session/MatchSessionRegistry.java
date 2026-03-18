package org.nicolie.towersforpgm.session;

import java.util.IdentityHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchUnloadEvent;

public final class MatchSessionRegistry {

  private static final Map<Match, MatchSession> SESSIONS = new IdentityHashMap<>();
  private static TowersForPGM plugin;

  public static void register(TowersForPGM towersPlugin) {
    plugin = towersPlugin;
    plugin.getServer().getPluginManager().registerEvents(new CleanupListener(), plugin);
  }

  public static MatchSession of(Match match) {
    return SESSIONS.computeIfAbsent(match, m -> new MatchSession(m, plugin));
  }

  @Nullable
  public static MatchSession get(Match match) {
    return SESSIONS.get(match);
  }

  public static int size() {
    return SESSIONS.size();
  }

  public static void destroy(Match match) {
    MatchSession session = SESSIONS.remove(match);
    if (session != null) session.destroy();
  }

  /**
   * Se ejecuta al finalizar el match para limpiar la sesión. PRIORIDAD MONITOR: asegura que otros
   * listeners puedan leer el estado antes de destruirlo.
   */
  private static final class CleanupListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMatchFinish(MatchFinishEvent event) {
      destroy(event.getMatch());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMatchUnload(MatchUnloadEvent event) {
      destroy(event.getMatch());
    }
  }
}
