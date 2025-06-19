package org.nicolie.towersforpgm.rankeds;

public class Queue {
    private static boolean queueEnabled = false;
    private static boolean rankedActive = false;

    public static boolean isQueueEnabled() {
        return queueEnabled;
    }
    
    public static boolean isRankedActive() {return rankedActive;}
    public static void setQueueEnabled(boolean enabled) {queueEnabled = enabled;}
}
