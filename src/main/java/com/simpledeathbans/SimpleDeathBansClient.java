package com.simpledeathbans;

import com.simpledeathbans.client.SinglePlayerBanHandler;
import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.config.ModMenuIntegration;
import com.simpledeathbans.network.ConfigSyncPayload;
import com.simpledeathbans.network.SinglePlayerBanPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleDeathBansClient implements ClientModInitializer {
    public static final String MOD_ID = "simpledeathbans";
    public static final Logger LOGGER = LoggerFactory.getLogger("SimpleDeathBans-Client");
    
    // Define a custom category for our keybindings
    private static final KeyBinding.Category SIMPLEDEATHBANS_CATEGORY =
            new KeyBinding.Category(Identifier.of(MOD_ID, "category"));
    
    // Keybindings - all unbound by default
    public static KeyBinding openConfigKeyBinding;
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Simple Death Bans Client");
        
        // Register keybindings
        // Keybinding is unbound by default (GLFW_KEY_UNKNOWN) - users can set their own in Controls
        openConfigKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simpledeathbans.config",
                GLFW.GLFW_KEY_UNKNOWN, // Unbound by default
                SIMPLEDEATHBANS_CATEGORY
        ));
        
        // Register tick event for keybinding handling
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Open config screen
            while (openConfigKeyBinding.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(ModMenuIntegration.getConfigScreen(null));
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
            if (client.isInSingleplayer()) {
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
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.isInSingleplayer() && SinglePlayerBanHandler.isBannedRaw()) {
                int screenWidth = client.getWindow().getScaledWidth();
                int screenHeight = client.getWindow().getScaledHeight();
                
                // Draw directly here instead of in separate method
                int centerX = screenWidth / 2;
                int centerY = screenHeight / 2;
                
                // Dark background
                context.fill(0, 0, screenWidth, screenHeight, 0xAA000000);
                
                // Draw text using context.drawText
                var textRenderer = client.textRenderer;
                if (textRenderer != null) {
                    // Title
                    String title = "BANNED";
                    context.drawText(textRenderer, title, centerX - textRenderer.getWidth(title) / 2, centerY - 60, 0xFFFF5555, true);
                    
                    // Subtitle
                    String subtitle = "You have been claimed by the void.";
                    context.drawText(textRenderer, subtitle, centerX - textRenderer.getWidth(subtitle) / 2, centerY - 40, 0xFFAA00AA, true);
                    
                    // Time label
                    String timeLabel = "Time remaining:";
                    context.drawText(textRenderer, timeLabel, centerX - textRenderer.getWidth(timeLabel) / 2, centerY - 10, 0xFFAAAAAA, true);
                    
                    // Time value
                    String timeValue = SinglePlayerBanHandler.getRemainingTimeFormatted();
                    context.drawText(textRenderer, timeValue, centerX - textRenderer.getWidth(timeValue) / 2, centerY + 5, 0xFFFF5555, true);
                    
                    // Ban tier
                    String tier = "Ban Tier: " + SinglePlayerBanHandler.getBanTier();
                    context.drawText(textRenderer, tier, centerX - textRenderer.getWidth(tier) / 2, centerY + 30, 0xFFAAAAAA, true);
                    
                    // Hint
                    String hint = "Open Mod Menu -> Simple Death Bans -> disable Single-Player Mode";
                    context.drawText(textRenderer, hint, centerX - textRenderer.getWidth(hint) / 2, centerY + 60, 0xFF555555, true);
                }
            }
        });
        
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
