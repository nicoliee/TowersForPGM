package org.nicolie.towersforpgm.utils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RegisterCodeManager {

  private static final Map<UUID, String> activeCodes = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> codeExpiry = new ConcurrentHashMap<>();
  private static final long CODE_EXPIRY_MINUTES = 10;

  public static void storeCode(UUID playerUuid, String code) {
    activeCodes.put(playerUuid, code);
    codeExpiry.put(playerUuid, System.currentTimeMillis() + (CODE_EXPIRY_MINUTES * 60 * 1000));
  }

  public static UUID validateAndConsumeCode(String code) {
    cleanExpiredCodes();

    for (Map.Entry<UUID, String> entry : activeCodes.entrySet()) {
      if (entry.getValue().equals(code)) {
        UUID playerUuid = entry.getKey();

        // Verificar que no haya expirado
        Long expiry = codeExpiry.get(playerUuid);
        if (expiry != null && System.currentTimeMillis() < expiry) {
          // Código válido, consumirlo
          activeCodes.remove(playerUuid);
          codeExpiry.remove(playerUuid);

          return playerUuid;
        }
      }
    }

    return null;
  }

  public static boolean hasActiveCode(UUID playerUuid) {
    cleanExpiredCodes();
    return activeCodes.containsKey(playerUuid);
  }

  public static String getActiveCode(UUID playerUuid) {
    cleanExpiredCodes();
    return activeCodes.get(playerUuid);
  }

  private static void cleanExpiredCodes() {
    long currentTime = System.currentTimeMillis();

    codeExpiry.entrySet().removeIf(entry -> {
      if (currentTime > entry.getValue()) {
        UUID playerUuid = entry.getKey();
        activeCodes.remove(playerUuid);

        return true;
      }
      return false;
    });
  }

  public static int getActiveCodesCount() {
    cleanExpiredCodes();
    return activeCodes.size();
  }
}
