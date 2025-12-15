package com.simpledeathbans.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.config.ModConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Soul Link partnerships between players.
 */
public class SoulLinkManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final MinecraftServer server;
    private final Path dataFile;
    private final Path cooldownsFile;
    
    // Player UUID -> Partner UUID (bidirectional)
    private final Map<UUID, UUID> soulLinks = new ConcurrentHashMap<>();
    
    // Pool of online players waiting for a partner (random mode)
    private final Set<UUID> waitingPool = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // Pending link requests: requester UUID -> target UUID (manual mode)
    private final Map<UUID, UUID> pendingLinkRequests = new ConcurrentHashMap<>();
    
    // === COOLDOWN TRACKING (persisted) ===
    // Player UUID -> timestamp when sever cooldown ends (30 min - cannot link with ANYONE)
    private final Map<UUID, Long> severCooldowns = new ConcurrentHashMap<>();
    
    // Player UUID -> timestamp when random reassign cooldown ends (12 hours - won't be randomly assigned)
    private final Map<UUID, Long> randomReassignCooldowns = new ConcurrentHashMap<>();
    
    // "player1:player2" (sorted) -> timestamp when ex-partner cooldown ends (24 hours)
    private final Map<String, Long> exPartnerCooldowns = new ConcurrentHashMap<>();
    
    // Player UUID -> timestamp when compass cooldown ends (10 min)
    private final Map<UUID, Long> compassCooldowns = new ConcurrentHashMap<>();
    
    public SoulLinkManager(MinecraftServer server) {
        this.server = server;
        Path rootPath = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT);
        this.dataFile = rootPath.resolve("simpledeathbans_soullinks.json");
        this.cooldownsFile = rootPath.resolve("simpledeathbans_soullink_cooldowns.json");
    }
    
    public void load() {
        // Load soul links
        if (Files.exists(dataFile)) {
            try {
                String json = Files.readString(dataFile);
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                Map<String, String> loaded = GSON.fromJson(json, type);
                if (loaded != null) {
                    soulLinks.clear();
                    for (Map.Entry<String, String> entry : loaded.entrySet()) {
                        soulLinks.put(UUID.fromString(entry.getKey()), UUID.fromString(entry.getValue()));
                    }
                }
                SimpleDeathBans.LOGGER.info("Loaded {} soul links", soulLinks.size() / 2);
            } catch (IOException e) {
                SimpleDeathBans.LOGGER.error("Failed to load soul links", e);
            }
        }
        
        // Load cooldowns
        if (Files.exists(cooldownsFile)) {
            try {
                String json = Files.readString(cooldownsFile);
                Type type = new TypeToken<CooldownData>(){}.getType();
                CooldownData data = GSON.fromJson(json, type);
                if (data != null) {
                    long now = System.currentTimeMillis();
                    // Load and clean expired cooldowns
                    if (data.severCooldowns != null) {
                        data.severCooldowns.forEach((k, v) -> {
                            if (v > now) severCooldowns.put(UUID.fromString(k), v);
                        });
                    }
                    if (data.randomReassignCooldowns != null) {
                        data.randomReassignCooldowns.forEach((k, v) -> {
                            if (v > now) randomReassignCooldowns.put(UUID.fromString(k), v);
                        });
                    }
                    if (data.exPartnerCooldowns != null) {
                        data.exPartnerCooldowns.forEach((k, v) -> {
                            if (v > now) exPartnerCooldowns.put(k, v);
                        });
                    }
                }
                SimpleDeathBans.LOGGER.info("Loaded soul link cooldowns");
            } catch (IOException e) {
                SimpleDeathBans.LOGGER.error("Failed to load soul link cooldowns", e);
            }
        }
    }
    
    public void save() {
        // Save soul links
        try {
            Files.createDirectories(dataFile.getParent());
            Map<String, String> toSave = new HashMap<>();
            for (Map.Entry<UUID, UUID> entry : soulLinks.entrySet()) {
                toSave.put(entry.getKey().toString(), entry.getValue().toString());
            }
            Files.writeString(dataFile, GSON.toJson(toSave));
        } catch (IOException e) {
            SimpleDeathBans.LOGGER.error("Failed to save soul links", e);
        }
        
        // Save cooldowns
        saveCooldowns();
    }
    
    private void saveCooldowns() {
        try {
            Files.createDirectories(cooldownsFile.getParent());
            CooldownData data = new CooldownData();
            data.severCooldowns = new HashMap<>();
            data.randomReassignCooldowns = new HashMap<>();
            data.exPartnerCooldowns = new HashMap<>(exPartnerCooldowns);
            
            severCooldowns.forEach((k, v) -> data.severCooldowns.put(k.toString(), v));
            randomReassignCooldowns.forEach((k, v) -> data.randomReassignCooldowns.put(k.toString(), v));
            
            Files.writeString(cooldownsFile, GSON.toJson(data));
        } catch (IOException e) {
            SimpleDeathBans.LOGGER.error("Failed to save soul link cooldowns", e);
        }
    }
    
    // Helper class for JSON serialization
    private static class CooldownData {
        Map<String, Long> severCooldowns;
        Map<String, Long> randomReassignCooldowns;
        Map<String, Long> exPartnerCooldowns;
    }
    
    // === COOLDOWN HELPER METHODS ===
    
    /**
     * Create a consistent key for ex-partner pairs (sorted UUIDs)
     */
    private String getExPartnerKey(UUID player1, UUID player2) {
        String s1 = player1.toString();
        String s2 = player2.toString();
        return s1.compareTo(s2) < 0 ? s1 + ":" + s2 : s2 + ":" + s1;
    }
    
    /**
     * Check if a player is on sever cooldown (cannot link with ANYONE)
     */
    public boolean isOnSeverCooldown(UUID player) {
        Long cooldownEnd = severCooldowns.get(player);
        if (cooldownEnd == null) return false;
        if (System.currentTimeMillis() >= cooldownEnd) {
            severCooldowns.remove(player);
            return false;
        }
        return true;
    }
    
    /**
     * Get remaining sever cooldown time in minutes
     */
    public long getSeverCooldownRemaining(UUID player) {
        Long cooldownEnd = severCooldowns.get(player);
        if (cooldownEnd == null) return 0;
        long remaining = cooldownEnd - System.currentTimeMillis();
        return remaining > 0 ? remaining / 60000 : 0;
    }
    
    /**
     * Check if a player is on random reassign cooldown (won't be auto-assigned)
     */
    public boolean isOnRandomReassignCooldown(UUID player) {
        Long cooldownEnd = randomReassignCooldowns.get(player);
        if (cooldownEnd == null) return false;
        if (System.currentTimeMillis() >= cooldownEnd) {
            randomReassignCooldowns.remove(player);
            return false;
        }
        return true;
    }
    
    /**
     * Check if two players are on ex-partner cooldown (cannot be paired)
     */
    public boolean isOnExPartnerCooldown(UUID player1, UUID player2) {
        String key = getExPartnerKey(player1, player2);
        Long cooldownEnd = exPartnerCooldowns.get(key);
        if (cooldownEnd == null) return false;
        if (System.currentTimeMillis() >= cooldownEnd) {
            exPartnerCooldowns.remove(key);
            return false;
        }
        return true;
    }
    
    /**
     * Check if a player can link with a specific target (considering all cooldowns)
     */
    public boolean canLinkWith(UUID player, UUID target, ModConfig config) {
        // Check sever cooldown (blocks ALL linking)
        if (isOnSeverCooldown(player) || isOnSeverCooldown(target)) {
            return false;
        }
        // Check ex-partner cooldown
        if (isOnExPartnerCooldown(player, target)) {
            return false;
        }
        return true;
    }
    
    /**
     * Check if a player can be randomly assigned a partner
     */
    public boolean canBeRandomlyAssigned(UUID player, ModConfig config) {
        // Check sever cooldown first
        if (isOnSeverCooldown(player)) {
            return false;
        }
        // Check random reassign cooldown
        if (isOnRandomReassignCooldown(player)) {
            return false;
        }
        return true;
    }
    
    /**
     * Record a soul link severance - sets all relevant cooldowns
     */
    public void recordSever(UUID player, UUID exPartner, ModConfig config) {
        long now = System.currentTimeMillis();
        
        // Set sever cooldown (30 min default) for both players
        long severEnd = now + (config.soulLinkSeverCooldownMinutes * 60000L);
        severCooldowns.put(player, severEnd);
        severCooldowns.put(exPartner, severEnd);
        
        // Set random reassign cooldown (12 hours default) for both players
        // This starts AFTER sever cooldown, so add both times
        long randomEnd = now + (config.soulLinkSeverCooldownMinutes * 60000L) 
                            + (config.soulLinkRandomReassignCooldownHours * 3600000L);
        randomReassignCooldowns.put(player, randomEnd);
        randomReassignCooldowns.put(exPartner, randomEnd);
        
        // Set ex-partner cooldown (24 hours default)
        long exPartnerEnd = now + (config.soulLinkExPartnerCooldownHours * 3600000L);
        exPartnerCooldowns.put(getExPartnerKey(player, exPartner), exPartnerEnd);
        
        // Remove both from waiting pool
        waitingPool.remove(player);
        waitingPool.remove(exPartner);
        
        saveCooldowns();
        SimpleDeathBans.LOGGER.info("Recorded soul link severance between {} and {}", player, exPartner);
    }
    
    /**
     * Check compass cooldown
     */
    public boolean isOnCompassCooldown(UUID player) {
        Long cooldownEnd = compassCooldowns.get(player);
        if (cooldownEnd == null) return false;
        if (System.currentTimeMillis() >= cooldownEnd) {
            compassCooldowns.remove(player);
            return false;
        }
        return true;
    }
    
    /**
     * Get remaining compass cooldown time in minutes
     */
    public long getCompassCooldownRemaining(UUID player) {
        Long cooldownEnd = compassCooldowns.get(player);
        if (cooldownEnd == null) return 0;
        long remaining = cooldownEnd - System.currentTimeMillis();
        return remaining > 0 ? (remaining / 60000) + 1 : 0; // Round up
    }
    
    /**
     * Set compass cooldown
     */
    public void setCompassCooldown(UUID player, int minutes) {
        compassCooldowns.put(player, System.currentTimeMillis() + (minutes * 60000L));
    }
    
    /**
     * Check if a player has a soul partner
     */
    public boolean hasPartner(UUID playerUuid) {
        return soulLinks.containsKey(playerUuid);
    }
    
    /**
     * Get a player's soul partner UUID
     */
    public Optional<UUID> getPartner(UUID playerUuid) {
        return Optional.ofNullable(soulLinks.get(playerUuid));
    }
    
    /**
     * Get a player's soul partner as a ServerPlayerEntity (if online)
     */
    public Optional<ServerPlayerEntity> getPartnerPlayer(UUID playerUuid) {
        UUID partnerUuid = soulLinks.get(playerUuid);
        if (partnerUuid == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(server.getPlayerManager().getPlayer(partnerUuid));
    }
    
    /**
     * Create a soul link between two players
     */
    public void createLink(UUID player1, UUID player2) {
        soulLinks.put(player1, player2);
        soulLinks.put(player2, player1);
        waitingPool.remove(player1);
        waitingPool.remove(player2);
        save();
        
        SimpleDeathBans.LOGGER.info("Soul link created between {} and {}", player1, player2);
    }
    
    /**
     * Break a soul link (called when one partner dies)
     */
    public void breakLink(UUID playerUuid) {
        UUID partner = soulLinks.remove(playerUuid);
        if (partner != null) {
            soulLinks.remove(partner);
            save();
            SimpleDeathBans.LOGGER.info("Soul link broken for {} and {}", playerUuid, partner);
        }
    }
    
    /**
     * Clear a player's soul link (admin command)
     */
    public boolean clearLink(UUID playerUuid) {
        if (hasPartner(playerUuid)) {
            breakLink(playerUuid);
            return true;
        }
        return false;
    }
    
    /**
     * Manually set a soul link between two players (admin command)
     */
    public void setLink(UUID player1, UUID player2) {
        // Clear existing links first
        breakLink(player1);
        breakLink(player2);
        // Create new link
        createLink(player1, player2);
    }
    
    /**
     * Try to assign a soul partner when a player joins
     * Returns the partner's UUID if successfully paired
     */
    public Optional<UUID> tryAssignPartner(UUID joiningPlayer) {
        // Don't assign if already has a partner
        if (hasPartner(joiningPlayer)) {
            return Optional.empty();
        }
        
        // Get config for cooldown checks
        SimpleDeathBans mod = SimpleDeathBans.getInstance();
        ModConfig config = mod != null ? mod.getConfig() : null;
        
        // Check if this player can be randomly assigned
        if (config != null && !canBeRandomlyAssigned(joiningPlayer, config)) {
            // Still on cooldown, don't add to waiting pool or assign
            return Optional.empty();
        }
        
        // Look for someone in the waiting pool
        for (UUID waitingPlayer : waitingPool) {
            if (!waitingPlayer.equals(joiningPlayer)) {
                // Check if the waiting player can be randomly assigned
                if (config != null && !canBeRandomlyAssigned(waitingPlayer, config)) {
                    continue; // Skip players on cooldown
                }
                // Check ex-partner cooldown
                if (config != null && isOnExPartnerCooldown(joiningPlayer, waitingPlayer)) {
                    continue; // Skip ex-partners on cooldown
                }
                // Found a valid match!
                createLink(joiningPlayer, waitingPlayer);
                return Optional.of(waitingPlayer);
            }
        }
        
        // No match found, add to waiting pool (if not on cooldown)
        waitingPool.add(joiningPlayer);
        return Optional.empty();
    }
    
    /**
     * Handle player disconnect - remove from waiting pool and clear pending requests
     */
    public void onPlayerDisconnect(UUID playerUuid) {
        waitingPool.remove(playerUuid);
        clearPendingRequestsFor(playerUuid);
        // Note: We don't break soul links on disconnect, only on death
    }
    
    /**
     * Check if a player is in the waiting pool
     */
    public boolean isWaitingForPartner(UUID playerUuid) {
        return waitingPool.contains(playerUuid);
    }
    
    /**
     * Add a player to the waiting pool
     */
    public void addToWaitingPool(UUID playerUuid) {
        waitingPool.add(playerUuid);
    }
    
    /**
     * Remove a player from the waiting pool
     */
    public void removeFromWaitingPool(UUID playerUuid) {
        waitingPool.remove(playerUuid);
    }
    
    /**
     * Get all current soul links (for admin display)
     */
    public Map<UUID, UUID> getAllLinks() {
        return new HashMap<>(soulLinks);
    }
    
    /**
     * Check if there's a pending link request from one player to another
     */
    public boolean hasPendingRequest(UUID requester, UUID target) {
        UUID pendingTarget = pendingLinkRequests.get(requester);
        return target.equals(pendingTarget);
    }
    
    /**
     * Add a pending link request
     */
    public void addPendingRequest(UUID requester, UUID target) {
        pendingLinkRequests.put(requester, target);
    }
    
    /**
     * Remove a pending link request
     */
    public void removePendingRequest(UUID requester) {
        pendingLinkRequests.remove(requester);
    }
    
    /**
     * Clear all pending requests involving a player (when they link or disconnect)
     */
    public void clearPendingRequestsFor(UUID player) {
        pendingLinkRequests.remove(player);
        // Also remove any requests TO this player
        pendingLinkRequests.entrySet().removeIf(entry -> entry.getValue().equals(player));
    }
}
