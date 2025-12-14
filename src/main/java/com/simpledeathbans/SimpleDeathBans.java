package com.simpledeathbans;

import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.data.BanDataManager;
import com.simpledeathbans.data.PlayerDataManager;
import com.simpledeathbans.data.SoulLinkManager;
import com.simpledeathbans.event.BlockInteractionHandler;
import com.simpledeathbans.event.DeathEventHandler;
import com.simpledeathbans.event.MercyCooldownHandler;
import com.simpledeathbans.event.SharedHealthHandler;
import com.simpledeathbans.event.SoulLinkEventHandler;
import com.simpledeathbans.item.ModItems;
import com.simpledeathbans.command.ModCommands;
import com.simpledeathbans.network.ConfigSyncPayload;
import com.simpledeathbans.ritual.ResurrectionRitualManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleDeathBans implements ModInitializer {
    public static final String MOD_ID = "simpledeathbans";
    public static final Logger LOGGER = LoggerFactory.getLogger("SimpleDeathBans");
    
    private static SimpleDeathBans instance;
    private ModConfig config;
    private BanDataManager banDataManager;
    private PlayerDataManager playerDataManager;
    private SoulLinkManager soulLinkManager;
    private ResurrectionRitualManager ritualManager;
    
    @Override
    public void onInitialize() {
        instance = this;
        LOGGER.info("Initializing SimpleDeathBans...");
        
        // Load config
        config = ModConfig.load();
        
        // Register network payloads
        registerNetworking();
        
        // Register items
        ModItems.register();
        
        // Register commands
        ModCommands.register();
        
        // Server lifecycle events
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Loading Simple Death Bans data...");
            banDataManager = new BanDataManager(server);
            playerDataManager = new PlayerDataManager(server);
            soulLinkManager = new SoulLinkManager(server);
            ritualManager = new ResurrectionRitualManager(server);
            
            banDataManager.load();
            playerDataManager.load();
            soulLinkManager.load();
        });
        
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Saving Simple Death Bans data...");
            if (banDataManager != null) banDataManager.save();
            if (playerDataManager != null) playerDataManager.save();
            if (soulLinkManager != null) soulLinkManager.save();
        });
        
        // Player connection events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            
            // Check if player was previously banned (ban expired)
            if (banDataManager != null) {
                banDataManager.checkAndAnnounceReturn(player, server);
            }
            
            // Assign soul partner on join if enabled
            if (config.enableSoulLink && soulLinkManager != null) {
                SoulLinkEventHandler.onPlayerJoin(player, soulLinkManager);
            }
        });
        
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            // Clean up soul links on disconnect
            if (soulLinkManager != null) {
                soulLinkManager.onPlayerDisconnect(handler.getPlayer().getUuid());
            }
            // Cancel ritual if committed player disconnects
            if (ritualManager != null) {
                ritualManager.onPlayerDisconnect(handler.getPlayer().getUuid());
            }
        });
        
        // Server tick events for mercy cooldown
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (playerDataManager != null && config != null) {
                MercyCooldownHandler.onServerTick(server);
            }
        });
        
        // Register death event handler
        DeathEventHandler.register();
        
        // Register soul link damage sharing
        SoulLinkEventHandler.register();
        
        // Register server-wide shared health
        SharedHealthHandler.register();
        
        // Register block interaction tracking for mercy cooldown
        BlockInteractionHandler.register();
        
        LOGGER.info("SimpleDeathBans initialized successfully!");
    }
    
    /**
     * Register network payloads and handlers for config sync.
     * Server-side permission validation is MANDATORY - never trust client!
     */
    private void registerNetworking() {
        // Register the config sync payload type for both directions
        PayloadTypeRegistry.playC2S().register(ConfigSyncPayload.ID, ConfigSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ConfigSyncPayload.ID, ConfigSyncPayload.CODEC);
        
        // Register server-side handler with permission validation
        ServerPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            
            context.server().execute(() -> {
                // === MANDATORY SERVER-SIDE PERMISSION CHECK ===
                // Check operator status
                boolean isOp = context.server().isSingleplayer();
                
                if (!isOp) {
                    // In multiplayer, check operator status using player name
                    // OperatorList getNames() returns all operator names - works across versions
                    var opList = context.server().getPlayerManager().getOpList();
                    String[] opNames = opList.getNames();
                    String playerName = player.getName().getString();
                    for (String name : opNames) {
                        if (name.equalsIgnoreCase(playerName)) {
                            isOp = true;
                            break;
                        }
                    }
                }
                
                if (isOp) {
                    // Player IS an operator - apply config changes
                    config.baseBanMinutes = payload.baseBanMinutes();
                    config.banMultiplier = payload.banMultiplier();
                    config.maxBanTier = payload.maxBanTier();
                    config.exponentialBanMode = payload.exponentialBanMode();
                    config.enableGhostEcho = payload.enableGhostEcho();
                    config.enableSoulLink = payload.enableSoulLink();
                    config.soulLinkDamageShare = payload.soulLinkDamageShare();
                    config.soulLinkRandomPartner = payload.soulLinkRandomPartner();
                    config.soulLinkTotemSavesAll = payload.soulLinkTotemSavesAll();
                    config.enableSharedHealth = payload.enableSharedHealth();
                    config.sharedHealthDamagePercent = payload.sharedHealthDamagePercent();
                    config.sharedHealthTotemSavesAll = payload.sharedHealthTotemSavesAll();
                    config.enableMercyCooldown = payload.enableMercyCooldown();
                    config.mercyPlaytimeHours = payload.mercyPlaytimeHours();
                    config.mercyMovementBlocks = payload.mercyMovementBlocks();
                    config.mercyBlockInteractions = payload.mercyBlockInteractions();
                    config.mercyCheckIntervalMinutes = payload.mercyCheckIntervalMinutes();
                    config.pvpBanMultiplier = payload.pvpBanMultiplier();
                    config.pveBanMultiplier = payload.pveBanMultiplier();
                    config.enableResurrectionAltar = payload.enableResurrectionAltar();
                    
                    config.save();
                    
                    LOGGER.info("Config updated by operator {} - baseBanMinutes: {}, enableSoulLink: {}, enableSharedHealth: {}, enableMercyCooldown: {}",
                        player.getName().getString(),
                        config.baseBanMinutes,
                        config.enableSoulLink,
                        config.enableSharedHealth,
                        config.enableMercyCooldown);
                    
                    // Broadcast updated config to ALL online players so they see the changes
                    ConfigSyncPayload broadcastPayload = new ConfigSyncPayload(
                        config.baseBanMinutes,
                        config.banMultiplier,
                        config.maxBanTier,
                        config.exponentialBanMode,
                        config.enableGhostEcho,
                        config.enableSoulLink,
                        config.soulLinkDamageShare,
                        config.soulLinkRandomPartner,
                        config.soulLinkTotemSavesAll,
                        config.enableSharedHealth,
                        config.sharedHealthDamagePercent,
                        config.sharedHealthTotemSavesAll,
                        config.enableMercyCooldown,
                        config.mercyPlaytimeHours,
                        config.mercyMovementBlocks,
                        config.mercyBlockInteractions,
                        config.mercyCheckIntervalMinutes,
                        config.pvpBanMultiplier,
                        config.pveBanMultiplier,
                        config.enableResurrectionAltar
                    );
                    
                    for (ServerPlayerEntity onlinePlayer : context.server().getPlayerManager().getPlayerList()) {
                        ServerPlayNetworking.send(onlinePlayer, broadcastPayload);
                    }
                    
                    player.sendMessage(
                        Text.literal("✔ Configuration saved successfully.")
                            .formatted(Formatting.GREEN),
                        false
                    );
                } else {
                    // Player is NOT an operator - reject and log
                    LOGGER.warn("Non-operator {} attempted to modify config", player.getName().getString());
                    
                    player.sendMessage(
                        Text.literal("✖ Must be Operator level 4 in order to make changes.")
                            .formatted(Formatting.RED),
                        false
                    );
                }
            });
        });
        
        LOGGER.info("Network payloads registered");
    }
    
    public static SimpleDeathBans getInstance() {
        return instance;
    }
    
    public ModConfig getConfig() {
        return config;
    }
    
    public void reloadConfig() {
        config = ModConfig.load();
        LOGGER.info("Config reloaded from file");
    }
    
    public void saveConfig() {
        if (config != null) {
            config.save();
            LOGGER.info("Config saved to file");
        }
    }
    
    public BanDataManager getBanDataManager() {
        return banDataManager;
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    public SoulLinkManager getSoulLinkManager() {
        return soulLinkManager;
    }
    
    public ResurrectionRitualManager getRitualManager() {
        return ritualManager;
    }
}
