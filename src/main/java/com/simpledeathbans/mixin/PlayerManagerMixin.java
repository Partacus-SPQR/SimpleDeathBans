package com.simpledeathbans.mixin;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.data.BanDataManager;
import net.minecraft.server.PlayerManager;
//? if >=1.21.11
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;
import java.util.UUID;

/**
 * Mixin to check for bans when players try to connect.
 * 
 * Note: In 1.21.11+, checkCanJoin uses PlayerConfigEntry instead of GameProfile.
 * In single-player, bans are still recorded but not enforced (player uses /sdb unban).
 */
@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    
    /**
     * Check if the player is banned before allowing them to join
     * Uses PlayerConfigEntry in 1.21.11+, GameProfile in earlier versions.
     * 
     * SINGLE-PLAYER: We skip ban enforcement - the ban is recorded but can only 
     * be removed via /sdb unban with cheats enabled. This prevents world corruption
     * from forced disconnects in integrated server.
     */
    //? if >=1.21.11 {
    @Inject(method = "checkCanJoin", at = @At("HEAD"), cancellable = true)
    private void onCheckCanJoin(SocketAddress address, PlayerConfigEntry configEntry, CallbackInfoReturnable<Text> cir) {
        UUID playerId = configEntry.id();
    //?} else {
    /*@Inject(method = "checkCanJoin", at = @At("HEAD"), cancellable = true)
    private void onCheckCanJoin(SocketAddress address, com.mojang.authlib.GameProfile profile, CallbackInfoReturnable<Text> cir) {
        UUID playerId = profile.id();
    *///?}
        
        SimpleDeathBans instance = SimpleDeathBans.getInstance();
        if (instance == null) return;
        
        // Ban enforcement applies to both single-player and multiplayer
        // In single-player, players must use /sdb unban with cheats enabled to remove ban
        
        BanDataManager banManager = instance.getBanDataManager();
        if (banManager == null) return;
        
        BanDataManager.BanEntry entry = banManager.getEntry(playerId);
        if (entry != null && !entry.isExpired()) {
            String timeFormatted = entry.getRemainingTimeFormatted();
            
            // Styled ban message with obfuscated header, dark purple theme
            Text banMessage = Text.empty()
                .append(Text.literal("§c§k><§r §4§lBANNED §c§k><§r\n\n"))
                .append(Text.literal("§5You have been claimed by the void.\n\n"))
                .append(Text.literal("§7Time remaining: §c" + timeFormatted + "§r\n"))
                .append(Text.literal("§7Ban Tier: §c" + entry.banTier() + "§r\n\n"))
                .append(Text.literal("§8Death results in temporary bans.\n"))
                .append(Text.literal("§8Your ban tier increases with each death."));
            
            cir.setReturnValue(banMessage);
        }
    }
}
