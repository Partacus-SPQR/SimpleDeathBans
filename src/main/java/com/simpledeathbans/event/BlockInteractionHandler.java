package com.simpledeathbans.event;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.data.PlayerDataManager;
import com.simpledeathbans.ritual.ResurrectionRitualManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

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
    private static void onBlockBreak(Level world, Player player, BlockPos pos, 
                                      BlockState state, BlockEntity blockEntity) {
        if (world.isClientSide()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        PlayerDataManager dataManager = SimpleDeathBans.getInstance().getPlayerDataManager();
        if (dataManager == null) return;
        
        // Increment block mined counter
        dataManager.onBlockMined(serverPlayer.getUUID(), serverPlayer.getName().getString());
    }
    
    /**
     * Called when a player uses (right-clicks) a block.
     * Handles:
     * - Block placement tracking for anti-AFK
     * - Beacon interaction for ritual commitment (players without totem)
     */
    private static InteractionResult onBlockUse(Player player, Level world, 
                                            InteractionHand hand, BlockHitResult hitResult) {
        if (world.isClientSide()) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        
        // Only track main hand
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = world.getBlockState(pos);
        
        // Check for beacon interaction (for ritual commitment without totem)
        if (state.getBlock() == Blocks.BEACON && player.isShiftKeyDown()) {
            // Check if there's an active ritual AND player doesn't have a totem
            // (Players with totems will use the item's useOnBlock instead)
            if (!player.getMainHandItem().is(com.simpledeathbans.item.ModItems.RESURRECTION_TOTEM) &&
                !player.getOffhandItem().is(com.simpledeathbans.item.ModItems.RESURRECTION_TOTEM)) {
                
                ResurrectionRitualManager ritualManager = SimpleDeathBans.getInstance().getRitualManager();
                if (ritualManager != null && ritualManager.isRitualActive()) {
                    // Try to commit to the ritual
                    boolean success = ritualManager.onBeaconInteract(serverPlayer, (ServerLevel) world, pos);
                    if (success) {
                        return InteractionResult.SUCCESS;
                    }
                } else if (ritualManager != null && !ritualManager.isRitualActive()) {
                    // No active ritual, inform player they need a totem to start
                    serverPlayer.sendSystemMessage(Component.literal("§eYou need a Resurrection Totem to start a ritual.")
                        .withStyle(ChatFormatting.YELLOW));
                    serverPlayer.sendSystemMessage(Component.literal("§7(Sneak + right-click beacon while holding the totem)")
                        .withStyle(ChatFormatting.GRAY));
                }
            }
        }
        
        // Track block placement for anti-AFK
        if (!player.getItemInHand(hand).isEmpty()) {
            PlayerDataManager dataManager = SimpleDeathBans.getInstance().getPlayerDataManager();
            if (dataManager != null) {
                dataManager.onBlockPlaced(serverPlayer.getUUID(), serverPlayer.getName().getString());
            }
        }
        
        return InteractionResult.PASS;
    }
}
