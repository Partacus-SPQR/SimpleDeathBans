package com.simpledeathbans.event;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.damage.SoulSeverDamageSource;
import com.simpledeathbans.data.SoulLinkManager;
import com.simpledeathbans.item.ModItems;
import com.simpledeathbans.util.DamageShareTracker;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles Soul Link mechanics:
 * - Auto-assignment of partners on join (if random partner mode)
 * - Manual partner selection via shift+right-click (if not random mode)
 * - Damage sharing between linked players
 * - Totem saves partner feature
 */
public class SoulLinkEventHandler {
    
    // Cooldown tracker to prevent duplicate interactions (player UUID -> last interaction time)
    private static final Map<UUID, Long> interactionCooldowns = new HashMap<>();
    private static final long INTERACTION_COOLDOWN_MS = 500; // 500ms cooldown
    
    public static void register() {
        // Register damage event for soul link damage sharing
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer player)) {
                return true; // Allow damage for non-players
            }
            
            SimpleDeathBans mod = SimpleDeathBans.getInstance();
            if (mod == null) return true;
            
            ModConfig config = mod.getConfig();
            SoulLinkManager soulLinkManager = mod.getSoulLinkManager();
            
            if (config == null || !config.enableSoulLink || soulLinkManager == null) {
                return true;
            }
            
            // MUTUAL EXCLUSIVITY: If Shared Health is enabled, Soul Link damage sharing is disabled
            if (config.enableSharedHealth) {
                return true; // Let SharedHealthHandler handle all damage sharing
            }
            
            // SINGLE-PLAYER: Skip soul link entirely (no other players possible)
            ServerLevel world = (ServerLevel) player.level();
            if (world.getServer().isSingleplayer()) {
                return true;
            }
            
            // Don't share soul sever damage (prevents infinite loops)
            if (SoulSeverDamageSource.isSoulSever(source)) {
                return true;
            }
            
            // Prevent recursive damage sharing (GLOBAL check across all damage share handlers)
            UUID playerId = player.getUUID();
            if (DamageShareTracker.isProcessing(playerId)) {
                return true;
            }
            
            // Share damage to partner (mixin handles totem saves for lethal damage)
            // soulLinkDamageSharePercent: 100 = 100% of damage shared (1:1 ratio)
            float sharedDamage = (float) (amount * config.soulLinkDamageSharePercent / 100.0);
            shareDamageToPartner(player, soulLinkManager, sharedDamage);
            
            return true; // Allow original damage
        });
        
        // Register shift+right-click on player for manual soul linking
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide() || hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            
            if (!(entity instanceof ServerPlayer targetPlayer)) {
                return InteractionResult.PASS;
            }
            
            // Check if shift+right-clicking
            if (!player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            
            // Cooldown check to prevent duplicate messages
            UUID playerId = serverPlayer.getUUID();
            long currentTime = System.currentTimeMillis();
            Long lastInteraction = interactionCooldowns.get(playerId);
            if (lastInteraction != null && (currentTime - lastInteraction) < INTERACTION_COOLDOWN_MS) {
                return InteractionResult.SUCCESS; // Still on cooldown, silently ignore
            }
            interactionCooldowns.put(playerId, currentTime);
            
            SimpleDeathBans mod = SimpleDeathBans.getInstance();
            if (mod == null) return InteractionResult.PASS;
            
            ModConfig config = mod.getConfig();
            SoulLinkManager soulLinkManager = mod.getSoulLinkManager();
            
            // Only works if soul link is enabled AND random partner is OFF
            if (config == null || !config.enableSoulLink || config.soulLinkRandomPartner || soulLinkManager == null) {
                return InteractionResult.PASS;
            }
            
            // REQUIRE Soul Link Totem item to be held in main hand
            ItemStack heldItem = serverPlayer.getMainHandItem();
            if (!heldItem.is(ModItems.SOUL_LINK_TOTEM)) {
                return InteractionResult.PASS; // Not holding Soul Link Totem, let other mods handle this
            }
            
            UUID targetId = targetPlayer.getUUID();
            String playerName = serverPlayer.getName().getString();
            String targetName = targetPlayer.getName().getString();
            
            // CASE 1: Check if player already has a partner
            if (soulLinkManager.hasPartner(playerId)) {
                Optional<UUID> partnerUuid = soulLinkManager.getPartner(playerId);
                
                // Check if clicking on their existing partner - show status
                if (partnerUuid.isPresent() && partnerUuid.get().equals(targetId)) {
                    serverPlayer.sendSystemMessage(
                        Component.literal("§5✦ Your souls are intertwined with §d§l" + targetName + " §5✦"));
                    return InteractionResult.SUCCESS;
                }
                
                // Clicking on someone else - show who they're bound to
                String partnerName = partnerUuid.map(uuid -> {
                    ServerPlayer p = world.getServer().getPlayerList().getPlayer(uuid);
                    return p != null ? p.getName().getString() : "unknown";
                }).orElse("unknown");
                
                serverPlayer.sendSystemMessage(
                    Component.literal("Your soul is already bound to ")
                        .append(Component.literal(partnerName).withStyle(ChatFormatting.GOLD))
                        .append(Component.literal("."))
                        .withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            
            // CASE 1.5: Check sever cooldown (cannot link with ANYONE)
            if (soulLinkManager.isOnSeverCooldown(playerId)) {
                long remaining = soulLinkManager.getSeverCooldownRemaining(playerId);
                serverPlayer.sendSystemMessage(
                    Component.literal("§5✦ §7Your soul is still healing... §c" + remaining + " minutes §7remaining §5✦"));
                return InteractionResult.FAIL;
            }
            
            // CASE 1.6: Check if target is on sever cooldown
            if (soulLinkManager.isOnSeverCooldown(targetId)) {
                serverPlayer.sendSystemMessage(
                    Component.literal("§5✦ §7" + targetName + "'s soul is still healing... §5✦"));
                return InteractionResult.FAIL;
            }
            
            // CASE 1.7: Check ex-partner cooldown
            if (soulLinkManager.isOnExPartnerCooldown(playerId, targetId)) {
                serverPlayer.sendSystemMessage(
                    Component.literal("§5✦ §7The wounds between your souls are still fresh... §5✦"));
                return InteractionResult.FAIL;
            }
            
            // CASE 2: Check if target already has a partner (and it's not us)
            if (soulLinkManager.hasPartner(targetId)) {
                serverPlayer.sendSystemMessage(
                    Component.literal(targetName + "'s soul is already bound to another.").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            
            // CASE 3: Check if target has already sent us a request (mutual consent!)
            if (soulLinkManager.hasPendingRequest(targetId, playerId)) {
                // They requested us, we're accepting - CREATE THE LINK!
                soulLinkManager.removePendingRequest(targetId);
                soulLinkManager.removePendingRequest(playerId);
                soulLinkManager.createLink(playerId, targetId);
                
                // Notify both players with new mystical message
                serverPlayer.sendSystemMessage(
                    Component.literal("§5✦ Your souls are now intertwined with §d§l" + targetName + " §5✦"));
                targetPlayer.sendSystemMessage(
                    Component.literal("§5✦ Your souls are now intertwined with §d§l" + playerName + " §5✦"));
                
                // Play binding sound to both
                ServerLevel serverWorld = (ServerLevel) world;
                serverWorld.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 1.0f, 0.8f);
                serverWorld.playSound(null, targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(),
                    SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 1.0f, 0.8f);
                
                SimpleDeathBans.LOGGER.info("Soul link manually created (mutual consent): {} <-> {}", playerName, targetName);
                
                return InteractionResult.SUCCESS;
            }
            
            // CASE 4: Send a link request to target
            soulLinkManager.addPendingRequest(playerId, targetId);
            
            // Notify player that request was sent
            serverPlayer.sendSystemMessage(
                Component.literal("Soul link request sent to ")
                    .append(Component.literal(targetName).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(". They must Shift+Right-click you to accept."))
                    .withStyle(ChatFormatting.DARK_PURPLE));
            
            // Notify target that they received a request
            targetPlayer.sendSystemMessage(
                Component.literal("")
                    .append(Component.literal(playerName).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" wants to bind souls with you! "))
                    .append(Component.literal("Shift+Right-click").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                    .append(Component.literal(" them to accept."))
                    .withStyle(ChatFormatting.DARK_PURPLE));
            
            // Play subtle sound to indicate request sent
            ServerLevel serverWorld = (ServerLevel) world;
            serverWorld.playSound(null, targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
            
            SimpleDeathBans.LOGGER.info("Soul link request: {} -> {}", playerName, targetName);
            
            return InteractionResult.SUCCESS;
        });
    }
    
    /**
     * Share damage to soul-linked partner.
     * Only shares to partner if they're online, alive, and in a different position.
     */
    private static void shareDamageToPartner(ServerPlayer damagedPlayer, 
                                              SoulLinkManager soulLinkManager, 
                                              double damageAmount) {
        UUID playerId = damagedPlayer.getUUID();
        
        soulLinkManager.getPartner(playerId).ifPresent(partnerUuid -> {
            // Prevent self-link damage (safety check)
            if (playerId.equals(partnerUuid)) {
                SimpleDeathBans.LOGGER.warn("Player {} is linked to themselves - skipping damage share", damagedPlayer.getName().getString());
                return;
            }
            
            ServerLevel world = (ServerLevel) damagedPlayer.level();
            ServerPlayer partner = world.getServer().getPlayerList().getPlayer(partnerUuid);
            
            if (partner != null && partner.isAlive()) {
                // Mark as processing GLOBALLY to prevent cross-handler recursion
                DamageShareTracker.markProcessing(partnerUuid);
                try {
                    ServerLevel partnerWorld = (ServerLevel) partner.level();
                    // Use magic damage for soul link damage sharing (0.5 hearts = 1.0 damage)
                    partner.hurtServer(partnerWorld, partnerWorld.damageSources().magic(), (float) damageAmount);
                } finally {
                    DamageShareTracker.clearProcessing(partnerUuid);
                }
            }
        });
    }
    
    /**
     * Called when a player joins to:
     * 1. Auto-assign a soul partner if none exists (if random partner mode)
     * 2. Notify them of active soul link
     * 3. Tell them to shift+right-click to bind (if manual partner mode)
     */
    public static void onPlayerJoin(ServerPlayer player, SoulLinkManager soulLinkManager) {
        if (soulLinkManager == null) return;
        
        SimpleDeathBans mod = SimpleDeathBans.getInstance();
        ModConfig config = mod != null ? mod.getConfig() : null;
        
        // Skip soul link messages in single-player (no other players possible)
        ServerLevel world = (ServerLevel) player.level();
        if (world.getServer().isSingleplayer()) {
            return; // Soul link is meaningless in single-player
        }
        
        UUID playerId = player.getUUID();
        
        // Check if already has a partner
        if (soulLinkManager.hasPartner(playerId)) {
            // Notify of existing link with new mystical message
            soulLinkManager.getPartner(playerId).ifPresent(partnerUuid -> {
                // Get partner name if online
                ServerPlayer partner = world.getServer().getPlayerList().getPlayer(partnerUuid);
                String partnerName = partner != null ? partner.getName().getString() : "unknown";
                
                player.sendSystemMessage(
                    Component.literal("§5✦ Your souls are intertwined with §d§l" + partnerName + " §5✦"));
                
                // Play subtle sound
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5f, 1.2f);
            });
            return;
        }
        
        // Check if random partner mode is enabled
        boolean randomMode = config == null || config.soulLinkRandomPartner;
        
        if (randomMode) {
            // Check if on cooldown - show message but don't try to assign
            if (soulLinkManager.isOnSeverCooldown(playerId)) {
                long remaining = soulLinkManager.getSeverCooldownRemaining(playerId);
                player.sendSystemMessage(
                    Component.literal("§5✦ §7Your soul is still healing... §c" + remaining + " minutes §7remaining §5✦"));
                return;
            }
            
            // Try to auto-assign a partner from the waiting pool
            Optional<UUID> assignedPartner = soulLinkManager.tryAssignPartner(playerId);
            
            if (assignedPartner.isPresent()) {
                // Successfully paired!
                ServerPlayer partner = world.getServer().getPlayerList().getPlayer(assignedPartner.get());
                
                if (partner != null) {
                    String partnerName = partner.getName().getString();
                    String playerName = player.getName().getString();
                    
                    // Notify both players with new mystical message
                    player.sendSystemMessage(
                        Component.literal("§5✦ Your souls are now intertwined with §d§l" + partnerName + " §5✦"));
                    partner.sendSystemMessage(
                        Component.literal("§5✦ Your souls are now intertwined with §d§l" + playerName + " §5✦"));
                    
                    // Play binding sound to both
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 1.0f, 0.8f);
                    
                    ServerLevel partnerWorld = (ServerLevel) partner.level();
                    partnerWorld.playSound(null, partner.getX(), partner.getY(), partner.getZ(),
                        SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 1.0f, 0.8f);
                    
                    SimpleDeathBans.LOGGER.info("Soul link auto-assigned: {} <-> {}", playerName, partnerName);
                }
            } else {
                // Added to waiting pool or on random reassign cooldown - show waiting message
                // Randomize between two messages
                String waitingMessage = Math.random() < 0.5 
                    ? "§5✦ §7Your soul yearns for a bond... §5✦"
                    : "§k><§r §5Your soul wanders the void, seeking another... §k><§r";
                player.sendSystemMessage(Component.literal(waitingMessage));
            }
        } else {
            // Manual partner mode - tell player how to link with Soul Link Totem
            player.sendSystemMessage(
                Component.literal("Soul Link: ").withStyle(ChatFormatting.DARK_PURPLE)
                    .append(Component.literal("Shift+Right-click").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                    .append(Component.literal(" a player with a ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("Soul Link Totem").withStyle(ChatFormatting.LIGHT_PURPLE))
                    .append(Component.literal(" to request a soul link.").withStyle(ChatFormatting.GRAY)));
        }
    }
}
