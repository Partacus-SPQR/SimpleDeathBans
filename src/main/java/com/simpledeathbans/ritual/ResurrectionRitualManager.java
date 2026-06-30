package com.simpledeathbans.ritual;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.data.BanDataManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Manages the Altar of Resurrection ritual mechanics.
 * 
 * Ritual Requirements:
 * - Fully powered Beacon with complete Netherite base (4 layers, 164 blocks)
 * - Player must be sneaking and holding a Resurrection Totem
 * - Right-click the Beacon to initiate/commit to the ritual
 * - ALL online players must commit for the ritual to succeed
 * 
 * Recipe for Resurrection Totem (3x3 shaped):
 * - Row 1: Totem of Undying | Nether Star | Totem of Undying
 * - Row 2: Nether Star | Heavy Core | Nether Star
 * - Row 3: Totem of Undying | Nether Star | Totem of Undying
 */
public class ResurrectionRitualManager {
    
    private final MinecraftServer server;
    
    // Active ritual tracking
    private BlockPos activeRitualBeacon = null;
    private ServerLevel activeRitualWorld = null;
    private final Set<UUID> committedPlayers = new HashSet<>();
    private UUID initiatorUuid = null;
    private long ritualStartTime = 0;
    private static final long RITUAL_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes to complete
    
    public ResurrectionRitualManager(MinecraftServer server) {
        this.server = server;
    }
    
    /**
     * Checks if there is an active ritual in progress.
     * @return true if a ritual has been initiated and is awaiting completion
     */
    public boolean isRitualActive() {
        return activeRitualBeacon != null;
    }
    
    /**
     * Called when a player interacts with a beacon while sneaking with the totem.
     * @return true if the interaction was handled
     */
    public boolean onBeaconInteract(ServerPlayer player, ServerLevel world, BlockPos beaconPos) {
        if (!SimpleDeathBans.getInstance().getConfig().enableResurrectionAltar) {
            player.sendSystemMessage(Component.literal("Resurrection rituals are disabled on this server.")
                .withStyle(ChatFormatting.RED));
            return false;
        }
        
        // Verify it's actually a beacon
        BlockState state = world.getBlockState(beaconPos);
        if (state.getBlock() != Blocks.BEACON) {
            return false;
        }
        
        // Check if beacon is fully powered with netherite base
        if (!isFullyPoweredNetheriteBeacon(world, beaconPos)) {
            player.sendSystemMessage(Component.literal("§c✦ The Beacon must be fully powered with a complete Netherite base! ✦")
                .withStyle(ChatFormatting.RED));
            player.sendSystemMessage(Component.literal("§7(4 layers: 9×9, 7×7, 5×5, 3×3 = 164 Netherite Blocks)")
                .withStyle(ChatFormatting.GRAY));
            return false;
        }
        
        // Check for banned players to resurrect
        BanDataManager banManager = SimpleDeathBans.getInstance().getBanDataManager();
        if (banManager == null || banManager.getAllBannedPlayers().isEmpty()) {
            player.sendSystemMessage(Component.literal("§eThere are no banned souls to resurrect.")
                .withStyle(ChatFormatting.YELLOW));
            return false;
        }
        
        // Check if there's an active ritual at a different location
        if (activeRitualBeacon != null && (!beaconPos.equals(activeRitualBeacon) || world != activeRitualWorld)) {
            player.sendSystemMessage(Component.literal("§cA ritual is already in progress at another altar!")
                .withStyle(ChatFormatting.RED));
            return false;
        }
        
        // Check for ritual timeout
        if (activeRitualBeacon != null && System.currentTimeMillis() - ritualStartTime > RITUAL_TIMEOUT_MS) {
            cancelRitual("§c✦ The Resurrection Ritual has timed out. ✦");
        }
        
        // If no active ritual, start one
        if (activeRitualBeacon == null) {
            return initiateRitual(player, world, beaconPos);
        }
        
        // If ritual is active, process commitment
        return commitToRitual(player, world, beaconPos);
    }
    
