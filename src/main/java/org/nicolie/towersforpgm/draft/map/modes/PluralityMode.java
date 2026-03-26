package org.nicolie.towersforpgm.draft.map.modes;

import java.util.*;
import org.nicolie.towersforpgm.draft.map.MapVoteConfig;

public class PluralityMode {
  private final MapVoteConfig config;
  private final List<String> displayedMaps;
  private final Map<UUID, String> votes;
  private final String SECRET_KEY;
  private final String secretMapName;

  public PluralityMode(
      MapVoteConfig config,
      List<String> displayedMaps,
      Map<UUID, String> votes,
      String SECRET_KEY,
      String secretMapName) {
    this.config = config;
    this.displayedMaps = displayedMaps;
    this.votes = votes;
    this.SECRET_KEY = SECRET_KEY;
    this.secretMapName = secretMapName;
  }

  public void castPluralityVote(UUID voterUUID, String displayedName) {
    if (!displayedMaps.contains(displayedName)) return;
    votes.put(voterUUID, displayedName);
  }

  public String resolvePluralityWinner() {
    Map<String, Integer> counts = getVoteCounts();
    if (counts.isEmpty())
      return resolveSecret(
          displayedMaps.isEmpty() ? config.getMaps().get(0) : displayedMaps.get(0));

    int max = Collections.max(counts.values());
    List<String> tied = new ArrayList<>();
    for (Map.Entry<String, Integer> e : counts.entrySet()) {
      if (e.getValue() == max) tied.add(e.getKey());
    }
    Collections.shuffle(tied, new Random());
    return resolveSecret(tied.get(0));
  }

  private Map<String, Integer> getVoteCounts() {
    Map<String, Integer> counts = new HashMap<>();
    for (String m : displayedMaps) counts.put(m, 0);
    for (String m : votes.values()) counts.merge(m, 1, Integer::sum);
    return counts;
  }

  private String resolveSecret(String name) {
    return SECRET_KEY.equals(name) ? secretMapName : name;
  }
}
