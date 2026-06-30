package com.simpledeathbans;

import com.simpledeathbans.client.SinglePlayerBanHandler;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.config.ModMenuIntegration;
import com.simpledeathbans.network.ConfigSyncPayload;
import com.simpledeathbans.network.SinglePlayerBanPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//?} else {
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
*///?}
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.simpledeathbans.compat.ScreenCompat;
import net.minecraft.network.chat.Component;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;*/
//?}
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleDeathBansClient implements ClientModInitializer {
    public static final String MOD_ID = "simpledeathbans";
    public static final Logger LOGGER = LoggerFactory.getLogger("SimpleDeathBans-Client");
    
    // Define a custom category for our keybindings
    //? if >=1.21.11 {
    private static final KeyMapping.Category SIMPLEDEATHBANS_CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(MOD_ID, "category"));
    //?} else {
    /*private static final KeyMapping.Category SIMPLEDEATHBANS_CATEGORY =
            new KeyMapping.Category(ResourceLocation.fromNamespaceAndPath(MOD_ID, "category"));*/
    //?}
    
    // Keybindings - all unbound by default
    public static KeyMapping openConfigKeyBinding;
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Simple Death Bans Client");
        
        // Register keybindings
        // Keybinding is unbound by default (GLFW_KEY_UNKNOWN) - users can set their own in Controls
        //? if >=26.1 {
        openConfigKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.simpledeathbans.config",
                GLFW.GLFW_KEY_UNKNOWN, // Unbound by default
                SIMPLEDEATHBANS_CATEGORY
        ));
        //?} else {
        /*openConfigKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.simpledeathbans.config",
                GLFW.GLFW_KEY_UNKNOWN, // Unbound by default
                SIMPLEDEATHBANS_CATEGORY
        ));*/
        //?}
        
        // Register tick event for keybinding handling
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Open config screen
            while (openConfigKeyBinding.consumeClick()) {
                if (ScreenCompat.current(client) == null) {
                    ScreenCompat.open(client, ModMenuIntegration.getConfigScreen(null));
                }
            }
            
            // Tick the single-player ban handler to check for expiration
            SinglePlayerBanHandler.tick();
        });
        
        // Register handler for single-player ban payload
        ClientPlayNetworking.registerGlobalReceiver(SinglePlayerBanPayload.ID, (payload, context) -> {
            LOGGER.info("Received single-player ban notification - tier: {}, duration: {}ms, formatted: {}", 
                payload.banTier(), payload.banDurationMs(), payload.timeFormatted());
            
            // Activate the client-side freeze system
            SinglePlayerBanHandler.activateBan(payload.banTier(), payload.banDurationMs(), payload.timeFormatted());
        });
        
        // Register world join event to load persisted ban data (single-player)
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Delay slightly to ensure world is fully loaded
            if (client.hasSingleplayerServer()) {
                LOGGER.info("Single-player world join detected - will check for persisted ban data");
                // Use a small delay to ensure the world is fully initialized
                client.execute(() -> {
                    // Additional delay using execute to ensure we're after the initial tick
                    SinglePlayerBanHandler.onWorldJoin();
                });
            }
        });
        
        // Register world disconnect event to reset in-memory state
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.info("World disconnect detected - resetting in-memory ban state");
            SinglePlayerBanHandler.onWorldLeave();
        });
        
        // Register HUD render callback to draw the ban overlay
        //? if >=26.1 {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "ban_overlay"), (context, tickCounter) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.hasSingleplayerServer() && SinglePlayerBanHandler.isBannedRaw()) {
                int screenWidth = client.getWindow().getGuiScaledWidth();
                int screenHeight = client.getWindow().getGuiScaledHeight();
                int centerX = screenWidth / 2;
                int centerY = screenHeight / 2;
                context.fill(0, 0, screenWidth, screenHeight, 0xAA000000);
                var textRenderer = client.font;
                if (textRenderer != null) {
                    String title = "BANNED";
                    context.text(textRenderer, title, centerX - textRenderer.width(title) / 2, centerY - 60, 0xFFFF5555, true);
                    String subtitle = "You have been claimed by the void.";
                    context.text(textRenderer, subtitle, centerX - textRenderer.width(subtitle) / 2, centerY - 40, 0xFFAA00AA, true);
                    String timeLabel = "Time remaining:";
                    context.text(textRenderer, timeLabel, centerX - textRenderer.width(timeLabel) / 2, centerY - 10, 0xFFAAAAAA, true);
                    String timeValue = SinglePlayerBanHandler.getRemainingTimeFormatted();
                    context.text(textRenderer, timeValue, centerX - textRenderer.width(timeValue) / 2, centerY + 5, 0xFFFF5555, true);
                    String tier = "Ban Tier: " + SinglePlayerBanHandler.getBanTier();
                    context.text(textRenderer, tier, centerX - textRenderer.width(tier) / 2, centerY + 30, 0xFFAAAAAA, true);
                    String hint = "Open Mod Menu -> Simple Death Bans -> disable Single-Player Mode";
                    context.text(textRenderer, hint, centerX - textRenderer.width(hint) / 2, centerY + 60, 0xFF555555, true);
                }
            }
        });
        //?} else {
        /*HudRenderCallback.EVENT.register((context, tickCounter) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.hasSingleplayerServer() && SinglePlayerBanHandler.isBannedRaw()) {
                int screenWidth = client.getWindow().getGuiScaledWidth();
                int screenHeight = client.getWindow().getGuiScaledHeight();
                int centerX = screenWidth / 2;
                int centerY = screenHeight / 2;
                context.fill(0, 0, screenWidth, screenHeight, 0xAA000000);
                var textRenderer = client.font;
                if (textRenderer != null) {
                    String title = "BANNED";
                    context.drawString(textRenderer, title, centerX - textRenderer.width(title) / 2, centerY - 60, 0xFFFF5555, true);
                    String subtitle = "You have been claimed by the void.";
                    context.drawString(textRenderer, subtitle, centerX - textRenderer.width(subtitle) / 2, centerY - 40, 0xFFAA00AA, true);
                    String timeLabel = "Time remaining:";
                    context.drawString(textRenderer, timeLabel, centerX - textRenderer.width(timeLabel) / 2, centerY - 10, 0xFFAAAAAA, true);
                    String timeValue = SinglePlayerBanHandler.getRemainingTimeFormatted();
                    context.drawString(textRenderer, timeValue, centerX - textRenderer.width(timeValue) / 2, centerY + 5, 0xFFFF5555, true);
                    String tier = "Ban Tier: " + SinglePlayerBanHandler.getBanTier();
                    context.drawString(textRenderer, tier, centerX - textRenderer.width(tier) / 2, centerY + 30, 0xFFAAAAAA, true);
                    String hint = "Open Mod Menu -> Simple Death Bans -> disable Single-Player Mode";
                    context.drawString(textRenderer, hint, centerX - textRenderer.width(hint) / 2, centerY + 60, 0xFF555555, true);
                }
            }
        });
        *///?}
        
        // Register client-side config sync handler to receive server updates
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ModConfig config = SimpleDeathBans.getInstance().getConfig();
                if (config != null) {
                    // Update local config with server's values
                    config.enableDeathBans = payload.enableDeathBans();
                    config.baseBanMinutes = payload.baseBanMinutes();
                    config.banMultiplierPercent = payload.banMultiplierPercent();
                    config.maxBanTier = payload.maxBanTier();
                    config.exponentialBanMode = payload.exponentialBanMode();
                    config.enableGhostEcho = payload.enableGhostEcho();
                    config.enableSoulLink = payload.enableSoulLink();
                    config.soulLinkDamageSharePercent = payload.soulLinkDamageSharePercent();
                    config.soulLinkShareHunger = payload.soulLinkShareHunger();
                    config.soulLinkRandomPartner = payload.soulLinkRandomPartner();
                    config.soulLinkTotemSavesPartner = payload.soulLinkTotemSavesPartner();
                    config.soulLinkSeverCooldownMinutes = payload.soulLinkSeverCooldownMinutes();
                    config.soulLinkSeverBanTierIncrease = payload.soulLinkSeverBanTierIncrease();
                    config.soulLinkExPartnerCooldownHours = payload.soulLinkExPartnerCooldownHours();
                    config.soulLinkRandomReassignCooldownHours = payload.soulLinkRandomReassignCooldownHours();
                    config.soulLinkRandomAssignCheckIntervalMinutes = payload.soulLinkRandomAssignCheckIntervalMinutes();
                    config.soulLinkCompassMaxUses = payload.soulLinkCompassMaxUses();
                    config.soulLinkCompassCooldownMinutes = payload.soulLinkCompassCooldownMinutes();
                    config.enableSharedHealth = payload.enableSharedHealth();
                    config.sharedHealthDamagePercent = payload.sharedHealthDamagePercent();
                    config.sharedHealthShareHunger = payload.sharedHealthShareHunger();
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
                    
                    LOGGER.info("Received config update from server - enableDeathBans: {}, enableSoulLink: {}, enableSharedHealth: {}",
                        config.enableDeathBans, config.enableSoulLink, config.enableSharedHealth);
                }
            });
        });
        
        LOGGER.info("Simple Death Bans Client initialized successfully!");
    }
}
