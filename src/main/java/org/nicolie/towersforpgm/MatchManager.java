package org.nicolie.towersforpgm;

import tc.oc.pgm.api.match.Match;

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
}
