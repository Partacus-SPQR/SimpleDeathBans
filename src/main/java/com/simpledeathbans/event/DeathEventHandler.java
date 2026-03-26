package com.simpledeathbans.event;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.damage.SoulSeverDamageSource;
import com.simpledeathbans.data.BanDataManager;
import com.simpledeathbans.data.SoulLinkManager;
import com.simpledeathbans.network.SinglePlayerBanPayload;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.UUID;

/**
 * Handles player death events and implements the ban system.
 */
public class DeathEventHandler {
    
    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer player) {
                handlePlayerDeath(player, damageSource);
            }
        });
    }
    
    private static void handlePlayerDeath(ServerPlayer player, DamageSource damageSource) {
        SimpleDeathBans mod = SimpleDeathBans.getInstance();
        if (mod == null) return;
        
        ModConfig config = mod.getConfig();
        BanDataManager banManager = mod.getBanDataManager();
        SoulLinkManager soulLinkManager = mod.getSoulLinkManager();
        
        if (banManager == null) return;
        
        // Check if death bans are disabled globally
        if (!config.enableDeathBans) {
            SimpleDeathBans.LOGGER.debug("Death bans disabled - skipping ban for {}", 
                    player.getName().getString());
            return;
        }
        
        // Check if mod is disabled in single-player
        ServerLevel world = (ServerLevel) player.level();
        if (world.getServer().isSingleplayer() && !config.singlePlayerEnabled) {
            SimpleDeathBans.LOGGER.info("Single-player mod disabled - skipping death processing for {}", 
                    player.getName().getString());
            return;
        }
        
        // Check if this is a Soul Sever death (from soul link)
        if (SoulSeverDamageSource.isSoulSever(damageSource)) {
            // This player died from their partner dying - handle as normal death
            handleBanAndDisconnect(player, banManager, config, false);
            return;
        }
        
        // Check for soul link death pact (MULTIPLAYER ONLY)
        if (config.enableSoulLink && soulLinkManager != null && !world.getServer().isSingleplayer()) {
            handleSoulLinkDeath(player, soulLinkManager);
        }
        
        // Determine if PvP death
        boolean isPvP = isPvPDeath(damageSource);
        
        // Create ban
        handleBanAndDisconnect(player, banManager, config, isPvP);
    }
    
    private static boolean isPvPDeath(DamageSource source) {
        // Direct player attack
        if (source.getEntity() instanceof Player) {
            return true;
        }
        
        // Indirect kill (player pushed into hazard)
        if (source.getDirectEntity() instanceof Player) {
            return true;
        }
        
        // Check for projectiles from players
        var attacker = source.getEntity();
        if (attacker != null && source.getDirectEntity() != attacker) {
            if (source.getDirectEntity() instanceof Player) {
                return true;
            }
        }
        
        return false;
    }
    
    private static void handleSoulLinkDeath(ServerPlayer deadPlayer, SoulLinkManager soulLinkManager) {
        UUID deadPlayerId = deadPlayer.getUUID();
        
        soulLinkManager.getPartner(deadPlayerId).ifPresent(partnerUuid -> {
            ServerLevel world = (ServerLevel) deadPlayer.level();
            ServerPlayer partner = world.getServer().getPlayerList().getPlayer(partnerUuid);
            
            if (partner != null && partner.isAlive()) {
                // Play wither spawn sound to both
                world.playSound(
                    null, deadPlayer.getX(), deadPlayer.getY(), deadPlayer.getZ(),
                    SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS,
                    1.0f, 1.0f
                );
                
                ServerLevel partnerWorld = (ServerLevel) partner.level();
                partnerWorld.playSound(
                    null, partner.getX(), partner.getY(), partner.getZ(),
                    SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS,
                    1.0f, 1.0f
                );
                
                // Send soul sever message
                Component soulSeverMessage = Component.literal("Your soul has been severed")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withItalic(true));
                partner.sendSystemMessage(soulSeverMessage);
                deadPlayer.sendSystemMessage(soulSeverMessage);
                
                // Kill partner with soul sever damage (delayed to avoid recursion)
                SoulSeverDamageSource.markSoulSeverTarget(partnerUuid);
                partnerWorld.getServer().execute(() -> {
                    try {
                        DamageSource soulSeverDamage = SoulSeverDamageSource.create(partnerWorld, deadPlayer);
                        partner.hurtServer(partnerWorld, soulSeverDamage, Float.MAX_VALUE);
                    } finally {
                        SoulSeverDamageSource.clearSoulSeverTarget(partnerUuid);
                    }
                });
            }
        });
    }
    
    private static void handleBanAndDisconnect(ServerPlayer player, BanDataManager banManager, ModConfig config, boolean isPvP) {
        // Calculate ban time
        int banMinutes = banManager.calculateBanMinutes(player.getUUID(), isPvP);
        
        // Create ban entry
        BanDataManager.BanEntry banEntry = banManager.createBan(player.getUUID(), player.getName().getString(), banMinutes);
        
        // Ghost Echo effect
        if (config.enableGhostEcho) {
            performGhostEcho(player, banEntry);
        }
        
        // Build kick message
        int tier = banManager.getTier(player.getUUID());
        String timeRemaining = banEntry.getRemainingTimeFormatted();
        
        Component kickMessage = Component.empty()
            .append(Component.literal("§c§k><§r §4§lBANNED §c§k><§r\n\n"))
            .append(Component.literal("§5You have been claimed by the void.\n\n"))
            .append(Component.literal("§7Time remaining: §c" + timeRemaining + "§r\n"))
            .append(Component.literal("§7Ban Tier: §c" + tier + "§r\n\n"))
            .append(Component.literal("§8Death results in temporary bans.\n"))
            .append(Component.literal("§8Your ban tier increases with each death."));
        
        ServerLevel world = (ServerLevel) player.level();
        
        // SINGLE-PLAYER: Send a network payload to trigger client-side disconnect
        // This allows the client to properly save the world before disconnecting,
        // avoiding the OverlappingFileLockException that occurs with server-side disconnect.
        if (world.getServer().isSingleplayer()) {
            SimpleDeathBans.LOGGER.info("Single-player death: Sending ban notification to {} ({} minutes, tier {})", 
                player.getName().getString(), banMinutes, tier);
            
            // Send the ban payload to the client
            // The client will handle the disconnect properly, saving the world first
            SinglePlayerBanPayload payload = new SinglePlayerBanPayload(tier, banEntry.getRemainingTime(), timeRemaining);
            
            // Send after a brief delay to ensure player respawns first and can see the effect
            world.getServer().execute(() -> {
                // Small delay to let respawn complete
                world.getServer().execute(() -> {
                    ServerPlayNetworking.send(player, payload);
                    SimpleDeathBans.LOGGER.info("Ban notification sent to client for {}", player.getName().getString());
                });
            });
            return;
        }
        
        // MULTIPLAYER: Disconnect the player
        world.getServer().execute(() -> {
            player.connection.disconnect(kickMessage);
        });
    }
    
    private static void performGhostEcho(ServerPlayer player, BanDataManager.BanEntry banEntry) {
        ServerLevel world = (ServerLevel) player.level();
        
        // Spawn cosmetic lightning at death location (no damage, no fire)
        LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, world);
        lightning.teleportTo(player.getX(), player.getY(), player.getZ());
        lightning.setVisualOnly(true); // No damage, no fire
        world.addFreshEntity(lightning);
        
        // Broadcast custom death message with styled formatting
        // §k = obfuscated, §4 = dark red, §5 = dark purple, §c = red
        String banTime = banEntry.getRemainingTimeFormatted();
        Component deathMessage = Component.empty()
            .append(Component.literal("§c§k><§r "))
            .append(Component.literal(player.getName().getString()).withStyle(ChatFormatting.DARK_PURPLE))
            .append(Component.literal(" has been lost to the void for ").withStyle(ChatFormatting.DARK_PURPLE))
            .append(Component.literal(banTime).withStyle(ChatFormatting.RED))
            .append(Component.literal(" §c§k><§r"));
        
        world.getServer().getPlayerList().broadcastSystemMessage(deathMessage, false);
    }
}
