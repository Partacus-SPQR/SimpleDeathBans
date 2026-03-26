package com.simpledeathbans.mixin;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.util.DamageShareTracker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin to intercept lethal damage and handle Shared Health Death Pact mechanics.
 * 
 * SHARED HEALTH DEATH FLOW:
 * 
 * When sharedHealthTotemSavesAll = ENABLED:
 * - Anyone has totem: ALL players saved, totem(s) consumed
 * - No one has totem: ALL players die
 * 
 * When sharedHealthTotemSavesAll = DISABLED:
 * - Players WITH totems: Survive (totem consumed)
 * - Players WITHOUT totems: Die
 * 
 * NOTIFICATIONS (all obfuscated + purple):
 * - TotemSavesAll ON, 1 totem: "[Player]'s totem has saved everyone from the void!"
 * - TotemSavesAll ON, multiple totems: "Multiple people have saved others from the void!"
 * - TotemSavesAll OFF, 1 survivor: "[Player] is the only one to survive from the void!"
 * - TotemSavesAll OFF, multiple survivors: "Multiple people have survived the voids grasp!"
 * - No totems: All die with default ban logic
 */
@Mixin(LivingEntity.class)
public abstract class SharedHealthMixin {

    /**
     * Intercept damage method to handle Shared Health Death Pact scenarios.
     * Priority lower than LivingEntityMixin to not conflict with Soul Link.
     */
    @Inject(
        method = "hurtServer",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onDamageForSharedHealth(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        
        // Only for server-side players
        if (!(self instanceof ServerPlayer player)) {
            return;
        }
        
        // Skip in single-player
        if (world.getServer().isSingleplayer()) {
            return;
        }
        
        // CRITICAL: Prevent recursion - check if we're already processing this player
        if (DamageShareTracker.isProcessing(player.getUUID())) {
            return;
        }
        
        SimpleDeathBans mod = SimpleDeathBans.getInstance();
        if (mod == null) return;
        
        ModConfig config = mod.getConfig();
        
        // Check if shared health is enabled
        if (config == null || !config.enableSharedHealth) {
            return;
        }
        
        // Check if this damage would be lethal
        float currentHealth = player.getHealth();
        if (amount < currentHealth) {
            return; // Not lethal, let SharedHealthHandler handle non-lethal sharing
        }
        
        // === LETHAL DAMAGE DETECTED - DEATH PACT ACTIVATED ===
        MinecraftServer server = world.getServer();
        List<ServerPlayer> allPlayers = new ArrayList<>(server.getPlayerList().getPlayers());
        
        if (allPlayers.size() <= 1) {
            return; // Only one player, vanilla behavior
        }
        
        // Find all totem holders
        List<ServerPlayer> totemHolders = new ArrayList<>();
        for (ServerPlayer p : allPlayers) {
            if (hasTotemOfUndying(p)) {
                totemHolders.add(p);
            }
        }
        
        String triggerPlayerName = player.getName().getString();
        
        // === SCENARIO HANDLING ===
        
        if (config.sharedHealthTotemSavesAll) {
            // TotemSavesAll = ON
            
            if (!totemHolders.isEmpty()) {
                // === SCENARIO: At least one person has a totem - EVERYONE SAVED ===
                
                // Consume ALL totems from holders
                for (ServerPlayer holder : totemHolders) {
                    consumeTotem(holder);
                }
                
                // Apply totem effects to ALL players
                for (ServerPlayer p : allPlayers) {
                    applyTotemEffects(p, (ServerLevel) p.level());
                }
                
                // Server-wide notification
                Component serverMsg;
                if (totemHolders.size() == 1) {
                    // Single totem holder
                    String holderName = totemHolders.get(0).getName().getString();
                    serverMsg = Component.literal("§k><§r ")
                        .append(Component.literal(holderName).withStyle(ChatFormatting.GOLD))
                        .append(Component.literal("'s totem has saved everyone from the void!").withStyle(ChatFormatting.DARK_PURPLE))
                        .append(Component.literal(" §k><§r"));
                } else {
                    // Multiple totem holders
                    serverMsg = Component.literal("§k><§r ")
                        .append(Component.literal("Multiple people have saved others from the void!").withStyle(ChatFormatting.DARK_PURPLE))
                        .append(Component.literal(" §k><§r"));
                }
                
                server.getPlayerList().broadcastSystemMessage(serverMsg, false);
                
                SimpleDeathBans.LOGGER.info("Shared Health: {} totem holder(s) saved all {} players", 
                    totemHolders.size(), allPlayers.size());
                
                cir.setReturnValue(false); // Cancel damage
                return;
            }
            
            // === SCENARIO: No one has totems - EVERYONE DIES ===
            // Send notification before death
            Component voidPullMsg = Component.literal("§k><§r ")
                .append(Component.literal("The void claims all...").withStyle(ChatFormatting.DARK_PURPLE))
                .append(Component.literal(" §k><§r"));
            
            for (ServerPlayer p : allPlayers) {
                p.sendSystemMessage(voidPullMsg);
            }
            
            // Mark all players as processing to prevent recursion
            for (ServerPlayer p : allPlayers) {
                DamageShareTracker.markProcessing(p.getUUID());
            }
            
            try {
                // Kill all OTHER players (the original player will die from the damage)
                for (ServerPlayer p : allPlayers) {
                    if (!p.getUUID().equals(player.getUUID())) {
                        p.kill(world); // Death pact - everyone dies
                    }
                }
            } finally {
                // Clear processing flags
                for (ServerPlayer p : allPlayers) {
                    DamageShareTracker.clearProcessing(p.getUUID());
                }
            }
            
            SimpleDeathBans.LOGGER.info("Shared Health Death Pact: {} triggered, all {} players die (no totems)", 
                triggerPlayerName, allPlayers.size());
            
            // Let the original damage proceed for the triggering player
            return;
            
        } else {
            // TotemSavesAll = OFF
            
            if (!totemHolders.isEmpty()) {
                // === SCENARIO: Some have totems - ONLY TOTEM HOLDERS SURVIVE ===
                
                // Players WITHOUT totems will die
                List<ServerPlayer> willDie = new ArrayList<>();
                for (ServerPlayer p : allPlayers) {
                    if (!hasTotemOfUndying(p)) {
                        willDie.add(p);
                    }
                }
                
                // Consume totems and apply effects to holders only
                for (ServerPlayer holder : totemHolders) {
                    consumeTotem(holder);
                    applyTotemEffects(holder, (ServerLevel) holder.level());
                }
                
                // Server-wide notification
                Component serverMsg;
                if (totemHolders.size() == 1) {
                    // Single survivor
                    String survivorName = totemHolders.get(0).getName().getString();
                    serverMsg = Component.literal("§k><§r ")
                        .append(Component.literal(survivorName).withStyle(ChatFormatting.GOLD))
                        .append(Component.literal(" is the only one to survive from the void!").withStyle(ChatFormatting.DARK_PURPLE))
                        .append(Component.literal(" §k><§r"));
                } else {
                    // Multiple survivors
                    serverMsg = Component.literal("§k><§r ")
                        .append(Component.literal("Multiple people have survived the voids grasp!").withStyle(ChatFormatting.DARK_PURPLE))
                        .append(Component.literal(" §k><§r"));
                }
                
                server.getPlayerList().broadcastSystemMessage(serverMsg, false);
                
                // Mark players as processing to prevent recursion
                for (ServerPlayer p : willDie) {
                    DamageShareTracker.markProcessing(p.getUUID());
                }
                
                try {
                    // Kill players without totems
                    for (ServerPlayer p : willDie) {
                        if (!p.getUUID().equals(player.getUUID())) {
                            p.kill(world);
                        }
                    }
                } finally {
                    // Clear processing flags
                    for (ServerPlayer p : willDie) {
                        DamageShareTracker.clearProcessing(p.getUUID());
                    }
                }
                
                SimpleDeathBans.LOGGER.info("Shared Health: {} totem holder(s) survived, {} players died", 
                    totemHolders.size(), willDie.size());
                
                // If the triggering player had a totem, cancel damage
                if (hasTotemOfUndying(player) || totemHolders.stream().anyMatch(h -> h.getUUID().equals(player.getUUID()))) {
                    cir.setReturnValue(false);
                }
                // Otherwise let original damage proceed (they'll die)
                return;
            }
            
            // === SCENARIO: No one has totems - EVERYONE DIES ===
            Component voidPullMsg = Component.literal("§k><§r ")
                .append(Component.literal("The void claims all...").withStyle(ChatFormatting.DARK_PURPLE))
                .append(Component.literal(" §k><§r"));
            
            for (ServerPlayer p : allPlayers) {
                p.sendSystemMessage(voidPullMsg);
            }
            
            // Mark all players as processing to prevent recursion
            for (ServerPlayer p : allPlayers) {
                DamageShareTracker.markProcessing(p.getUUID());
            }
            
            try {
                // Kill all OTHER players
                for (ServerPlayer p : allPlayers) {
                    if (!p.getUUID().equals(player.getUUID())) {
                        p.kill(world);
                    }
                }
            } finally {
                // Clear processing flags
                for (ServerPlayer p : allPlayers) {
                    DamageShareTracker.clearProcessing(p.getUUID());
                }
            }
            
            SimpleDeathBans.LOGGER.info("Shared Health Death Pact: {} triggered, all {} players die (no totems, TotemSavesAll=OFF)", 
                triggerPlayerName, allPlayers.size());
        }
    }
    
    /**
     * Check if player has a Totem of Undying in either hand.
     */
    @Unique
    private boolean hasTotemOfUndying(ServerPlayer player) {
        return player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.TOTEM_OF_UNDYING) ||
               player.getItemInHand(InteractionHand.OFF_HAND).is(Items.TOTEM_OF_UNDYING);
    }
    
