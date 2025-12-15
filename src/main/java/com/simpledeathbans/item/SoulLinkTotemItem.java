package com.simpledeathbans.item;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.data.SoulLinkManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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
    
    public SoulLinkTotemItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        return true; // Give it the enchantment glint
    }
    
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            return ActionResult.PASS;
        }
        
        // Only handle main hand to avoid double-triggering
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        
        if (!(user instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }
        
        SimpleDeathBans mod = SimpleDeathBans.getInstance();
        if (mod == null) {
            return ActionResult.PASS;
        }
        
        ModConfig config = mod.getConfig();
        SoulLinkManager soulLinkManager = mod.getSoulLinkManager();
        
        if (config == null || !config.enableSoulLink || soulLinkManager == null) {
            return ActionResult.PASS;
        }
        
        UUID playerId = player.getUuid();
        
        // Check if player has a soul link
        Optional<UUID> partnerOpt = soulLinkManager.getPartner(playerId);
        if (partnerOpt.isEmpty()) {
            // No partner - this is just a regular use, pass to let other handlers work
            return ActionResult.PASS;
        }
        
        // Player has a partner - this is the Soul Compass feature!
        return useSoulCompass(player, hand, soulLinkManager, config, partnerOpt.get());
    }
    
    private ActionResult useSoulCompass(ServerPlayerEntity player, Hand hand, 
                                         SoulLinkManager soulLinkManager, ModConfig config, UUID partnerId) {
        UUID playerId = player.getUuid();
        ItemStack stack = player.getStackInHand(hand);
        
        // Check cooldown
        if (soulLinkManager.isOnCompassCooldown(playerId)) {
            long remaining = soulLinkManager.getCompassCooldownRemaining(playerId);
            player.sendMessage(
                Text.literal("§5✦ §7Your bond pulses faintly... wait §c" + remaining + " minutes §5✦"),
                false
            );
            return ActionResult.FAIL;
        }
        
        // Check remaining uses
        int usesRemaining = getCompassUses(stack, config);
        if (usesRemaining <= 0) {
            player.sendMessage(
                Text.literal("§5✦ §7This totem's compass power has faded... §5✦"),
                false
            );
            return ActionResult.FAIL;
        }
        
        // Get partner
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        ServerPlayerEntity partner = world.getServer().getPlayerManager().getPlayer(partnerId);
        
        if (partner == null) {
            player.sendMessage(
                Text.literal("§5✦ §7Your partner's soul is not in this realm... §5✦"),
                false
            );
            return ActionResult.PASS; // Don't consume use when partner is offline
        }
        
        String partnerName = partner.getName().getString();
        String playerName = player.getName().getString();
        
        // Check if same dimension
        RegistryKey<World> playerDim = player.getEntityWorld().getRegistryKey();
        RegistryKey<World> partnerDim = partner.getEntityWorld().getRegistryKey();
        
        if (!playerDim.equals(partnerDim)) {
            // Different dimensions
            String dimName = getDimensionDisplayName(partnerDim);
            
            // Notify player
            player.sendMessage(
                Text.literal("§5✦ §dYour bond pulses... §7" + partnerName + " §dis in the §c" + dimName + " §5✦"),
                false
            );
            
            // Notify partner
            partner.sendMessage(
                Text.literal("§5✦ §dYour partner seeks you... §7" + playerName + " §dis in the §c" + getDimensionDisplayName(playerDim) + " §5✦"),
                false
            );
        } else {
            // Same dimension - calculate direction and distance
            BlockPos playerPos = player.getBlockPos();
            BlockPos partnerPos = partner.getBlockPos();
            
            double dx = partnerPos.getX() - playerPos.getX();
            double dz = partnerPos.getZ() - playerPos.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            
            // Round to nearest 50
            int approxDistance = ((int) Math.round(distance / 50.0)) * 50;
            if (approxDistance < 50) approxDistance = 50; // Minimum display
            
            // Get direction
            String direction = getDirection(dx, dz);
            
            // Notify player
            player.sendMessage(
                Text.literal("§5✦ §dYour bond pulses... §7" + partnerName + " §dis ~" + approxDistance + " blocks to the §e" + direction + " §5✦"),
                false
            );
            
            // Notify partner with reverse direction/distance
            String reverseDirection = getDirection(-dx, -dz);
            partner.sendMessage(
                Text.literal("§5✦ §dYour partner seeks you... §7" + playerName + " §dis ~" + approxDistance + " blocks to the §e" + reverseDirection + " §5✦"),
                false
            );
        }
        
        // Play sound for both
        ServerWorld serverWorld = (ServerWorld) player.getEntityWorld();
        serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.0f);
        
        ServerWorld partnerWorld = (ServerWorld) partner.getEntityWorld();
        partnerWorld.playSound(null, partner.getX(), partner.getY(), partner.getZ(),
            SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.0f);
        
        // Consume use and set cooldown
        int newUses = usesRemaining - 1;
        setCompassUses(stack, newUses);
        soulLinkManager.setCompassCooldown(playerId, config.soulLinkCompassCooldownMinutes);
        
        // Check if totem broke
        if (newUses <= 0) {
            stack.decrement(1);
            player.sendMessage(
                Text.literal("§5✦ §4The totem's compass power has been exhausted! §5✦"),
                false
            );
            
            // Play break sound
            serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
        
        return ActionResult.SUCCESS;
    }
    
    private int getCompassUses(ItemStack stack, ModConfig config) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            NbtCompound nbt = customData.copyNbt();
            if (nbt.contains(COMPASS_USES_KEY)) {
                return nbt.getInt(COMPASS_USES_KEY).orElse(config.soulLinkCompassMaxUses);
            }
        }
        // First use - return max uses
        return config.soulLinkCompassMaxUses;
    }
    
    private void setCompassUses(ItemStack stack, int uses) {
        NbtCompound nbt = new NbtCompound();
        NbtComponent existing = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (existing != null) {
            nbt = existing.copyNbt();
        }
        nbt.putInt(COMPASS_USES_KEY, uses);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
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
    
    private String getDimensionDisplayName(RegistryKey<World> dim) {
        String path = dim.getValue().getPath();
        return switch (path) {
            case "overworld" -> "Overworld";
            case "the_nether" -> "Nether";
            case "the_end" -> "End";
            default -> path.substring(0, 1).toUpperCase() + path.substring(1).replace("_", " ");
        };
    }
}