    /**
     * Initiates a new resurrection ritual.
     */
    private boolean initiateRitual(ServerPlayer initiator, ServerLevel world, BlockPos beaconPos) {
        activeRitualBeacon = beaconPos;
        activeRitualWorld = world;
        committedPlayers.clear();
        committedPlayers.add(initiator.getUUID());
        initiatorUuid = initiator.getUUID();
        ritualStartTime = System.currentTimeMillis();
        
        int totalPlayers = server.getPlayerList().getPlayers().size();
        
        // Play dramatic sound
        world.playSound(null, beaconPos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 0.5f);
        
        // Broadcast initiation message
        String message = String.format("§d§l✦ %s has initiated the Altar of Resurrection! ✦", 
            initiator.getName().getString());
        server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
        
        String progressMessage = String.format("§e[%d/%d] players have committed. All players must commit to finalize.",
            committedPlayers.size(), totalPlayers);
        server.getPlayerList().broadcastSystemMessage(Component.literal(progressMessage), false);
        
        server.getPlayerList().broadcastSystemMessage(Component.literal("§7(Sneak + right-click the beacon with a Resurrection Totem to commit)")
            .withStyle(ChatFormatting.GRAY), false);
        
        SimpleDeathBans.LOGGER.info("Resurrection ritual initiated by {} at {}", 
            initiator.getName().getString(), beaconPos);
        
        // Check if single player (ritual completes immediately)
        if (totalPlayers == 1) {
            return completeRitual(world, beaconPos);
        }
        
        return true;
    }
    
    /**
     * Processes a player committing to an active ritual.
     */
    private boolean commitToRitual(ServerPlayer player, ServerLevel world, BlockPos beaconPos) {
        UUID playerId = player.getUUID();
        
        // Check if already committed
        if (committedPlayers.contains(playerId)) {
            player.sendSystemMessage(Component.literal("§eYou have already committed to this ritual.")
                .withStyle(ChatFormatting.YELLOW));
            return false;
        }
        
        // Add commitment
        committedPlayers.add(playerId);
        
        int totalPlayers = server.getPlayerList().getPlayers().size();
        
        // Play commitment sound
        world.playSound(null, beaconPos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0f, 1.2f);
        
        // Broadcast progress
        String progressMessage = String.format("§a%s has committed to the ritual! §e[%d/%d]",
            player.getName().getString(), committedPlayers.size(), totalPlayers);
        server.getPlayerList().broadcastSystemMessage(Component.literal(progressMessage), false);
        
        SimpleDeathBans.LOGGER.info("Player {} committed to ritual ({}/{})", 
            player.getName().getString(), committedPlayers.size(), totalPlayers);
        
        // Check if ritual can complete
        if (committedPlayers.size() >= totalPlayers) {
            return completeRitual(world, beaconPos);
        }
        
        return true;
    }
    
    /**
     * Completes the ritual and resurrects a random banned player.
     */
    private boolean completeRitual(ServerLevel world, BlockPos beaconPos) {
        BanDataManager banManager = SimpleDeathBans.getInstance().getBanDataManager();
        if (banManager == null) {
            cancelRitual("§cRitual failed: Unable to access ban data.");
            return false;
        }
        
        // Get a random banned player
        BanDataManager.BanEntry bannedPlayer = banManager.getRandomBannedPlayer();
        if (bannedPlayer == null) {
            cancelRitual("§cRitual failed: No banned players found.");
            return false;
        }
        
        // Consume ONE totem from the initiator only (the player who started the ritual)
        ServerPlayer initiator = server.getPlayerList().getPlayer(initiatorUuid);
        if (initiator != null) {
            // Find and consume the totem from their hand
            if (initiator.getMainHandItem().is(com.simpledeathbans.item.ModItems.RESURRECTION_TOTEM)) {
                initiator.getMainHandItem().shrink(1);
            } else if (initiator.getOffhandItem().is(com.simpledeathbans.item.ModItems.RESURRECTION_TOTEM)) {
                initiator.getOffhandItem().shrink(1);
            }
        }
        
        // Play Totem of Undying activation effects on all committed players
        for (UUID playerId : committedPlayers) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                // Play visual effect on each player
                spawnTotemParticles(player);
                
                // Play totem activation sound at each player's location
                ServerLevel playerWorld = (ServerLevel) player.level();
                playerWorld.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, 
                    SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }
        
