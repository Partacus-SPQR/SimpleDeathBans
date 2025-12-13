package com.simpledeathbans.event;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.data.BanDataManager;
import com.simpledeathbans.data.PlayerDataManager;
import com.simpledeathbans.data.PlayerDataManager.PlayerActivityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * Handles the Mercy Cooldown mechanic.
 * Tracks playtime and activity since last death, and reduces ban tier accordingly.
 * Uses configurable playtime hours and activity thresholds (anti-AFK).
 */
public class MercyCooldownHandler {
    
    private static long lastActivityCheck = 0;
    
    /**
     * Called on server tick to process mercy cooldown tier reductions.
     * Checks if enough playtime + activity has passed to reduce ban tier.
     */
    public static void onServerTick(MinecraftServer server) {
        SimpleDeathBans mod = SimpleDeathBans.getInstance();
        if (mod == null) return;
        
        ModConfig config = mod.getConfig();
        PlayerDataManager playerDataManager = mod.getPlayerDataManager();
        BanDataManager banDataManager = mod.getBanDataManager();
        
        if (config == null || playerDataManager == null || banDataManager == null) return;
        
        long currentTime = System.currentTimeMillis();
        
        // Increment playtime for all online players every tick
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerId = player.getUuid();
            playerDataManager.incrementPlaytime(playerId, 1);
        }
        
        // Only do activity checks at configured interval (convert minutes to millis)
        long checkInterval = config.mercyCheckIntervalMinutes * 60000L;
        if (currentTime - lastActivityCheck < checkInterval) return;
        lastActivityCheck = currentTime;
        
        // Check each online player for mercy cooldown eligibility
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerId = player.getUuid();
            String playerName = player.getName().getString();
            
            // Get player activity data
            PlayerActivityData data = playerDataManager.get(playerId);
            if (data == null || data.lastDeathTime == 0) continue; // Never died
            
            // Get current tier from ban data manager
            int currentTier = banDataManager.getTier(playerId);
            if (currentTier <= 0) continue; // No tier to reduce
            
            // Calculate playtime since death in hours
            long requiredPlaytimeTicks = config.mercyPlaytimeHours * 60L * 60L * 20L; // hours to ticks
            
            // Check if player has enough playtime
            if (data.totalPlaytimeSinceDeathTicks >= requiredPlaytimeTicks) {
                // Check if player has been active (anti-AFK)
                boolean wasActive = data.checkActivity(player, 
                    config.mercyMovementBlocks, 
                    config.mercyBlockInteractions);
                
                if (wasActive) {
                    // Reduce tier by 1
                    banDataManager.decrementTier(playerId);
                    int newTier = banDataManager.getTier(playerId);
                    
                    // Reset playtime counter for next mercy cycle
                    data.totalPlaytimeSinceDeathTicks = 0;
                    data.lastDeathTime = currentTime; // Reset to track next mercy period
                    playerDataManager.save();
                    
                    // Play feedback sound (only this player hears it)
                    server.getOverworld().playSound(
                        null, // no player exclusion
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                        SoundCategory.PLAYERS,
                        1.0f, 1.0f
                    );
                    
                    // Show action bar message: "Your past sins are forgotten."
                    player.sendMessage(
                        Text.translatable("simpledeathbans.mercy.forgiven"),
                        true // action bar
                    );
                    
                    SimpleDeathBans.LOGGER.info("Mercy cooldown: {} tier reduced to {}", 
                        playerName, newTier);
                }
            }
            
            // Start next activity check period
            data.startActivityCheck(player);
        }
    }
}
