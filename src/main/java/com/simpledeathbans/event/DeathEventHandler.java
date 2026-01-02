package com.simpledeathbans.event;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.damage.SoulSeverDamageSource;
import com.simpledeathbans.data.BanDataManager;
import com.simpledeathbans.data.SoulLinkManager;
import com.simpledeathbans.network.SinglePlayerBanPayload;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

/**
 * Handles player death events and implements the ban system.
 */
public class DeathEventHandler {
    
    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity player) {
                handlePlayerDeath(player, damageSource);
            }
        });
    }
    
    private static void handlePlayerDeath(ServerPlayerEntity player, DamageSource damageSource) {
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
        ServerWorld world = (ServerWorld) player.getEntityWorld();
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
        if (source.getAttacker() instanceof PlayerEntity) {
            return true;
        }
        
        // Indirect kill (player pushed into hazard)
        if (source.getSource() instanceof PlayerEntity) {
            return true;
        }
        
        // Check for projectiles from players
        var attacker = source.getAttacker();
        if (attacker != null && source.getSource() != attacker) {
            if (source.getSource() instanceof PlayerEntity) {
                return true;
            }
        }
        
        return false;
    }
    
    private static void handleSoulLinkDeath(ServerPlayerEntity deadPlayer, SoulLinkManager soulLinkManager) {
        UUID deadPlayerId = deadPlayer.getUuid();
        
        soulLinkManager.getPartner(deadPlayerId).ifPresent(partnerUuid -> {
            ServerWorld world = (ServerWorld) deadPlayer.getEntityWorld();
            ServerPlayerEntity partner = world.getServer().getPlayerManager().getPlayer(partnerUuid);
            
            if (partner != null && partner.isAlive()) {
                // Play wither spawn sound to both
                world.playSound(
                    null, deadPlayer.getX(), deadPlayer.getY(), deadPlayer.getZ(),
                    SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS,
                    1.0f, 1.0f
                );
                
                ServerWorld partnerWorld = (ServerWorld) partner.getEntityWorld();
                partnerWorld.playSound(
                    null, partner.getX(), partner.getY(), partner.getZ(),
                    SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS,
                    1.0f, 1.0f
                );
                
                // Send soul sever message
                Text soulSeverMessage = Text.literal("Your soul has been severed")
                    .setStyle(Style.EMPTY.withColor(Formatting.DARK_RED).withItalic(true));
                partner.sendMessage(soulSeverMessage, false);
                deadPlayer.sendMessage(soulSeverMessage, false);
                
                // Kill partner with soul sever damage (delayed to avoid recursion)
                SoulSeverDamageSource.markSoulSeverTarget(partnerUuid);
                partnerWorld.getServer().execute(() -> {
                    try {
                        DamageSource soulSeverDamage = SoulSeverDamageSource.create(partnerWorld, deadPlayer);
                        partner.damage(partnerWorld, soulSeverDamage, Float.MAX_VALUE);
                    } finally {
                        SoulSeverDamageSource.clearSoulSeverTarget(partnerUuid);
                    }
                });
            }
        });
    }
    
    private static void handleBanAndDisconnect(ServerPlayerEntity player, BanDataManager banManager, ModConfig config, boolean isPvP) {
        // Calculate ban time
        int banMinutes = banManager.calculateBanMinutes(player.getUuid(), isPvP);
        
        // Create ban entry
        BanDataManager.BanEntry banEntry = banManager.createBan(player.getUuid(), player.getName().getString(), banMinutes);
        
        // Ghost Echo effect
        if (config.enableGhostEcho) {
            performGhostEcho(player, banEntry);
        }
        
        // Build kick message
        int tier = banManager.getTier(player.getUuid());
        String timeRemaining = banEntry.getRemainingTimeFormatted();
        
        Text kickMessage = Text.empty()
            .append(Text.literal("§c§k><§r §4§lBANNED §c§k><§r\n\n"))
            .append(Text.literal("§5You have been claimed by the void.\n\n"))
            .append(Text.literal("§7Time remaining: §c" + timeRemaining + "§r\n"))
            .append(Text.literal("§7Ban Tier: §c" + tier + "§r\n\n"))
            .append(Text.literal("§8Death results in temporary bans.\n"))
            .append(Text.literal("§8Your ban tier increases with each death."));
        
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        
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
            player.networkHandler.disconnect(kickMessage);
        });
    }
    
    private static void performGhostEcho(ServerPlayerEntity player, BanDataManager.BanEntry banEntry) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        
        // Spawn cosmetic lightning at death location (no damage, no fire)
        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
        lightning.refreshPositionAfterTeleport(player.getX(), player.getY(), player.getZ());
        lightning.setCosmetic(true); // No damage, no fire
        world.spawnEntity(lightning);
        
        // Broadcast custom death message with styled formatting
        // §k = obfuscated, §4 = dark red, §5 = dark purple, §c = red
        String banTime = banEntry.getRemainingTimeFormatted();
        Text deathMessage = Text.empty()
            .append(Text.literal("§c§k><§r "))
            .append(Text.literal(player.getName().getString()).formatted(Formatting.DARK_PURPLE))
            .append(Text.literal(" has been lost to the void for ").formatted(Formatting.DARK_PURPLE))
            .append(Text.literal(banTime).formatted(Formatting.RED))
            .append(Text.literal(" §c§k><§r"));
        
        world.getServer().getPlayerManager().broadcast(deathMessage, false);
    }
}
