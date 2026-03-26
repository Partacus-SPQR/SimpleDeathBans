package com.simpledeathbans.mixin;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.data.BanDataManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.NameAndId;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;
import java.util.UUID;

/**
 * Mixin to check for bans when players try to connect.
 * 
 * Note: All supported MC versions (1.21.9+) use PlayerConfigEntry for checkCanJoin.
 * 
 * SINGLE-PLAYER HANDLING:
 * We SKIP ban enforcement in this mixin for single-player because the integrated 
 * server has already loaded the world before checkCanJoin is called. Blocking the
 * player at this point causes file lock issues and world save corruption.
 * 
 * Single-player bans are instead enforced client-side via SinglePlayerBanPayload:
 * - Server sends payload to client on death
 * - Client properly saves world and disconnects to title screen
 * - On next join attempt, ban is checked but not enforced here (player sees title screen)
 */
@Mixin(PlayerList.class)
public class PlayerManagerMixin {
    
    @Shadow @Final private MinecraftServer server;
    
    /**
     * Check if the player is banned before allowing them to join.
     * All supported versions (1.21.9+) use PlayerConfigEntry.
     * 
     * MULTIPLAYER ONLY: In single-player, we skip ban enforcement entirely
     * to prevent world corruption from blocking player join after server start.
     */
    @Inject(method = "canPlayerLogin", at = @At("HEAD"), cancellable = true)
    private void onCheckCanJoin(SocketAddress address, NameAndId configEntry, CallbackInfoReturnable<Component> cir) {
        UUID playerId = configEntry.id();
        
        // CRITICAL: Skip ban enforcement in single-player to prevent world corruption
        // Single-player bans are handled via client-side payload (SinglePlayerBanPayload)
        // which triggers a proper client disconnect with world save
        if (server.isSingleplayer()) {
            SimpleDeathBans.LOGGER.debug("Skipping ban check in single-player - handled client-side");
            return;
        }
        
        SimpleDeathBans instance = SimpleDeathBans.getInstance();
        if (instance == null) return;
        
        BanDataManager banManager = instance.getBanDataManager();
        if (banManager == null) return;
        
        BanDataManager.BanEntry entry = banManager.getEntry(playerId);
        if (entry != null && !entry.isExpired()) {
            String timeFormatted = entry.getRemainingTimeFormatted();
            
            // Styled ban message with obfuscated header, dark purple theme
            Component banMessage = Component.empty()
                .append(Component.literal("§c§k><§r §4§lBANNED §c§k><§r\n\n"))
                .append(Component.literal("§5You have been claimed by the void.\n\n"))
                .append(Component.literal("§7Time remaining: §c" + timeFormatted + "§r\n"))
                .append(Component.literal("§7Ban Tier: §c" + entry.banTier() + "§r\n\n"))
                .append(Component.literal("§8Death results in temporary bans.\n"))
                .append(Component.literal("§8Your ban tier increases with each death."));
            
            cir.setReturnValue(banMessage);
        }
    }
}
