package com.simpledeathbans.mixin;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.data.BanDataManager;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to suppress the default "left the game" message when a player is banned.
 */
@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    
    @Shadow
    public ServerPlayerEntity player;
    
    // This flag indicates if the disconnect was due to our ban system
    private static final ThreadLocal<Boolean> isBanDisconnect = ThreadLocal.withInitial(() -> false);
    
    public static void markBanDisconnect() {
        isBanDisconnect.set(true);
    }
    
    public static boolean isBanDisconnect() {
        return isBanDisconnect.get();
    }
    
    public static void clearBanDisconnect() {
        isBanDisconnect.set(false);
    }
}
