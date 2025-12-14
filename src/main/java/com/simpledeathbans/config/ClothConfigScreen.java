package com.simpledeathbans.config;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.SimpleDeathBansClient;
import com.simpledeathbans.network.ConfigSyncPayload;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
//? if >=1.21.11
import net.minecraft.command.permission.Permission;
//? if >=1.21.11
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cloth Config screen implementation.
 * This class is only loaded when Cloth Config is available.
 * Server-affecting settings require Operator Level 4.
 */
public class ClothConfigScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger("SimpleDeathBans-ClothConfig");
    
    public static Screen create(Screen parent) {
        ModConfig config;
        if (SimpleDeathBans.getInstance() != null && SimpleDeathBans.getInstance().getConfig() != null) {
            config = SimpleDeathBans.getInstance().getConfig();
        } else {
            config = ModConfig.load();
        }
        
        // Check operator permissions
        MinecraftClient client = MinecraftClient.getInstance();
        boolean isSingleplayer = client.isInSingleplayer();
        boolean isOperator = false;
        if (client.player != null) {
            //? if >=1.21.11 {
            isOperator = client.player.getPermissions().hasPermission(new Permission.Level(PermissionLevel.OWNERS));
            //?} else {
            /*// Pre-1.21.11: Use networkHandler permission level (level 4 = op)
            isOperator = client.player.networkHandler.getCommandSource().hasPermissionLevel(4);
            *///?}
        }
        boolean canEdit = isSingleplayer || isOperator;
        
        // Store original values for reverting non-operator changes
        final int origBaseBanMinutes = config.baseBanMinutes;
        final int origBanMultiplierPercent = config.banMultiplierPercent;
        final int origMaxBanTier = config.maxBanTier;
        final boolean origExponentialBanMode = config.exponentialBanMode;
        final boolean origEnableGhostEcho = config.enableGhostEcho;
        final boolean origEnableSoulLink = config.enableSoulLink;
        final int origSoulLinkDamageSharePercent = config.soulLinkDamageSharePercent;
        final boolean origSoulLinkRandomPartner = config.soulLinkRandomPartner;
        final boolean origSoulLinkTotemSavesPartner = config.soulLinkTotemSavesPartner;
        final boolean origEnableSharedHealth = config.enableSharedHealth;
        final int origSharedHealthDamagePercent = config.sharedHealthDamagePercent;
        final boolean origSharedHealthTotemSavesAll = config.sharedHealthTotemSavesAll;
        final boolean origEnableMercyCooldown = config.enableMercyCooldown;
        final int origMercyPlaytimeHours = config.mercyPlaytimeHours;
        final int origMercyMovementBlocks = config.mercyMovementBlocks;
        final int origMercyBlockInteractions = config.mercyBlockInteractions;
        final int origMercyCheckIntervalMinutes = config.mercyCheckIntervalMinutes;
        final int origPvpBanMultiplierPercent = config.pvpBanMultiplierPercent;
        final int origPveBanMultiplierPercent = config.pveBanMultiplierPercent;
        final boolean origEnableResurrectionAltar = config.enableResurrectionAltar;
        
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.translatable("config.simpledeathbans.title"))
            .setEditable(canEdit) // Disable all inputs for non-operators
            .setSavingRunnable(() -> {
                if (canEdit) {
                    config.save();
                    LOGGER.info("Config saved via Cloth Config screen");
                    
                    // Send config to server for validation and sync
                    if (ClientPlayNetworking.canSend(ConfigSyncPayload.ID)) {
                        ClientPlayNetworking.send(new ConfigSyncPayload(
                            config.baseBanMinutes,
                            config.banMultiplierPercent,
                            config.maxBanTier,
                            config.exponentialBanMode,
                            config.enableGhostEcho,
                            config.enableSoulLink,
                            config.soulLinkDamageSharePercent,
                            config.soulLinkRandomPartner,
                            config.soulLinkTotemSavesPartner,
                            config.enableSharedHealth,
                            config.sharedHealthDamagePercent,
                            config.sharedHealthTotemSavesAll,
                            config.enableMercyCooldown,
                            config.mercyPlaytimeHours,
                            config.mercyMovementBlocks,
                            config.mercyBlockInteractions,
                            config.mercyCheckIntervalMinutes,
                            config.pvpBanMultiplierPercent,
                            config.pveBanMultiplierPercent,
                            config.enableResurrectionAltar
                        ));
                        LOGGER.info("Sent config update to server");
                    }
                } else {
                    // Revert all changes for non-operators
                    config.baseBanMinutes = origBaseBanMinutes;
                    config.banMultiplierPercent = origBanMultiplierPercent;
                    config.maxBanTier = origMaxBanTier;
                    config.exponentialBanMode = origExponentialBanMode;
                    config.enableGhostEcho = origEnableGhostEcho;
                    config.enableSoulLink = origEnableSoulLink;
                    config.soulLinkDamageSharePercent = origSoulLinkDamageSharePercent;
                    config.soulLinkRandomPartner = origSoulLinkRandomPartner;
                    config.soulLinkTotemSavesPartner = origSoulLinkTotemSavesPartner;
                    config.enableSharedHealth = origEnableSharedHealth;
                    config.sharedHealthDamagePercent = origSharedHealthDamagePercent;
                    config.sharedHealthTotemSavesAll = origSharedHealthTotemSavesAll;
                    config.enableMercyCooldown = origEnableMercyCooldown;
                    config.mercyPlaytimeHours = origMercyPlaytimeHours;
                    config.mercyMovementBlocks = origMercyMovementBlocks;
                    config.mercyBlockInteractions = origMercyBlockInteractions;
                    config.mercyCheckIntervalMinutes = origMercyCheckIntervalMinutes;
                    config.pvpBanMultiplierPercent = origPvpBanMultiplierPercent;
                    config.pveBanMultiplierPercent = origPveBanMultiplierPercent;
                    config.enableResurrectionAltar = origEnableResurrectionAltar;
                    
                    if (client.player != null) {
                        client.player.sendMessage(
                            Text.literal("✖ Must be Operator level 4 in order to make changes.")
                                .formatted(Formatting.RED),
                            false
                        );
                    }
                }
            });
        
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        
        // ============================================
        // CATEGORY: General Settings
        // ============================================
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("config.simpledeathbans.general"));
        
        // Permission notice for non-operators
        if (!canEdit) {
            general.addEntry(entryBuilder.startTextDescription(
                Text.literal("⚠ Viewing only - Operator level 4 required to edit.")
                    .formatted(Formatting.GOLD)
            ).build());
        }
        
        general.addEntry(entryBuilder.startIntSlider(
                Text.translatable("config.simpledeathbans.baseBanMinutes"), config.baseBanMinutes, 1, 60)
            .setDefaultValue(1)
            .setTextGetter(val -> Text.literal(val + " min"))
            .setTooltip(Text.literal("Base ban time in minutes per tier. Range: 1-60. Default: 1"))
            .setSaveConsumer(val -> config.baseBanMinutes = val)
            .build());
        
        // Ban Multiplier as percentage slider (10-1000%, where 100% = 1.0x)
        general.addEntry(entryBuilder.startIntSlider(
                Text.translatable("config.simpledeathbans.banMultiplier"), config.banMultiplierPercent, 10, 1000)
            .setDefaultValue(100)
            .setTextGetter(val -> Text.literal(val + "%"))
            .setTooltip(Text.literal("Multiplier for ban time. 100% = 1x. Range: 10%-1000%. Default: 100%"))
            .setSaveConsumer(val -> config.banMultiplierPercent = val)
            .build());
        
        // Display max tier as slider: -1 = infinite, 1-100 = actual tiers
        general.addEntry(entryBuilder.startIntSlider(
                Text.translatable("config.simpledeathbans.maxBanTier"), config.maxBanTier, -1, 100)
            .setDefaultValue(-1)
            .setTextGetter(val -> val == -1 ? Text.literal("Infinite") : Text.literal(String.valueOf(val)))
            .setTooltip(Text.literal("Maximum ban tier. -1 = Infinite. Range: -1 to 100. Default: Infinite"))
            .setSaveConsumer(val -> config.maxBanTier = val)
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
        
        // --- PvP Settings Header ---
        general.addEntry(entryBuilder.startTextDescription(
            Text.literal("═══ PvP Settings ═══").formatted(Formatting.GOLD)
        ).build());
        
        // PvP Ban Multiplier as percentage slider (0-500%)
        general.addEntry(entryBuilder.startIntSlider(
                Text.translatable("config.simpledeathbans.pvpBanMultiplier"), config.pvpBanMultiplierPercent, 0, 500)
            .setDefaultValue(50)
            .setTextGetter(val -> Text.literal(val + "%"))
            .setTooltip(Text.literal("Ban time multiplier for PvP deaths. 50% = half ban time. Default: 50%"))
            .setSaveConsumer(val -> config.pvpBanMultiplierPercent = val)
            .build());
        
        // PvE Ban Multiplier as percentage slider (0-500%)
        general.addEntry(entryBuilder.startIntSlider(
                Text.literal("PvE Ban Multiplier"), config.pveBanMultiplierPercent, 0, 500)
            .setDefaultValue(100)
            .setTextGetter(val -> Text.literal(val + "%"))
            .setTooltip(Text.literal("Ban time multiplier for PvE deaths. 100% = normal ban time. Default: 100%"))
            .setSaveConsumer(val -> config.pveBanMultiplierPercent = val)
            .build());
        
        // --- Altar of Resurrection Header ---
        general.addEntry(entryBuilder.startTextDescription(
            Text.literal("═══ Altar of Resurrection ═══").formatted(Formatting.GOLD)
        ).build());
        
        general.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("config.simpledeathbans.enableResurrectionAltar"), config.enableResurrectionAltar)
            .setDefaultValue(true)
            .setTooltip(Text.literal("Enable Resurrection Altar feature. Default: ON"))
            .setSaveConsumer(val -> config.enableResurrectionAltar = val)
            .build());
        
        // ============================================
        // CATEGORY: Soul Link/Health Settings
        // ============================================
        ConfigCategory soulLinkHealth = builder.getOrCreateCategory(Text.literal("Soul Link/Health Settings"));
        
        // --- Soul Link Header ---
        soulLinkHealth.addEntry(entryBuilder.startTextDescription(
            Text.literal("═══ Soul Link ═══").formatted(Formatting.GOLD)
        ).build());
        
        soulLinkHealth.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("config.simpledeathbans.enableSoulLink"), config.enableSoulLink)
            .setDefaultValue(false)
            .setTooltip(Text.literal("Enable Soul Link feature. Default: OFF"))
            .setSaveConsumer(val -> config.enableSoulLink = val)
            .build());
        
        // Soul Link Damage Share as percentage slider (0-200%, where 100% = 1:1 ratio)
        soulLinkHealth.addEntry(entryBuilder.startIntSlider(
                Text.translatable("config.simpledeathbans.soulLinkDamageShare"), config.soulLinkDamageSharePercent, 0, 200)
            .setDefaultValue(100)
            .setTextGetter(val -> Text.literal(val + "%"))
            .setTooltip(
                Text.literal("Percentage of damage shared to soul partner.").formatted(Formatting.WHITE),
                Text.literal("100% = 1:1 ratio (take 2 hearts, partner takes 2 hearts)").formatted(Formatting.GRAY),
                Text.literal("50% = half damage, 200% = double damage").formatted(Formatting.GRAY),
                Text.literal("").formatted(Formatting.GRAY),
                Text.literal("⚠ IMPORTANT: Only affects NON-LETHAL damage!").formatted(Formatting.GOLD),
                Text.literal("LETHAL damage triggers Death Pact = instant death").formatted(Formatting.RED),
                Text.literal("for BOTH players regardless of this setting.").formatted(Formatting.RED),
                Text.literal("Default: 100%").formatted(Formatting.GRAY))
            .setSaveConsumer(val -> config.soulLinkDamageSharePercent = val)
            .build());
        
        soulLinkHealth.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Random Partner Assignment"), config.soulLinkRandomPartner)
            .setDefaultValue(true)
            .setTooltip(Text.literal("ON: Auto-pair with random player on join. OFF: Shift+right-click player to link. Default: ON"))
            .setSaveConsumer(val -> config.soulLinkRandomPartner = val)
            .build());
        
        soulLinkHealth.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Totem Saves Partner"), config.soulLinkTotemSavesPartner)
            .setDefaultValue(true)
            .setTooltip(
                Text.literal("Controls totem behavior for Soul Link Death Pact.").formatted(Formatting.WHITE),
                Text.literal("").formatted(Formatting.GRAY),
                Text.literal("ON: Any totem saves BOTH players").formatted(Formatting.GREEN),
                Text.literal("OFF: Totem only saves holder, partner dies").formatted(Formatting.RED),
                Text.literal("").formatted(Formatting.GRAY),
                Text.literal("If BOTH have totems: Both consumed, both live").formatted(Formatting.GRAY),
                Text.literal("Default: ON").formatted(Formatting.GRAY))
            .setSaveConsumer(val -> config.soulLinkTotemSavesPartner = val)
            .build());
        
        // --- Shared Health Header ---
        soulLinkHealth.addEntry(entryBuilder.startTextDescription(
            Text.literal("═══ Shared Health ═══").formatted(Formatting.GOLD)
        ).build());
        
        soulLinkHealth.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Enable Shared Health"), config.enableSharedHealth)
            .setDefaultValue(false)
            .setTooltip(
                Text.literal("SERVER-WIDE damage sharing!").formatted(Formatting.WHITE),
                Text.literal("ALL players share damage - if one takes damage, everyone does.").formatted(Formatting.GRAY),
                Text.literal("").formatted(Formatting.GRAY),
                Text.literal("⚠ Death Pact: If ANY player takes lethal damage,").formatted(Formatting.RED),
                Text.literal("ALL players die instantly (unless someone has a totem).").formatted(Formatting.RED),
                Text.literal("Default: OFF").formatted(Formatting.GRAY))
            .setSaveConsumer(val -> config.enableSharedHealth = val)
            .build());
        
        // Shared Damage Percent as slider (0-200%)
        soulLinkHealth.addEntry(entryBuilder.startIntSlider(
                Text.literal("Shared Damage Percent"), config.sharedHealthDamagePercent, 0, 200)
            .setDefaultValue(100)
            .setTextGetter(val -> Text.literal(val + "%"))
            .setTooltip(
                Text.literal("Percent of damage shared to ALL players.").formatted(Formatting.WHITE),
                Text.literal("100% = full damage (1:1), 50% = half damage").formatted(Formatting.GRAY),
                Text.literal("").formatted(Formatting.GRAY),
                Text.literal("⚠ IMPORTANT: Only affects NON-LETHAL damage!").formatted(Formatting.GOLD),
                Text.literal("LETHAL damage triggers Death Pact = everyone dies").formatted(Formatting.RED),
                Text.literal("regardless of this setting.").formatted(Formatting.RED),
                Text.literal("Default: 100%").formatted(Formatting.GRAY))
            .setSaveConsumer(val -> config.sharedHealthDamagePercent = val)
            .build());
        
        soulLinkHealth.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Totem Saves All"), config.sharedHealthTotemSavesAll)
            .setDefaultValue(true)
            .setTooltip(
                Text.literal("Controls totem behavior for Shared Health Death Pact.").formatted(Formatting.WHITE),
                Text.literal("").formatted(Formatting.GRAY),
                Text.literal("ON: Any player's totem saves EVERYONE").formatted(Formatting.GREEN),
                Text.literal("OFF: Only totem holders survive, others die").formatted(Formatting.RED),
                Text.literal("").formatted(Formatting.GRAY),
                Text.literal("Multiple totems: All consumed, all holders notified").formatted(Formatting.GRAY),
                Text.literal("Default: ON").formatted(Formatting.GRAY))
            .setSaveConsumer(val -> config.sharedHealthTotemSavesAll = val)
            .build());
        
        // ============================================
        // CATEGORY: Mercy Cooldown Settings
        // ============================================
        ConfigCategory mercy = builder.getOrCreateCategory(Text.translatable("config.simpledeathbans.mercy"));
        
        mercy.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Enable Mercy Cooldown"), config.enableMercyCooldown)
            .setDefaultValue(true)
            .setTooltip(Text.literal("Enable Mercy Cooldown system that reduces ban tier over time. Default: ON"))
            .setSaveConsumer(val -> config.enableMercyCooldown = val)
            .build());
        
        mercy.addEntry(entryBuilder.startIntSlider(
                Text.translatable("config.simpledeathbans.mercyPlaytimeHours"), config.mercyPlaytimeHours, 1, 168)
            .setDefaultValue(24)
            .setTooltip(Text.literal("Real playtime hours (not AFK) without deaths to reduce ban tier. Range: 1-168. Default: 24"))
            .setSaveConsumer(val -> config.mercyPlaytimeHours = val)
            .build());
        
        mercy.addEntry(entryBuilder.startIntSlider(
                Text.translatable("config.simpledeathbans.mercyMovementBlocks"), config.mercyMovementBlocks, 0, 500)
            .setDefaultValue(50)
            .setTooltip(Text.literal("Blocks moved in check interval to count as active. Range: 0-500. Default: 50"))
            .setSaveConsumer(val -> config.mercyMovementBlocks = val)
            .build());
        
        mercy.addEntry(entryBuilder.startIntSlider(
                Text.translatable("config.simpledeathbans.mercyBlockInteractions"), config.mercyBlockInteractions, 0, 200)
            .setDefaultValue(20)
            .setTooltip(Text.literal("Block interactions in check interval to count as active. Range: 0-200. Default: 20"))
            .setSaveConsumer(val -> config.mercyBlockInteractions = val)
            .build());
        
        mercy.addEntry(entryBuilder.startIntSlider(
                Text.literal("Check Interval (minutes)"), config.mercyCheckIntervalMinutes, 1, 60)
            .setDefaultValue(15)
            .setTooltip(Text.literal("Minutes between activity checks. Range: 1-60. Default: 15"))
            .setSaveConsumer(val -> config.mercyCheckIntervalMinutes = val)
            .build());
        
        // ============================================
        // CATEGORY: Keybinds
        // ============================================
        ConfigCategory keybinds = builder.getOrCreateCategory(Text.literal("Keybinds"));
        
        keybinds.addEntry(entryBuilder.fillKeybindingField(
                Text.literal("Open Config Screen"),
                SimpleDeathBansClient.openConfigKeyBinding)
            .setTooltip(Text.literal("Keybind to open the SimpleDeathBans config screen.\nCan also be changed in: Options → Controls → Key Binds..."))
            .build());
        
        return builder.build();
    }
}
