package com.simpledeathbans.item;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.data.BanDataManager;
import com.simpledeathbans.data.SoulLinkManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/**
 * The Void Crystal Totem item used to sever soul links.
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
public class VoidCrystalTotemItem extends Item {
    
    public VoidCrystalTotemItem(Item.Properties settings) {
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
        
        if (!(user instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }
        
        SimpleDeathBans mod = SimpleDeathBans.getInstance();
        if (mod == null) {
            return InteractionResult.FAIL;
        }
        
        ModConfig config = mod.getConfig();
        SoulLinkManager soulLinkManager = mod.getSoulLinkManager();
        BanDataManager banDataManager = mod.getBanDataManager();
        
        // Check if Soul Link is enabled
        if (config == null || !config.enableSoulLink) {
            player.sendSystemMessage(Component.literal("Soul Link is not enabled on this server.").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        
        if (soulLinkManager == null || banDataManager == null) {
            return InteractionResult.FAIL;
        }
        
        UUID playerId = player.getUUID();
        
        // Check if player has a soul link to sever
        Optional<UUID> partnerOpt = soulLinkManager.getPartner(playerId);
        if (partnerOpt.isEmpty()) {
            player.sendSystemMessage(Component.literal("§5✦ §7You have no soul bond to sever... §5✦"));
            return InteractionResult.FAIL;
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
        ItemStack heldItem = player.getItemInHand(hand);
        heldItem.shrink(1);
        
        // Notify the player who severed
        player.sendSystemMessage(Component.literal("§5✦ §4Breaking a soul bond leaves scars on your soul! §5✦"));
        
        // Play dramatic sound for the player
        ServerLevel serverWorld = (ServerLevel) world;
        serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 0.5f);
        
        // Notify the ex-partner if online
        ServerPlayer partner = serverWorld.getServer().getPlayerList().getPlayer(partnerId);
        if (partner != null) {
            partner.sendSystemMessage(Component.literal("§5✦ §4Your soul bond has been severed! §5✦"));
            // Play dramatic sound for partner too
            ServerLevel partnerWorld = (ServerLevel) partner.level();
            partnerWorld.playSound(null, partner.getX(), partner.getY(), partner.getZ(),
                SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.8f, 0.5f);
        }
        
        SimpleDeathBans.LOGGER.info("{} severed their soul link with {} using Void Crystal", 
            player.getName().getString(), partnerName);
        
        return InteractionResult.SUCCESS;
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
