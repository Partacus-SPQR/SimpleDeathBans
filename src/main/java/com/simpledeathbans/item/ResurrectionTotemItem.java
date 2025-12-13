package com.simpledeathbans.item;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.ritual.ResurrectionRitualManager;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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
    
    public ResurrectionTotemItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        
        if (world.isClient() || player == null) {
            return ActionResult.PASS;
        }
        
        // Must be sneaking to activate the ritual
        if (!player.isSneaking()) {
            return ActionResult.PASS;
        }
        
        // Check if clicked on a beacon
        if (world.getBlockState(pos).getBlock() != Blocks.BEACON) {
            return ActionResult.PASS;
        }
        
        // Must be server side
        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }
        
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }
        
        // Check if resurrection altars are enabled
        if (!SimpleDeathBans.getInstance().getConfig().enableResurrectionAltar) {
            serverPlayer.sendMessage(Text.literal("Resurrection rituals are disabled on this server.")
                .formatted(Formatting.RED), false);
            return ActionResult.FAIL;
        }
        
        ResurrectionRitualManager ritualManager = SimpleDeathBans.getInstance().getRitualManager();
        if (ritualManager == null) {
            return ActionResult.FAIL;
        }
        
        // Attempt to interact with the ritual (initiate or commit)
        boolean success = ritualManager.onBeaconInteract(serverPlayer, serverWorld, pos);
        
        if (success) {
            // Don't consume totem here - it's consumed when ritual completes
            return ActionResult.SUCCESS;
        }
        
        return ActionResult.FAIL;
    }
}
