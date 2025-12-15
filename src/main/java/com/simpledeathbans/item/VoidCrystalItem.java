package com.simpledeathbans.item;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.data.BanDataManager;
import com.simpledeathbans.data.SoulLinkManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

/**
 * The Void Crystal item used to sever soul links.
 * 
 * Usage:
 * - Right-click while holding to sever your current soul link
 * - Increases your ban tier by configured amount (default: 1)
 * - Both you and your ex-partner enter cooldown periods
 * 
 * Recipe (3x3 shaped):
 * - Row 1: End Crystal | Empty | End Crystal
 * - Row 2: Empty | Soul Link Totem | Empty
 * - Row 3: End Crystal | Empty | End Crystal
 */
public class VoidCrystalItem extends Item {
    
    public VoidCrystalItem(Settings settings) {
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
        
        if (!(user instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }
        
        SimpleDeathBans mod = SimpleDeathBans.getInstance();
        if (mod == null) {
            return ActionResult.FAIL;
        }
        
        ModConfig config = mod.getConfig();
        SoulLinkManager soulLinkManager = mod.getSoulLinkManager();
        BanDataManager banDataManager = mod.getBanDataManager();
        
        // Check if Soul Link is enabled
        if (config == null || !config.enableSoulLink) {
            player.sendMessage(
                Text.literal("Soul Link is not enabled on this server.").formatted(Formatting.RED),
                false
            );
            return ActionResult.FAIL;
        }
        
        if (soulLinkManager == null || banDataManager == null) {
            return ActionResult.FAIL;
        }
        
        UUID playerId = player.getUuid();
        
        // Check if player has a soul link to sever
        Optional<UUID> partnerOpt = soulLinkManager.getPartner(playerId);
        if (partnerOpt.isEmpty()) {
            player.sendMessage(
                Text.literal("§5✦ §7You have no soul bond to sever... §5✦"),
                false
            );
            return ActionResult.FAIL;
        }
        
        UUID partnerId = partnerOpt.get();
        String partnerName = getPlayerName(partnerId);
        
        // Perform the severance
        soulLinkManager.breakLink(playerId);
        soulLinkManager.recordSever(playerId, partnerId, config);
        
        // Increase ban tier for the player who severed (penalty)
        if (config.soulLinkSeverBanTierIncrease > 0) {
            banDataManager.incrementTier(playerId, config.soulLinkSeverBanTierIncrease);
        }
        
        // Consume the item
        ItemStack heldItem = player.getStackInHand(hand);
        heldItem.decrement(1);
        
        // Notify the player who severed
        player.sendMessage(
            Text.literal("§5✦ §4Breaking a soul bond leaves scars on your soul! §5✦"),
            false
        );
        
        // Play dramatic sound for the player
        ServerWorld serverWorld = (ServerWorld) world;
        serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.5f, 0.5f);
        
        // Notify the ex-partner if online
        ServerPlayerEntity partner = serverWorld.getServer().getPlayerManager().getPlayer(partnerId);
        if (partner != null) {
            partner.sendMessage(
                Text.literal("§5✦ §4Your soul bond has been severed! §5✦"),
                false
            );
            // Play dramatic sound for partner too
            ServerWorld partnerWorld = (ServerWorld) partner.getEntityWorld();
            partnerWorld.playSound(null, partner.getX(), partner.getY(), partner.getZ(),
                SoundEvents.ENTITY_WITHER_HURT, SoundCategory.PLAYERS, 0.8f, 0.5f);
        }
        
        SimpleDeathBans.LOGGER.info("{} severed their soul link with {} using Void Crystal", 
            player.getName().getString(), partnerName);
        
        return ActionResult.SUCCESS;
    }
    
    private String getPlayerName(UUID playerId) {
        SimpleDeathBans mod = SimpleDeathBans.getInstance();
        if (mod == null) return "unknown";
        
        SoulLinkManager soulLinkManager = mod.getSoulLinkManager();
        if (soulLinkManager == null) return "unknown";
        
        // We don't have direct server access, but we can try through soul link manager
        return "unknown"; // Name will be fetched when player is online in use() method
    }
}
