package org.nicolie.towersforpgm.utils;

import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
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
      mapPoolManager
          .getActiveMapPool()
          .getMaps(); // esto se debe usar para los mapas de ranked (debe primero tomar el nombre de
      // la pool y luego buscar esa pool y tomar sus mapas en cada onMatchLoad)
      return mapPoolManager.getActiveMapPool().getName();
    }
    return null;
  }
}
