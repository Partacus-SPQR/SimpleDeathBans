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
    public boolean enableDeathBans = true; // Master toggle to enable/disable death bans
    public int baseBanMinutes = 1;
    public int banMultiplierPercent = 100; // 100 = 1.0x, stored as percentage
    public int maxBanTier = -1; // -1 = infinite, 1-100 = actual max tier
    public boolean exponentialBanMode = false; // If true: 1, 2, 4, 8, 16... (doubles each death)
    
    // Single-Player Settings
    public boolean singlePlayerEnabled = true; // Enable/disable mod in single-player (requires OP Level 4 / cheats)
    
    // Soul Link Settings (Operator Level 4 only)
    public boolean enableSoulLink = false;
    public int soulLinkDamageSharePercent = 100; // 100 = 100% = 1:1 ratio
    public boolean soulLinkShareHunger = false; // Share hunger between soul-linked partners
    public boolean soulLinkRandomPartner = true; // If true: auto-random pairing, if false: shift+right-click to choose
    public boolean soulLinkTotemSavesPartner = true; // If partner uses totem, both players are saved
    
    // Soul Link Sever Settings
    public int soulLinkSeverCooldownMinutes = 30; // Minutes before can link with ANYONE after severing
    public int soulLinkSeverBanTierIncrease = 1; // Ban tier increase when severing a link
    public int soulLinkExPartnerCooldownHours = 24; // Hours before can re-pair with same ex-partner
    public int soulLinkRandomReassignCooldownHours = 12; // Hours before system randomly assigns after sever cooldown ends
    public int soulLinkRandomAssignCheckIntervalMinutes = 60; // Minutes between random partner assignment checks (1-1440, default 60 = 1 hour)
    
    // Soul Compass Settings (Soul Link Totem feature)
    public int soulLinkCompassMaxUses = 10; // Max uses per totem
    public int soulLinkCompassCooldownMinutes = 10; // Minutes between compass uses
    
    // Shared Health Settings (Server-wide damage sharing)
    public boolean enableSharedHealth = false; // Server-wide health pool
    public int sharedHealthDamagePercent = 100; // 100 = 100% of damage shared to all players
    public boolean sharedHealthShareHunger = false; // Share hunger drain across all players
    public boolean sharedHealthTotemSavesAll = true; // Any player's totem can save everyone
    
    // Mercy Cooldown Settings
    public boolean enableMercyCooldown = true; // Enable mercy cooldown system
    public int mercyPlaytimeHours = 24; // 1 real day of playtime
    public int mercyMovementBlocks = 50;
    public int mercyBlockInteractions = 20;
    public int mercyCheckIntervalMinutes = 15;
    
    // PvP Settings
    public int pvpBanMultiplierPercent = 50; // 50 = 0.5x
    public int pveBanMultiplierPercent = 100; // 100 = 1.0x
    
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
        // Clamp values to valid slider ranges (all integers now!)
        baseBanMinutes = Math.max(1, Math.min(60, baseBanMinutes));
        banMultiplierPercent = Math.max(10, Math.min(1000, banMultiplierPercent));
        maxBanTier = Math.max(-1, Math.min(100, maxBanTier));
        soulLinkDamageSharePercent = Math.max(0, Math.min(200, soulLinkDamageSharePercent));
        soulLinkSeverCooldownMinutes = Math.max(0, Math.min(120, soulLinkSeverCooldownMinutes));
        soulLinkSeverBanTierIncrease = Math.max(0, Math.min(10, soulLinkSeverBanTierIncrease));
        soulLinkExPartnerCooldownHours = Math.max(0, Math.min(168, soulLinkExPartnerCooldownHours));
        soulLinkRandomReassignCooldownHours = Math.max(0, Math.min(72, soulLinkRandomReassignCooldownHours));
        soulLinkRandomAssignCheckIntervalMinutes = Math.max(1, Math.min(1440, soulLinkRandomAssignCheckIntervalMinutes));
        soulLinkCompassMaxUses = Math.max(1, Math.min(100, soulLinkCompassMaxUses));
        soulLinkCompassCooldownMinutes = Math.max(0, Math.min(60, soulLinkCompassCooldownMinutes));
        sharedHealthDamagePercent = Math.max(0, Math.min(200, sharedHealthDamagePercent));
        mercyPlaytimeHours = Math.max(1, Math.min(168, mercyPlaytimeHours));
        mercyMovementBlocks = Math.max(0, Math.min(500, mercyMovementBlocks));
        mercyBlockInteractions = Math.max(0, Math.min(200, mercyBlockInteractions));
        mercyCheckIntervalMinutes = Math.max(1, Math.min(60, mercyCheckIntervalMinutes));
        pvpBanMultiplierPercent = Math.max(0, Math.min(500, pvpBanMultiplierPercent));
        pveBanMultiplierPercent = Math.max(0, Math.min(500, pveBanMultiplierPercent));
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
        double banMult = banMultiplierPercent / 100.0;
        double pvpPveMult = isPvP ? (pvpBanMultiplierPercent / 100.0) : (pveBanMultiplierPercent / 100.0);
        return Math.round(baseTime * banMult * pvpPveMult);
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
