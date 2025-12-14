package com.simpledeathbans.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.simpledeathbans.SimpleDeathBans;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("simpledeathbans.json");
    
    // General Settings
    public int baseBanMinutes = 1;
    public double banMultiplier = 1.0;
    public int maxBanTier = Integer.MAX_VALUE; // Essentially infinite - tiers keep growing
    public boolean exponentialBanMode = false; // If true: 1, 2, 4, 8, 16... (doubles each death)
    
    // Soul Link Settings (Operator Level 4 only)
    public boolean enableSoulLink = false;
    public double soulLinkDamageShare = 1.0; // 0.5 hearts = 1.0 damage
    public boolean soulLinkRandomPartner = true; // If true: auto-random pairing, if false: shift+right-click to choose
    public boolean soulLinkTotemSavesAll = true; // If partner uses totem, both players are saved
    
    // Shared Health Settings (Server-wide damage sharing)
    public boolean enableSharedHealth = false; // Server-wide health pool
    public double sharedHealthDamagePercent = 1.0; // 100% of damage shared to all players
    public boolean sharedHealthTotemSavesAll = true; // Any player's totem can save everyone
    
    // Mercy Cooldown Settings
    public boolean enableMercyCooldown = true; // Enable mercy cooldown system
    public int mercyPlaytimeHours = 24; // 1 real day of playtime
    public int mercyMovementBlocks = 50;
    public int mercyBlockInteractions = 20;
    public int mercyCheckIntervalMinutes = 15;
    
    // PvP Settings
    public double pvpBanMultiplier = 0.5;
    public double pveBanMultiplier = 1.0;
    
    // Ghost Echo Settings
    public boolean enableGhostEcho = true;
    
    // Resurrection Altar Settings
    public boolean enableResurrectionAltar = true;
    
    // Transient fields (not saved)
    private transient boolean dirty = false;
    
    public static ModConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                ModConfig config = GSON.fromJson(json, ModConfig.class);
                if (config != null) {
                    config.validateAndClamp();
                    SimpleDeathBans.LOGGER.info("Configuration loaded from {}", CONFIG_PATH);
                    return config;
                }
            } catch (IOException e) {
                SimpleDeathBans.LOGGER.error("Failed to load config, using defaults", e);
            }
        }
        
        ModConfig config = new ModConfig();
        config.save();
        return config;
    }
    
    /**
     * Validate and clamp config values to valid ranges to prevent slider issues.
     */
    private void validateAndClamp() {
        // Clamp values to valid slider ranges
        baseBanMinutes = Math.max(1, Math.min(60, baseBanMinutes));
        banMultiplier = Math.max(0.1, Math.min(10.0, banMultiplier));
        // maxBanTier: -1 to 100 in UI, but stored as Integer.MAX_VALUE for infinite
        if (maxBanTier != Integer.MAX_VALUE && (maxBanTier < 1 || maxBanTier > 100)) {
            maxBanTier = Integer.MAX_VALUE; // Default to infinite
        }
        soulLinkDamageShare = Math.max(0.0, Math.min(10.0, soulLinkDamageShare));
        sharedHealthDamagePercent = Math.max(0.0, Math.min(2.0, sharedHealthDamagePercent));
        mercyPlaytimeHours = Math.max(1, Math.min(168, mercyPlaytimeHours));
        mercyMovementBlocks = Math.max(0, Math.min(500, mercyMovementBlocks));
        mercyBlockInteractions = Math.max(0, Math.min(200, mercyBlockInteractions));
        mercyCheckIntervalMinutes = Math.max(1, Math.min(60, mercyCheckIntervalMinutes));
        pvpBanMultiplier = Math.max(0.0, Math.min(5.0, pvpBanMultiplier));
        pveBanMultiplier = Math.max(0.0, Math.min(5.0, pveBanMultiplier));
    }
    
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
            dirty = false;
            SimpleDeathBans.LOGGER.info("Configuration saved to {}", CONFIG_PATH);
        } catch (IOException e) {
            SimpleDeathBans.LOGGER.error("Failed to save config", e);
        }
    }
    
    public void markDirty() {
        dirty = true;
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    /**
     * Calculate ban time in minutes based on tier and death type
     * @param tier The player's current ban tier
     * @param isPvP Whether the death was from PvP
     * @return Ban time in minutes
     */
    public long calculateBanMinutes(int tier, boolean isPvP) {
        double baseTime = baseBanMinutes * tier;
        double multiplier = isPvP ? pvpBanMultiplier : pveBanMultiplier;
        return Math.round(baseTime * banMultiplier * multiplier);
    }
    
    /**
     * Get ban time as a formatted string
     */
    public static String formatBanTime(long minutes) {
        if (minutes < 60) {
            return minutes + " minute" + (minutes != 1 ? "s" : "");
        } else if (minutes < 1440) {
            long hours = minutes / 60;
            long mins = minutes % 60;
            return hours + " hour" + (hours != 1 ? "s" : "") + 
                   (mins > 0 ? " " + mins + " minute" + (mins != 1 ? "s" : "") : "");
        } else {
            long days = minutes / 1440;
            long hours = (minutes % 1440) / 60;
            return days + " day" + (days != 1 ? "s" : "") +
                   (hours > 0 ? " " + hours + " hour" + (hours != 1 ? "s" : "") : "");
        }
    }
}
