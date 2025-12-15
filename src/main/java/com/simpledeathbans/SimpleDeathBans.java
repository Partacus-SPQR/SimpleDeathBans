package com.simpledeathbans;

import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.data.BanDataManager;
import com.simpledeathbans.data.PlayerDataManager;
import com.simpledeathbans.data.SoulLinkManager;
import com.simpledeathbans.event.BlockInteractionHandler;
import com.simpledeathbans.event.DeathEventHandler;
import com.simpledeathbans.event.MercyCooldownHandler;
import com.simpledeathbans.event.SharedHealthHandler;
import com.simpledeathbans.event.SoulLinkCooldownHandler;
import com.simpledeathbans.event.SoulLinkEventHandler;
import com.simpledeathbans.item.ModItems;
import com.simpledeathbans.command.ModCommands;
import com.simpledeathbans.network.ConfigSyncPayload;
import com.simpledeathbans.network.SinglePlayerBanPayload;
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
            // Check soul link cooldowns for auto-reassignment
            if (soulLinkManager != null && config != null && config.enableSoulLink) {
                SoulLinkCooldownHandler.onServerTick(server);
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
        
        // Register single-player ban payload (server to client only)
        PayloadTypeRegistry.playS2C().register(SinglePlayerBanPayload.ID, SinglePlayerBanPayload.CODEC);
        
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
                    config.banMultiplierPercent = payload.banMultiplierPercent();
                    config.maxBanTier = payload.maxBanTier();
                    config.exponentialBanMode = payload.exponentialBanMode();
                    config.enableGhostEcho = payload.enableGhostEcho();
                    
                    // MUTUAL EXCLUSIVITY: Handle Soul Link and Shared Health
                    // Compare what the user WANTS vs what is CURRENTLY set
                    boolean wantsSoulLink = payload.enableSoulLink();
                    boolean wantsSharedHealth = payload.enableSharedHealth();
                    boolean currentSoulLink = config.enableSoulLink;
                    boolean currentSharedHealth = config.enableSharedHealth;
                    
                    boolean soulLinkBlocked = false;
                    boolean soulLinkOverridden = false;  // When Shared Health actively disables Soul Link
                    boolean soulLinkEnabled = false;     // When Soul Link is newly enabled
                    boolean sharedHealthEnabled = false; // When Shared Health is newly enabled
                    
                    // Case 1: User is trying to enable Shared Health (wasn't on before, now wants it on)
                    if (wantsSharedHealth && !currentSharedHealth) {
                        config.enableSharedHealth = true;
                        sharedHealthEnabled = true;
                        
                        // If Soul Link was on, it gets overridden
                        if (currentSoulLink) {
                            config.enableSoulLink = false;
                            soulLinkOverridden = true;
                        } else {
                            config.enableSoulLink = false; // Can't enable Soul Link with Shared Health
                        }
                    }
                    // Case 2: Shared Health already on, user tries to enable Soul Link
                    else if (currentSharedHealth && wantsSharedHealth && wantsSoulLink && !currentSoulLink) {
                        // Block - must disable Shared Health first
                        config.enableSoulLink = false;
                        config.enableSharedHealth = true;
                        soulLinkBlocked = true;
                    }
                    // Case 3: User is enabling Soul Link (Shared Health is off)
                    else if (wantsSoulLink && !currentSoulLink && !wantsSharedHealth && !currentSharedHealth) {
                        config.enableSoulLink = true;
                        config.enableSharedHealth = false;
                        soulLinkEnabled = true;
                    }
                    // Case 4: Normal - just apply what user wants
                    else {
                        config.enableSoulLink = wantsSoulLink;
                        config.enableSharedHealth = wantsSharedHealth;
                    }
                    
                    config.soulLinkDamageSharePercent = payload.soulLinkDamageSharePercent();
                    config.soulLinkRandomPartner = payload.soulLinkRandomPartner();
                    config.soulLinkTotemSavesPartner = payload.soulLinkTotemSavesPartner();
                    config.soulLinkSeverCooldownMinutes = payload.soulLinkSeverCooldownMinutes();
                    config.soulLinkSeverBanTierIncrease = payload.soulLinkSeverBanTierIncrease();
                    config.soulLinkExPartnerCooldownHours = payload.soulLinkExPartnerCooldownHours();
                    config.soulLinkRandomReassignCooldownHours = payload.soulLinkRandomReassignCooldownHours();
                    config.soulLinkRandomAssignCheckIntervalMinutes = payload.soulLinkRandomAssignCheckIntervalMinutes();
                    config.soulLinkCompassMaxUses = payload.soulLinkCompassMaxUses();
                    config.soulLinkCompassCooldownMinutes = payload.soulLinkCompassCooldownMinutes();
                    config.sharedHealthDamagePercent = payload.sharedHealthDamagePercent();
                    config.sharedHealthTotemSavesAll = payload.sharedHealthTotemSavesAll();
                    config.enableMercyCooldown = payload.enableMercyCooldown();
                    config.mercyPlaytimeHours = payload.mercyPlaytimeHours();
                    config.mercyMovementBlocks = payload.mercyMovementBlocks();
                    config.mercyBlockInteractions = payload.mercyBlockInteractions();
                    config.mercyCheckIntervalMinutes = payload.mercyCheckIntervalMinutes();
                    config.pvpBanMultiplierPercent = payload.pvpBanMultiplierPercent();
                    config.pveBanMultiplierPercent = payload.pveBanMultiplierPercent();
                    config.enableResurrectionAltar = payload.enableResurrectionAltar();
                    config.singlePlayerEnabled = payload.singlePlayerEnabled();
                    
                    config.save();
                    
                    // Send mutual exclusivity messages to server
                    if (soulLinkOverridden) {
                        // Soul Link was actively disabled by enabling Shared Health
                        Text msg1 = Text.literal("\u00a7k><\u00a7r ")
                            .append(Text.literal("Soul Link has been disabled.").formatted(Formatting.RED))
                            .append(Text.literal(" \u00a7k><\u00a7r"));
                        context.server().getPlayerManager().broadcast(msg1, false);
                        
                        Text msg2 = Text.literal("\u00a7k><\u00a7r ")
                            .append(Text.literal("Shared Health has been enabled.").formatted(Formatting.GREEN))
                            .append(Text.literal(" \u00a7k><\u00a7r"));
                        context.server().getPlayerManager().broadcast(msg2, false);
                        LOGGER.info("Soul Link disabled, Shared Health enabled by {}", player.getName().getString());
                    } else if (sharedHealthEnabled) {
                        // Just Shared Health enabled (Soul Link wasn't on)
                        Text serverMsg = Text.literal("\u00a7k><\u00a7r ")
                            .append(Text.literal("Shared Health has been enabled.").formatted(Formatting.GREEN))
                            .append(Text.literal(" \u00a7k><\u00a7r"));
                        context.server().getPlayerManager().broadcast(serverMsg, false);
                        LOGGER.info("Shared Health enabled by {}", player.getName().getString());
                    }
                    
                    if (soulLinkEnabled) {
                        // Soul Link newly enabled
                        Text serverMsg = Text.literal("\u00a7k><\u00a7r ")
                            .append(Text.literal("Soul Link has been enabled.").formatted(Formatting.GREEN))
                            .append(Text.literal(" \u00a7k><\u00a7r"));
                        context.server().getPlayerManager().broadcast(serverMsg, false);
                        LOGGER.info("Soul Link enabled by {}", player.getName().getString());
                    }
                    
                    if (soulLinkBlocked) {
                        // Tried to enable Soul Link while Shared Health is on - just tell them no
                        Text serverMsg = Text.literal("\u00a7k><\u00a7r ")
                            .append(Text.literal("Must disable Shared Health before enabling Soul Link.").formatted(Formatting.RED))
                            .append(Text.literal(" \u00a7k><\u00a7r"));
                        player.sendMessage(serverMsg, false);
                        LOGGER.info("Soul Link enable blocked - Shared Health is active (attempted by {})", player.getName().getString());
                    }
                    
                    LOGGER.info("Config updated by operator {} - baseBanMinutes: {}, enableSoulLink: {}, enableSharedHealth: {}, enableMercyCooldown: {}",
                        player.getName().getString(),
                        config.baseBanMinutes,
                        config.enableSoulLink,
                        config.enableSharedHealth,
                        config.enableMercyCooldown);
                    
                    // Broadcast updated config to ALL online players so they see the changes
                    ConfigSyncPayload broadcastPayload = new ConfigSyncPayload(
                        config.baseBanMinutes,
                        config.banMultiplierPercent,
                        config.maxBanTier,
                        config.exponentialBanMode,
                        config.enableGhostEcho,
                        config.enableSoulLink,
                        config.soulLinkDamageSharePercent,
                        config.soulLinkRandomPartner,
                        config.soulLinkTotemSavesPartner,
                        config.soulLinkSeverCooldownMinutes,
                        config.soulLinkSeverBanTierIncrease,
                        config.soulLinkExPartnerCooldownHours,
                        config.soulLinkRandomReassignCooldownHours,
                        config.soulLinkRandomAssignCheckIntervalMinutes,
                        config.soulLinkCompassMaxUses,
                        config.soulLinkCompassCooldownMinutes,
                        config.enableSharedHealth,
                        config.sharedHealthDamagePercent,
                        config.sharedHealthTotemSavesAll,
                        config.enableMercyCooldown,
                        config.mercyPlaytimeHours,
                        config.mercyMovementBlocks,
                        config.mercyBlockInteractions,
                        config.mercyCheckIntervalMinutes,
                        config.pvpBanMultiplierPercent,
                        config.pveBanMultiplierPercent,
                        config.enableResurrectionAltar,
                        config.singlePlayerEnabled
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