    /**
     * Consume totem from player's hand.
     */
    @Unique
    private void consumeTotem(ServerPlayer player) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        
        if (mainHand.is(Items.TOTEM_OF_UNDYING)) {
            mainHand.shrink(1);
        } else if (offHand.is(Items.TOTEM_OF_UNDYING)) {
            offHand.shrink(1);
        }
    }
    
    /**
     * Apply totem effects to a player.
     */
    @Unique
    private void applyTotemEffects(ServerPlayer player, ServerLevel world) {
        // Set health to 1 (totem effect)
        player.setHealth(1.0F);
        
        // Clear harmful effects
        player.removeAllEffects();
        
        // Apply regeneration, absorption, and fire resistance like vanilla totem
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        
        // Play totem animation and sound
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        
        // Spawn totem particles
        spawnTotemParticles(world, player);
    }

    /**
     * Spawns Totem of Undying particles around a player.
     */
    @Unique
    private void spawnTotemParticles(ServerLevel world, ServerPlayer player) {
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();
        
        // Spawn particles in a burst pattern
        for (int i = 0; i < 50; i++) {
            double offsetX = (world.getRandom().nextDouble() - 0.5) * 2.0;
            double offsetY = (world.getRandom().nextDouble() - 0.5) * 2.0;
            double offsetZ = (world.getRandom().nextDouble() - 0.5) * 2.0;
            
            world.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                x + offsetX, y + offsetY, z + offsetZ,
                1, 0, 0, 0, 0.5);
        }
    }
}
