package com.simpledeathbans.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Mixin to access ServerGamePacketListenerImpl internals.
 * Ban disconnect tracking is handled by BanDisconnectTracker utility class.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerPlayNetworkHandlerMixin {
    
    @Shadow
    public ServerPlayer player;
}
