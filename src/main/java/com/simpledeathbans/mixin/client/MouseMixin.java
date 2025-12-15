package com.simpledeathbans.mixin.client;

import com.simpledeathbans.client.SinglePlayerBanHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to block mouse input when banned in single-player.
 * Allows input when screens are open (for pause menu, chat, etc.)
 */
@Mixin(Mouse.class)
public class MouseMixin {
    
    @Shadow @Final private MinecraftClient client;
    
    /**
     * Block mouse button clicks when banned (unless in a screen).
     */
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, MouseInput mouseInput, int action, CallbackInfo ci) {
        if (client.isInSingleplayer() && SinglePlayerBanHandler.isBanned() && client.currentScreen == null) {
            ci.cancel();
        }
    }
    
    /**
     * Block mouse movement (camera rotation) when banned (unless in a screen).
     */
    @Inject(method = "onCursorPos", at = @At("HEAD"), cancellable = true)
    private void onCursorPos(long window, double x, double y, CallbackInfo ci) {
        if (client.isInSingleplayer() && SinglePlayerBanHandler.isBanned() && client.currentScreen == null) {
            ci.cancel();
        }
    }
    
    /**
     * Block scroll wheel when banned (unless in a screen).
     */
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (client.isInSingleplayer() && SinglePlayerBanHandler.isBanned() && client.currentScreen == null) {
            ci.cancel();
        }
    }
}
