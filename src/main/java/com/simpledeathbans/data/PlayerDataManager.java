package com.simpledeathbans.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.simpledeathbans.SimpleDeathBans;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-player data for mercy cooldown tracking.
 */
public class PlayerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final MinecraftServer server;
    private final Path dataFile;
    
    // UUID -> PlayerActivityData
    private final Map<UUID, PlayerActivityData> playerData = new ConcurrentHashMap<>();
    
    public static class PlayerActivityData {
        public UUID playerUuid;
        public String playerName;
        
        // Mercy cooldown tracking
        public long lastDeathTime = 0; // millis since epoch
        public long totalPlaytimeSinceDeathTicks = 0; // ticks
        public long lastActivityCheckTime = 0; // millis since epoch
        
        // Activity tracking for anti-AFK
        public double lastCheckX = 0;
        public double lastCheckY = 0;
        public double lastCheckZ = 0;
        public int lastCheckBlocksMined = 0;
        public int lastCheckBlocksPlaced = 0;
        
        // Activity accumulated since last check
        public int currentBlocksMined = 0;
        public int currentBlocksPlaced = 0;
        
        public PlayerActivityData() {}
        
        public PlayerActivityData(UUID uuid, String name) {
            this.playerUuid = uuid;
            this.playerName = name;
        }
        
        public void onDeath() {
            lastDeathTime = System.currentTimeMillis();
            totalPlaytimeSinceDeathTicks = 0;
        }
        
        public void startActivityCheck(ServerPlayerEntity player) {
            lastActivityCheckTime = System.currentTimeMillis();
            lastCheckX = player.getX();
            lastCheckY = player.getY();
            lastCheckZ = player.getZ();
            lastCheckBlocksMined = currentBlocksMined;
            lastCheckBlocksPlaced = currentBlocksPlaced;
        }
        
        public boolean checkActivity(ServerPlayerEntity player, int requiredMovement, int requiredInteractions) {
            double dx = player.getX() - lastCheckX;
            double dy = player.getY() - lastCheckY;
            double dz = player.getZ() - lastCheckZ;
            double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
            
            int blocksMined = currentBlocksMined - lastCheckBlocksMined;
            int blocksPlaced = currentBlocksPlaced - lastCheckBlocksPlaced;
            int totalInteractions = blocksMined + blocksPlaced;
            
            return distance >= requiredMovement || totalInteractions >= requiredInteractions;
        }
        
        public void incrementBlocksMined() {
            currentBlocksMined++;
        }
        
        public void incrementBlocksPlaced() {
            currentBlocksPlaced++;
        }
    }
    
    public PlayerDataManager(MinecraftServer server) {
        this.server = server;
        this.dataFile = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("simpledeathbans_players.json");
    }
    
    public void load() {
        if (Files.exists(dataFile)) {
            try {
                String json = Files.readString(dataFile);
                Type type = new TypeToken<Map<String, PlayerActivityData>>(){}.getType();
                Map<String, PlayerActivityData> loaded = GSON.fromJson(json, type);
                if (loaded != null) {
                    playerData.clear();
                    for (Map.Entry<String, PlayerActivityData> entry : loaded.entrySet()) {
                        playerData.put(UUID.fromString(entry.getKey()), entry.getValue());
                    }
                }
                SimpleDeathBans.LOGGER.info("Loaded {} player activity entries", playerData.size());
            } catch (IOException e) {
                SimpleDeathBans.LOGGER.error("Failed to load player data", e);
            }
        }
    }
    
    public void save() {
        try {
            Files.createDirectories(dataFile.getParent());
            Map<String, PlayerActivityData> toSave = new HashMap<>();
            for (Map.Entry<UUID, PlayerActivityData> entry : playerData.entrySet()) {
                toSave.put(entry.getKey().toString(), entry.getValue());
            }
            Files.writeString(dataFile, GSON.toJson(toSave));
        } catch (IOException e) {
            SimpleDeathBans.LOGGER.error("Failed to save player data", e);
        }
    }
    
    public PlayerActivityData getOrCreate(UUID uuid, String name) {
        return playerData.computeIfAbsent(uuid, id -> new PlayerActivityData(uuid, name));
    }
    
    public PlayerActivityData get(UUID uuid) {
        return playerData.get(uuid);
    }
    
    public void onPlayerDeath(UUID uuid, String name) {
        PlayerActivityData data = getOrCreate(uuid, name);
        data.onDeath();
        save();
    }
    
    public void incrementPlaytime(UUID uuid, long ticks) {
        PlayerActivityData data = playerData.get(uuid);
        if (data != null) {
            data.totalPlaytimeSinceDeathTicks += ticks;
        }
    }
    
    public void onBlockMined(UUID uuid, String name) {
        PlayerActivityData data = getOrCreate(uuid, name);
        data.incrementBlocksMined();
    }
    
    public void onBlockPlaced(UUID uuid, String name) {
        PlayerActivityData data = getOrCreate(uuid, name);
        data.incrementBlocksPlaced();
    }
}
