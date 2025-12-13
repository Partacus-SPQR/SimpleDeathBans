package com.simpledeathbans.event;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.damage.SoulSeverDamageSource;
import com.simpledeathbans.data.BanDataManager;
import com.simpledeathbans.data.SoulLinkManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
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
        
        // Check if this is a Soul Sever death (from soul link)
        if (SoulSeverDamageSource.isSoulSever(damageSource)) {
            // This player died from their partner dying - handle as normal death
            handleBanAndDisconnect(player, banManager, config, false);
            return;
        }
        
        // Check for soul link death pact
        if (config.enableSoulLink && soulLinkManager != null) {
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
                partnerWorld.getServer().execute(() -> {
                    DamageSource soulSeverDamage = SoulSeverDamageSource.create(partnerWorld, deadPlayer);
                    partner.damage(partnerWorld, soulSeverDamage, Float.MAX_VALUE);
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
        
        // Disconnect player
        Text kickMessage = Text.translatable("simpledeathbans.banned", banEntry.getRemainingTimeFormatted())
            .setStyle(Style.EMPTY.withColor(Formatting.RED));
        player.networkHandler.disconnect(kickMessage);
    }
    
    private static void performGhostEcho(ServerPlayerEntity player, BanDataManager.BanEntry banEntry) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        
        // Spawn cosmetic lightning at death location (no damage, no fire)
        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
        lightning.refreshPositionAfterTeleport(player.getX(), player.getY(), player.getZ());
        lightning.setCosmetic(true); // No damage, no fire
        world.spawnEntity(lightning);
        
        // Broadcast custom death message in gray italics
        // This replaces the standard death/disconnect message
        String banTime = banEntry.getRemainingTimeFormatted();
        Text deathMessage = Text.literal(player.getName().getString() + " has been lost to the void for " + banTime + ".")
            .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true));
        
        world.getServer().getPlayerManager().broadcast(deathMessage, false);
    }
}
