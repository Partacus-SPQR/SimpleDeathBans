package com.simpledeathbans.mixin.client;

import com.simpledeathbans.client.SinglePlayerBanHandler;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to block keyboard input when banned in single-player.
 * Allows ESC key for pause menu, F3 for debug, T for chat (to use /sdb unban),
 * ENTER for sending chat, and slash for commands.
 */
@Mixin(Keyboard.class)
public class KeyboardMixin {
    
    @Shadow @Final private MinecraftClient client;
    
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKeyPress(long window, int key, KeyInput keyInput, CallbackInfo ci) {
        // Only block in single-player when banned
        if (client.isInSingleplayer() && SinglePlayerBanHandler.isBanned()) {
            // Allow these keys for menu/chat access:
            // GLFW_KEY_ESCAPE = 256 (pause menu)
            // GLFW_KEY_F3 = 292 (debug menu)
            // GLFW_KEY_T = 84 (chat)
            // GLFW_KEY_SLASH = 47 (command)
            // GLFW_KEY_ENTER = 257 (send chat)
            // Also allow if a screen is open (like chat screen)
            if (client.currentScreen != null) {
                // Allow all input in screens (needed for typing in chat)
                return;
            }
            
            if (key != 256 && key != 292 && key != 84 && key != 47 && key != 257) {
                ci.cancel();
            }
        }
    }
}
