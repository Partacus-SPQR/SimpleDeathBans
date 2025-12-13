package com.simpledeathbans.config;

import com.simpledeathbans.SimpleDeathBans;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Cloth Config screen implementation.
 * This class is only loaded when Cloth Config is available.
 */
public class ClothConfigScreen {
    
    public static Screen create(Screen parent) {
        ModConfig config;
        if (SimpleDeathBans.getInstance() != null && SimpleDeathBans.getInstance().getConfig() != null) {
            config = SimpleDeathBans.getInstance().getConfig();
        } else {
            config = ModConfig.load();
        }
        
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.translatable("config.simpledeathbans.title"))
            .setSavingRunnable(config::save);
        
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        
        // General Settings Category
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("config.simpledeathbans.general"));
        
        general.addEntry(entryBuilder.startIntField(
                Text.translatable("config.simpledeathbans.baseBanMinutes"), config.baseBanMinutes)
            .setDefaultValue(1)
            .setMin(1)
            .setMax(60)
            .setTooltip(Text.literal("Base ban time in minutes per tier. Default: 1"))
            .setSaveConsumer(val -> config.baseBanMinutes = val)
            .build());
        
        general.addEntry(entryBuilder.startDoubleField(
                Text.translatable("config.simpledeathbans.banMultiplier"), config.banMultiplier)
            .setDefaultValue(1.0)
            .setMin(0.1)
            .setMax(10.0)
            .setTooltip(Text.literal("Multiplier for ban time. Default: 1.0"))
            .setSaveConsumer(val -> config.banMultiplier = val)
            .build());
        
        // Display max tier as slider with 100 = infinite
        int displayMaxTier = config.maxBanTier >= 100 ? 100 : config.maxBanTier;
        general.addEntry(entryBuilder.startIntSlider(
                Text.translatable("config.simpledeathbans.maxBanTier"), displayMaxTier, 1, 100)
            .setDefaultValue(100)
            .setTooltip(Text.literal("Max tier (100 = infinite). Default: Infinite"))
            .setSaveConsumer(val -> config.maxBanTier = val >= 100 ? Integer.MAX_VALUE : val)
            .build());
        
        general.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Exponential Ban Mode"), config.exponentialBanMode)
            .setDefaultValue(false)
            .setTooltip(Text.literal("Use doubling formula (1,2,4,8,16...) instead of linear. Default: OFF"))
            .setSaveConsumer(val -> config.exponentialBanMode = val)
            .build());
        
        general.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("config.simpledeathbans.enableGhostEcho"), config.enableGhostEcho)
            .setDefaultValue(true)
            .setTooltip(Text.literal("Enable lightning strike and custom death message on ban. Default: ON"))
            .setSaveConsumer(val -> config.enableGhostEcho = val)
            .build());
        
        // Soul Link Settings Category
        ConfigCategory soulLink = builder.getOrCreateCategory(Text.translatable("config.simpledeathbans.soullink"));
        
        soulLink.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("config.simpledeathbans.enableSoulLink"), config.enableSoulLink)
            .setDefaultValue(false)
            .setTooltip(Text.literal("Enable Soul Link feature. Default: OFF"))
            .setSaveConsumer(val -> config.enableSoulLink = val)
            .build());
        
        soulLink.addEntry(entryBuilder.startDoubleField(
                Text.translatable("config.simpledeathbans.soulLinkDamageShare"), config.soulLinkDamageShare)
            .setDefaultValue(1.0)
            .setMin(0.0)
            .setMax(10.0)
            .setTooltip(Text.literal("Damage shared to soul partner (1.0 = 0.5 hearts). Default: 1.0"))
            .setSaveConsumer(val -> config.soulLinkDamageShare = val)
            .build());
        
        // Shared Health Settings Category
        ConfigCategory sharedHealth = builder.getOrCreateCategory(Text.literal("Shared Health Settings"));
        
        sharedHealth.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Enable Shared Health"), config.enableSharedHealth)
            .setDefaultValue(false)
            .setTooltip(Text.literal("Server-wide damage sharing (all players share health). Default: OFF"))
            .setSaveConsumer(val -> config.enableSharedHealth = val)
            .build());
        
        sharedHealth.addEntry(entryBuilder.startDoubleField(
                Text.literal("Shared Damage Percent"), config.sharedHealthDamagePercent)
            .setDefaultValue(1.0)
            .setMin(0.0)
            .setMax(2.0)
            .setTooltip(Text.literal("Percent of damage shared to all players (1.0 = 100%). Default: 1.0"))
            .setSaveConsumer(val -> config.sharedHealthDamagePercent = val)
            .build());
        
        sharedHealth.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Totem Saves All"), config.sharedHealthTotemSavesAll)
            .setDefaultValue(true)
            .setTooltip(Text.literal("Any player's totem can save all players from death. Default: ON"))
            .setSaveConsumer(val -> config.sharedHealthTotemSavesAll = val)
            .build());
        
        // Mercy Cooldown Settings Category
        ConfigCategory mercy = builder.getOrCreateCategory(Text.translatable("config.simpledeathbans.mercy"));
        
        mercy.addEntry(entryBuilder.startIntField(
                Text.translatable("config.simpledeathbans.mercyPlaytimeHours"), config.mercyPlaytimeHours)
            .setDefaultValue(24)
            .setMin(1)
            .setMax(2500)
            .setTooltip(Text.literal("Real playtime hours (not AFK) without deaths to reduce ban tier. Default: 24"))
            .setSaveConsumer(val -> config.mercyPlaytimeHours = val)
            .build());
        
        mercy.addEntry(entryBuilder.startIntField(
                Text.translatable("config.simpledeathbans.mercyMovementBlocks"), config.mercyMovementBlocks)
            .setDefaultValue(50)
            .setMin(0)
            .setMax(1000)
            .setTooltip(Text.literal("Blocks moved in check interval to count as active. Default: 50"))
            .setSaveConsumer(val -> config.mercyMovementBlocks = val)
            .build());
        
        mercy.addEntry(entryBuilder.startIntField(
                Text.translatable("config.simpledeathbans.mercyBlockInteractions"), config.mercyBlockInteractions)
            .setDefaultValue(20)
            .setMin(0)
            .setMax(1000)
            .setTooltip(Text.literal("Block interactions in check interval to count as active. Default: 20"))
            .setSaveConsumer(val -> config.mercyBlockInteractions = val)
            .build());
        
        mercy.addEntry(entryBuilder.startIntField(
                Text.literal("Check Interval (minutes)"), config.mercyCheckIntervalMinutes)
            .setDefaultValue(15)
            .setMin(1)
            .setMax(60)
            .setTooltip(Text.literal("Minutes between activity checks. Default: 15"))
            .setSaveConsumer(val -> config.mercyCheckIntervalMinutes = val)
            .build());
        
        // PvP Settings Category
        ConfigCategory pvp = builder.getOrCreateCategory(Text.translatable("config.simpledeathbans.pvp"));
        
        pvp.addEntry(entryBuilder.startDoubleField(
                Text.translatable("config.simpledeathbans.pvpBanMultiplier"), config.pvpBanMultiplier)
            .setDefaultValue(0.5)
            .setMin(0.0)
            .setMax(5.0)
            .setTooltip(Text.literal("Ban time multiplier for PvP deaths. Default: 0.5"))
            .setSaveConsumer(val -> config.pvpBanMultiplier = val)
            .build());
        
        pvp.addEntry(entryBuilder.startDoubleField(
                Text.literal("PvE Ban Multiplier"), config.pveBanMultiplier)
            .setDefaultValue(1.0)
            .setMin(0.0)
            .setMax(5.0)
            .setTooltip(Text.literal("Ban time multiplier for PvE deaths. Default: 1.0"))
            .setSaveConsumer(val -> config.pveBanMultiplier = val)
            .build());
        
        // Altar Settings Category
        ConfigCategory altar = builder.getOrCreateCategory(Text.translatable("config.simpledeathbans.altar"));
        
        altar.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("config.simpledeathbans.enableResurrectionAltar"), config.enableResurrectionAltar)
            .setDefaultValue(true)
            .setTooltip(Text.literal("Enable Resurrection Altar feature. Default: ON"))
            .setSaveConsumer(val -> config.enableResurrectionAltar = val)
            .build());
        
        return builder.build();
    }
}
