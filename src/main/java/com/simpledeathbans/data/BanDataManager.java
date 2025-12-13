package com.simpledeathbans.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.simpledeathbans.SimpleDeathBans;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ban data including active bans and tier history.
 */
public class BanDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final Path dataPath;
    private final Map<UUID, BanEntry> activeBans = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> tierHistory = new ConcurrentHashMap<>();
    
    // Track players who have returned from a ban (for announcement)
    private final Set<UUID> recentlyExpiredBans = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    public BanDataManager(MinecraftServer server) {
        this.dataPath = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT)
            .resolve("simpledeathbans");
        load();
    }
    
    /**
     * Ban entry record containing all ban information.
     */
    public record BanEntry(
        UUID playerId,
        String playerName,
        int banTier,
        long banStartTime,
        long banEndTime
    ) {
        public boolean isExpired() {
            return System.currentTimeMillis() >= banEndTime;
        }
        
        public long getRemainingTime() {
            return Math.max(0, banEndTime - System.currentTimeMillis());
        }
        
        public String getRemainingTimeFormatted() {
            long remaining = getRemainingTime();
            long minutes = remaining / 60000;
            long seconds = (remaining % 60000) / 1000;
            
            if (minutes > 60) {
                long hours = minutes / 60;
                minutes = minutes % 60;
                return String.format("%dh %dm", hours, minutes);
            }
            return String.format("%dm %ds", minutes, seconds);
        }
    }
    
    /**
     * Creates a ban for the specified player.
     */
    public BanEntry createBan(UUID playerId, String playerName, int banMinutes) {
        // Increment tier
        int currentTier = tierHistory.getOrDefault(playerId, 0) + 1;
        int maxTier = SimpleDeathBans.getInstance().getConfig().maxBanTier;
        currentTier = Math.min(currentTier, maxTier);
        
        tierHistory.put(playerId, currentTier);
        
        long now = System.currentTimeMillis();
        long duration = banMinutes * 60000L;
        
        BanEntry entry = new BanEntry(playerId, playerName, currentTier, now, now + duration);
        activeBans.put(playerId, entry);
        
        // Track that this player will need an announcement when they return
        recentlyExpiredBans.add(playerId);
        
        save();
        return entry;
    }
    
    /**
     * Check if a returning player was previously banned and announce their return.
     */
    public void checkAndAnnounceReturn(ServerPlayerEntity player, MinecraftServer server) {
        UUID playerId = player.getUuid();
        
        // Check if this player had an expired ban we should announce
        if (recentlyExpiredBans.remove(playerId)) {
            // Player had a ban that expired - announce their return!
            String playerName = player.getName().getString();
            
            Text returnMessage = Text.literal("✦ ")
                .append(Text.literal(playerName).formatted(Formatting.GOLD))
                .append(Text.literal(" has found their way back from the void!"))
                .formatted(Formatting.GRAY, Formatting.ITALIC);
            
            server.getPlayerManager().broadcast(returnMessage, false);
            
            // Play a mystical sound for the returning player
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1.0f, 1.2f);
            
            SimpleDeathBans.LOGGER.info("{} has returned from ban (tier {})", playerName, tierHistory.getOrDefault(playerId, 0));
        }
    }
    
    /**
     * Checks if a player is currently banned.
     */
    public boolean isBanned(UUID playerId) {
        BanEntry entry = activeBans.get(playerId);
        if (entry == null) return false;
        
        if (entry.isExpired()) {
            activeBans.remove(playerId);
            save();
            return false;
        }
        return true;
    }
    
    /**
     * Gets ban entry for a player if they are banned.
     */
    public BanEntry getEntry(UUID playerId) {
        BanEntry entry = activeBans.get(playerId);
        if (entry != null && entry.isExpired()) {
            activeBans.remove(playerId);
            save();
            return null;
        }
        return entry;
    }
    
    /**
     * Gets the current tier for a player.
     */
    public int getTier(UUID playerId) {
        return tierHistory.getOrDefault(playerId, 0);
    }
    
    /**
     * Sets the tier for a player (admin command).
     */
    public void setTier(UUID playerId, String playerName, int tier) {
        tierHistory.put(playerId, tier);
        save();
    }
    
    /**
     * Decrements the tier for a player (mercy cooldown).
     */
    public void decrementTier(UUID playerId) {
        int current = tierHistory.getOrDefault(playerId, 0);
        if (current > 0) {
            tierHistory.put(playerId, current - 1);
            save();
        }
    }
    
    /**
     * Unbans a player (admin command).
     */
    public boolean unbanPlayer(UUID playerId) {
        BanEntry removed = activeBans.remove(playerId);
        if (removed != null) {
            save();
            return true;
        }
        return false;
    }
    
    /**
     * Unbans a player by name (for offline players).
     */
    public boolean unbanPlayerByName(String playerName) {
        UUID toRemove = null;
        for (Map.Entry<UUID, BanEntry> entry : activeBans.entrySet()) {
            if (entry.getValue().playerName().equalsIgnoreCase(playerName)) {
                toRemove = entry.getKey();
                break;
            }
        }
        if (toRemove != null) {
            activeBans.remove(toRemove);
            save();
            return true;
        }
        return false;
    }
    
    /**
     * Gets a random banned player for resurrection ritual.
     */
    public BanEntry getRandomBannedPlayer() {
        // Clean up expired bans first
        activeBans.values().removeIf(BanEntry::isExpired);
        
        if (activeBans.isEmpty()) return null;
        
        List<BanEntry> entries = new ArrayList<>(activeBans.values());
        return entries.get(new Random().nextInt(entries.size()));
    }
    
    /**
     * Gets all currently banned players.
     */
    public List<BanEntry> getAllBannedPlayers() {
        // Clean up expired bans first
        activeBans.values().removeIf(BanEntry::isExpired);
        return new ArrayList<>(activeBans.values());
    }
    
    /**
     * Clears all active bans (admin command).
     */
    public void clearAllBans() {
        activeBans.clear();
        save();
    }
    
    /**
     * Calculate ban minutes based on tier and config.
     * Linear mode: baseBan * tier * multiplier
     * Exponential mode: baseBan * 2^(tier-1) * multiplier (doubles each death)
     */
    public int calculateBanMinutes(UUID playerId, boolean isPvP) {
        int tier = tierHistory.getOrDefault(playerId, 0) + 1;
        var config = SimpleDeathBans.getInstance().getConfig();
        
        double pvpPveMultiplier = isPvP ? config.pvpBanMultiplier : config.pveBanMultiplier;
        
        double banTime;
        if (config.exponentialBanMode) {
            // Exponential: 1, 2, 4, 8, 16, 32... (2^(tier-1))
            // Cap at tier 30 to prevent overflow (2^30 = ~1 billion minutes)
            int cappedTier = Math.min(tier, 30);
            banTime = config.baseBanMinutes * Math.pow(2, cappedTier - 1) * config.banMultiplier * pvpPveMultiplier;
        } else {
            // Linear: 1, 2, 3, 4, 5...
            banTime = config.baseBanMinutes * tier * config.banMultiplier * pvpPveMultiplier;
        }
        
        return (int) Math.ceil(banTime);
    }
    
    public void load() {
        try {
            Files.createDirectories(dataPath);
            
            // Load active bans
            Path bansFile = dataPath.resolve("bans.json");
            if (Files.exists(bansFile)) {
                String json = Files.readString(bansFile);
                Type type = new TypeToken<Map<String, BanEntryData>>(){}.getType();
                Map<String, BanEntryData> data = GSON.fromJson(json, type);
                if (data != null) {
                    data.forEach((key, value) -> {
                        UUID id = UUID.fromString(key);
                        activeBans.put(id, new BanEntry(id, value.playerName, value.banTier, value.banStartTime, value.banEndTime));
                    });
                }
            }
            
            // Load tier history
            Path tiersFile = dataPath.resolve("tiers.json");
            if (Files.exists(tiersFile)) {
                String json = Files.readString(tiersFile);
                Type type = new TypeToken<Map<String, Integer>>(){}.getType();
                Map<String, Integer> data = GSON.fromJson(json, type);
                if (data != null) {
                    data.forEach((key, value) -> tierHistory.put(UUID.fromString(key), value));
                }
            }
        } catch (IOException e) {
            SimpleDeathBans.LOGGER.error("Failed to load ban data", e);
        }
    }
    
    public void save() {
        try {
            Files.createDirectories(dataPath);
            
            // Save active bans
            Map<String, BanEntryData> bansData = new HashMap<>();
            activeBans.forEach((id, entry) -> 
                bansData.put(id.toString(), new BanEntryData(entry.playerName, entry.banTier, entry.banStartTime, entry.banEndTime)));
            Files.writeString(dataPath.resolve("bans.json"), GSON.toJson(bansData));
            
            // Save tier history
            Map<String, Integer> tiersData = new HashMap<>();
            tierHistory.forEach((id, tier) -> tiersData.put(id.toString(), tier));
            Files.writeString(dataPath.resolve("tiers.json"), GSON.toJson(tiersData));
        } catch (IOException e) {
            SimpleDeathBans.LOGGER.error("Failed to save ban data", e);
        }
    }
    
    // Helper class for JSON serialization
    private record BanEntryData(String playerName, int banTier, long banStartTime, long banEndTime) {}
}
