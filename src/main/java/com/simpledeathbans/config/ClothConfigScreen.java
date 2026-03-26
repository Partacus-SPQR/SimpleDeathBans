package com.simpledeathbans.config;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.SimpleDeathBansClient;
import com.simpledeathbans.network.ConfigSyncPayload;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.11
import net.minecraft.server.permissions.Permission;
//? if >=1.21.11
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
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
        Minecraft client = Minecraft.getInstance();
        boolean isSingleplayer = client.hasSingleplayerServer();
        boolean isOperator = false;
        if (client.player != null) {
            //? if >=1.21.11 {
            isOperator = client.player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.OWNERS));
            //?} else {
            /*// Pre-1.21.11: Use player's getPermissionLevel directly
            isOperator = client.player.getPermissionLevel() >= 4;
            *///?}
        }
        boolean canEdit = isSingleplayer || isOperator;
        
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.simpledeathbans.title"))
                .setTransparentBackground(true);
        
        // Save handler
        builder.setSavingRunnable(() -> {
            if (canEdit) {
                config.save();
                LOGGER.info("Config saved via Cloth Config screen");
                
                // Send config to server for validation and sync
                if (ClientPlayNetworking.canSend(ConfigSyncPayload.ID)) {
                    ClientPlayNetworking.send(new ConfigSyncPayload(
                        config.enableDeathBans,
                        config.baseBanMinutes,
                        config.banMultiplierPercent,
                        config.maxBanTier,
                        config.exponentialBanMode,
                        config.enableGhostEcho,
                        config.enableSoulLink,
                        config.soulLinkDamageSharePercent,
                        config.soulLinkShareHunger,
                        config.soulLinkRandomPartner,
                        config.soulLinkTotemSavesPartner,
                        config.soulLinkSeverCooldownMinutes,
                        config.soulLinkSeverBanTierIncrease,
                        config.soulLinkExPartnerCooldownHours,
                        config.soulLinkRandomReassignCooldownHours,
                        config.soulLinkRandomAssignCheckIntervalMinutes,
                        config.soulLinkCompassMaxUses,
                        config.soulLinkCompassCooldownMinutes,
                        config.enableSharedHealth,
                        config.sharedHealthDamagePercent,
                        config.sharedHealthShareHunger,
                        config.sharedHealthTotemSavesAll,
                        config.enableMercyCooldown,
                        config.mercyPlaytimeHours,
                        config.mercyMovementBlocks,
                        config.mercyBlockInteractions,
                        config.mercyCheckIntervalMinutes,
                        config.pvpBanMultiplierPercent,
                        config.pveBanMultiplierPercent,
                        config.enableResurrectionAltar,
                        config.singlePlayerEnabled
                    ));
                    LOGGER.info("Sent config update to server");
                }
            } else {
                if (client.player != null) {
                    client.player.displayClientMessage(
                        Component.literal("✖ Must be Operator level 4 in order to make changes.")
                            .withStyle(ChatFormatting.RED), false);
                }
            }
        });
        
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        
        // ============================================
        // CATEGORY: General Settings
        // ============================================
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.simpledeathbans.general"));
        
        // Permission notice for non-operators
        if (!canEdit) {
            general.addEntry(entryBuilder.startTextDescription(
                    Component.literal("⚠ Viewing only - Operator level 4 required to edit.")
                            .withStyle(ChatFormatting.GOLD))
                    .build());
        }
        
        // Enable Death Bans toggle
        general.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Enable Death Bans"),
                config.enableDeathBans)
                .setDefaultValue(true)
                .setTooltip(
                        Component.literal("Master toggle for death ban system."),
                        Component.literal("OFF: Deaths have no ban consequences.").withStyle(ChatFormatting.YELLOW),
                        Component.literal("ON: Deaths result in temporary bans.").withStyle(ChatFormatting.GREEN),
                        Component.literal("Default: ON").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.enableDeathBans = newValue; })
                .build());
        
        // Base Ban Minutes (1-60)
        general.addEntry(entryBuilder.startIntField(
                Component.translatable("config.simpledeathbans.baseBanMinutes"),
                config.baseBanMinutes)
                .setDefaultValue(1)
                .setMin(1)
                .setMax(60)
                .setTooltip(
                        Component.literal("Base ban duration per tier (in minutes)."),
                        Component.literal("Formula: baseBan × tier × multiplier").withStyle(ChatFormatting.GRAY),
                        Component.literal("Range: 1-60 | Default: 1").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.baseBanMinutes = newValue; })
                .build());
        
        // Ban Multiplier (10-1000%)
        general.addEntry(entryBuilder.startIntField(
                Component.translatable("config.simpledeathbans.banMultiplier"),
                config.banMultiplierPercent)
                .setDefaultValue(100)
                .setMin(10)
                .setMax(1000)
                .setTooltip(
                        Component.literal("Global ban time multiplier (percentage)."),
                        Component.literal("100 = normal, 200 = double, 50 = half").withStyle(ChatFormatting.GRAY),
                        Component.literal("Range: 10-1000 | Default: 100").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.banMultiplierPercent = newValue; })
                .build());
        
        // Max Ban Tier (-1 to 100, where -1 = infinite)
        general.addEntry(entryBuilder.startIntField(
                Component.translatable("config.simpledeathbans.maxBanTier"),
                config.maxBanTier)
                .setDefaultValue(-1)
                .setMin(-1)
                .setMax(100)
                .setTooltip(
                        Component.literal("Maximum ban tier a player can reach."),
                        Component.literal("-1 = No limit (infinite scaling)").withStyle(ChatFormatting.YELLOW),
                        Component.literal("Tier resets via Mercy Cooldown or commands.").withStyle(ChatFormatting.GRAY),
                        Component.literal("Range: -1 to 100 | Default: -1").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.maxBanTier = newValue; })
                .build());
        
        // Exponential Ban Mode toggle
        general.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Exponential Ban Mode"),
                config.exponentialBanMode)
                .setDefaultValue(false)
                .setTooltip(
                        Component.literal("Changes ban time calculation formula."),
                        Component.literal("OFF: Linear (1, 2, 3, 4, 5...)").withStyle(ChatFormatting.GREEN),
                        Component.literal("ON: Doubling (1, 2, 4, 8, 16...)").withStyle(ChatFormatting.RED),
                        Component.literal("Default: OFF").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.exponentialBanMode = newValue; })
                .build());
        
        // Ghost Echo toggle
        general.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.simpledeathbans.enableGhostEcho"),
                config.enableGhostEcho)
                .setDefaultValue(true)
                .setTooltip(
                        Component.literal("Cosmetic effects when a player is banned."),
                        Component.literal("• Lightning strike at death location").withStyle(ChatFormatting.GRAY),
                        Component.literal("• Custom message: 'lost to the void'").withStyle(ChatFormatting.GRAY),
                        Component.literal("Default: ON").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.enableGhostEcho = newValue; })
                .build());
        
        // --- PvP Settings Header ---
        general.addEntry(entryBuilder.startTextDescription(
                Component.literal("═══ PvP Settings ═══").withStyle(ChatFormatting.GOLD))
                .build());
        
        // PvP Ban Multiplier (0-500%)
        general.addEntry(entryBuilder.startIntField(
                Component.translatable("config.simpledeathbans.pvpBanMultiplier"),
                config.pvpBanMultiplierPercent)
                .setDefaultValue(50)
                .setMin(0)
                .setMax(500)
                .setTooltip(
                        Component.literal("Ban time modifier for player-caused deaths."),
                        Component.literal("50 = half ban, 0 = no ban for PvP").withStyle(ChatFormatting.GRAY),
                        Component.literal("Includes indirect kills (knockback, etc.)").withStyle(ChatFormatting.GRAY),
                        Component.literal("Range: 0-500 | Default: 50").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.pvpBanMultiplierPercent = newValue; })
                .build());
        
        // PvE Ban Multiplier (0-500%)
        general.addEntry(entryBuilder.startIntField(
                Component.literal("PvE Ban Multiplier"),
                config.pveBanMultiplierPercent)
                .setDefaultValue(100)
                .setMin(0)
                .setMax(500)
                .setTooltip(
                        Component.literal("Ban time modifier for environment deaths."),
                        Component.literal("Mobs, fall damage, lava, void, etc.").withStyle(ChatFormatting.GRAY),
                        Component.literal("100 = normal, 0 = no ban for PvE").withStyle(ChatFormatting.GRAY),
                        Component.literal("Range: 0-500 | Default: 100").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.pveBanMultiplierPercent = newValue; })
                .build());
        
        // --- Altar of Resurrection Header ---
        general.addEntry(entryBuilder.startTextDescription(
                Component.literal("═══ Altar of Resurrection ═══").withStyle(ChatFormatting.GOLD))
                .build());
        
        // Resurrection Altar toggle
        general.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.simpledeathbans.enableResurrectionAltar"),
                config.enableResurrectionAltar)
                .setDefaultValue(true)
                .setTooltip(
                        Component.literal("Endgame feature to unban players."),
                        Component.literal("Requires: Netherite beacon + Resurrection Totem").withStyle(ChatFormatting.GRAY),
                        Component.literal("ALL online players must participate!").withStyle(ChatFormatting.YELLOW),
                        Component.literal("Default: ON").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.enableResurrectionAltar = newValue; })
                .build());
        
        // --- Single-Player Settings Header ---
        general.addEntry(entryBuilder.startTextDescription(
                Component.literal("═══ Single-Player Settings ═══").withStyle(ChatFormatting.GOLD))
                .build());
        
        // Single-player toggle (only visible in single-player with cheats)
        if (isSingleplayer && canEdit) {
            general.addEntry(entryBuilder.startBooleanToggle(
                    Component.literal("Enable Mod in Single-Player"),
                    config.singlePlayerEnabled)
                    .setDefaultValue(true)
                    .setTooltip(
                            Component.literal("Enable or disable death bans in single-player."),
                            Component.literal("§c§lRequires cheats enabled!").withStyle(ChatFormatting.RED),
                            Component.literal("Turning OFF disables death processing.").withStyle(ChatFormatting.GRAY),
                            Component.literal("Default: ON").withStyle(ChatFormatting.DARK_GRAY))
                    .setSaveConsumer(newValue -> { if (canEdit) config.singlePlayerEnabled = newValue; })
                    .build());
        } else if (isSingleplayer && !canEdit) {
            general.addEntry(entryBuilder.startTextDescription(
                    Component.literal("⚠ Single-Player toggle requires cheats enabled.")
                            .withStyle(ChatFormatting.RED))
                    .build());
        } else {
            general.addEntry(entryBuilder.startTextDescription(
                    Component.literal("Single-Player toggle only available in single-player worlds.")
                            .withStyle(ChatFormatting.GRAY))
                    .build());
        }
        
        // ============================================
        // CATEGORY: Soul Link/Health Settings
        // ============================================
        ConfigCategory soulLinkHealth = builder.getOrCreateCategory(Component.literal("Soul Link/Health Settings"));
        
        // --- Soul Link Header ---
        soulLinkHealth.addEntry(entryBuilder.startTextDescription(
                Component.literal("═══ Soul Link ═══").withStyle(ChatFormatting.GOLD))
                .build());
        
        // Enable Soul Link toggle
        soulLinkHealth.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.simpledeathbans.enableSoulLink"),
                config.enableSoulLink)
                .setDefaultValue(false)
                .setTooltip(
                        Component.literal("Pairs players together as Soul Partners."),
                        Component.literal("Partners share damage and have a Death Pact.").withStyle(ChatFormatting.GRAY),
                        Component.literal("LETHAL damage kills BOTH partners!").withStyle(ChatFormatting.RED),
                        Component.literal("Default: OFF").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.enableSoulLink = newValue; })
                .build());
        
        // Soul Link Damage Share (0-200%)
        soulLinkHealth.addEntry(entryBuilder.startIntField(
                Component.translatable("config.simpledeathbans.soulLinkDamageShare"),
                config.soulLinkDamageSharePercent)
                .setDefaultValue(100)
                .setMin(0)
                .setMax(200)
                .setTooltip(
                        Component.literal("Damage transferred to your soul partner."),
                        Component.literal("100 = 1:1, 50 = half, 0 = none").withStyle(ChatFormatting.GRAY),
                        Component.literal("Only affects NON-LETHAL hits!").withStyle(ChatFormatting.YELLOW),
                        Component.literal("Lethal damage = Death Pact (both die)").withStyle(ChatFormatting.RED),
                        Component.literal("Range: 0-200 | Default: 100").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.soulLinkDamageSharePercent = newValue; })
                .build());
        
        // Soul Link Share Hunger toggle
        soulLinkHealth.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Share Hunger"),
                config.soulLinkShareHunger)
                .setDefaultValue(false)
                .setTooltip(
                        Component.literal("Share hunger drain with your soul partner."),
                        Component.literal("ON: When you lose hunger, partner loses too").withStyle(ChatFormatting.YELLOW),
                        Component.literal("OFF: Hunger is independent").withStyle(ChatFormatting.GREEN),
                        Component.literal("Default: OFF").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.soulLinkShareHunger = newValue; })
                .build());
        
        // Random Partner Assignment toggle
        soulLinkHealth.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Random Partner Assignment"),
                config.soulLinkRandomPartner)
                .setDefaultValue(true)
                .setTooltip(
                        Component.literal("How players get paired with partners."),
                        Component.literal("ON: Auto-paired randomly on join").withStyle(ChatFormatting.GREEN),
                        Component.literal("OFF: Manual - Sneak+click with Soul Link Totem").withStyle(ChatFormatting.YELLOW),
                        Component.literal("Default: ON").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.soulLinkRandomPartner = newValue; })
                .build());
        
        // Totem Saves Partner toggle
        soulLinkHealth.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Totem Saves Partner"),
                config.soulLinkTotemSavesPartner)
                .setDefaultValue(true)
                .setTooltip(
                        Component.literal("Totem of Undying behavior for Soul Links."),
                        Component.literal("ON: One totem saves BOTH partners").withStyle(ChatFormatting.GREEN),
                        Component.literal("OFF: Only the holder survives").withStyle(ChatFormatting.RED),
                        Component.literal("If BOTH have totems: both consumed, both live").withStyle(ChatFormatting.GRAY),
                        Component.literal("Default: ON").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.soulLinkTotemSavesPartner = newValue; })
                .build());
        
        // --- Soul Link Sever Settings ---
        soulLinkHealth.addEntry(entryBuilder.startTextDescription(
                Component.literal("═══ Soul Link Sever Settings ═══").withStyle(ChatFormatting.GOLD))
                .build());
        
        // Sever Cooldown (0-120 min)
        soulLinkHealth.addEntry(entryBuilder.startIntField(
                Component.literal("Sever Cooldown (minutes)"),
                config.soulLinkSeverCooldownMinutes)
                .setDefaultValue(30)
                .setMin(0)
                .setMax(120)
                .setTooltip(
                        Component.literal("Cooldown after breaking a soul link."),
                        Component.literal("Cannot link with ANY player during this time.").withStyle(ChatFormatting.GRAY),
                        Component.literal("Range: 0-120 | Default: 30").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.soulLinkSeverCooldownMinutes = newValue; })
                .build());
        
        // Sever Ban Tier Penalty (0-10)
        soulLinkHealth.addEntry(entryBuilder.startIntField(
                Component.literal("Sever Ban Tier Penalty"),
                config.soulLinkSeverBanTierIncrease)
                .setDefaultValue(1)
                .setMin(0)
                .setMax(10)
                .setTooltip(
                        Component.literal("Punishment for breaking a soul link."),
                        Component.literal("Your ban tier increases by this amount.").withStyle(ChatFormatting.GRAY),
                        Component.literal("Set to 0 to disable penalty.").withStyle(ChatFormatting.GRAY),
                        Component.literal("Range: 0-10 | Default: 1").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.soulLinkSeverBanTierIncrease = newValue; })
                .build());
        
        // Ex-Partner Cooldown (0-168 hours)
        soulLinkHealth.addEntry(entryBuilder.startIntField(
                Component.literal("Ex-Partner Cooldown (hours)"),
                config.soulLinkExPartnerCooldownHours)
                .setDefaultValue(24)
                .setMin(0)
                .setMax(168)
                .setTooltip(
                        Component.literal("Time before re-linking with an ex-partner."),
                        Component.literal("Prevents quick on/off abuse with same player.").withStyle(ChatFormatting.GRAY),
                        Component.literal("168 hours = 1 week maximum").withStyle(ChatFormatting.GRAY),
                        Component.literal("Range: 0-168 | Default: 24").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.soulLinkExPartnerCooldownHours = newValue; })
                .build());
        
        // Random Reassign Cooldown (0-72 hours)
        soulLinkHealth.addEntry(entryBuilder.startIntField(
                Component.literal("Random Reassign Cooldown (hours)"),
                config.soulLinkRandomReassignCooldownHours)
                .setDefaultValue(12)
                .setMin(0)
                .setMax(72)
                .setTooltip(
                        Component.literal("Grace period before auto-reassignment."),
                        Component.literal("After severing, wait this long before").withStyle(ChatFormatting.GRAY),
                        Component.literal("system assigns you a new random partner.").withStyle(ChatFormatting.GRAY),
                        Component.literal("Only applies if Random Assignment is ON.").withStyle(ChatFormatting.YELLOW),
                        Component.literal("Range: 0-72 | Default: 12").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.soulLinkRandomReassignCooldownHours = newValue; })
                .build());
        
        // Random Assign Check Interval (1-1440 min)
        soulLinkHealth.addEntry(entryBuilder.startIntField(
                Component.literal("Random Assign Check Interval (min)"),
                config.soulLinkRandomAssignCheckIntervalMinutes)
                .setDefaultValue(60)
                .setMin(1)
                .setMax(1440)
                .setTooltip(
                        Component.literal("How often the server checks for unlinked players."),
                        Component.literal("Lower = faster pairing, more server load.").withStyle(ChatFormatting.GRAY),
                        Component.literal("1440 min = 24 hours maximum").withStyle(ChatFormatting.GRAY),
                        Component.literal("Range: 1-1440 | Default: 60").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.soulLinkRandomAssignCheckIntervalMinutes = newValue; })
                .build());
        
        // --- Soul Compass Settings ---
        soulLinkHealth.addEntry(entryBuilder.startTextDescription(
                Component.literal("═══ Soul Compass Settings ═══").withStyle(ChatFormatting.GOLD))
                .build());
        
        // Compass Max Uses (1-100)
        soulLinkHealth.addEntry(entryBuilder.startIntField(
                Component.literal("Compass Max Uses"),
                config.soulLinkCompassMaxUses)
                .setDefaultValue(10)
                .setMin(1)
                .setMax(100)
                .setTooltip(
                        Component.literal("Soul Link Totem can track your partner."),
                        Component.literal("Right-click totem to locate partner.").withStyle(ChatFormatting.GRAY),
                        Component.literal("Each totem has limited tracking uses.").withStyle(ChatFormatting.GRAY),
                        Component.literal("Range: 1-100 | Default: 10").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.soulLinkCompassMaxUses = newValue; })
                .build());
        
        // Compass Cooldown (0-60 min)
        soulLinkHealth.addEntry(entryBuilder.startIntField(
                Component.literal("Compass Cooldown (minutes)"),
                config.soulLinkCompassCooldownMinutes)
                .setDefaultValue(10)
                .setMin(0)
                .setMax(60)
                .setTooltip(
                        Component.literal("Wait time between tracking uses."),
                        Component.literal("Prevents spam-tracking your partner.").withStyle(ChatFormatting.GRAY),
                        Component.literal("Set to 0 for no cooldown.").withStyle(ChatFormatting.GRAY),
                        Component.literal("Range: 0-60 | Default: 10").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.soulLinkCompassCooldownMinutes = newValue; })
                .build());
        
        // --- Shared Health Header ---
        soulLinkHealth.addEntry(entryBuilder.startTextDescription(
                Component.literal("═══ Shared Health ═══").withStyle(ChatFormatting.GOLD))
                .build());
        
        // Enable Shared Health toggle
        soulLinkHealth.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Enable Shared Health"),
                config.enableSharedHealth)
                .setDefaultValue(false)
                .setTooltip(
                        Component.literal("⚠ EXTREME MODE - Server-wide health pool!").withStyle(ChatFormatting.RED),
                        Component.literal("ALL online players share damage.").withStyle(ChatFormatting.YELLOW),
                        Component.literal("If ONE player dies, EVERYONE dies!").withStyle(ChatFormatting.RED),
                        Component.literal("Stacks with Soul Link if both enabled.").withStyle(ChatFormatting.GRAY),
                        Component.literal("Default: OFF").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.enableSharedHealth = newValue; })
                .build());
        
        // Shared Damage Percent (0-200%)
        soulLinkHealth.addEntry(entryBuilder.startIntField(
                Component.literal("Shared Damage Percent"),
                config.sharedHealthDamagePercent)
                .setDefaultValue(100)
                .setMin(0)
                .setMax(200)
                .setTooltip(
                        Component.literal("Damage dealt to ALL other players."),
                        Component.literal("100 = 1:1, 50 = half, 200 = double").withStyle(ChatFormatting.GRAY),
                        Component.literal("Only affects NON-LETHAL damage!").withStyle(ChatFormatting.YELLOW),
                        Component.literal("Lethal damage = instant death for ALL").withStyle(ChatFormatting.RED),
                        Component.literal("Range: 0-200 | Default: 100").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.sharedHealthDamagePercent = newValue; })
                .build());
        
        // Shared Health Share Hunger toggle
        soulLinkHealth.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Share Hunger"),
                config.sharedHealthShareHunger)
                .setDefaultValue(false)
                .setTooltip(
                        Component.literal("Share hunger drain with all players."),
                        Component.literal("ON: When anyone loses hunger, all lose it").withStyle(ChatFormatting.YELLOW),
                        Component.literal("OFF: Hunger is independent").withStyle(ChatFormatting.GREEN),
                        Component.literal("Default: OFF").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.sharedHealthShareHunger = newValue; })
                .build());
        
        // Totem Saves All toggle
        soulLinkHealth.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Totem Saves All"),
                config.sharedHealthTotemSavesAll)
                .setDefaultValue(true)
                .setTooltip(
                        Component.literal("Totem of Undying behavior for Shared Health."),
                        Component.literal("ON: One totem saves EVERYONE").withStyle(ChatFormatting.GREEN),
                        Component.literal("OFF: Only holders survive, others die").withStyle(ChatFormatting.RED),
                        Component.literal("Multiple totems = all consumed, all saved").withStyle(ChatFormatting.GRAY),
                        Component.literal("Default: ON").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.sharedHealthTotemSavesAll = newValue; })
                .build());
        
        // ============================================
        // CATEGORY: Mercy Cooldown Settings
        // ============================================
        ConfigCategory mercy = builder.getOrCreateCategory(Component.translatable("config.simpledeathbans.mercy"));
        
        // Enable Mercy Cooldown toggle
        mercy.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Enable Mercy Cooldown"),
                config.enableMercyCooldown)
                .setDefaultValue(true)
                .setTooltip(
                        Component.literal("Forgiveness system for active players."),
                        Component.literal("Ban tier decreases over time without dying.").withStyle(ChatFormatting.GRAY),
                        Component.literal("Requires activity (not AFK) to count.").withStyle(ChatFormatting.YELLOW),
                        Component.literal("Default: ON").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.enableMercyCooldown = newValue; })
                .build());
        
        // Mercy Playtime (1-168 hours)
        mercy.addEntry(entryBuilder.startIntField(
                Component.translatable("config.simpledeathbans.mercyPlaytimeHours"),
                config.mercyPlaytimeHours)
                .setDefaultValue(24)
                .setMin(1)
                .setMax(168)
                .setTooltip(
                        Component.literal("Active playtime needed to reduce ban tier by 1."),
                        Component.literal("Must be ACTIVE time (not AFK).").withStyle(ChatFormatting.YELLOW),
                        Component.literal("Resets to 0 on death.").withStyle(ChatFormatting.RED),
                        Component.literal("168 hours = 1 week maximum").withStyle(ChatFormatting.GRAY),
                        Component.literal("Range: 1-168 | Default: 24").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.mercyPlaytimeHours = newValue; })
                .build());
        
        // Mercy Movement (0-500 blocks)
        mercy.addEntry(entryBuilder.startIntField(
                Component.translatable("config.simpledeathbans.mercyMovementBlocks"),
                config.mercyMovementBlocks)
                .setDefaultValue(50)
                .setMin(0)
                .setMax(500)
                .setTooltip(
                        Component.literal("Blocks traveled to count as 'active' each check."),
                        Component.literal("Must move this OR interact with blocks.").withStyle(ChatFormatting.GRAY),
                        Component.literal("Prevents AFK farming mercy cooldown.").withStyle(ChatFormatting.YELLOW),
                        Component.literal("Range: 0-500 | Default: 50").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.mercyMovementBlocks = newValue; })
                .build());
        
        // Mercy Block Interactions (0-200)
        mercy.addEntry(entryBuilder.startIntField(
                Component.translatable("config.simpledeathbans.mercyBlockInteractions"),
                config.mercyBlockInteractions)
                .setDefaultValue(20)
                .setMin(0)
                .setMax(200)
                .setTooltip(
                        Component.literal("Block interactions to count as 'active' each check."),
                        Component.literal("Breaking, placing, using blocks, etc.").withStyle(ChatFormatting.GRAY),
                        Component.literal("Must interact this OR move enough blocks.").withStyle(ChatFormatting.GRAY),
                        Component.literal("Range: 0-200 | Default: 20").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.mercyBlockInteractions = newValue; })
                .build());
        
        // Mercy Check Interval (1-60 min)
        mercy.addEntry(entryBuilder.startIntField(
                Component.literal("Check Interval (minutes)"),
                config.mercyCheckIntervalMinutes)
                .setDefaultValue(15)
                .setMin(1)
                .setMax(60)
                .setTooltip(
                        Component.literal("How often to check if player is active."),
                        Component.literal("Each check, player must meet activity threshold.").withStyle(ChatFormatting.GRAY),
                        Component.literal("Lower = stricter AFK detection.").withStyle(ChatFormatting.YELLOW),
                        Component.literal("Range: 1-60 | Default: 15").withStyle(ChatFormatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.mercyCheckIntervalMinutes = newValue; })
                .build());
        
        // ============================================
        // CATEGORY: Keybinds
        // ============================================
        ConfigCategory keybinds = builder.getOrCreateCategory(Component.literal("Keybinds"));
        
        keybinds.addEntry(entryBuilder.fillKeybindingField(
                Component.literal("Open Config Screen"),
                SimpleDeathBansClient.openConfigKeyBinding)
                .setTooltip(
                        Component.literal("Quick access to this config screen."),
                        Component.literal("Can also be set in: Options → Controls").withStyle(ChatFormatting.GRAY),
                        Component.literal("Default: Unbound").withStyle(ChatFormatting.DARK_GRAY))
                .build());
        
        return builder.build();
    }
}
