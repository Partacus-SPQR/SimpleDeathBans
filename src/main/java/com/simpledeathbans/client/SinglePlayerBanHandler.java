package com.simpledeathbans.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.simpledeathbans.SimpleDeathBans;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles single-player ban state on the client side.
 * Instead of disconnecting (which causes world corruption), we "freeze" the player
 * and display a ban screen overlay until the timer expires.
 * 
 * Ban state is persisted to file so players cannot bypass by quitting and rejoining.
 */
public class SinglePlayerBanHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("SimpleDeathBans-Client");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Ban state
    private static boolean isBanned = false;
    private static long banEndTime = 0;
    private static int banTier = 0;
    private static String initialTimeFormatted = "";
    private static String currentWorldName = null;
    
    // Persistence data class
    private static class BanData {
        long banEndTime;
        int banTier;
        String worldName;
        
        BanData(long banEndTime, int banTier, String worldName) {
            this.banEndTime = banEndTime;
            this.banTier = banTier;
            this.worldName = worldName;
        }
    }
    
    /**
     * Gets the path to the ban data file for the current world.
     */
    private static Path getBanDataPath() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() != null) {
            return client.getServer().getSavePath(net.minecraft.util.WorldSavePath.ROOT)
                    .resolve("simpledeathbans_singleplayer_ban.json");
        }
        return null;
    }
    
    /**
     * Activates the ban freeze state and persists it to file.
     */
    public static void activateBan(int tier, long durationMs, String timeFormatted) {
        isBanned = true;
        banTier = tier;
        banEndTime = System.currentTimeMillis() + durationMs;
        initialTimeFormatted = timeFormatted;
        
        // Get current world name
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() != null) {
            currentWorldName = client.getServer().getSaveProperties().getLevelName();
        }
        
        LOGGER.info("Single-player ban freeze activated - tier: {}, duration: {}ms, ends at: {}", 
                tier, durationMs, banEndTime);
        
        // Persist to file so it survives quit/reload
        saveBanData();
        
        // Send fancy chat notification to player
        if (client.player != null) {
            client.player.sendMessage(Text.literal(""), false); // Empty line for spacing
            client.player.sendMessage(
                Text.literal("§c§k><§r §4§l☠ DEATH BAN ACTIVATED ☠ §c§k><§r"),
                false
            );
            client.player.sendMessage(
                Text.literal("§7Ban Tier: §c§l" + tier + " §7| Duration: §c" + timeFormatted),
                false
            );
            client.player.sendMessage(
                Text.literal("§9§lYou are FROZEN §r§7but §a§lIMMUNE§7 to damage."),
                false
            );
            client.player.sendMessage(
                Text.literal("§8This ban §npersists§8 if you save & quit - no escaping!"),
                false
            );
            client.player.sendMessage(
                Text.literal("§8To escape: §7Mod Menu§8 → disable §7Single-Player Mode§8."),
                false
            );
            client.player.sendMessage(Text.literal(""), false); // Empty line for spacing
        }
    }
    
    /**
     * Saves ban data to the world folder.
     */
    private static void saveBanData() {
        Path path = getBanDataPath();
        if (path == null) return;
        
        try {
            Files.createDirectories(path.getParent());
            BanData data = new BanData(banEndTime, banTier, currentWorldName);
            Files.writeString(path, GSON.toJson(data));
            LOGGER.info("Saved single-player ban data to {}", path);
        } catch (IOException e) {
            LOGGER.error("Failed to save single-player ban data", e);
        }
    }
    
    /**
     * Loads ban data from the world folder on world load.
     */
    public static void loadBanData() {
        Path path = getBanDataPath();
        if (path == null || !Files.exists(path)) {
            LOGGER.info("No existing single-player ban data found");
            return;
        }
        
        try {
            String json = Files.readString(path);
            BanData data = GSON.fromJson(json, BanData.class);
            
            if (data != null && data.banEndTime > System.currentTimeMillis()) {
                // Ban is still active
                isBanned = true;
                banEndTime = data.banEndTime;
                banTier = data.banTier;
                currentWorldName = data.worldName;
                
                long remainingMs = banEndTime - System.currentTimeMillis();
                LOGGER.info("Restored single-player ban - tier: {}, remaining: {}ms", banTier, remainingMs);
                
                // Show fancy message to player
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal(""), false);
                    client.player.sendMessage(
                        Text.literal("§c§k><§r §4§l☠ BAN RESTORED ☠ §c§k><§r"),
                        false
                    );
                    client.player.sendMessage(
                        Text.literal("§7Ban Tier: §c§l" + banTier + " §7| Remaining: §c" + getRemainingTimeFormatted()),
                        false
                    );
                    client.player.sendMessage(
                        Text.literal("§9§lYou are FROZEN §r§7but §a§lIMMUNE§7 to damage."),
                        false
                    );
                    client.player.sendMessage(
                        Text.literal("§8Nice try! Your ban was saved and restored."),
                        false
                    );
                    client.player.sendMessage(Text.literal(""), false);
                }
            } else {
                // Ban expired while offline - delete the file
                LOGGER.info("Single-player ban expired while offline, clearing data");
                deleteBanData();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load single-player ban data", e);
        }
    }
    
    /**
     * Deletes the ban data file.
     */
    private static void deleteBanData() {
        Path path = getBanDataPath();
        if (path != null && Files.exists(path)) {
            try {
                Files.delete(path);
                LOGGER.info("Deleted single-player ban data file");
            } catch (IOException e) {
                LOGGER.error("Failed to delete ban data file", e);
            }
        }
    }
    
    /**
     * Clears the ban state (called when ban expires or player uses /sdb unban).
     */
    public static void clearBan() {
        if (isBanned) {
            LOGGER.info("Single-player ban cleared");
        }
        isBanned = false;
        banEndTime = 0;
        banTier = 0;
        initialTimeFormatted = "";
        
        // Delete persisted data
        deleteBanData();
    }
    
    /**
     * Checks if the single-player mod is disabled in config.
     * If disabled, any existing ban should be immediately cleared.
     */
    private static boolean isModDisabledInConfig() {
        SimpleDeathBans mod = SimpleDeathBans.getInstance();
        if (mod != null && mod.getConfig() != null) {
            return !mod.getConfig().singlePlayerEnabled;
        }
        return false;
    }
    
    /**
     * Tick method called every client tick to check for ban expiration.
     */
    public static void tick() {
        if (!isBanned) return;
        
        // Check if mod was disabled in config - immediately clear ban
        if (isModDisabledInConfig()) {
            LOGGER.info("Single-player mod disabled in config - clearing ban immediately");
            isBanned = false;
            banEndTime = 0;
            banTier = 0;
            initialTimeFormatted = "";
            deleteBanData();
            
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal(""), false);
                client.player.sendMessage(
                    Text.literal("§a§k><§r §2§l✓ MOD DISABLED ✓ §a§k><§r"),
                    false
                );
                client.player.sendMessage(
                    Text.literal("§aSingle-player death bans have been §l§ndisabled§r§a."),
                    false
                );
                client.player.sendMessage(Text.literal(""), false);
            }
            return;
        }
        
        // Check if ban has expired
        if (System.currentTimeMillis() >= banEndTime) {
            LOGGER.info("Single-player ban expired via tick - unfreezing player");
            isBanned = false;
            banEndTime = 0;
            banTier = 0;
            initialTimeFormatted = "";
            
            // Delete persisted data
            deleteBanData();
            
            // Notify player with fancy message
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal(""), false);
                client.player.sendMessage(
                    Text.literal("§a§k><§r §2§l✓ BAN EXPIRED ✓ §a§k><§r"),
                    false
                );
                client.player.sendMessage(
                    Text.literal("§aYou are §l§nfree§r§a to continue playing. Stay alive!"),
                    false
                );
                client.player.sendMessage(Text.literal(""), false);
            }
        }
    }
    
    /**
     * Raw check if player is banned - does NOT trigger expiration logic.
     * Use this for rendering to avoid side effects.
     */
    public static boolean isBannedRaw() {
        // Also check if mod is disabled
        if (isModDisabledInConfig()) return false;
        return isBanned && System.currentTimeMillis() < banEndTime;
    }
    
    /**
     * Checks if the player is currently banned/frozen.
     * This method also handles expiration and sends messages.
     */
    public static boolean isBanned() {
        if (!isBanned) return false;
        
        // Check if mod was disabled in config
        if (isModDisabledInConfig()) {
            LOGGER.info("Single-player mod disabled - clearing ban");
            clearBan();
            return false;
        }
        
        // Check if ban has expired
        if (System.currentTimeMillis() >= banEndTime) {
            LOGGER.info("Single-player ban expired - unfreezing player");
            clearBan();
            
            // Notify player with styled message
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal(""), false);
                client.player.sendMessage(
                    Text.literal("§a§k><§r §2§l✓ BAN EXPIRED ✓ §a§k><§r"),
                    false
                );
                client.player.sendMessage(
                    Text.literal("§aYou are §l§nfree§r§a to continue playing. Stay alive!"),
                    false
                );
                client.player.sendMessage(Text.literal(""), false);
            }
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets remaining ban time in milliseconds.
     */
    public static long getRemainingTimeMs() {
        if (!isBanned) return 0;
        return Math.max(0, banEndTime - System.currentTimeMillis());
    }
    
    /**
     * Gets formatted remaining time string.
     */
    public static String getRemainingTimeFormatted() {
        long remainingMs = getRemainingTimeMs();
        if (remainingMs <= 0) return "0s";
        
        long totalSeconds = remainingMs / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        
        return sb.toString().trim();
    }
    
    /**
     * Gets the current ban tier.
     */
    public static int getBanTier() {
        return banTier;
    }
    
    /**
     * Renders the ban overlay screen.
     * NOTE: Uses raw isBanned flag to avoid triggering expiration check during render.
     */
    public static void renderBanOverlay(DrawContext context, int screenWidth, int screenHeight) {
        // Use raw flag check - isBanned() method may clear the ban!
        if (!isBanned) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.textRenderer == null) {
            LOGGER.warn("TextRenderer is null - cannot render overlay text");
            return;
        }
        
        // Draw semi-transparent dark background - 0xAA = ~67% opacity
        int backgroundColor = 0xAA000000;
        context.fill(0, 0, screenWidth, screenHeight, backgroundColor);
        
        // Calculate center positions
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        
        // Use the text renderer directly with drawText instead of drawCenteredTextWithShadow
        var textRenderer = client.textRenderer;
        
        // Title - "BANNED" - Red color
        String title = "BANNED";
        int titleWidth = textRenderer.getWidth(title);
        context.drawText(textRenderer, title, centerX - titleWidth / 2, centerY - 60, 0xFFFF5555, true);
        
        // Subtitle - Purple
        String subtitle = "You have been claimed by the void.";
        int subtitleWidth = textRenderer.getWidth(subtitle);
        context.drawText(textRenderer, subtitle, centerX - subtitleWidth / 2, centerY - 40, 0xFFAA00AA, true);
        
        // Time remaining label - Gray
        String timeLabel = "Time remaining:";
        int timeLabelWidth = textRenderer.getWidth(timeLabel);
        context.drawText(textRenderer, timeLabel, centerX - timeLabelWidth / 2, centerY - 10, 0xFFAAAAAA, true);
        
        // Time value - Red
        String timeValue = getRemainingTimeFormatted();
        int timeValueWidth = textRenderer.getWidth(timeValue);
        context.drawText(textRenderer, timeValue, centerX - timeValueWidth / 2, centerY + 5, 0xFFFF5555, true);
        
        // Ban tier - Gray with red number
        String tierText = "Ban Tier: " + banTier;
        int tierWidth = textRenderer.getWidth(tierText);
        context.drawText(textRenderer, tierText, centerX - tierWidth / 2, centerY + 30, 0xFFAAAAAA, true);
        
        // Info text - Dark gray
        String info1 = "Death results in temporary bans.";
        int info1Width = textRenderer.getWidth(info1);
        context.drawText(textRenderer, info1, centerX - info1Width / 2, centerY + 55, 0xFF555555, true);
        
        String info2 = "Your ban tier increases with each death.";
        int info2Width = textRenderer.getWidth(info2);
        context.drawText(textRenderer, info2, centerX - info2Width / 2, centerY + 67, 0xFF555555, true);
        
        // Hint about disabling mod - Dark gray
        String hint = "Open Mod Menu -> Simple Death Bans -> disable Single-Player Mode";
        int hintWidth = textRenderer.getWidth(hint);
        context.drawText(textRenderer, hint, centerX - hintWidth / 2, centerY + 90, 0xFF555555, true);
    }
    
    /**
     * Called when leaving a world - DO NOT clear ban state, it should persist!
     * Just reset the in-memory state, the file will remain for when they rejoin.
     */
    public static void onWorldLeave() {
        // Don't call clearBan() - we want the ban to persist!
        // Just reset the in-memory state
        isBanned = false;
        banEndTime = 0;
        banTier = 0;
        initialTimeFormatted = "";
        currentWorldName = null;
        LOGGER.info("World leave - in-memory ban state reset (file persisted)");
    }
    
    /**
     * Called when joining a world to check for persisted bans.
     */
    public static void onWorldJoin() {
        LOGGER.info("World join - checking for persisted ban data");
        // Small delay to ensure world is fully loaded
        MinecraftClient.getInstance().execute(() -> {
            loadBanData();
        });
    }
}
