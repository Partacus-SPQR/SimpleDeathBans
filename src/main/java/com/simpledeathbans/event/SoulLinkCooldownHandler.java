package com.simpledeathbans.event;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.data.SoulLinkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

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
        long currentTick = server.getTicks();
        
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
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerId = player.getUuid();
            
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
                ServerPlayerEntity partner = server.getPlayerManager().getPlayer(assignedPartner.get());
                
                if (partner != null) {
                    String partnerName = partner.getName().getString();
                    String playerName = player.getName().getString();
                    
                    // Notify both players with mystical message
                    player.sendMessage(
                        Text.literal("§5✦ Your souls are now intertwined with §d§l" + partnerName + " §5✦"),
                        false
                    );
                    partner.sendMessage(
                        Text.literal("§5✦ Your souls are now intertwined with §d§l" + playerName + " §5✦"),
                        false
                    );
                    
                    // Play binding sound to both
                    ServerWorld world = (ServerWorld) player.getEntityWorld();
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.0f, 0.8f);
                    
                    ServerWorld partnerWorld = (ServerWorld) partner.getEntityWorld();
                    partnerWorld.playSound(null, partner.getX(), partner.getY(), partner.getZ(),
                        SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.0f, 0.8f);
                    
                    SimpleDeathBans.LOGGER.info("Soul link auto-assigned after cooldown expired: {} <-> {}", 
                        playerName, partnerName);
                }
            }
        }
    }
}
