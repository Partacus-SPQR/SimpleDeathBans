package com.simpledeathbans.item;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.ritual.ResurrectionRitualManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * The Resurrection Totem item used for the Altar of Resurrection ritual.
 * 
 * Usage: 
 * - ONE player (the initiator) holds the totem and sneak + right-clicks a beacon
 * - The beacon must be fully powered with a NETHERITE base (164 blocks)
 * - ALL online players must commit by sneak + right-clicking the beacon (no totem needed)
 * - Once all players commit, the totem is consumed and a random banned player is resurrected
 * 
 * Key points:
 * - Only ONE totem is needed (consumed from the initiator)
 * - Other players can pass the totem around if needed
 * - Committing to the ritual doesn't require holding the totem
 * 
 * Recipe (3x3 shaped):
 * - Row 1: Totem of Undying | Nether Star | Totem of Undying
 * - Row 2: Nether Star | Heavy Core | Nether Star
 * - Row 3: Totem of Undying | Nether Star | Totem of Undying
 */
public class ResurrectionTotemItem extends Item {
    
    public ResurrectionTotemItem(Item.Properties settings) {
        super(settings);
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        
        if (world.isClientSide() || player == null) {
            return InteractionResult.PASS;
        }
        
        // Must be sneaking to activate the ritual
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        
        // Check if clicked on a beacon
        if (world.getBlockState(pos).getBlock() != Blocks.BEACON) {
            return InteractionResult.PASS;
        }
        
        // Must be server side
        if (!(world instanceof ServerLevel serverWorld)) {
            return InteractionResult.PASS;
        }
        
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        
        // Check if resurrection altars are enabled
        if (!SimpleDeathBans.getInstance().getConfig().enableResurrectionAltar) {
            serverPlayer.sendSystemMessage(Component.literal("Resurrection rituals are disabled on this server.")
                .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        
        ResurrectionRitualManager ritualManager = SimpleDeathBans.getInstance().getRitualManager();
        if (ritualManager == null) {
            return InteractionResult.FAIL;
        }
        
        // Attempt to interact with the ritual (initiate or commit)
        boolean success = ritualManager.onBeaconInteract(serverPlayer, serverWorld, pos);
        
        if (success) {
            // Don't consume totem here - it's consumed when ritual completes
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.FAIL;
    }
}