        // Unban the player (but DO NOT reset their tier - they keep their punishment level)
        banManager.unbanPlayer(bannedPlayer.playerId());
        // Note: Ban tier is preserved so repeated deaths still accumulate punishment
        
        // Spawn multiple lightning bolts for dramatic effect
        spawnRitualLightning(world, beaconPos);
        
        // Play completion sounds at beacon
        world.playSound(null, beaconPos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        world.playSound(null, beaconPos, SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 0.5f, 1.5f);
        
        // Broadcast success message
        server.getPlayerList().broadcastSystemMessage(Component.literal(""), false);
        server.getPlayerList().broadcastSystemMessage(Component.literal("§d§l════════════════════════════════"), false);
        server.getPlayerList().broadcastSystemMessage(Component.literal("§d§l✦ THE RITUAL IS COMPLETE! ✦"), false);
        server.getPlayerList().broadcastSystemMessage(Component.literal(""), false);
        server.getPlayerList().broadcastSystemMessage(Component.literal(String.format("§b§l%s §fhas been resurrected!", bannedPlayer.playerName())), false);
        server.getPlayerList().broadcastSystemMessage(Component.literal("§k><§r §5Their soul has been freed from the void. §k><§r"), false);
        server.getPlayerList().broadcastSystemMessage(Component.literal(String.format("§c(Ban tier %d preserved)", bannedPlayer.banTier())), false);
        server.getPlayerList().broadcastSystemMessage(Component.literal("§d§l════════════════════════════════"), false);
        server.getPlayerList().broadcastSystemMessage(Component.literal(""), false);
        
        SimpleDeathBans.LOGGER.info("Resurrection ritual completed! {} has been resurrected.", bannedPlayer.playerName());
        
        // Reset ritual state
        resetRitual();
        
        return true;
    }
    
