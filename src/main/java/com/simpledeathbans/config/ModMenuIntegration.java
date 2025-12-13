package com.simpledeathbans.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModMenuIntegration implements ModMenuApi {
    private static final Logger LOGGER = LoggerFactory.getLogger("SimpleDeathBans");
    
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> getConfigScreen(parent);
    }
    
    /**
     * Gets the appropriate config screen based on available dependencies.
     * Uses Cloth Config if available, falls back to vanilla widgets otherwise.
     */
    public static Screen getConfigScreen(Screen parent) {
        // Try Cloth Config first
        if (isClothConfigAvailable()) {
            try {
                return ClothConfigScreen.create(parent);
            } catch (Throwable e) {
                LOGGER.warn("Failed to create Cloth Config screen, using fallback", e);
            }
        }
        
        LOGGER.info("Using fallback config screen (Cloth Config unavailable or incompatible)");
        return new FallbackConfigScreen(parent);
    }
    
    /**
     * Checks if Cloth Config is available at runtime.
     */
    public static boolean isClothConfigAvailable() {
        try {
            Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
