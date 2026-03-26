package org.nicolie.towersforpgm.draft.map.modes;

import java.util.*;
import org.nicolie.towersforpgm.draft.map.MapVoteConfig;

public class VetoMode {
  private final List<String> displayedMaps;
  private final List<String> remainingMaps;
  private final Map<UUID, String> votes;
  private final String SECRET_KEY;

  public VetoMode(
      MapVoteConfig config,
      List<String> displayedMaps,
      List<String> remainingMaps,
      Map<UUID, String> votes,
      String SECRET_KEY) {
    this.displayedMaps = displayedMaps;
    this.remainingMaps = remainingMaps;
    this.votes = votes;
    this.SECRET_KEY = SECRET_KEY;
  }

  public void castVetoVote(UUID voterUUID, String displayedName) {
    if (!displayedMaps.contains(displayedName)) return;
    if (SECRET_KEY.equals(displayedName)) return;

    String previousVote = votes.get(voterUUID);
    if (displayedName.equals(previousVote)) return;
    if (!remainingMaps.contains(displayedName)) return;

    votes.put(voterUUID, displayedName);
    rebuildRemainingMapsForVeto();
  }

  public void rebuildRemainingMapsForVeto() {
    remainingMaps.clear();
    for (String mapName : displayedMaps) {
      if (SECRET_KEY.equals(mapName)) continue;
      if (!votes.containsValue(mapName)) {
        remainingMaps.add(mapName);
      }
    }
  }

  public String resolveVetoWinner() {
    if (remainingMaps.size() == 1) return remainingMaps.get(0);
    List<String> copy = new ArrayList<>(remainingMaps);
    Collections.shuffle(copy, new Random());
    return copy.get(0);
  }
}