    /**
     * Cancels the current ritual with a message.
     */
    public void cancelRitual(String reason) {
        if (activeRitualBeacon != null) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(reason), false);
            SimpleDeathBans.LOGGER.info("Resurrection ritual cancelled: {}", reason);
        }
        resetRitual();
    }
    
    /**
     * Resets all ritual state.
     */
    private void resetRitual() {
        activeRitualBeacon = null;
        activeRitualWorld = null;
        committedPlayers.clear();
        initiatorUuid = null;
        ritualStartTime = 0;
    }
    
    /**
     * Called when a player disconnects - may cancel ritual if they were committed.
     */
    public void onPlayerDisconnect(UUID playerId) {
        if (activeRitualBeacon != null && committedPlayers.contains(playerId)) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            String playerName = player != null ? player.getName().getString() : "A player";
            cancelRitual(String.format("§c✦ %s disconnected. The ritual has been cancelled. ✦", playerName));
        }
    }
    
    /**
     * Checks if the beacon at the given position is fully powered with a netherite base.
     * A fully powered beacon requires 4 layers: 3×3, 5×5, 7×7, 9×9 = 164 blocks total.
     */
    public boolean isFullyPoweredNetheriteBeacon(ServerLevel world, BlockPos beaconPos) {
        // First check if beacon block entity exists and has max level
        BlockEntity blockEntity = world.getBlockEntity(beaconPos);
        if (!(blockEntity instanceof BeaconBlockEntity beacon)) {
            return false;
        }
        
        // Check beacon level (4 = fully powered)
        // We need to verify the structure manually since BeaconBlockEntity doesn't expose level easily
        
        // Check all 4 layers of the pyramid are netherite
        // Layer 1 (directly below beacon): 3×3 = 9 blocks
        // Layer 2: 5×5 = 25 blocks
        // Layer 3: 7×7 = 49 blocks
        // Layer 4 (bottom): 9×9 = 81 blocks
        // Total: 164 netherite blocks
        
        int[][] layerSizes = {{1, 3}, {2, 5}, {3, 7}, {4, 9}}; // {yOffset, size}
        
        for (int[] layer : layerSizes) {
            int yOffset = layer[0];
            int size = layer[1];
            int halfSize = size / 2;
            
            BlockPos layerCenter = beaconPos.below(yOffset);
            
            for (int x = -halfSize; x <= halfSize; x++) {
                for (int z = -halfSize; z <= halfSize; z++) {
                    BlockPos checkPos = layerCenter.offset(x, 0, z);
                    if (world.getBlockState(checkPos).getBlock() != Blocks.NETHERITE_BLOCK) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Spawns dramatic lightning effects for ritual completion.
     */
    private void spawnRitualLightning(ServerLevel world, BlockPos centerPos) {
        // Spawn lightning at center
        spawnLightning(world, centerPos);
        
        // Spawn lightning at corners of the beacon pyramid
        int[][] offsets = {{-4, -4}, {-4, 4}, {4, -4}, {4, 4}};
        for (int[] offset : offsets) {
            BlockPos lightningPos = centerPos.offset(offset[0], 0, offset[1]);
            spawnLightning(world, lightningPos);
        }
    }
    
    /**
     * Spawns a cosmetic lightning bolt at the given position.
     */
    private void spawnLightning(ServerLevel world, BlockPos pos) {
        //? if >=26.2 {
        LightningBolt lightning = new LightningBolt(net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT, world);
        //?} else {
        /*LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, world);*/
        //?}
        lightning.setPos(Vec3.atBottomCenterOf(pos));
        lightning.setVisualOnly(true); // Won't cause fire or damage
        world.addFreshEntity(lightning);
    }
    
    /**
     * Spawns Totem of Undying activation particles around a player.
     * This creates the iconic golden particle burst effect.
     */
    private void spawnTotemParticles(ServerPlayer player) {
        ServerLevel world = (ServerLevel) player.level();
        double x = player.getX();
        double y = player.getY() + 1.0; // Center on player body
        double z = player.getZ();
        
        // Spawn many totem particles in a burst around the player
        // TOTEM_OF_UNDYING particles create the golden swirl effect
        for (int i = 0; i < 100; i++) {
            double offsetX = (world.getRandom().nextDouble() - 0.5) * 2.0;
            double offsetY = (world.getRandom().nextDouble() - 0.5) * 2.0;
            double offsetZ = (world.getRandom().nextDouble() - 0.5) * 2.0;
            double velocityX = (world.getRandom().nextDouble() - 0.5) * 0.5;
            double velocityY = world.getRandom().nextDouble() * 0.5;
            double velocityZ = (world.getRandom().nextDouble() - 0.5) * 0.5;
            
            world.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, 
                x + offsetX, y + offsetY, z + offsetZ, 
                1, velocityX, velocityY, velocityZ, 0.5);
        }
        
        // Also spawn some enchant particles for extra effect
        for (int i = 0; i < 30; i++) {
            double offsetX = (world.getRandom().nextDouble() - 0.5) * 1.5;
            double offsetY = world.getRandom().nextDouble() * 2.0;
            double offsetZ = (world.getRandom().nextDouble() - 0.5) * 1.5;
            
            world.sendParticles(ParticleTypes.ENCHANT, 
                x + offsetX, y + offsetY, z + offsetZ, 
                1, 0, 0.1, 0, 0.5);
        }
    }
    
    /**
     * Checks if there's an active ritual.
     */
    public boolean hasActiveRitual() {
        return activeRitualBeacon != null;
    }
    
    /**
     * Gets the number of committed players.
     */
    public int getCommittedCount() {
        return committedPlayers.size();
    }
    
    // Legacy method for backwards compatibility - now delegates to beacon check
    public boolean isValidAltar(ServerLevel world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() == Blocks.BEACON 
            && isFullyPoweredNetheriteBeacon(world, pos);
    }
    
    // Legacy method - now uses consensus system
    public boolean performRitual(ServerPlayer performer, ServerLevel world, BlockPos altarPos) {
        return onBeaconInteract(performer, world, altarPos);
    }
}
