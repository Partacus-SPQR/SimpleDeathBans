package com.simpledeathbans.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Mixin for Gui - overlay rendering moved to HudRenderCallback in SimpleDeathBansClient.
 */
@Mixin(Gui.class)
public class InGameHudMixin {
    
    @Shadow @Final private Minecraft minecraft;
    
    // Ban overlay rendering is now handled by HudRenderCallback in SimpleDeathBansClient
    // This mixin is kept for potential future HUD modifications
}
