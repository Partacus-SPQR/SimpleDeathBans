package com.simpledeathbans.mixin;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.data.SoulLinkManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Mixin to intercept Totem of Undying usage and apply "Totem Saves Partner" feature.
 * 
 * Totem Save Logic (when soulLinkTotemSavesAll is enabled):
 * - If Player A (totem holder) takes lethal damage: A saved by totem, B also saved
 * - If Player B (no totem) takes lethal damage: Check if Partner A has totem → if yes, B is saved (no totem consumed from A)
 * - If both have totems: Each only consumes their own totem when THEY take lethal damage
 * 
 * When soulLinkTotemSavesAll is disabled:
 * - If Player A (totem) takes lethal damage: Only A is saved
 * - If Player B (no totem) takes lethal damage: B dies, soul link death pact may kill A too
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow public abstract ItemStack getStackInHand(Hand hand);

    /**
     * Intercept damage method to:
     * 1. When totem holder takes lethal damage: save their partner too (if enabled)
     * 2. When non-totem player takes lethal damage: check if partner has totem, save this player (if enabled)
     */
    @Inject(
        method = "damage",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onDamageHead(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        
        // Only for server-side players
        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }
        
        // Skip in single-player (no soul link mechanics)
        if (world.getServer().isSingleplayer()) {
            return;
        }
        
        // Check if this damage would be lethal
        float currentHealth = player.getHealth();
        if (amount < currentHealth) {
            return; // Not lethal, don't interfere
        }
        
        SimpleDeathBans mod = SimpleDeathBans.getInstance();
        if (mod == null) return;
        
        ModConfig config = mod.getConfig();
        SoulLinkManager soulLinkManager = mod.getSoulLinkManager();
        
        // Check if soul link is enabled
        if (config == null || !config.enableSoulLink || soulLinkManager == null) {
            return;
        }
        
        UUID playerId = player.getUuid();
        
        // Check if player has a totem
        boolean hasTotem = player.getStackInHand(Hand.MAIN_HAND).isOf(Items.TOTEM_OF_UNDYING) ||
                          player.getStackInHand(Hand.OFF_HAND).isOf(Items.TOTEM_OF_UNDYING);
        
        // CASE 1: Player HAS a totem and soulLinkTotemSavesAll is enabled
        // → Vanilla will save this player, we schedule saving their partner
        if (hasTotem && config.soulLinkTotemSavesAll) {
            soulLinkManager.getPartner(playerId).ifPresent(partnerUuid -> {
                ServerPlayerEntity partner = world.getServer().getPlayerManager().getPlayer(partnerUuid);
                
                if (partner != null && partner.isAlive()) {
                    // Schedule partner save after vanilla totem effect
                    world.getServer().execute(() -> {
                        // Verify totem was used (player survived with regeneration)
                        if (player.isAlive() && player.hasStatusEffect(StatusEffects.REGENERATION)) {
                            saveSoulLinkedPlayer(player, partner, world, false);
                        }
                    });
                }
            });
            return; // Let vanilla handle the totem for this player
        }
        
        // CASE 2: Player does NOT have a totem but soulLinkTotemSavesAll is enabled
        // → Check if partner has a totem → if yes, save this player
        if (!hasTotem && config.soulLinkTotemSavesAll) {
            soulLinkManager.getPartner(playerId).ifPresent(partnerUuid -> {
                ServerPlayerEntity partner = world.getServer().getPlayerManager().getPlayer(partnerUuid);
                
                if (partner != null && partner.isAlive()) {
                    boolean partnerHasTotem = partner.getStackInHand(Hand.MAIN_HAND).isOf(Items.TOTEM_OF_UNDYING) ||
                                               partner.getStackInHand(Hand.OFF_HAND).isOf(Items.TOTEM_OF_UNDYING);
                    
                    if (partnerHasTotem) {
                        // Partner has a totem! Consume it and save BOTH players
                        SimpleDeathBans.LOGGER.info("Soul link totem save: Partner {} has totem, consuming it to save {} from lethal damage", 
                            partner.getName().getString(), player.getName().getString());
                        
                        // CONSUME the partner's totem
                        ItemStack mainHand = partner.getStackInHand(Hand.MAIN_HAND);
                        ItemStack offHand = partner.getStackInHand(Hand.OFF_HAND);
                        if (mainHand.isOf(Items.TOTEM_OF_UNDYING)) {
                            mainHand.decrement(1);
                        } else if (offHand.isOf(Items.TOTEM_OF_UNDYING)) {
                            offHand.decrement(1);
                        }
                        
                        // Apply totem effects to BOTH players (since totem holder is also protected)
                        applyTotemEffects(player, world);
                        applyTotemEffects(partner, (ServerWorld) partner.getEntityWorld());
                        
                        // Notify both players
                        player.sendMessage(
                            Text.literal("Your partner's Totem of Undying saved you both from death!")
                                .formatted(Formatting.GOLD, Formatting.BOLD),
                            false
                        );
                        
                        partner.sendMessage(
                            Text.literal("Your Totem of Undying saved both you and ")
                                .append(Text.literal(player.getName().getString()).formatted(Formatting.GOLD))
                                .append(Text.literal(" from death!"))
                                .formatted(Formatting.DARK_PURPLE, Formatting.BOLD),
                            false
                        );
                        
                        // Cancel the damage
                        cir.setReturnValue(false);
                    }
                }
            });
        }
        
        // CASE 3: soulLinkTotemSavesAll is disabled OR player has no partner
        // → Let vanilla handle everything normally
    }
    
    /**
     * Apply totem effects without consuming a totem.
     * Used when partner's totem grants protection.
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
     * Save a soul-linked player when their partner used a totem.
     * @param totemUser The player who used the totem
     * @param savedPlayer The partner being saved
     * @param world The server world
     * @param consumeTotem Whether to consume a totem from the saved player
     */
    @Unique
    private void saveSoulLinkedPlayer(ServerPlayerEntity totemUser, ServerPlayerEntity savedPlayer, ServerWorld world, boolean consumeTotem) {
        String totemUserName = totemUser.getName().getString();
        String savedPlayerName = savedPlayer.getName().getString();
        
        // Apply regeneration, absorption, and fire resistance like vanilla totem
        savedPlayer.setHealth(Math.max(1.0F, savedPlayer.getHealth()));
        savedPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
        savedPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
        savedPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
        
        // Play totem animation and sound for saved player
        ServerWorld savedPlayerWorld = (ServerWorld) savedPlayer.getEntityWorld();
        savedPlayerWorld.playSound(null, savedPlayer.getX(), savedPlayer.getY(), savedPlayer.getZ(),
            SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
        
        // Spawn totem particles around saved player
        spawnTotemParticles(savedPlayerWorld, savedPlayer);
        
        // Notify both players
        savedPlayer.sendMessage(
            Text.literal("Your partner's Totem of Undying granted you protection!")
                .formatted(Formatting.GOLD, Formatting.BOLD),
            false
        );
        
        totemUser.sendMessage(
            Text.literal("Your Totem of Undying also protected ")
                .append(Text.literal(savedPlayerName).formatted(Formatting.GOLD))
                .append(Text.literal("!"))
                .formatted(Formatting.DARK_PURPLE),
            false
        );
        
        SimpleDeathBans.LOGGER.info("Soul link totem save: {} granted protection to partner {}", 
            totemUserName, savedPlayerName);
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
