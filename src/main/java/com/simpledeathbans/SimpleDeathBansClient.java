package com.simpledeathbans;

import com.simpledeathbans.config.ModConfig;
import com.simpledeathbans.config.ModMenuIntegration;
import com.simpledeathbans.network.ConfigSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
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
        });
        
        // Register client-side config sync handler to receive server updates
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ModConfig config = SimpleDeathBans.getInstance().getConfig();
                if (config != null) {
                    // Update local config with server's values
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
                    
                    LOGGER.info("Received config update from server - enableSoulLink: {}, enableSharedHealth: {}, enableMercyCooldown: {}",
                        config.enableSoulLink, config.enableSharedHealth, config.enableMercyCooldown);
                }
            });
        });
        
        LOGGER.info("Simple Death Bans Client initialized successfully!");
    }
}
