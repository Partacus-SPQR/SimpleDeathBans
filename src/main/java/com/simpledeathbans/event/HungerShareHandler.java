package com.simpledeathbans.event;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.data.SoulLinkManager;
import com.simpledeathbans.util.DamageShareTracker;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.*;

/**
 * Handles hunger sharing for Soul Link and Shared Health systems.
 * 
 * HUNGER SHARING FLOW:
 * - Soul Link: When one partner loses hunger, the other loses the same amount
 * - Shared Health: When any player loses hunger, all players lose the same amount
 * 
 * Uses tick-based tracking to detect hunger changes between ticks.
 */
public class HungerShareHandler {
    
    // Track previous hunger levels per player
    private static final Map<UUID, Integer> previousHunger = new HashMap<>();
    
    // Prevent recursion during hunger sharing
    private static final Set<UUID> processingHunger = new HashSet<>();
    
    public static void register() {
        // Register end of server tick handler
        ServerTickEvents.END_SERVER_TICK.register(HungerShareHandler::onServerTick);
    }
    
    private static void onServerTick(MinecraftServer server) {
        SimpleDeathBans mod = SimpleDeathBans.getInstance();
        if (mod == null) return;
        
        ModConfig config = mod.getConfig();
        if (config == null) return;
        
        // Skip if both hunger sharing options are disabled
        if (!config.soulLinkShareHunger && !config.sharedHealthShareHunger) {
            // Clear tracking when disabled
            previousHunger.clear();
            return;
        }
        
        // Skip in single-player (no one to share with)
        if (server.isSingleplayer()) {
            return;
        }
        
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        if (players.isEmpty()) return;
        
        SoulLinkManager soulLinkManager = mod.getSoulLinkManager();
        
        // Process hunger changes
        for (ServerPlayerEntity player : players) {
            UUID playerId = player.getUuid();
            int currentHunger = player.getHungerManager().getFoodLevel();
            
            // Get previous hunger (default to current if not tracked)
            Integer previousLevel = previousHunger.get(playerId);
            
            if (previousLevel != null && currentHunger < previousLevel) {
                // Player lost hunger - check if we should share it
                int hungerLost = previousLevel - currentHunger;
                
                // Skip if already processing this player's hunger (prevents recursion)
                if (processingHunger.contains(playerId)) {
                    continue;
                }
                
                // Shared Health takes priority (server-wide sharing)
                if (config.enableSharedHealth && config.sharedHealthShareHunger) {
                    shareHungerToAll(player, hungerLost, server);
                }
                // Soul Link only if Shared Health is off
                else if (config.enableSoulLink && config.soulLinkShareHunger && soulLinkManager != null) {
                    shareHungerToPartner(player, hungerLost, soulLinkManager);
                }
            }
            
            // Update tracked hunger
            previousHunger.put(playerId, currentHunger);
        }
        
        // Clean up players who left
        previousHunger.keySet().removeIf(uuid -> 
            server.getPlayerManager().getPlayer(uuid) == null
        );
    }
    
    /**
     * Share hunger loss to all other players (Shared Health mode)
     */
    private static void shareHungerToAll(ServerPlayerEntity source, int hungerLost, MinecraftServer server) {
        UUID sourceId = source.getUuid();
        
        // Mark source as processing to prevent recursion
        processingHunger.add(sourceId);
        
        try {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.getUuid().equals(sourceId)) continue;
                
                UUID playerId = player.getUuid();
                
                // Skip if this player is also being processed
                if (processingHunger.contains(playerId)) continue;
                
                // Mark as processing
                processingHunger.add(playerId);
                
                try {
                    // Reduce hunger (minimum 0)
                    int currentHunger = player.getHungerManager().getFoodLevel();
                    int newHunger = Math.max(0, currentHunger - hungerLost);
                    player.getHungerManager().setFoodLevel(newHunger);
                    
                    // Update tracking to prevent re-triggering
                    previousHunger.put(playerId, newHunger);
                } finally {
                    processingHunger.remove(playerId);
                }
            }
        } finally {
            processingHunger.remove(sourceId);
        }
    }
    
    /**
     * Share hunger loss to soul-linked partner (Soul Link mode)
     */
    private static void shareHungerToPartner(ServerPlayerEntity source, int hungerLost, SoulLinkManager soulLinkManager) {
        UUID sourceId = source.getUuid();
        
        Optional<UUID> partnerUuid = soulLinkManager.getPartner(sourceId);
        if (partnerUuid.isEmpty()) return;
        
        ServerWorld world = (ServerWorld) source.getEntityWorld();
        ServerPlayerEntity partner = world.getServer().getPlayerManager().getPlayer(partnerUuid.get());
        
        if (partner == null || !partner.isAlive()) return;
        
        UUID partnerId = partner.getUuid();
        
        // Skip if partner is already being processed
        if (processingHunger.contains(partnerId)) return;
        
        // Mark both as processing
        processingHunger.add(sourceId);
        processingHunger.add(partnerId);
        
        try {
            // Reduce partner's hunger (minimum 0)
            int currentHunger = partner.getHungerManager().getFoodLevel();
            int newHunger = Math.max(0, currentHunger - hungerLost);
            partner.getHungerManager().setFoodLevel(newHunger);
            
            // Update tracking to prevent re-triggering
            previousHunger.put(partnerId, newHunger);
        } finally {
            processingHunger.remove(sourceId);
            processingHunger.remove(partnerId);
        }
    }
    
    /**
     * Clear tracking for a player (called when they disconnect)
     */
    public static void clearPlayer(UUID playerId) {
        previousHunger.remove(playerId);
        processingHunger.remove(playerId);
    }
}
