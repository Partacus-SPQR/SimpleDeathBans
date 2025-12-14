package com.simpledeathbans.event;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.damage.SoulSeverDamageSource;
import com.simpledeathbans.util.DamageShareTracker;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles server-wide shared health mechanics.
 * When enabled, ALL players share damage - if one player takes damage, everyone takes it.
 * If any player has a Totem of Undying in their offhand, it can save the entire team.
 */
public class SharedHealthHandler {
    
    // Damage source name for shared damage
    private static final String SHARED_DAMAGE_ID = "simpledeathbans.shared_health";
    
    public static void register() {
        // Register damage event for shared health
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
            
            // Don't share soul sever damage or shared damage (prevents infinite loops)
            if (SoulSeverDamageSource.isSoulSever(source)) {
                return true;
            }
            
            // Check GLOBAL tracker to prevent cross-handler recursion with SoulLink
            UUID playerId = player.getUuid();
            if (DamageShareTracker.isProcessing(playerId)) {
                return true;
            }
            
            // Calculate shared damage amount
            float sharedDamage = (float) (amount * config.sharedHealthDamagePercent);
            
            // Share damage to all other players
            shareHealthDamage(player, sharedDamage, config);
            
            return true; // Allow original damage to the initial player
        });
    }
    
    /**
     * Share damage to all online players.
     * If any player would die and someone has a totem, save everyone.
     */
    private static void shareHealthDamage(ServerPlayerEntity sourcePlayer, float damage, ModConfig config) {
        ServerWorld sourceWorld = (ServerWorld) sourcePlayer.getEntityWorld();
        MinecraftServer server = sourceWorld.getServer();
        if (server == null) return;
        
        List<ServerPlayerEntity> allPlayers = new ArrayList<>(server.getPlayerManager().getPlayerList());
        
        // Remove the source player (they already took damage)
        allPlayers.removeIf(p -> p.getUuid().equals(sourcePlayer.getUuid()));
        
        if (allPlayers.isEmpty()) return;
        
        // Check if anyone would die from this damage
        List<ServerPlayerEntity> wouldDie = new ArrayList<>();
        for (ServerPlayerEntity player : allPlayers) {
            if (player.getHealth() - damage <= 0) {
                wouldDie.add(player);
            }
        }
        
        // Also check the source player
        if (sourcePlayer.getHealth() - damage <= 0) {
            wouldDie.add(sourcePlayer);
        }
        
        // If someone would die, check for totems
        if (!wouldDie.isEmpty() && config.sharedHealthTotemSavesAll) {
            ServerPlayerEntity totemHolder = findTotemHolder(server);
            
            if (totemHolder != null) {
                // Totem saves everyone!
                activateSharedTotem(totemHolder, server);
                return; // Don't apply damage to others
            }
        }
        
        // Apply damage to all other players
        for (ServerPlayerEntity player : allPlayers) {
            UUID playerId = player.getUuid();
            
            // Mark as processing GLOBALLY to prevent cross-handler recursion
            DamageShareTracker.markProcessing(playerId);
            try {
                ServerWorld world = (ServerWorld) player.getEntityWorld();
                player.damage(world, world.getDamageSources().magic(), damage);
            } finally {
                DamageShareTracker.clearProcessing(playerId);
            }
        }
    }
    
    /**
     * Find a player holding a Totem of Undying in their offhand.
     */
    private static ServerPlayerEntity findTotemHolder(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ItemStack offhand = player.getStackInHand(Hand.OFF_HAND);
            if (offhand.isOf(Items.TOTEM_OF_UNDYING)) {
                return player;
            }
        }
        return null;
    }
    
    /**
     * Activate the totem to save all players.
     * Consumes the totem and applies totem effects to everyone.
     */
    private static void activateSharedTotem(ServerPlayerEntity totemHolder, MinecraftServer server) {
        // Consume the totem
        ItemStack offhand = totemHolder.getStackInHand(Hand.OFF_HAND);
        if (offhand.isOf(Items.TOTEM_OF_UNDYING)) {
            offhand.decrement(1);
        }
        
        String holderName = totemHolder.getName().getString();
        
        // Apply totem effects to ALL players
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // Set health to 1 heart (vanilla totem behavior)
            if (player.getHealth() <= 0) {
                player.setHealth(1.0f);
            } else if (player.getHealth() < 2.0f) {
                player.setHealth(2.0f); // At least 1 heart
            }
            
            // Clear negative effects
            player.clearStatusEffects();
            
            // Apply totem buffs (same as vanilla)
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1)); // 45 sec Regen II
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1)); // 5 sec Absorption II
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0)); // 40 sec Fire Res
            
            // Play totem sound and particles
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
            
            // Spawn totem particles
            world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,
                player.getX(), player.getY() + 1, player.getZ(),
                30, 0.5, 1.0, 0.5, 0.2);
            
            // Notify player who saved them
            if (!player.getUuid().equals(totemHolder.getUuid())) {
                player.sendMessage(
                    Text.literal("⚡ ")
                        .append(Text.literal(holderName).formatted(Formatting.GOLD))
                        .append(Text.literal("'s totem saved everyone!"))
                        .formatted(Formatting.GREEN),
                    false
                );
            } else {
                player.sendMessage(
                    Text.literal("⚡ Your totem saved the entire team!").formatted(Formatting.GREEN, Formatting.BOLD),
                    false
                );
            }
        }
        
        SimpleDeathBans.LOGGER.info("Shared totem activated by {} - saved {} players", 
            holderName, server.getPlayerManager().getPlayerList().size());
    }
}
