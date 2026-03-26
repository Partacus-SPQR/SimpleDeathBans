package com.simpledeathbans.event;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.data.SoulLinkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Handles periodic checking of soul link cooldowns and auto-reassignment.
 * Runs at a configurable interval to check if any players' cooldowns have expired and
 * attempts to reassign them if random partner mode is enabled.
 */
public class SoulLinkCooldownHandler {
    
    private static long lastCheckTick = 0;
    
    /**
     * Called on server tick to periodically check cooldowns.
     */
    public static void onServerTick(MinecraftServer server) {
        long currentTick = server.getTickCount();
        
        SimpleDeathBans mod = SimpleDeathBans.getInstance();
        if (mod == null) return;
        
        ModConfig config = mod.getConfig();
        if (config == null) return;
        
        // Calculate check interval from config (default 60 minutes = 1 hour)
        // Convert minutes to ticks: minutes * 60 seconds * 20 ticks/second
        long checkIntervalTicks = config.soulLinkRandomAssignCheckIntervalMinutes * 60L * 20L;
        
        // Only check at the configured interval
        if (currentTick - lastCheckTick < checkIntervalTicks) {
            return;
        }
        lastCheckTick = currentTick;
        
        SoulLinkManager soulLinkManager = mod.getSoulLinkManager();
        
        if (!config.enableSoulLink || soulLinkManager == null) {
            return;
        }
        
        // Only process in random partner mode
        if (!config.soulLinkRandomPartner) {
            return;
        }
        
        // Check each online player who doesn't have a partner
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            
            // Skip if already has a partner
            if (soulLinkManager.hasPartner(playerId)) {
                continue;
            }
            
            // Skip if still on sever cooldown
            if (soulLinkManager.isOnSeverCooldown(playerId)) {
                continue;
            }
            
            // Skip if on random reassign cooldown
            if (soulLinkManager.isOnRandomReassignCooldown(playerId)) {
                continue;
            }
            
            // Player is eligible for random assignment - try to find a partner
            Optional<UUID> assignedPartner = soulLinkManager.tryAssignPartner(playerId);
            
            if (assignedPartner.isPresent()) {
                // Successfully paired!
                ServerPlayer partner = server.getPlayerList().getPlayer(assignedPartner.get());
                
                if (partner != null) {
                    String partnerName = partner.getName().getString();
                    String playerName = player.getName().getString();
                    
                    // Notify both players with mystical message
                    player.sendSystemMessage(
                        Component.literal("§5✦ Your souls are now intertwined with §d§l" + partnerName + " §5✦"));
                    partner.sendSystemMessage(
                        Component.literal("§5✦ Your souls are now intertwined with §d§l" + playerName + " §5✦"));
                    
                    // Play binding sound to both
                    ServerLevel world = (ServerLevel) player.level();
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 1.0f, 0.8f);
                    
                    ServerLevel partnerWorld = (ServerLevel) partner.level();
                    partnerWorld.playSound(null, partner.getX(), partner.getY(), partner.getZ(),
                        SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 1.0f, 0.8f);
                    
                    SimpleDeathBans.LOGGER.info("Soul link auto-assigned after cooldown expired: {} <-> {}", 
                        playerName, partnerName);
                }
            }
        }
    }
}
