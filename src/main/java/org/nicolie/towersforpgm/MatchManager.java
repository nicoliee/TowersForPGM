package org.nicolie.towersforpgm;

import tc.oc.pgm.api.match.Match;

// Toma en cuenta que solo hay un mundo en el plugin como lo hace actualmente PGM (10/03/2025)

public class MatchManager {
    private Match currentMatch; // Match actual

    // Constructor
    public MatchManager() {
        this.currentMatch = null;
    }

    // Getter y Setter para currentMatch
    public Match getMatch() { 
        return currentMatch;
    }

    public void setCurrentMatch(Match match) { 
        this.currentMatch = match;
    }
}
