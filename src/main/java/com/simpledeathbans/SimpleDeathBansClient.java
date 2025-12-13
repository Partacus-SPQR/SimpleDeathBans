package com.simpledeathbans;

import com.simpledeathbans.config.ModMenuIntegration;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleDeathBansClient implements ClientModInitializer {
    public static final String MOD_ID = "simpledeathbans";
    public static final Logger LOGGER = LoggerFactory.getLogger("SimpleDeathBans-Client");
    
    // Define a custom category for our keybindings (1.21.11 API)
    private static final KeyBinding.Category SIMPLEDEATHBANS_CATEGORY =
            new KeyBinding.Category(Identifier.of(MOD_ID, "category"));
    
    // Keybindings - all unbound by default
    public static KeyBinding openConfigKeyBinding;
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Simple Death Bans Client");
        
        // Register keybindings with Category-based constructor (1.21.11 API)
        // Keybinding is unbound by default - users can set their own in Controls
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
        
        LOGGER.info("Simple Death Bans Client initialized successfully!");
    }
}
