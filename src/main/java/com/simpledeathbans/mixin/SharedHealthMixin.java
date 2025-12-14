package com.simpledeathbans.mixin;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
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
 * - TotemSavesAll ON, multiple totems: "Multiple people have saved everyone from the voids grasp!"
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
        method = "damage",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onDamageForSharedHealth(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        
        // Only for server-side players
        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }
        
        // Skip in single-player
        if (world.getServer().isSingleplayer()) {
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
        List<ServerPlayerEntity> allPlayers = new ArrayList<>(server.getPlayerManager().getPlayerList());
        
        if (allPlayers.size() <= 1) {
            return; // Only one player, vanilla behavior
        }
        
        // Find all totem holders
        List<ServerPlayerEntity> totemHolders = new ArrayList<>();
        for (ServerPlayerEntity p : allPlayers) {
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
                for (ServerPlayerEntity holder : totemHolders) {
                    consumeTotem(holder);
                }
                
                // Apply totem effects to ALL players
                for (ServerPlayerEntity p : allPlayers) {
                    applyTotemEffects(p, (ServerWorld) p.getEntityWorld());
                }
                
                // Server-wide notification
                Text serverMsg;
                if (totemHolders.size() == 1) {
                    // Single totem holder
                    String holderName = totemHolders.get(0).getName().getString();
                    serverMsg = Text.literal("§k><§r ")
                        .append(Text.literal(holderName).formatted(Formatting.GOLD))
                        .append(Text.literal("'s totem has saved everyone from the void!").formatted(Formatting.DARK_PURPLE))
                        .append(Text.literal(" §k><§r"));
                } else {
                    // Multiple totem holders
                    serverMsg = Text.literal("§k><§r ")
                        .append(Text.literal("Multiple people have saved everyone from the voids grasp!").formatted(Formatting.DARK_PURPLE))
                        .append(Text.literal(" §k><§r"));
                }
                
                server.getPlayerManager().broadcast(serverMsg, false);
                
                SimpleDeathBans.LOGGER.info("Shared Health: {} totem holder(s) saved all {} players", 
                    totemHolders.size(), allPlayers.size());
                
                cir.setReturnValue(false); // Cancel damage
                return;
            }
            
            // === SCENARIO: No one has totems - EVERYONE DIES ===
            // Send notification before death
            Text voidPullMsg = Text.literal("§k><§r ")
                .append(Text.literal("The void claims all...").formatted(Formatting.DARK_PURPLE))
                .append(Text.literal(" §k><§r"));
            
            for (ServerPlayerEntity p : allPlayers) {
                p.sendMessage(voidPullMsg, false);
            }
            
            // Kill all OTHER players (the original player will die from the damage)
            for (ServerPlayerEntity p : allPlayers) {
                if (!p.getUuid().equals(player.getUuid())) {
                    p.kill(world); // Death pact - everyone dies
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
                List<ServerPlayerEntity> willDie = new ArrayList<>();
                for (ServerPlayerEntity p : allPlayers) {
                    if (!hasTotemOfUndying(p)) {
                        willDie.add(p);
                    }
                }
                
                // Consume totems and apply effects to holders only
                for (ServerPlayerEntity holder : totemHolders) {
                    consumeTotem(holder);
                    applyTotemEffects(holder, (ServerWorld) holder.getEntityWorld());
                }
                
                // Server-wide notification
                Text serverMsg;
                if (totemHolders.size() == 1) {
                    // Single survivor
                    String survivorName = totemHolders.get(0).getName().getString();
                    serverMsg = Text.literal("§k><§r ")
                        .append(Text.literal(survivorName).formatted(Formatting.GOLD))
                        .append(Text.literal(" is the only one to survive from the void!").formatted(Formatting.DARK_PURPLE))
                        .append(Text.literal(" §k><§r"));
                } else {
                    // Multiple survivors
                    serverMsg = Text.literal("§k><§r ")
                        .append(Text.literal("Multiple people have survived the voids grasp!").formatted(Formatting.DARK_PURPLE))
                        .append(Text.literal(" §k><§r"));
                }
                
                server.getPlayerManager().broadcast(serverMsg, false);
                
                // Kill players without totems
                for (ServerPlayerEntity p : willDie) {
                    if (!p.getUuid().equals(player.getUuid())) {
                        p.kill(world);
                    }
                }
                
                SimpleDeathBans.LOGGER.info("Shared Health: {} totem holder(s) survived, {} players died", 
                    totemHolders.size(), willDie.size());
                
                // If the triggering player had a totem, cancel damage
                if (hasTotemOfUndying(player) || totemHolders.stream().anyMatch(h -> h.getUuid().equals(player.getUuid()))) {
                    cir.setReturnValue(false);
                }
                // Otherwise let original damage proceed (they'll die)
                return;
            }
            
            // === SCENARIO: No one has totems - EVERYONE DIES ===
            Text voidPullMsg = Text.literal("§k><§r ")
                .append(Text.literal("The void claims all...").formatted(Formatting.DARK_PURPLE))
                .append(Text.literal(" §k><§r"));
            
            for (ServerPlayerEntity p : allPlayers) {
                p.sendMessage(voidPullMsg, false);
            }
            
            // Kill all OTHER players
            for (ServerPlayerEntity p : allPlayers) {
                if (!p.getUuid().equals(player.getUuid())) {
                    p.kill(world);
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
    private boolean hasTotemOfUndying(ServerPlayerEntity player) {
        return player.getStackInHand(Hand.MAIN_HAND).isOf(Items.TOTEM_OF_UNDYING) ||
               player.getStackInHand(Hand.OFF_HAND).isOf(Items.TOTEM_OF_UNDYING);
    }
    
    /**
     * Consume totem from player's hand.
     */
    @Unique
    private void consumeTotem(ServerPlayerEntity player) {
        ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);
        ItemStack offHand = player.getStackInHand(Hand.OFF_HAND);
        
        if (mainHand.isOf(Items.TOTEM_OF_UNDYING)) {
            mainHand.decrement(1);
        } else if (offHand.isOf(Items.TOTEM_OF_UNDYING)) {
            offHand.decrement(1);
        }
    }
    
    /**
     * Apply totem effects to a player.
     */
    @Unique
    private void applyTotemEffects(ServerPlayerEntity player, ServerWorld world) {
        // Set health to 1 (totem effect)
        player.setHealth(1.0F);
        
        // Clear harmful effects
        player.clearStatusEffects();
        
        // Apply regeneration, absorption, and fire resistance like vanilla totem
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
        
        // Play totem animation and sound
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
        
        // Spawn totem particles
        spawnTotemParticles(world, player);
    }

    /**
     * Spawns Totem of Undying particles around a player.
     */
    @Unique
    private void spawnTotemParticles(ServerWorld world, ServerPlayerEntity player) {
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();
        
        // Spawn particles in a burst pattern
        for (int i = 0; i < 50; i++) {
            double offsetX = (world.getRandom().nextDouble() - 0.5) * 2.0;
            double offsetY = (world.getRandom().nextDouble() - 0.5) * 2.0;
            double offsetZ = (world.getRandom().nextDouble() - 0.5) * 2.0;
            
            world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,
                x + offsetX, y + offsetY, z + offsetZ,
                1, 0, 0, 0, 0.5);
        }
    }
}
