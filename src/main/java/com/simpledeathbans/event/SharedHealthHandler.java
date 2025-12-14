package com.simpledeathbans.event;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.damage.SoulSeverDamageSource;
import com.simpledeathbans.util.DamageShareTracker;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles server-wide shared health damage sharing.
 * 
 * SHARED HEALTH FLOW:
 * - This handler shares NON-LETHAL damage to all players
 * - Damage share % affects how much damage others take
 * - LETHAL damage is handled by SharedHealthMixin (Death Pact)
 * 
 * Damage Share % only affects NON-LETHAL damage!
 * Lethal damage triggers Death Pact = instant death for everyone (handled by mixin)
 */
public class SharedHealthHandler {
    
    public static void register() {
        // Register damage event for shared health (NON-LETHAL only)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) {
                return true;
            }
            
            SimpleDeathBans mod = SimpleDeathBans.getInstance();
            if (mod == null) return true;
            
            ModConfig config = mod.getConfig();
            if (config == null || !config.enableSharedHealth) {
                return true;
            }
            
            // SINGLE-PLAYER: Skip shared health (no other players)
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            if (world.getServer().isSingleplayer()) {
                return true;
            }
            
            // Don't share soul sever damage (prevents infinite loops)
            if (SoulSeverDamageSource.isSoulSever(source)) {
                return true;
            }
            
            // Check GLOBAL tracker to prevent cross-handler recursion with SoulLink
            UUID playerId = player.getUuid();
            if (DamageShareTracker.isProcessing(playerId)) {
                return true;
            }
            
            // Only share NON-LETHAL damage (mixin handles lethal via Death Pact)
            // Check if this damage would kill the player
            float currentHealth = player.getHealth();
            if (amount >= currentHealth) {
                // This is lethal damage - let the mixin handle Death Pact
                return true;
            }
            
            // Calculate shared damage amount (percentage of original)
            // sharedHealthDamagePercent: 100 = 100% = 1:1 ratio
            float sharedDamage = (float) (amount * config.sharedHealthDamagePercent / 100.0);
            
            // Share non-lethal damage to all other players
            shareNonLethalDamage(player, sharedDamage, config);
            
            return true; // Allow original damage to the initial player
        });
    }
    
    /**
     * Share non-lethal damage to all online players.
     * Lethal scenarios are handled by SharedHealthMixin.
     */
    private static void shareNonLethalDamage(ServerPlayerEntity sourcePlayer, float damage, ModConfig config) {
        ServerWorld sourceWorld = (ServerWorld) sourcePlayer.getEntityWorld();
        MinecraftServer server = sourceWorld.getServer();
        if (server == null) return;
        
        List<ServerPlayerEntity> allPlayers = new ArrayList<>(server.getPlayerManager().getPlayerList());
        
        // Remove the source player (they already took damage)
        allPlayers.removeIf(p -> p.getUuid().equals(sourcePlayer.getUuid()));
        
        if (allPlayers.isEmpty()) return;
        
        // Apply damage to all other players (non-lethal only - mixin catches lethal)
        for (ServerPlayerEntity player : allPlayers) {
            UUID playerId = player.getUuid();
            
            // Mark as processing GLOBALLY to prevent cross-handler recursion
            DamageShareTracker.markProcessing(playerId);
            try {
                ServerWorld world = (ServerWorld) player.getEntityWorld();
                // Use magic damage for shared damage
                player.damage(world, world.getDamageSources().magic(), damage);
            } finally {
                DamageShareTracker.clearProcessing(playerId);
            }
        }
    }
}
