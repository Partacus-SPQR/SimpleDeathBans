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
import com.simpledeathbans.ritual.ResurrectionRitualManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleDeathBans implements ModInitializer {
    public static final String MOD_ID = "simpledeathbans";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static SimpleDeathBans instance;
    private ModConfig config;
    private BanDataManager banDataManager;
    private PlayerDataManager playerDataManager;
    private SoulLinkManager soulLinkManager;
    private ResurrectionRitualManager ritualManager;
    
    @Override
    public void onInitialize() {
        instance = this;
        LOGGER.info("Simple Death Bans initializing...");
        
        // Load config
        config = ModConfig.load();
        
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
        
        LOGGER.info("Simple Death Bans initialized!");
    }
    
    public static SimpleDeathBans getInstance() {
        return instance;
    }
    
    public ModConfig getConfig() {
        return config;
    }
    
    public void reloadConfig() {
        config = ModConfig.load();
    }
    
    public void saveConfig() {
        if (config != null) {
            config.save();
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
