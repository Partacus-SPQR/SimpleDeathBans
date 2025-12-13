package com.simpledeathbans.mixin;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.data.BanDataManager;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

/**
 * Mixin to check for bans when players try to connect.
 */
@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    
    /**
     * Check if the player is banned before allowing them to join
     */
    @Inject(method = "checkCanJoin", at = @At("HEAD"), cancellable = true)
    private void onCheckCanJoin(SocketAddress address, com.mojang.authlib.GameProfile profile, CallbackInfoReturnable<Text> cir) {
        SimpleDeathBans instance = SimpleDeathBans.getInstance();
        if (instance == null) return;
        
        BanDataManager banManager = instance.getBanDataManager();
        if (banManager == null) return;
        
        BanDataManager.BanEntry entry = banManager.getEntry(profile.id());
        if (entry != null && !entry.isExpired()) {
            String timeFormatted = entry.getRemainingTimeFormatted();
            
            Text banMessage = Text.literal("§cYou are banned from this server!\n\n")
                .append(Text.literal("§7Time remaining: §f" + timeFormatted + "\n"))
                .append(Text.literal("§7Ban Tier: §f" + entry.banTier() + "\n\n"))
                .append(Text.literal("§8Death results in temporary bans.\n"))
                .append(Text.literal("§8Your ban tier increases with each death."));
            
            cir.setReturnValue(banMessage);
        }
    }
}
