package com.simpledeathbans.event;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.data.PlayerDataManager;
import com.simpledeathbans.ritual.ResurrectionRitualManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Tracks block interactions (mining, placing) for the mercy cooldown anti-AFK system.
 * Also handles beacon interactions for the resurrection ritual commitment.
 */
public class BlockInteractionHandler {
    
    /**
     * Registers block interaction event handlers.
     */
    public static void register() {
        // Track block breaking
        PlayerBlockBreakEvents.AFTER.register(BlockInteractionHandler::onBlockBreak);
        
        // Track block placement and beacon interaction for ritual commitment
        UseBlockCallback.EVENT.register(BlockInteractionHandler::onBlockUse);
    }
    
    /**
     * Called after a player breaks a block.
     */
    private static void onBlockBreak(World world, PlayerEntity player, BlockPos pos, 
                                      BlockState state, BlockEntity blockEntity) {
        if (world.isClient()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        
        PlayerDataManager dataManager = SimpleDeathBans.getInstance().getPlayerDataManager();
        if (dataManager == null) return;
        
        // Increment block mined counter
        dataManager.onBlockMined(serverPlayer.getUuid(), serverPlayer.getName().getString());
    }
    
    /**
     * Called when a player uses (right-clicks) a block.
     * Handles:
     * - Block placement tracking for anti-AFK
     * - Beacon interaction for ritual commitment (players without totem)
     */
    private static ActionResult onBlockUse(PlayerEntity player, World world, 
                                            Hand hand, BlockHitResult hitResult) {
        if (world.isClient()) return ActionResult.PASS;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
        
        // Only track main hand
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = world.getBlockState(pos);
        
        // Check for beacon interaction (for ritual commitment without totem)
        if (state.getBlock() == Blocks.BEACON && player.isSneaking()) {
            // Check if there's an active ritual AND player doesn't have a totem
            // (Players with totems will use the item's useOnBlock instead)
            if (!player.getMainHandStack().isOf(com.simpledeathbans.item.ModItems.RESURRECTION_TOTEM) &&
                !player.getOffHandStack().isOf(com.simpledeathbans.item.ModItems.RESURRECTION_TOTEM)) {
                
                ResurrectionRitualManager ritualManager = SimpleDeathBans.getInstance().getRitualManager();
                if (ritualManager != null && ritualManager.isRitualActive()) {
                    // Try to commit to the ritual
                    boolean success = ritualManager.onBeaconInteract(serverPlayer, (ServerWorld) world, pos);
                    if (success) {
                        return ActionResult.SUCCESS;
                    }
                } else if (ritualManager != null && !ritualManager.isRitualActive()) {
                    // No active ritual, inform player they need a totem to start
                    serverPlayer.sendMessage(Text.literal("§eYou need a Resurrection Totem to start a ritual.")
                        .formatted(Formatting.YELLOW), false);
                    serverPlayer.sendMessage(Text.literal("§7(Sneak + right-click beacon while holding the totem)")
                        .formatted(Formatting.GRAY), false);
                }
            }
        }
        
        // Track block placement for anti-AFK
        if (!player.getStackInHand(hand).isEmpty()) {
            PlayerDataManager dataManager = SimpleDeathBans.getInstance().getPlayerDataManager();
            if (dataManager != null) {
                dataManager.onBlockPlaced(serverPlayer.getUuid(), serverPlayer.getName().getString());
            }
        }
        
        return ActionResult.PASS;
    }
}
