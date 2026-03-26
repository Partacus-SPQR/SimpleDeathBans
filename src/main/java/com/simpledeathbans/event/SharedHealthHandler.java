package com.simpledeathbans.event;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.damage.SoulSeverDamageSource;
import com.simpledeathbans.util.DamageShareTracker;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

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
            if (!(entity instanceof ServerPlayer player)) {
                return true;
            }
            
            SimpleDeathBans mod = SimpleDeathBans.getInstance();
            if (mod == null) return true;
            
            ModConfig config = mod.getConfig();
            if (config == null || !config.enableSharedHealth) {
                return true;
            }
            
            // SINGLE-PLAYER: Skip shared health (no other players)
            ServerLevel world = (ServerLevel) player.level();
            if (world.getServer().isSingleplayer()) {
                return true;
            }
            
            // Don't share soul sever damage (prevents infinite loops)
            if (SoulSeverDamageSource.isSoulSever(source)) {
                return true;
            }
            
            // Check GLOBAL tracker to prevent cross-handler recursion with SoulLink
            UUID playerId = player.getUUID();
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
    private static void shareNonLethalDamage(ServerPlayer sourcePlayer, float damage, ModConfig config) {
        ServerLevel sourceWorld = (ServerLevel) sourcePlayer.level();
        MinecraftServer server = sourceWorld.getServer();
        if (server == null) return;
        
        List<ServerPlayer> allPlayers = new ArrayList<>(server.getPlayerList().getPlayers());
        
        // Remove the source player (they already took damage)
        allPlayers.removeIf(p -> p.getUUID().equals(sourcePlayer.getUUID()));
        
        if (allPlayers.isEmpty()) return;
        
        // Apply damage to all other players (non-lethal only - mixin catches lethal)
        for (ServerPlayer player : allPlayers) {
            UUID playerId = player.getUUID();
            
            // Mark as processing GLOBALLY to prevent cross-handler recursion
            DamageShareTracker.markProcessing(playerId);
            try {
                ServerLevel world = (ServerLevel) player.level();
                // Use magic damage for shared damage
                player.hurtServer(world, world.damageSources().magic(), damage);
            } finally {
                DamageShareTracker.clearProcessing(playerId);
            }
        }
    }
}
