package com.simpledeathbans.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.simpledeathbans.SimpleDeathBans;
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
    
    // Player UUID -> Partner UUID (bidirectional)
    private final Map<UUID, UUID> soulLinks = new ConcurrentHashMap<>();
    
    // Pool of online players waiting for a partner (random mode)
    private final Set<UUID> waitingPool = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // Pending link requests: requester UUID -> target UUID (manual mode)
    private final Map<UUID, UUID> pendingLinkRequests = new ConcurrentHashMap<>();
    
    public SoulLinkManager(MinecraftServer server) {
        this.server = server;
        this.dataFile = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("simpledeathbans_soullinks.json");
    }
    
    public void load() {
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
    }
    
    public void save() {
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
        
        // Look for someone in the waiting pool
        for (UUID waitingPlayer : waitingPool) {
            if (!waitingPlayer.equals(joiningPlayer)) {
                // Found a match!
                createLink(joiningPlayer, waitingPlayer);
                return Optional.of(waitingPlayer);
            }
        }
        
        // No match found, add to waiting pool
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
