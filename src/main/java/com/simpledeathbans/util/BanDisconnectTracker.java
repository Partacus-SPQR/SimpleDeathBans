package com.simpledeathbans.util;

/**
 * Utility class to track ban disconnect state.
 * Used to suppress the default "left the game" message when a player is banned.
 */
public class BanDisconnectTracker {
    
    // This flag indicates if the disconnect was due to our ban system
    private static final ThreadLocal<Boolean> isBanDisconnect = ThreadLocal.withInitial(() -> false);
    
    public static void markBanDisconnect() {
        isBanDisconnect.set(true);
    }
    
    public static boolean isBanDisconnect() {
        return isBanDisconnect.get();
    }
    
    public static void clearBanDisconnect() {
        isBanDisconnect.set(false);
    }
}
