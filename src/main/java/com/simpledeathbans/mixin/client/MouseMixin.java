package com.simpledeathbans.mixin.client;

import com.simpledeathbans.client.SinglePlayerBanHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
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
@Mixin(MouseHandler.class)
public class MouseMixin {
    
    @Shadow @Final private Minecraft minecraft;
    
    /**
     * Block mouse button clicks when banned (unless in a screen).
     */
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, MouseButtonInfo mouseInput, int action, CallbackInfo ci) {
        if (minecraft.hasSingleplayerServer() && SinglePlayerBanHandler.isBanned() && minecraft.screen == null) {
            ci.cancel();
        }
    }
    
    /**
     * Block mouse movement (camera rotation) when banned (unless in a screen).
     */
    @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
    private void onCursorPos(long window, double x, double y, CallbackInfo ci) {
        if (minecraft.hasSingleplayerServer() && SinglePlayerBanHandler.isBanned() && minecraft.screen == null) {
            ci.cancel();
        }
    }
    
    /**
     * Block scroll wheel when banned (unless in a screen).
     */
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (minecraft.hasSingleplayerServer() && SinglePlayerBanHandler.isBanned() && minecraft.screen == null) {
            ci.cancel();
        }
    }
}
