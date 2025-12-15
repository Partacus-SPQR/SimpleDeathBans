package com.simpledeathbans.mixin.client;

import com.simpledeathbans.client.SinglePlayerBanHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to freeze player movement and actions when banned in single-player.
 */
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    
    /**
     * Cancel all player ticks when banned - this freezes movement, attacking, etc.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Only freeze in single-player when banned
        if (client.isInSingleplayer() && SinglePlayerBanHandler.isBanned()) {
            // Still need to call super minimal tick to prevent crashes
            // but cancel the rest of the tick to freeze movement
            ci.cancel();
        }
    }
}
