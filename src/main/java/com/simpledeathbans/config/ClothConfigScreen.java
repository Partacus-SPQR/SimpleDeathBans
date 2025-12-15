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
            /*// Pre-1.21.11: Use player's hasPermissionLevel directly
            isOperator = client.player.hasPermissionLevel(4);
            *///?}
        }
        boolean canEdit = isSingleplayer || isOperator;
        
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.simpledeathbans.title"))
                .setTransparentBackground(true);
        
        // Save handler
        builder.setSavingRunnable(() -> {
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
                        config.soulLinkSeverCooldownMinutes,
                        config.soulLinkSeverBanTierIncrease,
                        config.soulLinkExPartnerCooldownHours,
                        config.soulLinkRandomReassignCooldownHours,
                        config.soulLinkRandomAssignCheckIntervalMinutes,
                        config.soulLinkCompassMaxUses,
                        config.soulLinkCompassCooldownMinutes,
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
                        config.enableResurrectionAltar,
                        config.singlePlayerEnabled
                    ));
                    LOGGER.info("Sent config update to server");
                }
            } else {
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
                            .formatted(Formatting.GOLD))
                    .build());
        }
        
        // Base Ban Minutes (1-60)
        general.addEntry(entryBuilder.startIntField(
                Text.translatable("config.simpledeathbans.baseBanMinutes"),
                config.baseBanMinutes)
                .setDefaultValue(1)
                .setMin(1)
                .setMax(60)
                .setTooltip(
                        Text.literal("Base ban duration per tier (in minutes)."),
                        Text.literal("Formula: baseBan × tier × multiplier").formatted(Formatting.GRAY),
                        Text.literal("Range: 1-60 | Default: 1").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.baseBanMinutes = newValue; })
                .build());
        
        // Ban Multiplier (10-1000%)
        general.addEntry(entryBuilder.startIntField(
                Text.translatable("config.simpledeathbans.banMultiplier"),
                config.banMultiplierPercent)
                .setDefaultValue(100)
                .setMin(10)
                .setMax(1000)
                .setTooltip(
                        Text.literal("Global ban time multiplier (percentage)."),
                        Text.literal("100 = normal, 200 = double, 50 = half").formatted(Formatting.GRAY),
                        Text.literal("Range: 10-1000 | Default: 100").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.banMultiplierPercent = newValue; })
                .build());
        
        // Max Ban Tier (-1 to 100, where -1 = infinite)
        general.addEntry(entryBuilder.startIntField(
                Text.translatable("config.simpledeathbans.maxBanTier"),
                config.maxBanTier)
                .setDefaultValue(-1)
                .setMin(-1)
                .setMax(100)
                .setTooltip(
                        Text.literal("Maximum ban tier a player can reach."),
                        Text.literal("-1 = No limit (infinite scaling)").formatted(Formatting.YELLOW),
                        Text.literal("Tier resets via Mercy Cooldown or commands.").formatted(Formatting.GRAY),
                        Text.literal("Range: -1 to 100 | Default: -1").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.maxBanTier = newValue; })
                .build());
        
        // Exponential Ban Mode toggle
        general.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Exponential Ban Mode"),
                config.exponentialBanMode)
                .setDefaultValue(false)
                .setTooltip(
                        Text.literal("Changes ban time calculation formula."),
                        Text.literal("OFF: Linear (1, 2, 3, 4, 5...)").formatted(Formatting.GREEN),
                        Text.literal("ON: Doubling (1, 2, 4, 8, 16...)").formatted(Formatting.RED),
                        Text.literal("Default: OFF").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.exponentialBanMode = newValue; })
                .build());
        
        // Ghost Echo toggle
        general.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("config.simpledeathbans.enableGhostEcho"),
                config.enableGhostEcho)
                .setDefaultValue(true)
                .setTooltip(
                        Text.literal("Cosmetic effects when a player is banned."),
                        Text.literal("• Lightning strike at death location").formatted(Formatting.GRAY),
                        Text.literal("• Custom message: 'lost to the void'").formatted(Formatting.GRAY),
                        Text.literal("Default: ON").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.enableGhostEcho = newValue; })
                .build());
        
        // --- PvP Settings Header ---
        general.addEntry(entryBuilder.startTextDescription(
                Text.literal("═══ PvP Settings ═══").formatted(Formatting.GOLD))
                .build());
        
        // PvP Ban Multiplier (0-500%)
        general.addEntry(entryBuilder.startIntField(
                Text.translatable("config.simpledeathbans.pvpBanMultiplier"),
                config.pvpBanMultiplierPercent)
                .setDefaultValue(50)
                .setMin(0)
                .setMax(500)
                .setTooltip(
                        Text.literal("Ban time modifier for player-caused deaths."),
                        Text.literal("50 = half ban, 0 = no ban for PvP").formatted(Formatting.GRAY),
                        Text.literal("Includes indirect kills (knockback, etc.)").formatted(Formatting.GRAY),
                        Text.literal("Range: 0-500 | Default: 50").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.pvpBanMultiplierPercent = newValue; })
                .build());
        
        // PvE Ban Multiplier (0-500%)
        general.addEntry(entryBuilder.startIntField(
                Text.literal("PvE Ban Multiplier"),
                config.pveBanMultiplierPercent)
                .setDefaultValue(100)
                .setMin(0)
                .setMax(500)
                .setTooltip(
                        Text.literal("Ban time modifier for environment deaths."),
                        Text.literal("Mobs, fall damage, lava, void, etc.").formatted(Formatting.GRAY),
                        Text.literal("100 = normal, 0 = no ban for PvE").formatted(Formatting.GRAY),
                        Text.literal("Range: 0-500 | Default: 100").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.pveBanMultiplierPercent = newValue; })
                .build());
        
        // --- Altar of Resurrection Header ---
        general.addEntry(entryBuilder.startTextDescription(
                Text.literal("═══ Altar of Resurrection ═══").formatted(Formatting.GOLD))
                .build());
        
        // Resurrection Altar toggle
        general.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("config.simpledeathbans.enableResurrectionAltar"),
                config.enableResurrectionAltar)
                .setDefaultValue(true)
                .setTooltip(
                        Text.literal("Endgame feature to unban players."),
                        Text.literal("Requires: Netherite beacon + Resurrection Totem").formatted(Formatting.GRAY),
                        Text.literal("ALL online players must participate!").formatted(Formatting.YELLOW),
                        Text.literal("Default: ON").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.enableResurrectionAltar = newValue; })
                .build());
        
        // --- Single-Player Settings Header ---
        general.addEntry(entryBuilder.startTextDescription(
                Text.literal("═══ Single-Player Settings ═══").formatted(Formatting.GOLD))
                .build());
        
        // Single-player toggle (only visible in single-player with cheats)
        if (isSingleplayer && canEdit) {
            general.addEntry(entryBuilder.startBooleanToggle(
                    Text.literal("Enable Mod in Single-Player"),
                    config.singlePlayerEnabled)
                    .setDefaultValue(true)
                    .setTooltip(
                            Text.literal("Enable or disable death bans in single-player."),
                            Text.literal("§c§lRequires cheats enabled!").formatted(Formatting.RED),
                            Text.literal("Turning OFF disables death processing.").formatted(Formatting.GRAY),
                            Text.literal("Default: ON").formatted(Formatting.DARK_GRAY))
                    .setSaveConsumer(newValue -> { if (canEdit) config.singlePlayerEnabled = newValue; })
                    .build());
        } else if (isSingleplayer && !canEdit) {
            general.addEntry(entryBuilder.startTextDescription(
                    Text.literal("⚠ Single-Player toggle requires cheats enabled.")
                            .formatted(Formatting.RED))
                    .build());
        } else {
            general.addEntry(entryBuilder.startTextDescription(
                    Text.literal("Single-Player toggle only available in single-player worlds.")
                            .formatted(Formatting.GRAY))
                    .build());
        }
        
        // ============================================
        // CATEGORY: Soul Link/Health Settings
        // ============================================
        ConfigCategory soulLinkHealth = builder.getOrCreateCategory(Text.literal("Soul Link/Health Settings"));
        
        // --- Soul Link Header ---
        soulLinkHealth.addEntry(entryBuilder.startTextDescription(
                Text.literal("═══ Soul Link ═══").formatted(Formatting.GOLD))
                .build());
        
        // Enable Soul Link toggle
        soulLinkHealth.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("config.simpledeathbans.enableSoulLink"),
                config.enableSoulLink)
                .setDefaultValue(false)
                .setTooltip(
                        Text.literal("Pairs players together as Soul Partners."),
                        Text.literal("Partners share damage and have a Death Pact.").formatted(Formatting.GRAY),
                        Text.literal("LETHAL damage kills BOTH partners!").formatted(Formatting.RED),
                        Text.literal("Default: OFF").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.enableSoulLink = newValue; })
                .build());
        
        // Soul Link Damage Share (0-200%)
        soulLinkHealth.addEntry(entryBuilder.startIntField(
                Text.translatable("config.simpledeathbans.soulLinkDamageShare"),
                config.soulLinkDamageSharePercent)
                .setDefaultValue(100)
                .setMin(0)
                .setMax(200)
                .setTooltip(
                        Text.literal("Damage transferred to your soul partner."),
                        Text.literal("100 = 1:1, 50 = half, 0 = none").formatted(Formatting.GRAY),
                        Text.literal("Only affects NON-LETHAL hits!").formatted(Formatting.YELLOW),
                        Text.literal("Lethal damage = Death Pact (both die)").formatted(Formatting.RED),
                        Text.literal("Range: 0-200 | Default: 100").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.soulLinkDamageSharePercent = newValue; })
                .build());
        
        // Random Partner Assignment toggle
        soulLinkHealth.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Random Partner Assignment"),
                config.soulLinkRandomPartner)
                .setDefaultValue(true)
                .setTooltip(
                        Text.literal("How players get paired with partners."),
                        Text.literal("ON: Auto-paired randomly on join").formatted(Formatting.GREEN),
                        Text.literal("OFF: Manual - Sneak+click with Soul Link Totem").formatted(Formatting.YELLOW),
                        Text.literal("Default: ON").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.soulLinkRandomPartner = newValue; })
                .build());
        
        // Totem Saves Partner toggle
        soulLinkHealth.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Totem Saves Partner"),
                config.soulLinkTotemSavesPartner)
                .setDefaultValue(true)
                .setTooltip(
                        Text.literal("Totem of Undying behavior for Soul Links."),
                        Text.literal("ON: One totem saves BOTH partners").formatted(Formatting.GREEN),
                        Text.literal("OFF: Only the holder survives").formatted(Formatting.RED),
                        Text.literal("If BOTH have totems: both consumed, both live").formatted(Formatting.GRAY),
                        Text.literal("Default: ON").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.soulLinkTotemSavesPartner = newValue; })
                .build());
        
        // --- Soul Link Sever Settings ---
        soulLinkHealth.addEntry(entryBuilder.startTextDescription(
                Text.literal("═══ Soul Link Sever Settings ═══").formatted(Formatting.GOLD))
                .build());
        
        // Sever Cooldown (0-120 min)
        soulLinkHealth.addEntry(entryBuilder.startIntField(
                Text.literal("Sever Cooldown (minutes)"),
                config.soulLinkSeverCooldownMinutes)
                .setDefaultValue(30)
                .setMin(0)
                .setMax(120)
                .setTooltip(
                        Text.literal("Cooldown after breaking a soul link."),
                        Text.literal("Cannot link with ANY player during this time.").formatted(Formatting.GRAY),
                        Text.literal("Range: 0-120 | Default: 30").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.soulLinkSeverCooldownMinutes = newValue; })
                .build());
        
        // Sever Ban Tier Penalty (0-10)
        soulLinkHealth.addEntry(entryBuilder.startIntField(
                Text.literal("Sever Ban Tier Penalty"),
                config.soulLinkSeverBanTierIncrease)
                .setDefaultValue(1)
                .setMin(0)
                .setMax(10)
                .setTooltip(
                        Text.literal("Punishment for breaking a soul link."),
                        Text.literal("Your ban tier increases by this amount.").formatted(Formatting.GRAY),
                        Text.literal("Set to 0 to disable penalty.").formatted(Formatting.GRAY),
                        Text.literal("Range: 0-10 | Default: 1").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.soulLinkSeverBanTierIncrease = newValue; })
                .build());
        
        // Ex-Partner Cooldown (0-168 hours)
        soulLinkHealth.addEntry(entryBuilder.startIntField(
                Text.literal("Ex-Partner Cooldown (hours)"),
                config.soulLinkExPartnerCooldownHours)
                .setDefaultValue(24)
                .setMin(0)
                .setMax(168)
                .setTooltip(
                        Text.literal("Time before re-linking with an ex-partner."),
                        Text.literal("Prevents quick on/off abuse with same player.").formatted(Formatting.GRAY),
                        Text.literal("168 hours = 1 week maximum").formatted(Formatting.GRAY),
                        Text.literal("Range: 0-168 | Default: 24").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.soulLinkExPartnerCooldownHours = newValue; })
                .build());
        
        // Random Reassign Cooldown (0-72 hours)
        soulLinkHealth.addEntry(entryBuilder.startIntField(
                Text.literal("Random Reassign Cooldown (hours)"),
                config.soulLinkRandomReassignCooldownHours)
                .setDefaultValue(12)
                .setMin(0)
                .setMax(72)
                .setTooltip(
                        Text.literal("Grace period before auto-reassignment."),
                        Text.literal("After severing, wait this long before").formatted(Formatting.GRAY),
                        Text.literal("system assigns you a new random partner.").formatted(Formatting.GRAY),
                        Text.literal("Only applies if Random Assignment is ON.").formatted(Formatting.YELLOW),
                        Text.literal("Range: 0-72 | Default: 12").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.soulLinkRandomReassignCooldownHours = newValue; })
                .build());
        
        // Random Assign Check Interval (1-1440 min)
        soulLinkHealth.addEntry(entryBuilder.startIntField(
                Text.literal("Random Assign Check Interval (min)"),
                config.soulLinkRandomAssignCheckIntervalMinutes)
                .setDefaultValue(60)
                .setMin(1)
                .setMax(1440)
                .setTooltip(
                        Text.literal("How often the server checks for unlinked players."),
                        Text.literal("Lower = faster pairing, more server load.").formatted(Formatting.GRAY),
                        Text.literal("1440 min = 24 hours maximum").formatted(Formatting.GRAY),
                        Text.literal("Range: 1-1440 | Default: 60").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.soulLinkRandomAssignCheckIntervalMinutes = newValue; })
                .build());
        
        // --- Soul Compass Settings ---
        soulLinkHealth.addEntry(entryBuilder.startTextDescription(
                Text.literal("═══ Soul Compass Settings ═══").formatted(Formatting.GOLD))
                .build());
        
        // Compass Max Uses (1-100)
        soulLinkHealth.addEntry(entryBuilder.startIntField(
                Text.literal("Compass Max Uses"),
                config.soulLinkCompassMaxUses)
                .setDefaultValue(10)
                .setMin(1)
                .setMax(100)
                .setTooltip(
                        Text.literal("Soul Link Totem can track your partner."),
                        Text.literal("Right-click totem to locate partner.").formatted(Formatting.GRAY),
                        Text.literal("Each totem has limited tracking uses.").formatted(Formatting.GRAY),
                        Text.literal("Range: 1-100 | Default: 10").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.soulLinkCompassMaxUses = newValue; })
                .build());
        
        // Compass Cooldown (0-60 min)
        soulLinkHealth.addEntry(entryBuilder.startIntField(
                Text.literal("Compass Cooldown (minutes)"),
                config.soulLinkCompassCooldownMinutes)
                .setDefaultValue(10)
                .setMin(0)
                .setMax(60)
                .setTooltip(
                        Text.literal("Wait time between tracking uses."),
                        Text.literal("Prevents spam-tracking your partner.").formatted(Formatting.GRAY),
                        Text.literal("Set to 0 for no cooldown.").formatted(Formatting.GRAY),
                        Text.literal("Range: 0-60 | Default: 10").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.soulLinkCompassCooldownMinutes = newValue; })
                .build());
        
        // --- Shared Health Header ---
        soulLinkHealth.addEntry(entryBuilder.startTextDescription(
                Text.literal("═══ Shared Health ═══").formatted(Formatting.GOLD))
                .build());
        
        // Enable Shared Health toggle
        soulLinkHealth.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Enable Shared Health"),
                config.enableSharedHealth)
                .setDefaultValue(false)
                .setTooltip(
                        Text.literal("⚠ EXTREME MODE - Server-wide health pool!").formatted(Formatting.RED),
                        Text.literal("ALL online players share damage.").formatted(Formatting.YELLOW),
                        Text.literal("If ONE player dies, EVERYONE dies!").formatted(Formatting.RED),
                        Text.literal("Stacks with Soul Link if both enabled.").formatted(Formatting.GRAY),
                        Text.literal("Default: OFF").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.enableSharedHealth = newValue; })
                .build());
        
        // Shared Damage Percent (0-200%)
        soulLinkHealth.addEntry(entryBuilder.startIntField(
                Text.literal("Shared Damage Percent"),
                config.sharedHealthDamagePercent)
                .setDefaultValue(100)
                .setMin(0)
                .setMax(200)
                .setTooltip(
                        Text.literal("Damage dealt to ALL other players."),
                        Text.literal("100 = 1:1, 50 = half, 200 = double").formatted(Formatting.GRAY),
                        Text.literal("Only affects NON-LETHAL damage!").formatted(Formatting.YELLOW),
                        Text.literal("Lethal damage = instant death for ALL").formatted(Formatting.RED),
                        Text.literal("Range: 0-200 | Default: 100").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.sharedHealthDamagePercent = newValue; })
                .build());
        
        // Totem Saves All toggle
        soulLinkHealth.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Totem Saves All"),
                config.sharedHealthTotemSavesAll)
                .setDefaultValue(true)
                .setTooltip(
                        Text.literal("Totem of Undying behavior for Shared Health."),
                        Text.literal("ON: One totem saves EVERYONE").formatted(Formatting.GREEN),
                        Text.literal("OFF: Only holders survive, others die").formatted(Formatting.RED),
                        Text.literal("Multiple totems = all consumed, all saved").formatted(Formatting.GRAY),
                        Text.literal("Default: ON").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.sharedHealthTotemSavesAll = newValue; })
                .build());
        
        // ============================================
        // CATEGORY: Mercy Cooldown Settings
        // ============================================
        ConfigCategory mercy = builder.getOrCreateCategory(Text.translatable("config.simpledeathbans.mercy"));
        
        // Enable Mercy Cooldown toggle
        mercy.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Enable Mercy Cooldown"),
                config.enableMercyCooldown)
                .setDefaultValue(true)
                .setTooltip(
                        Text.literal("Forgiveness system for active players."),
                        Text.literal("Ban tier decreases over time without dying.").formatted(Formatting.GRAY),
                        Text.literal("Requires activity (not AFK) to count.").formatted(Formatting.YELLOW),
                        Text.literal("Default: ON").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.enableMercyCooldown = newValue; })
                .build());
        
        // Mercy Playtime (1-168 hours)
        mercy.addEntry(entryBuilder.startIntField(
                Text.translatable("config.simpledeathbans.mercyPlaytimeHours"),
                config.mercyPlaytimeHours)
                .setDefaultValue(24)
                .setMin(1)
                .setMax(168)
                .setTooltip(
                        Text.literal("Active playtime needed to reduce ban tier by 1."),
                        Text.literal("Must be ACTIVE time (not AFK).").formatted(Formatting.YELLOW),
                        Text.literal("Resets to 0 on death.").formatted(Formatting.RED),
                        Text.literal("168 hours = 1 week maximum").formatted(Formatting.GRAY),
                        Text.literal("Range: 1-168 | Default: 24").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.mercyPlaytimeHours = newValue; })
                .build());
        
        // Mercy Movement (0-500 blocks)
        mercy.addEntry(entryBuilder.startIntField(
                Text.translatable("config.simpledeathbans.mercyMovementBlocks"),
                config.mercyMovementBlocks)
                .setDefaultValue(50)
                .setMin(0)
                .setMax(500)
                .setTooltip(
                        Text.literal("Blocks traveled to count as 'active' each check."),
                        Text.literal("Must move this OR interact with blocks.").formatted(Formatting.GRAY),
                        Text.literal("Prevents AFK farming mercy cooldown.").formatted(Formatting.YELLOW),
                        Text.literal("Range: 0-500 | Default: 50").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.mercyMovementBlocks = newValue; })
                .build());
        
        // Mercy Block Interactions (0-200)
        mercy.addEntry(entryBuilder.startIntField(
                Text.translatable("config.simpledeathbans.mercyBlockInteractions"),
                config.mercyBlockInteractions)
                .setDefaultValue(20)
                .setMin(0)
                .setMax(200)
                .setTooltip(
                        Text.literal("Block interactions to count as 'active' each check."),
                        Text.literal("Breaking, placing, using blocks, etc.").formatted(Formatting.GRAY),
                        Text.literal("Must interact this OR move enough blocks.").formatted(Formatting.GRAY),
                        Text.literal("Range: 0-200 | Default: 20").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.mercyBlockInteractions = newValue; })
                .build());
        
        // Mercy Check Interval (1-60 min)
        mercy.addEntry(entryBuilder.startIntField(
                Text.literal("Check Interval (minutes)"),
                config.mercyCheckIntervalMinutes)
                .setDefaultValue(15)
                .setMin(1)
                .setMax(60)
                .setTooltip(
                        Text.literal("How often to check if player is active."),
                        Text.literal("Each check, player must meet activity threshold.").formatted(Formatting.GRAY),
                        Text.literal("Lower = stricter AFK detection.").formatted(Formatting.YELLOW),
                        Text.literal("Range: 1-60 | Default: 15").formatted(Formatting.DARK_GRAY))
                .setSaveConsumer(newValue -> { if (canEdit) config.mercyCheckIntervalMinutes = newValue; })
                .build());
        
        // ============================================
        // CATEGORY: Keybinds
        // ============================================
        ConfigCategory keybinds = builder.getOrCreateCategory(Text.literal("Keybinds"));
        
        keybinds.addEntry(entryBuilder.fillKeybindingField(
                Text.literal("Open Config Screen"),
                SimpleDeathBansClient.openConfigKeyBinding)
                .setTooltip(
                        Text.literal("Quick access to this config screen."),
                        Text.literal("Can also be set in: Options → Controls").formatted(Formatting.GRAY),
                        Text.literal("Default: Unbound").formatted(Formatting.DARK_GRAY))
                .build());
        
        return builder.build();
    }
}
