package com.simpledeathbans.util;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global tracker to prevent infinite recursion between damage sharing systems.
 * Both SoulLinkEventHandler and SharedHealthHandler check this before sharing damage.
 * 
 * This prevents the following infinite loop:
 * 1. Player A takes damage → SoulLink shares to Partner B
 * 2. Partner B takes damage → SharedHealth shares to ALL (including A)
 * 3. Player A takes damage → SoulLink shares to Partner B again
 * 4. ... INFINITE LOOP
 */
public class DamageShareTracker {
    
    // Global set of players currently having damage shared TO them
    // Using ConcurrentHashMap for thread safety
    private static final Set<UUID> processingDamageShare = ConcurrentHashMap.newKeySet();
    
    /**
     * Mark a player as currently receiving shared damage.
     * Call this BEFORE damaging the player.
     */
    public static void markProcessing(UUID playerId) {
        processingDamageShare.add(playerId);
    }
    
    /**
     * Check if a player is currently receiving shared damage.
     * If true, DO NOT share damage to them (prevents infinite loops).
     */
    public static boolean isProcessing(UUID playerId) {
        return processingDamageShare.contains(playerId);
    }
    
    /**
     * Clear the processing flag for a player.
     * Call this AFTER the damage has been applied (in a finally block).
     */
    public static void clearProcessing(UUID playerId) {
        processingDamageShare.remove(playerId);
    }
}
