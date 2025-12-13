package com.simpledeathbans.event;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.damage.SoulSeverDamageSource;
import com.simpledeathbans.data.SoulLinkManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Optional;
import java.util.UUID;

/**
 * Handles Soul Link mechanics:
 * - Auto-assignment of partners on join
 * - Damage sharing between linked players
 * - Death pact notifications
 */
public class SoulLinkEventHandler {
    
    // Track to prevent infinite damage loops
    private static final java.util.Set<UUID> processingDamage = java.util.Collections.newSetFromMap(
        new java.util.concurrent.ConcurrentHashMap<>()
    );
    
    public static void register() {
        // Register damage event for soul link damage sharing
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) {
                return true; // Allow damage for non-players
            }
            
            SimpleDeathBans mod = SimpleDeathBans.getInstance();
            if (mod == null) return true;
            
            ModConfig config = mod.getConfig();
            SoulLinkManager soulLinkManager = mod.getSoulLinkManager();
            
            if (config == null || !config.enableSoulLink || soulLinkManager == null) {
                return true;
            }
            
            // Don't share soul sever damage (prevents infinite loops)
            if (SoulSeverDamageSource.isSoulSever(source)) {
                return true;
            }
            
            // Prevent recursive damage sharing
            UUID playerId = player.getUuid();
            if (processingDamage.contains(playerId)) {
                return true;
            }
            
            // Share damage to partner
            shareDamageToPartner(player, soulLinkManager, config.soulLinkDamageShare);
            
            return true; // Allow original damage
        });
    }
    
    /**
     * Share damage to soul-linked partner.
     */
    private static void shareDamageToPartner(ServerPlayerEntity damagedPlayer, 
                                              SoulLinkManager soulLinkManager, 
                                              double damageAmount) {
        UUID playerId = damagedPlayer.getUuid();
        
        soulLinkManager.getPartner(playerId).ifPresent(partnerUuid -> {
            ServerWorld world = (ServerWorld) damagedPlayer.getEntityWorld();
            ServerPlayerEntity partner = world.getServer().getPlayerManager().getPlayer(partnerUuid);
            
            if (partner != null && partner.isAlive()) {
                // Mark as processing to prevent infinite loops
                processingDamage.add(partnerUuid);
                try {
                    ServerWorld partnerWorld = (ServerWorld) partner.getEntityWorld();
                    // Use magic damage for soul link damage sharing (0.5 hearts = 1.0 damage)
                    partner.damage(partnerWorld, partnerWorld.getDamageSources().magic(), (float) damageAmount);
                } finally {
                    processingDamage.remove(partnerUuid);
                }
            }
        });
    }
    
    /**
     * Called when a player joins to:
     * 1. Auto-assign a soul partner if none exists
     * 2. Notify them of active soul link
     */
    public static void onPlayerJoin(ServerPlayerEntity player, SoulLinkManager soulLinkManager) {
        if (soulLinkManager == null) return;
        
        UUID playerId = player.getUuid();
        
        // Check if already has a partner
        if (soulLinkManager.hasPartner(playerId)) {
            // Notify of existing link
            soulLinkManager.getPartner(playerId).ifPresent(partnerUuid -> {
                // Get partner name if online
                ServerWorld world = (ServerWorld) player.getEntityWorld();
                ServerPlayerEntity partner = world.getServer().getPlayerManager().getPlayer(partnerUuid);
                String partnerName = partner != null ? partner.getName().getString() : "unknown";
                
                player.sendMessage(
                    Text.literal("Your soul is linked to ")
                        .append(Text.literal(partnerName).formatted(Formatting.GOLD))
                        .formatted(Formatting.DARK_PURPLE),
                    false
                );
                
                // Play subtle sound
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.5f, 1.2f);
            });
            return;
        }
        
        // Try to auto-assign a partner from the waiting pool
        Optional<UUID> assignedPartner = soulLinkManager.tryAssignPartner(playerId);
        
        if (assignedPartner.isPresent()) {
            // Successfully paired!
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            ServerPlayerEntity partner = world.getServer().getPlayerManager().getPlayer(assignedPartner.get());
            
            if (partner != null) {
                String partnerName = partner.getName().getString();
                String playerName = player.getName().getString();
                
                // Notify both players
                Text linkMessage1 = Text.literal("Your soul has been bound to ")
                    .append(Text.literal(partnerName).formatted(Formatting.GOLD))
                    .formatted(Formatting.DARK_PURPLE);
                Text linkMessage2 = Text.literal("Your soul has been bound to ")
                    .append(Text.literal(playerName).formatted(Formatting.GOLD))
                    .formatted(Formatting.DARK_PURPLE);
                
                player.sendMessage(linkMessage1, false);
                partner.sendMessage(linkMessage2, false);
                
                // Play binding sound to both
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.0f, 0.8f);
                
                ServerWorld partnerWorld = (ServerWorld) partner.getEntityWorld();
                partnerWorld.playSound(null, partner.getX(), partner.getY(), partner.getZ(),
                    SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.0f, 0.8f);
                
                SimpleDeathBans.LOGGER.info("Soul link auto-assigned: {} <-> {}", playerName, partnerName);
            }
        } else {
            // Added to waiting pool
            player.sendMessage(
                Text.literal("You await a soul partner...").formatted(Formatting.GRAY, Formatting.ITALIC),
                false
            );
        }
    }
}
