package com.simpledeathbans.mixin;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Mixin to access ServerPlayNetworkHandler internals.
 * Ban disconnect tracking is handled by BanDisconnectTracker utility class.
 */
@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    
    @Shadow
    public ServerPlayerEntity player;
}
