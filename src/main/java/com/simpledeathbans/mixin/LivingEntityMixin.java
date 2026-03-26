package com.simpledeathbans.mixin;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.damage.SoulSeverDamageSource;
import com.simpledeathbans.data.SoulLinkManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Mixin to intercept lethal damage and handle Soul Link totem mechanics.
 * 
 * SOUL LINK DEATH FLOW:
 * 
 * When soulLinkTotemSavesPartner = ENABLED:
 * - Both have totems: Both totems consumed, both live
 * - One has totem: That totem saves BOTH (one totem consumed)
 * - Neither has totem: Both die (handled by DeathEventHandler)
 * 
 * When soulLinkTotemSavesPartner = DISABLED:
 * - Both have totems: Both totems consumed, both live
 * - One has totem: Only totem holder lives, other dies
 * - Neither has totem: Both die
 * 
 * IMPORTANT: This mixin fires for BOTH players during death pact:
 * 1. First player takes lethal damage → mixin fires
 * 2. If that player dies, DeathEventHandler sends soul sever to partner
 * 3. Partner receives soul sever → mixin fires AGAIN for partner
 * 
 * So when TotemSavesPartner=OFF:
 * - Player without totem dies (no totem to save them)
 * - Partner with totem receives soul sever, their totem saves them
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow public abstract ItemStack getItemInHand(InteractionHand hand);

    /**
     * Intercept damage method to handle Soul Link totem scenarios.
     */
    @Inject(
        method = "hurtServer",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onDamageForSoulLink(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        
        // Only for server-side players
        if (!(self instanceof ServerPlayer player)) {
            return;
        }
        
        // SINGLE-PLAYER INVULNERABILITY: If player is frozen from a ban, make them invulnerable
        // This prevents mobs from killing them while they wait out their ban timer
        if (world.getServer().isSingleplayer()) {
            // Check if the mod is disabled for single-player
            SimpleDeathBans mod = SimpleDeathBans.getInstance();
            if (mod != null && mod.getConfig() != null && !mod.getConfig().singlePlayerEnabled) {
                return; // Mod disabled in single-player, let damage through
            }
            
            // Check ban status on server side using BanDataManager
            if (mod != null && mod.getBanDataManager() != null) {
                if (mod.getBanDataManager().isBanned(player.getUUID())) {
                    // Player is banned/frozen - make them invulnerable
                    SimpleDeathBans.LOGGER.debug("Canceling damage to frozen single-player user: {}", player.getName().getString());
                    cir.setReturnValue(false);
                    return;
                }
            }
            return; // Skip other soul link processing in single-player
        }
        
        // Check if this damage would be lethal
        float currentHealth = player.getHealth();
        if (amount < currentHealth) {
            return; // Not lethal, let vanilla handle it
        }
        
        SimpleDeathBans mod = SimpleDeathBans.getInstance();
        if (mod == null) return;
        
        ModConfig config = mod.getConfig();
        SoulLinkManager soulLinkManager = mod.getSoulLinkManager();
        
        // Check if soul link is enabled
        if (config == null || !config.enableSoulLink || soulLinkManager == null) {
            return;
        }
        
        // MUTUAL EXCLUSIVITY: If Shared Health is enabled, Soul Link is disabled
        // (SharedHealthMixin handles lethal damage for the server-wide pool)
        if (config.enableSharedHealth) {
            return;
        }
        
        UUID playerId = player.getUUID();
        
        // Check if player has a soul link partner
        if (!soulLinkManager.hasPartner(playerId)) {
            return; // No partner, vanilla behavior
        }
        
        // SPECIAL CASE: Soul Sever damage (death pact from partner dying)
        // When TotemSavesPartner=OFF and partner died, this player receives soul sever damage.
        // If THIS player has a totem, they should use it to save themselves.
        // Use both the flag-based check (reliable) and source-based check (fallback)
        if (SoulSeverDamageSource.isSoulSeverTarget(playerId) || SoulSeverDamageSource.isSoulSever(source)) {
            boolean playerHasTotem = hasTotemOfUndying(player);
            
            SimpleDeathBans.LOGGER.info("Soul Link: {} received soul sever damage, hasTotem={}", 
                player.getName().getString(), playerHasTotem);
            
            if (playerHasTotem) {
                // Use totem to save self from soul sever (partner is already dead)
                consumeTotem(player);
                applyTotemEffects(player, world);
                
                Component savedMsg = Component.literal("§k><§r ")
                    .append(Component.literal("Your totem has saved you from the void!").withStyle(ChatFormatting.DARK_PURPLE))
                    .append(Component.literal(" §k><§r"));
                player.sendSystemMessage(savedMsg);
                
                // Server-wide notification
                String playerName = player.getName().getString();
                Component serverMsg = Component.literal("§k><§r ")
                    .append(Component.literal(playerName).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" is the only one to survive from the void!").withStyle(ChatFormatting.DARK_PURPLE))
                    .append(Component.literal(" §k><§r"));
                world.getServer().getPlayerList().broadcastSystemMessage(serverMsg, false);
                
                SimpleDeathBans.LOGGER.info("Soul Link: {} survived soul sever with totem (partner died)", playerName);
                
                cir.setReturnValue(false); // Cancel damage
                return;
            }
            // No totem - let soul sever kill them
            return;
        }
        
        ServerPlayer partner = soulLinkManager.getPartner(playerId)
            .map(uuid -> world.getServer().getPlayerList().getPlayer(uuid))
            .orElse(null);
        
        if (partner == null || !partner.isAlive()) {
            return; // Partner offline or dead, vanilla behavior
        }
        
        // Check totem status for both players
        boolean playerHasTotem = hasTotemOfUndying(player);
        boolean partnerHasTotem = hasTotemOfUndying(partner);
        
        String playerName = player.getName().getString();
        String partnerName = partner.getName().getString();
        
        // SCENARIO 1: BOTH have totems
        if (playerHasTotem && partnerHasTotem) {
            // Both consume their own totems, both live
            consumeTotem(player);
            consumeTotem(partner);
            
            applyTotemEffects(player, world);
            applyTotemEffects(partner, (ServerLevel) partner.level());
            
            // Notifications (obfuscation + purple)
            Component bothSavedMsg = Component.literal("§k><§r ")
                .append(Component.literal("You have both avoided the pull from the void!").withStyle(ChatFormatting.DARK_PURPLE))
                .append(Component.literal(" §k><§r"));
            
            player.sendSystemMessage(bothSavedMsg);
            partner.sendSystemMessage(bothSavedMsg);
            
            // Server-wide notification
            Component serverMsg = Component.literal("§k><§r ")
                .append(Component.literal(playerName).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" and ").withStyle(ChatFormatting.DARK_PURPLE))
                .append(Component.literal(partnerName).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" have avoided the grasp of the void!").withStyle(ChatFormatting.DARK_PURPLE))
                .append(Component.literal(" §k><§r"));
            
            world.getServer().getPlayerList().broadcastSystemMessage(serverMsg, false);
            
            SimpleDeathBans.LOGGER.info("Soul Link: Both {} and {} used totems to survive", playerName, partnerName);
            
            cir.setReturnValue(false); // Cancel damage
            return;
        }
        
        // SCENARIO 2: Only THIS PLAYER has a totem
        if (playerHasTotem && !partnerHasTotem) {
            consumeTotem(player);
            applyTotemEffects(player, world);
            
            if (config.soulLinkTotemSavesPartner) {
                // Totem saves BOTH
                applyTotemEffects(partner, (ServerLevel) partner.level());
                
                // Player notification (obfuscation + purple)
                Component playerMsg = Component.literal("§k><§r ")
                    .append(Component.literal("Your totem has saved both yourself and ").withStyle(ChatFormatting.DARK_PURPLE))
                    .append(Component.literal(partnerName).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" from the void!").withStyle(ChatFormatting.DARK_PURPLE))
                    .append(Component.literal(" §k><§r"));
                player.sendSystemMessage(playerMsg);
                
                // Partner notification
                Component partnerMsg = Component.literal("§k><§r ")
                    .append(Component.literal(playerName).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" has saved you both from the void with their totem!").withStyle(ChatFormatting.DARK_PURPLE))
                    .append(Component.literal(" §k><§r"));
                partner.sendSystemMessage(partnerMsg);
                
                // Server-wide notification
                Component serverMsg = Component.literal("§k><§r ")
                    .append(Component.literal(playerName).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" has saved ").withStyle(ChatFormatting.DARK_PURPLE))
                    .append(Component.literal(partnerName).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" from the grasp of the void!").withStyle(ChatFormatting.DARK_PURPLE))
                    .append(Component.literal(" §k><§r"));
                world.getServer().getPlayerList().broadcastSystemMessage(serverMsg, false);
                
                SimpleDeathBans.LOGGER.info("Soul Link: {} totem saved both players (TotemSavesPartner=ON)", playerName);
            } else {
                // Totem saves ONLY the holder, partner MUST die via death pact
                Component playerMsg = Component.literal("§k><§r ")
                    .append(Component.literal("Your totem has saved you from the void!").withStyle(ChatFormatting.DARK_PURPLE))
                    .append(Component.literal(" §k><§r"));
                player.sendSystemMessage(playerMsg);
                
                // Server-wide notification - this player survived alone
                Component serverMsg = Component.literal("§k><§r ")
                    .append(Component.literal(playerName).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" is the only one to survive from the void!").withStyle(ChatFormatting.DARK_PURPLE))
                    .append(Component.literal(" §k><§r"));
                world.getServer().getPlayerList().broadcastSystemMessage(serverMsg, false);
                
                // KILL THE PARTNER - they have no totem and their soulbound partner took lethal damage
                ServerLevel partnerWorld = (ServerLevel) partner.level();
                UUID partnerUuid = partner.getUUID();
                SoulSeverDamageSource.markSoulSeverTarget(partnerUuid);
                partnerWorld.getServer().execute(() -> {
                    try {
                        DamageSource soulSeverDamage = SoulSeverDamageSource.create(partnerWorld, player);
                        partner.hurtServer(partnerWorld, soulSeverDamage, Float.MAX_VALUE);
                    } finally {
                        SoulSeverDamageSource.clearSoulSeverTarget(partnerUuid);
                    }
                });
                
                SimpleDeathBans.LOGGER.info("Soul Link: {} totem saved only themselves (TotemSavesPartner=OFF), killing partner {}", 
                    playerName, partnerName);
            }
            
            cir.setReturnValue(false); // Cancel damage for this player
            return;
        }
        
        // SCENARIO 3: Only PARTNER has a totem (this player has none)
        if (!playerHasTotem && partnerHasTotem) {
            if (config.soulLinkTotemSavesPartner) {
                // Partner's totem saves BOTH
                consumeTotem(partner);
                applyTotemEffects(player, world);
                applyTotemEffects(partner, (ServerLevel) partner.level());
                
                // Player notification (obfuscation + purple)
                Component playerMsg = Component.literal("§k><§r ")
                    .append(Component.literal(partnerName).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" has saved you both from the void with their totem!").withStyle(ChatFormatting.DARK_PURPLE))
                    .append(Component.literal(" §k><§r"));
                player.sendSystemMessage(playerMsg);
                
                // Partner notification
                Component partnerMsg = Component.literal("§k><§r ")
                    .append(Component.literal("Your totem has saved both yourself and ").withStyle(ChatFormatting.DARK_PURPLE))
                    .append(Component.literal(playerName).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" from the void!").withStyle(ChatFormatting.DARK_PURPLE))
                    .append(Component.literal(" §k><§r"));
                partner.sendSystemMessage(partnerMsg);
                
                // Server-wide notification
                Component serverMsg = Component.literal("§k><§r ")
                    .append(Component.literal(partnerName).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" has saved ").withStyle(ChatFormatting.DARK_PURPLE))
                    .append(Component.literal(playerName).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" from the grasp of the void!").withStyle(ChatFormatting.DARK_PURPLE))
                    .append(Component.literal(" §k><§r"));
                world.getServer().getPlayerList().broadcastSystemMessage(serverMsg, false);
                
                SimpleDeathBans.LOGGER.info("Soul Link: {} totem saved both players (TotemSavesPartner=ON)", partnerName);
                
                cir.setReturnValue(false); // Cancel damage
                return;
            }
            // If TotemSavesPartner=OFF, this player dies, vanilla handles it
            // Partner keeps their totem, but will die from death pact anyway
        }
        
        // SCENARIO 4: NEITHER has a totem
        // Both will die - send special notification before death
        if (!playerHasTotem && !partnerHasTotem) {
            // Send "pull from the void" notification to both
            Component voidPullMsg = Component.literal("§k><§r ")
                .append(Component.literal("You both feel the pull from the void!").withStyle(ChatFormatting.DARK_PURPLE))
                .append(Component.literal(" §k><§r"));
            
            player.sendSystemMessage(voidPullMsg);
            partner.sendSystemMessage(voidPullMsg);
            
            SimpleDeathBans.LOGGER.info("Soul Link: Neither {} nor {} have totems - both will die", playerName, partnerName);
            
            // Let vanilla handle the death, DeathEventHandler will do death pact
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
