package com.simpledeathbans.event;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.damage.SoulSeverDamageSource;
import com.simpledeathbans.data.SoulLinkManager;
import com.simpledeathbans.util.DamageShareTracker;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

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
    
    public static void register() {
        // Register damage event for soul link damage sharing
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) {
                return true; // Allow damage for non-players
            }
            
            SimpleDeathBans mod = SimpleDeathBans.getInstance();
            if (mod == null) return true;
            
            ModConfig config = mod.getConfig();
            SoulLinkManager soulLinkManager = mod.getSoulLinkManager();
            
            if (config == null || !config.enableSoulLink || soulLinkManager == null) {
                return true;
            }
            
            // SINGLE-PLAYER: Skip soul link entirely (no other players possible)
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            if (world.getServer().isSingleplayer()) {
                return true;
            }
            
            // Don't share soul sever damage (prevents infinite loops)
            if (SoulSeverDamageSource.isSoulSever(source)) {
                return true;
            }
            
            // Prevent recursive damage sharing (GLOBAL check across all damage share handlers)
            UUID playerId = player.getUuid();
            if (DamageShareTracker.isProcessing(playerId)) {
                return true;
            }
            
            // TOTEM CHECK: If this damage would be lethal and someone has a totem,
            // the totem will save both players (via LivingEntityMixin), so DON'T share damage
            float currentHealth = player.getHealth();
            if (amount >= currentHealth && config.soulLinkTotemSavesPartner) {
                // This would be lethal - check if either player has a totem
                boolean hasTotem = player.getStackInHand(Hand.MAIN_HAND).isOf(Items.TOTEM_OF_UNDYING) ||
                                   player.getStackInHand(Hand.OFF_HAND).isOf(Items.TOTEM_OF_UNDYING);
                
                if (hasTotem) {
                    // Player's totem will save both - don't share this damage
                    // The mixin will handle saving the partner
                    return true;
                }
                
                // Check if partner has totem (they would save this player)
                if (soulLinkManager.hasPartner(playerId)) {
                    Optional<UUID> partnerOpt = soulLinkManager.getPartner(playerId);
                    if (partnerOpt.isPresent()) {
                        ServerPlayerEntity partner = world.getServer().getPlayerManager().getPlayer(partnerOpt.get());
                        if (partner != null && partner.isAlive()) {
                            boolean partnerHasTotem = partner.getStackInHand(Hand.MAIN_HAND).isOf(Items.TOTEM_OF_UNDYING) ||
                                                      partner.getStackInHand(Hand.OFF_HAND).isOf(Items.TOTEM_OF_UNDYING);
                            if (partnerHasTotem) {
                                // Partner's totem will save both - don't share this damage
                                return true;
                            }
                        }
                    }
                }
            }
            
            // Share damage to partner (non-lethal or no totem protection)
            // soulLinkDamageSharePercent: 100 = 100% of damage shared (1:1 ratio)
            float sharedDamage = (float) (amount * config.soulLinkDamageSharePercent / 100.0);
            shareDamageToPartner(player, soulLinkManager, sharedDamage);
            
            return true; // Allow original damage
        });
        
        // Register shift+right-click on player for manual soul linking
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }
            
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }
            
            if (!(entity instanceof ServerPlayerEntity targetPlayer)) {
                return ActionResult.PASS;
            }
            
            // Check if shift+right-clicking
            if (!player.isSneaking()) {
                return ActionResult.PASS;
            }
            
            SimpleDeathBans mod = SimpleDeathBans.getInstance();
            if (mod == null) return ActionResult.PASS;
            
            ModConfig config = mod.getConfig();
            SoulLinkManager soulLinkManager = mod.getSoulLinkManager();
            
            // Only works if soul link is enabled AND random partner is OFF
            if (config == null || !config.enableSoulLink || config.soulLinkRandomPartner || soulLinkManager == null) {
                return ActionResult.PASS;
            }
            
            UUID playerId = serverPlayer.getUuid();
            UUID targetId = targetPlayer.getUuid();
            String playerName = serverPlayer.getName().getString();
            String targetName = targetPlayer.getName().getString();
            
            // CASE 1: Check if player already has a partner
            if (soulLinkManager.hasPartner(playerId)) {
                Optional<UUID> partnerUuid = soulLinkManager.getPartner(playerId);
                
                // Check if clicking on their existing partner - show status
                if (partnerUuid.isPresent() && partnerUuid.get().equals(targetId)) {
                    serverPlayer.sendMessage(
                        Text.literal("You are soul-linked with ")
                            .append(Text.literal(targetName).formatted(Formatting.GOLD))
                            .formatted(Formatting.DARK_PURPLE),
                        false
                    );
                    return ActionResult.SUCCESS;
                }
                
                // Clicking on someone else - show who they're bound to
                String partnerName = partnerUuid.map(uuid -> {
                    ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(uuid);
                    return p != null ? p.getName().getString() : "unknown";
                }).orElse("unknown");
                
                serverPlayer.sendMessage(
                    Text.literal("Your soul is already bound to ")
                        .append(Text.literal(partnerName).formatted(Formatting.GOLD))
                        .append(Text.literal("."))
                        .formatted(Formatting.RED),
                    false
                );
                return ActionResult.FAIL;
            }
            
            // CASE 2: Check if target already has a partner (and it's not us)
            if (soulLinkManager.hasPartner(targetId)) {
                serverPlayer.sendMessage(
                    Text.literal(targetName + "'s soul is already bound to another.").formatted(Formatting.RED),
                    false
                );
                return ActionResult.FAIL;
            }
            
            // CASE 3: Check if target has already sent us a request (mutual consent!)
            if (soulLinkManager.hasPendingRequest(targetId, playerId)) {
                // They requested us, we're accepting - CREATE THE LINK!
                soulLinkManager.removePendingRequest(targetId);
                soulLinkManager.removePendingRequest(playerId);
                soulLinkManager.createLink(playerId, targetId);
                
                // Notify both players
                Text linkMessage1 = Text.literal("Your soul has been bound to ")
                    .append(Text.literal(targetName).formatted(Formatting.GOLD))
                    .formatted(Formatting.DARK_PURPLE);
                Text linkMessage2 = Text.literal("Your soul has been bound to ")
                    .append(Text.literal(playerName).formatted(Formatting.GOLD))
                    .formatted(Formatting.DARK_PURPLE);
                
                serverPlayer.sendMessage(linkMessage1, false);
                targetPlayer.sendMessage(linkMessage2, false);
                
                // Play binding sound to both
                ServerWorld serverWorld = (ServerWorld) world;
                serverWorld.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.0f, 0.8f);
                serverWorld.playSound(null, targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(),
                    SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.0f, 0.8f);
                
                SimpleDeathBans.LOGGER.info("Soul link manually created (mutual consent): {} <-> {}", playerName, targetName);
                
                return ActionResult.SUCCESS;
            }
            
            // CASE 4: Send a link request to target
            soulLinkManager.addPendingRequest(playerId, targetId);
            
            // Notify player that request was sent
            serverPlayer.sendMessage(
                Text.literal("Soul link request sent to ")
                    .append(Text.literal(targetName).formatted(Formatting.GOLD))
                    .append(Text.literal(". They must Shift+Right-click you to accept."))
                    .formatted(Formatting.DARK_PURPLE),
                false
            );
            
            // Notify target that they received a request
            targetPlayer.sendMessage(
                Text.literal("")
                    .append(Text.literal(playerName).formatted(Formatting.GOLD))
                    .append(Text.literal(" wants to bind souls with you! "))
                    .append(Text.literal("Shift+Right-click").formatted(Formatting.GOLD, Formatting.BOLD))
                    .append(Text.literal(" them to accept."))
                    .formatted(Formatting.DARK_PURPLE),
                false
            );
            
            // Play subtle sound to indicate request sent
            ServerWorld serverWorld = (ServerWorld) world;
            serverWorld.playSound(null, targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(),
                SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.8f, 1.2f);
            
            SimpleDeathBans.LOGGER.info("Soul link request: {} -> {}", playerName, targetName);
            
            return ActionResult.SUCCESS;
        });
    }
    
    /**
     * Share damage to soul-linked partner.
     * Only shares to partner if they're online, alive, and in a different position.
     */
    private static void shareDamageToPartner(ServerPlayerEntity damagedPlayer, 
                                              SoulLinkManager soulLinkManager, 
                                              double damageAmount) {
        UUID playerId = damagedPlayer.getUuid();
        
        soulLinkManager.getPartner(playerId).ifPresent(partnerUuid -> {
            // Prevent self-link damage (safety check)
            if (playerId.equals(partnerUuid)) {
                SimpleDeathBans.LOGGER.warn("Player {} is linked to themselves - skipping damage share", damagedPlayer.getName().getString());
                return;
            }
            
            ServerWorld world = (ServerWorld) damagedPlayer.getEntityWorld();
            ServerPlayerEntity partner = world.getServer().getPlayerManager().getPlayer(partnerUuid);
            
            if (partner != null && partner.isAlive()) {
                // Mark as processing GLOBALLY to prevent cross-handler recursion
                DamageShareTracker.markProcessing(partnerUuid);
                try {
                    ServerWorld partnerWorld = (ServerWorld) partner.getEntityWorld();
                    // Use magic damage for soul link damage sharing (0.5 hearts = 1.0 damage)
                    partner.damage(partnerWorld, partnerWorld.getDamageSources().magic(), (float) damageAmount);
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
    public static void onPlayerJoin(ServerPlayerEntity player, SoulLinkManager soulLinkManager) {
        if (soulLinkManager == null) return;
        
        SimpleDeathBans mod = SimpleDeathBans.getInstance();
        ModConfig config = mod != null ? mod.getConfig() : null;
        
        // Skip soul link messages in single-player (no other players possible)
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        if (world.getServer().isSingleplayer()) {
            return; // Soul link is meaningless in single-player
        }
        
        UUID playerId = player.getUuid();
        
        // Check if already has a partner
        if (soulLinkManager.hasPartner(playerId)) {
            // Notify of existing link
            soulLinkManager.getPartner(playerId).ifPresent(partnerUuid -> {
                // Get partner name if online
                ServerPlayerEntity partner = world.getServer().getPlayerManager().getPlayer(partnerUuid);
                String partnerName = partner != null ? partner.getName().getString() : "unknown";
                
                player.sendMessage(
                    Text.literal("Your soul is linked to ")
                        .append(Text.literal(partnerName).formatted(Formatting.GOLD))
                        .formatted(Formatting.DARK_PURPLE),
                    false
                );
                
                // Play subtle sound
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.5f, 1.2f);
            });
            return;
        }
        
        // Check if random partner mode is enabled
        boolean randomMode = config == null || config.soulLinkRandomPartner;
        
        if (randomMode) {
            // Try to auto-assign a partner from the waiting pool
            Optional<UUID> assignedPartner = soulLinkManager.tryAssignPartner(playerId);
            
            if (assignedPartner.isPresent()) {
                // Successfully paired!
                ServerPlayerEntity partner = world.getServer().getPlayerManager().getPlayer(assignedPartner.get());
                
                if (partner != null) {
                    String partnerName = partner.getName().getString();
                    String playerName = player.getName().getString();
                    
                    // Notify both players
                    Text linkMessage1 = Text.literal("Your soul has been bound to ")
                        .append(Text.literal(partnerName).formatted(Formatting.GOLD))
                        .formatted(Formatting.DARK_PURPLE);
                    Text linkMessage2 = Text.literal("Your soul has been bound to ")
                        .append(Text.literal(playerName).formatted(Formatting.GOLD))
                        .formatted(Formatting.DARK_PURPLE);
                    
                    player.sendMessage(linkMessage1, false);
                    partner.sendMessage(linkMessage2, false);
                    
                    // Play binding sound to both
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.0f, 0.8f);
                    
                    ServerWorld partnerWorld = (ServerWorld) partner.getEntityWorld();
                    partnerWorld.playSound(null, partner.getX(), partner.getY(), partner.getZ(),
                        SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.0f, 0.8f);
                    
                    SimpleDeathBans.LOGGER.info("Soul link auto-assigned: {} <-> {}", playerName, partnerName);
                }
            } else {
                // Added to waiting pool
                player.sendMessage(
                    Text.literal("You await a soul partner...").formatted(Formatting.GRAY, Formatting.ITALIC),
                    false
                );
            }
        } else {
            // Manual partner mode - tell player how to link
            player.sendMessage(
                Text.literal("Soul Link: ").formatted(Formatting.DARK_PURPLE)
                    .append(Text.literal("Shift+Right-click").formatted(Formatting.GOLD, Formatting.BOLD))
                    .append(Text.literal(" a player to bind your souls together.").formatted(Formatting.GRAY)),
                false
            );
        }
    }
}
