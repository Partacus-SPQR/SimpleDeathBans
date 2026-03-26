package com.simpledeathbans.item;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.data.SoulLinkManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/**
 * The Soul Link Totem item used for manual soul linking and Soul Compass feature.
 * 
 * Usage:
 * - Hold this item and shift+right-click another player to request a soul link
 * - The target must also be holding a Soul Link Totem and shift+right-click you back
 * - Once both players consent, their souls are bound together
 * - Right-click (not on a player) while linked to use Soul Compass feature:
 *   - Shows direction and approximate distance to partner
 *   - Limited uses per totem (default: 10)
 *   - Cooldown between uses (default: 10 minutes)
 *   - Both players are notified
 * 
 * Recipe (3x3 shaped):
 * - Row 1: Amethyst Shard | Eye of Ender | Amethyst Shard
 * - Row 2: Eye of Ender | Totem of Undying | Eye of Ender
 * - Row 3: Amethyst Shard | Eye of Ender | Amethyst Shard
 */
public class SoulLinkTotemItem extends Item {
    
    private static final String COMPASS_USES_KEY = "compass_uses";
    
    public SoulLinkTotemItem(Item.Properties settings) {
        super(settings);
    }
    
    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Give it the enchantment glint
    }
    
    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (world.isClientSide()) {
            return InteractionResult.PASS;
        }
        
        // Only handle main hand to avoid double-triggering
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        
        if (!(user instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }
        
        SimpleDeathBans mod = SimpleDeathBans.getInstance();
        if (mod == null) {
            return InteractionResult.PASS;
        }
        
        ModConfig config = mod.getConfig();
        SoulLinkManager soulLinkManager = mod.getSoulLinkManager();
        
        if (config == null || !config.enableSoulLink || soulLinkManager == null) {
            return InteractionResult.PASS;
        }
        
        UUID playerId = player.getUUID();
        
        // Check if player has a soul link
        Optional<UUID> partnerOpt = soulLinkManager.getPartner(playerId);
        if (partnerOpt.isEmpty()) {
            // No partner - this is just a regular use, pass to let other handlers work
            return InteractionResult.PASS;
        }
        
        // Player has a partner - this is the Soul Compass feature!
        return useSoulCompass(player, hand, soulLinkManager, config, partnerOpt.get());
    }
    
    private InteractionResult useSoulCompass(ServerPlayer player, InteractionHand hand, 
                                         SoulLinkManager soulLinkManager, ModConfig config, UUID partnerId) {
        UUID playerId = player.getUUID();
        ItemStack stack = player.getItemInHand(hand);
        
        // Check cooldown
        if (soulLinkManager.isOnCompassCooldown(playerId)) {
            long remaining = soulLinkManager.getCompassCooldownRemaining(playerId);
            player.sendSystemMessage(Component.literal("§5✦ §7Your bond pulses faintly... wait §c" + remaining + " minutes §5✦"));
            return InteractionResult.FAIL;
        }
        
        // Check remaining uses
        int usesRemaining = getCompassUses(stack, config);
        if (usesRemaining <= 0) {
            player.sendSystemMessage(Component.literal("§5✦ §7This totem's compass power has faded... §5✦"));
            return InteractionResult.FAIL;
        }
        
        // Get partner
        ServerLevel world = (ServerLevel) player.level();
        ServerPlayer partner = world.getServer().getPlayerList().getPlayer(partnerId);
        
        if (partner == null) {
            player.sendSystemMessage(Component.literal("§5✦ §7Your partner's soul is not in this realm... §5✦"));
            return InteractionResult.PASS; // Don't consume use when partner is offline
        }
        
        String partnerName = partner.getName().getString();
        String playerName = player.getName().getString();
        
        // Check if same dimension
        ResourceKey<Level> playerDim = player.level().dimension();
        ResourceKey<Level> partnerDim = partner.level().dimension();
        
        if (!playerDim.equals(partnerDim)) {
            // Different dimensions
            String dimName = getDimensionDisplayName(partnerDim);
            
            // Notify player
            player.sendSystemMessage(Component.literal("§5✦ §dYour bond pulses... §7" + partnerName + " §dis in the §c" + dimName + " §5✦"));
            
            // Notify partner
            partner.sendSystemMessage(
                Component.literal("§5✦ §dYour partner seeks you... §7" + playerName + " §dis in the §c" + getDimensionDisplayName(playerDim) + " §5✦")
            );
        } else {
            // Same dimension - calculate direction and distance
            BlockPos playerPos = player.blockPosition();
            BlockPos partnerPos = partner.blockPosition();
            
            double dx = partnerPos.getX() - playerPos.getX();
            double dz = partnerPos.getZ() - playerPos.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            
            // Round to nearest 50
            int approxDistance = ((int) Math.round(distance / 50.0)) * 50;
            if (approxDistance < 50) approxDistance = 50; // Minimum display
            
            // Get direction
            String direction = getDirection(dx, dz);
            
            // Notify player
            player.sendSystemMessage(Component.literal("§5✦ §dYour bond pulses... §7" + partnerName + " §dis ~" + approxDistance + " blocks to the §e" + direction + " §5✦"));
            
            // Notify partner with reverse direction/distance
            String reverseDirection = getDirection(-dx, -dz);
            partner.sendSystemMessage(Component.literal("§5✦ §dYour partner seeks you... §7" + playerName + " §dis ~" + approxDistance + " blocks to the §e" + reverseDirection + " §5✦"));
        }
        
        // Play sound for both
        ServerLevel serverWorld = (ServerLevel) player.level();
        serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.0f);
        
        ServerLevel partnerWorld = (ServerLevel) partner.level();
        partnerWorld.playSound(null, partner.getX(), partner.getY(), partner.getZ(),
            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.0f);
        
        // Consume use and set cooldown
        int newUses = usesRemaining - 1;
        setCompassUses(stack, newUses);
        soulLinkManager.setCompassCooldown(playerId, config.soulLinkCompassCooldownMinutes);
        
        // Check if totem broke
        if (newUses <= 0) {
            stack.shrink(1);
            player.sendSystemMessage(Component.literal("§5✦ §4The totem's compass power has been exhausted! §5✦"));
            
            // Play break sound
            serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        
        return InteractionResult.SUCCESS;
    }
    
    private int getCompassUses(ItemStack stack, ModConfig config) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag nbt = customData.copyTag();
            if (nbt.contains(COMPASS_USES_KEY)) {
                return nbt.getInt(COMPASS_USES_KEY).orElse(config.soulLinkCompassMaxUses);
            }
        }
        // First use - return max uses
        return config.soulLinkCompassMaxUses;
    }
    
    private void setCompassUses(ItemStack stack, int uses) {
        CompoundTag nbt = new CompoundTag();
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        if (existing != null) {
            nbt = existing.copyTag();
        }
        nbt.putInt(COMPASS_USES_KEY, uses);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }
    
    private String getDirection(double dx, double dz) {
        // Calculate angle in degrees (0 = South, 90 = West, 180/-180 = North, -90 = East)
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        
        // Normalize to 0-360
        if (angle < 0) angle += 360;
        
        // Convert to compass direction
        if (angle >= 337.5 || angle < 22.5) return "South";
        if (angle >= 22.5 && angle < 67.5) return "Southwest";
        if (angle >= 67.5 && angle < 112.5) return "West";
        if (angle >= 112.5 && angle < 157.5) return "Northwest";
        if (angle >= 157.5 && angle < 202.5) return "North";
        if (angle >= 202.5 && angle < 247.5) return "Northeast";
        if (angle >= 247.5 && angle < 292.5) return "East";
        if (angle >= 292.5 && angle < 337.5) return "Southeast";
        return "Unknown";
    }
    
    private String getDimensionDisplayName(ResourceKey<Level> dim) {
        //? if >=1.21.11 {
        String path = dim.identifier().getPath();
        //?} else {
        /*String path = dim.location().getPath();*/
        //?}
        return switch (path) {
            case "overworld" -> "Overworld";
            case "the_nether" -> "Nether";
            case "the_end" -> "End";
            default -> path.substring(0, 1).toUpperCase() + path.substring(1).replace("_", " ");
        };
    }
}
