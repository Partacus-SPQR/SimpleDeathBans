package com.simpledeathbans.config;

import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.network.ConfigSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.Click;
//? if >=1.21.11
import net.minecraft.command.permission.Permission;
//? if >=1.21.11
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Fallback config screen when Cloth Config is not installed.
 * Implements all required features from the guide:
 * - Sliders for numeric values
 * - Tooltips for all options
 * - Reset buttons
 * - Scrollable content
 * - Operator permission checks
 */
public class SimpleFallbackConfigScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger("SimpleDeathBans-FallbackConfig");
    
    // Layout constants
    private static final int HEADER_HEIGHT = 35;
    private static final int FOOTER_HEIGHT = 35;
    private static final int ROW_HEIGHT = 26;
    private static final int WIDGET_WIDTH = 180;
    private static final int RESET_BTN_WIDTH = 40;
    private static final int SPACING = 4;
    private static final int SCROLL_SPEED = 10;
    private static final int SCROLLBAR_WIDTH = 6;
    
    private final Screen parent;
    private final ModConfig config;
    private final List<ScrollableWidget> scrollableWidgets = new ArrayList<>();
    private final List<TooltipArea> tooltipAreas = new ArrayList<>();
    
    // Permission state
    private final boolean isSingleplayer;
    private final boolean isOperator;
    private final boolean canEdit;
    
    // Scroll state
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int contentHeight = 0;
    private boolean isDraggingScrollbar = false;
    
    // Widgets - General
    private IntSlider baseBanMinutesSlider;
    private IntSlider maxBanTierSlider;
    private ButtonWidget exponentialModeToggle;
    private ButtonWidget ghostEchoToggle;
    
    // Widgets - Soul Link
    private ButtonWidget soulLinkToggle;
    private ButtonWidget soulLinkRandomPartnerToggle;
    private ButtonWidget soulLinkTotemSavesToggle;
    
    // Widgets - Shared Health
    private ButtonWidget sharedHealthToggle;
    private ButtonWidget totemSavesAllToggle;
    
    // Widgets - Mercy
    private IntSlider mercyPlaytimeSlider;
    private IntSlider mercyMovementSlider;
    private IntSlider mercyInteractionsSlider;
    private IntSlider mercyIntervalSlider;
    
    // Widgets - Altar
    private ButtonWidget altarToggle;
    
    // Footer buttons
    private final List<ClickableWidget> footerButtons = new ArrayList<>();
    
    public SimpleFallbackConfigScreen(Screen parent) {
        super(Text.literal("SimpleDeathBans Configuration"));
        this.parent = parent;
        
        // Load config
        if (SimpleDeathBans.getInstance() != null && SimpleDeathBans.getInstance().getConfig() != null) {
            this.config = SimpleDeathBans.getInstance().getConfig();
        } else {
            this.config = ModConfig.load();
        }
        
        // Check permissions
        MinecraftClient client = MinecraftClient.getInstance();
        this.isSingleplayer = client.isInSingleplayer();
        boolean op = false;
        if (client.player != null) {
            //? if >=1.21.11 {
            op = client.player.getPermissions().hasPermission(new Permission.Level(PermissionLevel.OWNERS));
            //?} else {
            /*// Pre-1.21.11: Use player's hasPermissionLevel directly
            op = client.player.hasPermissionLevel(4);
            *///?}
        }
        this.isOperator = op;
        this.canEdit = isSingleplayer || isOperator;
        
        LOGGER.info("Opening fallback config screen - canEdit: {}, isSingleplayer: {}, isOperator: {}", 
            canEdit, isSingleplayer, isOperator);
    }
    
    @Override
    protected void init() {
        scrollableWidgets.clear();
        tooltipAreas.clear();
        
        int centerX = this.width / 2;
        int widgetX = centerX - WIDGET_WIDTH / 2 - RESET_BTN_WIDTH / 2;
        int resetX = widgetX + WIDGET_WIDTH + SPACING;
        int y = 0;
        
        // ============================================
        // HEADER: Permission Warning (if not operator)
        // ============================================
        if (!canEdit) {
            // Warning will be drawn in render()
            y += ROW_HEIGHT;
        }
        
        // ============================================
        // SECTION: General Settings
        // ============================================
        y += ROW_HEIGHT; // Section header space
        
        // Enable Death Bans toggle
        ButtonWidget enableDeathBansToggle = ButtonWidget.builder(
            Text.literal("Enable Death Bans: " + (config.enableDeathBans ? "ON" : "OFF")),
            button -> {
                if (canEdit) {
                    config.enableDeathBans = !config.enableDeathBans;
                    button.setMessage(Text.literal("Enable Death Bans: " + (config.enableDeathBans ? "ON" : "OFF")));
                } else {
                    showPermissionDenied();
                }
            }
        ).dimensions(widgetX, y, WIDGET_WIDTH, 20).build();
        addScrollableWidget(enableDeathBansToggle, y);
        addResetButton(resetX, y, () -> {
            config.enableDeathBans = true;
            enableDeathBansToggle.setMessage(Text.literal("Enable Death Bans: ON"));
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Master toggle for death ban system."),
            Text.literal("OFF: Deaths have no ban consequences.").formatted(Formatting.YELLOW),
            Text.literal("ON: Deaths result in temporary bans.").formatted(Formatting.GREEN),
            Text.literal("Default: ON").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Base Ban Minutes (1-60)
        baseBanMinutesSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            Text.literal("Base Ban Time: " + config.baseBanMinutes + " min"),
            config.baseBanMinutes, 1, 60) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Base Ban Time: " + getValue() + " min"));
            }
            @Override
            protected void applyValue() {
                if (canEdit) config.baseBanMinutes = getValue();
            }
        };
        addScrollableWidget(baseBanMinutesSlider, y);
        addResetButton(resetX, y, () -> {
            baseBanMinutesSlider.setValue(1, 1, 60);
            config.baseBanMinutes = 1;
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Base ban duration per tier (in minutes)."),
            Text.literal("Formula: baseBan × tier × multiplier").formatted(Formatting.GRAY),
            Text.literal("Range: 1-60 | Default: 1").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Ban Multiplier (10-1000%)
        IntSlider banMultiplierSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            Text.literal("Ban Multiplier: " + config.banMultiplierPercent + "%"),
            config.banMultiplierPercent, 10, 1000) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Ban Multiplier: " + getValue() + "%"));
            }
            @Override
            protected void applyValue() {
                if (canEdit) config.banMultiplierPercent = getValue();
            }
        };
        addScrollableWidget(banMultiplierSlider, y);
        addResetButton(resetX, y, () -> {
            banMultiplierSlider.setValue(100, 10, 1000);
            config.banMultiplierPercent = 100;
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Global ban time multiplier (percentage)."),
            Text.literal("100 = normal, 200 = double, 50 = half").formatted(Formatting.GRAY),
            Text.literal("Range: 10-1000 | Default: 100").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Max Ban Tier (-1 = infinite, 1-100 = actual tiers)
        int displayMaxTier = config.maxBanTier < 0 || config.maxBanTier > 100 ? -1 : config.maxBanTier;
        maxBanTierSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            getMaxTierText(displayMaxTier),
            displayMaxTier, -1, 100) {
            @Override
            protected void updateMessage() {
                setMessage(getMaxTierText(getValue()));
            }
            @Override
            protected void applyValue() {
                if (canEdit) config.maxBanTier = getValue() == -1 ? Integer.MAX_VALUE : getValue();
            }
        };
        addScrollableWidget(maxBanTierSlider, y);
        addResetButton(resetX, y, () -> {
            maxBanTierSlider.setValue(-1, -1, 100);
            config.maxBanTier = Integer.MAX_VALUE;
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Maximum ban tier a player can reach."),
            Text.literal("-1 = No limit (infinite scaling)").formatted(Formatting.YELLOW),
            Text.literal("Range: -1 to 100 | Default: -1").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Exponential Ban Mode
        exponentialModeToggle = ButtonWidget.builder(
            Text.literal("Exponential Mode: " + (config.exponentialBanMode ? "ON" : "OFF")),
            button -> {
                if (canEdit) {
                    config.exponentialBanMode = !config.exponentialBanMode;
                    button.setMessage(Text.literal("Exponential Mode: " + (config.exponentialBanMode ? "ON" : "OFF")));
                } else {
                    showPermissionDenied();
                }
            }
        ).dimensions(widgetX, y, WIDGET_WIDTH, 20).build();
        addScrollableWidget(exponentialModeToggle, y);
        addResetButton(resetX, y, () -> {
            config.exponentialBanMode = false;
            exponentialModeToggle.setMessage(Text.literal("Exponential Mode: OFF"));
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Changes ban time calculation formula."),
            Text.literal("OFF: Linear (1, 2, 3, 4, 5...)").formatted(Formatting.GREEN),
            Text.literal("ON: Doubling (1, 2, 4, 8, 16...)").formatted(Formatting.RED),
            Text.literal("Default: OFF").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Ghost Echo
        ghostEchoToggle = ButtonWidget.builder(
            Text.literal("Ghost Echo: " + (config.enableGhostEcho ? "ON" : "OFF")),
            button -> {
                if (canEdit) {
                    config.enableGhostEcho = !config.enableGhostEcho;
                    button.setMessage(Text.literal("Ghost Echo: " + (config.enableGhostEcho ? "ON" : "OFF")));
                } else {
                    showPermissionDenied();
                }
            }
        ).dimensions(widgetX, y, WIDGET_WIDTH, 20).build();
        addScrollableWidget(ghostEchoToggle, y);
        addResetButton(resetX, y, () -> {
            config.enableGhostEcho = true;
            ghostEchoToggle.setMessage(Text.literal("Ghost Echo: ON"));
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Cosmetic effects when a player is banned."),
            Text.literal("• Lightning strike at death location").formatted(Formatting.GRAY),
            Text.literal("• Custom message: 'lost to the void'").formatted(Formatting.GRAY),
            Text.literal("Default: ON").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // PvP Ban Multiplier (0-500%)
        IntSlider pvpMultiplierSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            Text.literal("PvP Ban Multiplier: " + config.pvpBanMultiplierPercent + "%"),
            config.pvpBanMultiplierPercent, 0, 500) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("PvP Ban Multiplier: " + getValue() + "%"));
            }
            @Override
            protected void applyValue() {
                if (canEdit) config.pvpBanMultiplierPercent = getValue();
            }
        };
        addScrollableWidget(pvpMultiplierSlider, y);
        addResetButton(resetX, y, () -> {
            pvpMultiplierSlider.setValue(50, 0, 500);
            config.pvpBanMultiplierPercent = 50;
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Ban time modifier for player-caused deaths."),
            Text.literal("50 = half ban, 0 = no ban for PvP").formatted(Formatting.GRAY),
            Text.literal("Range: 0-500 | Default: 50").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // PvE Ban Multiplier (0-500%)
        IntSlider pveMultiplierSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            Text.literal("PvE Ban Multiplier: " + config.pveBanMultiplierPercent + "%"),
            config.pveBanMultiplierPercent, 0, 500) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("PvE Ban Multiplier: " + getValue() + "%"));
            }
            @Override
            protected void applyValue() {
                if (canEdit) config.pveBanMultiplierPercent = getValue();
            }
        };
        addScrollableWidget(pveMultiplierSlider, y);
        addResetButton(resetX, y, () -> {
            pveMultiplierSlider.setValue(100, 0, 500);
            config.pveBanMultiplierPercent = 100;
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Ban time modifier for environment deaths."),
            Text.literal("Mobs, fall damage, lava, void, etc.").formatted(Formatting.GRAY),
            Text.literal("Range: 0-500 | Default: 100").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // ============================================
        // SECTION: Soul Link Settings
        // ============================================
        y += ROW_HEIGHT / 2; // Section spacing
        
        // Enable Soul Link
        soulLinkToggle = ButtonWidget.builder(
            Text.literal("Soul Link: " + (config.enableSoulLink ? "ON" : "OFF")),
            button -> {
                if (canEdit) {
                    config.enableSoulLink = !config.enableSoulLink;
                    button.setMessage(Text.literal("Soul Link: " + (config.enableSoulLink ? "ON" : "OFF")));
                } else {
                    showPermissionDenied();
                }
            }
        ).dimensions(widgetX, y, WIDGET_WIDTH, 20).build();
        addScrollableWidget(soulLinkToggle, y);
        addResetButton(resetX, y, () -> {
            config.enableSoulLink = false;
            soulLinkToggle.setMessage(Text.literal("Soul Link: OFF"));
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Pairs players together as Soul Partners."),
            Text.literal("Partners share damage and have a Death Pact.").formatted(Formatting.GRAY),
            Text.literal("LETHAL damage kills BOTH partners!").formatted(Formatting.RED),
            Text.literal("Default: OFF").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Soul Link Damage Share (0-200%)
        IntSlider soulLinkDamageSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            Text.literal("Soul Damage Share: " + config.soulLinkDamageSharePercent + "%"),
            config.soulLinkDamageSharePercent, 0, 200) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Soul Damage Share: " + getValue() + "%"));
            }
            @Override
            protected void applyValue() {
                if (canEdit) config.soulLinkDamageSharePercent = getValue();
            }
        };
        addScrollableWidget(soulLinkDamageSlider, y);
        addResetButton(resetX, y, () -> {
            soulLinkDamageSlider.setValue(100, 0, 200);
            config.soulLinkDamageSharePercent = 100;
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Damage transferred to your soul partner."),
            Text.literal("Only affects NON-LETHAL hits!").formatted(Formatting.YELLOW),
            Text.literal("Lethal damage = Death Pact (both die)").formatted(Formatting.RED),
            Text.literal("Range: 0-200 | Default: 100").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Soul Link Share Hunger toggle
        ButtonWidget soulLinkShareHungerToggle = ButtonWidget.builder(
            Text.literal("Share Hunger: " + (config.soulLinkShareHunger ? "ON" : "OFF")),
            button -> {
                if (canEdit) {
                    config.soulLinkShareHunger = !config.soulLinkShareHunger;
                    button.setMessage(Text.literal("Share Hunger: " + (config.soulLinkShareHunger ? "ON" : "OFF")));
                } else {
                    showPermissionDenied();
                }
            }
        ).dimensions(widgetX, y, WIDGET_WIDTH, 20).build();
        addScrollableWidget(soulLinkShareHungerToggle, y);
        addResetButton(resetX, y, () -> {
            config.soulLinkShareHunger = false;
            soulLinkShareHungerToggle.setMessage(Text.literal("Share Hunger: OFF"));
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Share hunger drain with your soul partner."),
            Text.literal("ON: When you lose hunger, partner loses too").formatted(Formatting.YELLOW),
            Text.literal("OFF: Hunger is independent").formatted(Formatting.GREEN),
            Text.literal("Default: OFF").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Soul Link Random Partner
        soulLinkRandomPartnerToggle = ButtonWidget.builder(
            Text.literal("Random Partner: " + (config.soulLinkRandomPartner ? "ON" : "OFF")),
            button -> {
                if (canEdit) {
                    config.soulLinkRandomPartner = !config.soulLinkRandomPartner;
                    button.setMessage(Text.literal("Random Partner: " + (config.soulLinkRandomPartner ? "ON" : "OFF")));
                } else {
                    showPermissionDenied();
                }
            }
        ).dimensions(widgetX, y, WIDGET_WIDTH, 20).build();
        addScrollableWidget(soulLinkRandomPartnerToggle, y);
        addResetButton(resetX, y, () -> {
            config.soulLinkRandomPartner = true;
            soulLinkRandomPartnerToggle.setMessage(Text.literal("Random Partner: ON"));
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("How players get paired with partners."),
            Text.literal("ON: Auto-paired randomly on join").formatted(Formatting.GREEN),
            Text.literal("OFF: Manual - Sneak+click with Soul Link Totem").formatted(Formatting.YELLOW),
            Text.literal("Default: ON").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Soul Link Totem Saves Partner
        soulLinkTotemSavesToggle = ButtonWidget.builder(
            Text.literal("Totem Saves Partner: " + (config.soulLinkTotemSavesPartner ? "ON" : "OFF")),
            button -> {
                if (canEdit) {
                    config.soulLinkTotemSavesPartner = !config.soulLinkTotemSavesPartner;
                    button.setMessage(Text.literal("Totem Saves Partner: " + (config.soulLinkTotemSavesPartner ? "ON" : "OFF")));
                } else {
                    showPermissionDenied();
                }
            }
        ).dimensions(widgetX, y, WIDGET_WIDTH, 20).build();
        addScrollableWidget(soulLinkTotemSavesToggle, y);
        addResetButton(resetX, y, () -> {
            config.soulLinkTotemSavesPartner = true;
            soulLinkTotemSavesToggle.setMessage(Text.literal("Totem Saves Partner: ON"));
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Totem of Undying behavior for Soul Links."),
            Text.literal("ON: One totem saves BOTH partners").formatted(Formatting.GREEN),
            Text.literal("OFF: Only the holder survives").formatted(Formatting.RED),
            Text.literal("Default: ON").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // ============================================
        // SECTION: Soul Link Sever/Cooldown Settings
        // ============================================
        y += ROW_HEIGHT / 2;
        
        // Sever Cooldown (minutes)
        IntSlider severCooldownSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            Text.literal("Sever Cooldown: " + config.soulLinkSeverCooldownMinutes + " min"),
            config.soulLinkSeverCooldownMinutes, 0, 120) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Sever Cooldown: " + getValue() + " min"));
            }
            @Override
            protected void applyValue() {
                if (canEdit) config.soulLinkSeverCooldownMinutes = getValue();
            }
        };
        addScrollableWidget(severCooldownSlider, y);
        addResetButton(resetX, y, () -> {
            severCooldownSlider.setValue(30, 0, 120);
            config.soulLinkSeverCooldownMinutes = 30;
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Cooldown after breaking a soul link."),
            Text.literal("Cannot link with ANY player during this time.").formatted(Formatting.GRAY),
            Text.literal("Range: 0-120 | Default: 30").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Sever Ban Tier Penalty
        IntSlider severPenaltySlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            Text.literal("Sever Penalty: +" + config.soulLinkSeverBanTierIncrease + " tier"),
            config.soulLinkSeverBanTierIncrease, 0, 10) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Sever Penalty: +" + getValue() + " tier"));
            }
            @Override
            protected void applyValue() {
                if (canEdit) config.soulLinkSeverBanTierIncrease = getValue();
            }
        };
        addScrollableWidget(severPenaltySlider, y);
        addResetButton(resetX, y, () -> {
            severPenaltySlider.setValue(1, 0, 10);
            config.soulLinkSeverBanTierIncrease = 1;
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Punishment for breaking a soul link."),
            Text.literal("Your ban tier increases by this amount.").formatted(Formatting.GRAY),
            Text.literal("Range: 0-10 | Default: 1").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Ex-Partner Cooldown (hours)
        IntSlider exPartnerCooldownSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            Text.literal("Ex-Partner Cooldown: " + config.soulLinkExPartnerCooldownHours + " hr"),
            config.soulLinkExPartnerCooldownHours, 0, 168) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Ex-Partner Cooldown: " + getValue() + " hr"));
            }
            @Override
            protected void applyValue() {
                if (canEdit) config.soulLinkExPartnerCooldownHours = getValue();
            }
        };
        addScrollableWidget(exPartnerCooldownSlider, y);
        addResetButton(resetX, y, () -> {
            exPartnerCooldownSlider.setValue(24, 0, 168);
            config.soulLinkExPartnerCooldownHours = 24;
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Time before re-linking with an ex-partner."),
            Text.literal("Prevents quick on/off abuse with same player.").formatted(Formatting.GRAY),
            Text.literal("Range: 0-168 | Default: 24").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Random Reassign Cooldown (hours)
        IntSlider randomReassignSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            Text.literal("Random Reassign: " + config.soulLinkRandomReassignCooldownHours + " hr"),
            config.soulLinkRandomReassignCooldownHours, 0, 72) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Random Reassign: " + getValue() + " hr"));
            }
            @Override
            protected void applyValue() {
                if (canEdit) config.soulLinkRandomReassignCooldownHours = getValue();
            }
        };
        addScrollableWidget(randomReassignSlider, y);
        addResetButton(resetX, y, () -> {
            randomReassignSlider.setValue(12, 0, 72);
            config.soulLinkRandomReassignCooldownHours = 12;
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Grace period before auto-reassignment."),
            Text.literal("Only applies if Random Assignment is ON.").formatted(Formatting.YELLOW),
            Text.literal("Range: 0-72 | Default: 12").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Random Assign Check Interval (minutes)
        IntSlider randomAssignCheckSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            getIntervalText(config.soulLinkRandomAssignCheckIntervalMinutes),
            config.soulLinkRandomAssignCheckIntervalMinutes, 1, 1440) {
            @Override
            protected void updateMessage() {
                setMessage(getIntervalText(getValue()));
            }
            @Override
            protected void applyValue() {
                if (canEdit) config.soulLinkRandomAssignCheckIntervalMinutes = getValue();
            }
        };
        addScrollableWidget(randomAssignCheckSlider, y);
        addResetButton(resetX, y, () -> {
            randomAssignCheckSlider.setValue(60, 1, 1440);
            config.soulLinkRandomAssignCheckIntervalMinutes = 60;
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("How often the server checks for unlinked players."),
            Text.literal("Lower = faster pairing, more server load.").formatted(Formatting.GRAY),
            Text.literal("Range: 1-1440 | Default: 60").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // ============================================
        // SECTION: Soul Compass Settings
        // ============================================
        y += ROW_HEIGHT / 2;
        
        // Compass Max Uses
        IntSlider compassUsesSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            Text.literal("Compass Uses: " + config.soulLinkCompassMaxUses),
            config.soulLinkCompassMaxUses, 1, 100) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Compass Uses: " + getValue()));
            }
            @Override
            protected void applyValue() {
                if (canEdit) config.soulLinkCompassMaxUses = getValue();
            }
        };
        addScrollableWidget(compassUsesSlider, y);
        addResetButton(resetX, y, () -> {
            compassUsesSlider.setValue(10, 1, 100);
            config.soulLinkCompassMaxUses = 10;
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Soul Link Totem can track your partner."),
            Text.literal("Right-click totem to locate partner.").formatted(Formatting.GRAY),
            Text.literal("Range: 1-100 | Default: 10").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Compass Cooldown (minutes)
        IntSlider compassCooldownSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            Text.literal("Compass Cooldown: " + config.soulLinkCompassCooldownMinutes + " min"),
            config.soulLinkCompassCooldownMinutes, 0, 60) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Compass Cooldown: " + getValue() + " min"));
            }
            @Override
            protected void applyValue() {
                if (canEdit) config.soulLinkCompassCooldownMinutes = getValue();
            }
        };
        addScrollableWidget(compassCooldownSlider, y);
        addResetButton(resetX, y, () -> {
            compassCooldownSlider.setValue(10, 0, 60);
            config.soulLinkCompassCooldownMinutes = 10;
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Wait time between tracking uses."),
            Text.literal("Prevents spam-tracking your partner.").formatted(Formatting.GRAY),
            Text.literal("Range: 0-60 | Default: 10").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // ============================================
        // SECTION: Shared Health Settings
        // ============================================
        y += ROW_HEIGHT / 2;
        
        // Enable Shared Health
        sharedHealthToggle = ButtonWidget.builder(
            Text.literal("Shared Health: " + (config.enableSharedHealth ? "ON" : "OFF")),
            button -> {
                if (canEdit) {
                    config.enableSharedHealth = !config.enableSharedHealth;
                    button.setMessage(Text.literal("Shared Health: " + (config.enableSharedHealth ? "ON" : "OFF")));
                } else {
                    showPermissionDenied();
                }
            }
        ).dimensions(widgetX, y, WIDGET_WIDTH, 20).build();
        addScrollableWidget(sharedHealthToggle, y);
        addResetButton(resetX, y, () -> {
            config.enableSharedHealth = false;
            sharedHealthToggle.setMessage(Text.literal("Shared Health: OFF"));
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("⚠ EXTREME MODE - Server-wide health pool!").formatted(Formatting.RED),
            Text.literal("ALL online players share damage.").formatted(Formatting.YELLOW),
            Text.literal("If ONE player dies, EVERYONE dies!").formatted(Formatting.RED),
            Text.literal("Default: OFF").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Shared Health Damage Percent (0-200%)
        IntSlider sharedDamageSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            Text.literal("Shared Damage: " + config.sharedHealthDamagePercent + "%"),
            config.sharedHealthDamagePercent, 0, 200) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Shared Damage: " + getValue() + "%"));
            }
            @Override
            protected void applyValue() {
                if (canEdit) config.sharedHealthDamagePercent = getValue();
            }
        };
        addScrollableWidget(sharedDamageSlider, y);
        addResetButton(resetX, y, () -> {
            sharedDamageSlider.setValue(100, 0, 200);
            config.sharedHealthDamagePercent = 100;
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Damage dealt to ALL other players."),
            Text.literal("Only affects NON-LETHAL damage!").formatted(Formatting.YELLOW),
            Text.literal("Lethal damage = instant death for ALL").formatted(Formatting.RED),
            Text.literal("Range: 0-200 | Default: 100").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Shared Health Share Hunger toggle
        ButtonWidget sharedHealthShareHungerToggle = ButtonWidget.builder(
            Text.literal("Share Hunger: " + (config.sharedHealthShareHunger ? "ON" : "OFF")),
            button -> {
                if (canEdit) {
                    config.sharedHealthShareHunger = !config.sharedHealthShareHunger;
                    button.setMessage(Text.literal("Share Hunger: " + (config.sharedHealthShareHunger ? "ON" : "OFF")));
                } else {
                    showPermissionDenied();
                }
            }
        ).dimensions(widgetX, y, WIDGET_WIDTH, 20).build();
        addScrollableWidget(sharedHealthShareHungerToggle, y);
        addResetButton(resetX, y, () -> {
            config.sharedHealthShareHunger = false;
            sharedHealthShareHungerToggle.setMessage(Text.literal("Share Hunger: OFF"));
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Share hunger drain with all players."),
            Text.literal("ON: When anyone loses hunger, all lose it").formatted(Formatting.YELLOW),
            Text.literal("OFF: Hunger is independent").formatted(Formatting.GREEN),
            Text.literal("Default: OFF").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Totem Saves All
        totemSavesAllToggle = ButtonWidget.builder(
            Text.literal("Totem Saves All: " + (config.sharedHealthTotemSavesAll ? "ON" : "OFF")),
            button -> {
                if (canEdit) {
                    config.sharedHealthTotemSavesAll = !config.sharedHealthTotemSavesAll;
                    button.setMessage(Text.literal("Totem Saves All: " + (config.sharedHealthTotemSavesAll ? "ON" : "OFF")));
                } else {
                    showPermissionDenied();
                }
            }
        ).dimensions(widgetX, y, WIDGET_WIDTH, 20).build();
        addScrollableWidget(totemSavesAllToggle, y);
        addResetButton(resetX, y, () -> {
            config.sharedHealthTotemSavesAll = true;
            totemSavesAllToggle.setMessage(Text.literal("Totem Saves All: ON"));
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Totem of Undying behavior for Shared Health."),
            Text.literal("ON: One totem saves EVERYONE").formatted(Formatting.GREEN),
            Text.literal("OFF: Only holders survive, others die").formatted(Formatting.RED),
            Text.literal("Default: ON").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // ============================================
        // SECTION: Mercy Cooldown Settings
        // ============================================
        y += ROW_HEIGHT / 2;
        
        // Enable Mercy Cooldown
        ButtonWidget enableMercyToggle = ButtonWidget.builder(
            Text.literal("Enable Mercy Cooldown: " + (config.enableMercyCooldown ? "ON" : "OFF")),
            button -> {
                if (canEdit) {
                    config.enableMercyCooldown = !config.enableMercyCooldown;
                    button.setMessage(Text.literal("Enable Mercy Cooldown: " + (config.enableMercyCooldown ? "ON" : "OFF")));
                } else {
                    showPermissionDenied();
                }
            }
        ).dimensions(widgetX, y, WIDGET_WIDTH, 20).build();
        addScrollableWidget(enableMercyToggle, y);
        addResetButton(resetX, y, () -> {
            config.enableMercyCooldown = true;
            enableMercyToggle.setMessage(Text.literal("Enable Mercy Cooldown: ON"));
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Forgiveness system for active players."),
            Text.literal("Ban tier decreases over time without dying.").formatted(Formatting.GRAY),
            Text.literal("Requires activity (not AFK) to count.").formatted(Formatting.YELLOW),
            Text.literal("Default: ON").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Mercy Playtime Hours
        mercyPlaytimeSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            Text.literal("Mercy Playtime: " + config.mercyPlaytimeHours + "h"),
            config.mercyPlaytimeHours, 1, 168) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Mercy Playtime: " + getValue() + "h"));
            }
            @Override
            protected void applyValue() {
                if (canEdit) config.mercyPlaytimeHours = getValue();
            }
        };
        addScrollableWidget(mercyPlaytimeSlider, y);
        addResetButton(resetX, y, () -> {
            mercyPlaytimeSlider.setValue(24, 1, 168);
            config.mercyPlaytimeHours = 24;
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Active playtime needed to reduce ban tier by 1."),
            Text.literal("Must be ACTIVE time (not AFK). Resets on death.").formatted(Formatting.YELLOW),
            Text.literal("Range: 1-168 | Default: 24").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Mercy Movement Blocks
        mercyMovementSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            Text.literal("Movement Required: " + config.mercyMovementBlocks),
            config.mercyMovementBlocks, 0, 500) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Movement Required: " + getValue()));
            }
            @Override
            protected void applyValue() {
                if (canEdit) config.mercyMovementBlocks = getValue();
            }
        };
        addScrollableWidget(mercyMovementSlider, y);
        addResetButton(resetX, y, () -> {
            mercyMovementSlider.setValue(50, 0, 500);
            config.mercyMovementBlocks = 50;
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Blocks traveled to count as 'active' each check."),
            Text.literal("Must move this OR interact with blocks.").formatted(Formatting.GRAY),
            Text.literal("Range: 0-500 | Default: 50").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Mercy Block Interactions
        mercyInteractionsSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            Text.literal("Interactions Required: " + config.mercyBlockInteractions),
            config.mercyBlockInteractions, 0, 200) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Interactions Required: " + getValue()));
            }
            @Override
            protected void applyValue() {
                if (canEdit) config.mercyBlockInteractions = getValue();
            }
        };
        addScrollableWidget(mercyInteractionsSlider, y);
        addResetButton(resetX, y, () -> {
            mercyInteractionsSlider.setValue(20, 0, 200);
            config.mercyBlockInteractions = 20;
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Block interactions to count as 'active' each check."),
            Text.literal("Breaking, placing, using blocks, etc.").formatted(Formatting.GRAY),
            Text.literal("Range: 0-200 | Default: 20").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Mercy Check Interval
        mercyIntervalSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            Text.literal("Check Interval: " + config.mercyCheckIntervalMinutes + " min"),
            config.mercyCheckIntervalMinutes, 1, 60) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Check Interval: " + getValue() + " min"));
            }
            @Override
            protected void applyValue() {
                if (canEdit) config.mercyCheckIntervalMinutes = getValue();
            }
        };
        addScrollableWidget(mercyIntervalSlider, y);
        addResetButton(resetX, y, () -> {
            mercyIntervalSlider.setValue(15, 1, 60);
            config.mercyCheckIntervalMinutes = 15;
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("How often to check if player is active."),
            Text.literal("Lower = stricter AFK detection.").formatted(Formatting.YELLOW),
            Text.literal("Range: 1-60 | Default: 15").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // ============================================
        // SECTION: Altar Settings
        // ============================================
        y += ROW_HEIGHT / 2;
        
        // Enable Resurrection Altar
        altarToggle = ButtonWidget.builder(
            Text.literal("Resurrection Altar: " + (config.enableResurrectionAltar ? "ON" : "OFF")),
            button -> {
                if (canEdit) {
                    config.enableResurrectionAltar = !config.enableResurrectionAltar;
                    button.setMessage(Text.literal("Resurrection Altar: " + (config.enableResurrectionAltar ? "ON" : "OFF")));
                } else {
                    showPermissionDenied();
                }
            }
        ).dimensions(widgetX, y, WIDGET_WIDTH, 20).build();
        addScrollableWidget(altarToggle, y);
        addResetButton(resetX, y, () -> {
            config.enableResurrectionAltar = true;
            altarToggle.setMessage(Text.literal("Resurrection Altar: ON"));
        });
        addTooltip(widgetX, y, WIDGET_WIDTH + RESET_BTN_WIDTH + SPACING, 20,
            Text.literal("Endgame feature to unban players."),
            Text.literal("Requires: Netherite beacon + Resurrection Totem").formatted(Formatting.GRAY),
            Text.literal("ALL online players must participate!").formatted(Formatting.YELLOW),
            Text.literal("Default: ON").formatted(Formatting.DARK_GRAY));
        y += ROW_HEIGHT;
        
        // Calculate content height and max scroll
        contentHeight = y + ROW_HEIGHT;
        int viewportHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT;
        maxScroll = Math.max(0, contentHeight - viewportHeight);
        
        // ============================================
        // FOOTER: Save & Close, Key Binds, Cancel Buttons
        // ============================================
        int footerY = this.height - 28;
        int buttonWidth = 90;
        int totalButtonWidth = buttonWidth * 3 + SPACING * 2;
        int footerX = centerX - totalButtonWidth / 2;
        
        // Save & Close button
        footerButtons.add(ButtonWidget.builder(
            Text.literal("Save & Close"),
            button -> saveAndClose()
        ).dimensions(footerX, footerY, buttonWidth, 20).build());
        
        // Key Binds button - goes directly to KeybindsScreen
        footerButtons.add(ButtonWidget.builder(
            Text.literal("Key Binds"),
            button -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null) {
                    client.setScreen(new KeybindsScreen(this, client.options));
                }
            }
        ).dimensions(footerX + buttonWidth + SPACING, footerY, buttonWidth, 20).build());
        
        // Cancel button
        footerButtons.add(ButtonWidget.builder(
            Text.literal("Cancel"),
            button -> close()
        ).dimensions(footerX + (buttonWidth + SPACING) * 2, footerY, buttonWidth, 20).build());
        
        // Add footer buttons to screen
        for (ClickableWidget btn : footerButtons) {
            this.addDrawableChild(btn);
        }
    }
    
    private void addScrollableWidget(net.minecraft.client.gui.widget.ClickableWidget widget, int originalY) {
        scrollableWidgets.add(new ScrollableWidget(widget, originalY));
        this.addDrawableChild(widget);
    }
    
    private void addResetButton(int x, int y, Runnable resetAction) {
        ButtonWidget resetBtn = ButtonWidget.builder(Text.literal("↺"), button -> {
            if (canEdit) {
                resetAction.run();
            } else {
                showPermissionDenied();
            }
        }).dimensions(x, y, RESET_BTN_WIDTH, 20).build();
        addScrollableWidget(resetBtn, y);
    }
    
    private void addTooltip(int x, int y, int width, int height, Text... lines) {
        List<Text> tooltip = new ArrayList<>();
        for (Text line : lines) {
            tooltip.add(line);
        }
        tooltipAreas.add(new TooltipArea(x, y, width, height, tooltip));
    }
    
    private Text getMaxTierText(int value) {
        if (value == -1) return Text.literal("Max Ban Tier: Infinite");
        return Text.literal("Max Ban Tier: " + value);
    }
    
    private Text getIntervalText(int minutes) {
        if (minutes >= 60) {
            int hours = minutes / 60;
            int mins = minutes % 60;
            if (mins > 0) {
                return Text.literal("Check Interval: " + hours + "h " + mins + "m");
            }
            return Text.literal("Check Interval: " + hours + " hr");
        }
        return Text.literal("Check Interval: " + minutes + " min");
    }
    
    private void showPermissionDenied() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                Text.literal("✖ Must be Operator level 4 in order to make changes.")
                    .formatted(Formatting.RED),
                false
            );
        }
    }
    
    private void saveAndClose() {
        if (canEdit) {
            config.save();
            LOGGER.info("Config saved - baseBanMinutes: {}, maxBanTier: {}, enableSoulLink: {}", 
                config.baseBanMinutes, config.maxBanTier, config.enableSoulLink);
            
            // Send config to server
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
            showPermissionDenied();
        }
        close();
    }
    
    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render background manually to avoid blur issues
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        
        // Update widget positions based on scroll
        int viewportTop = HEADER_HEIGHT;
        for (ScrollableWidget sw : scrollableWidgets) {
            int newY = viewportTop + sw.originalY - scrollOffset;
            sw.widget.setY(newY);
            // Hide widgets outside viewport
            boolean visible = newY >= viewportTop - 20 && newY < this.height - FOOTER_HEIGHT;
            sw.widget.visible = visible;
        }
        
        // Draw header
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        
        // Draw permission warning if not operator
        if (!canEdit) {
            context.drawCenteredTextWithShadow(this.textRenderer, 
                Text.literal("⚠ Viewing only - Operator level 4 required to edit.")
                    .formatted(Formatting.GOLD),
                this.width / 2, HEADER_HEIGHT - 8, 0xFFAA00);
        }
        
        // Draw section headers
        int centerX = this.width / 2;
        int sectionX = centerX - WIDGET_WIDTH / 2 - RESET_BTN_WIDTH / 2;
        
        // Enable scissor for content area
        context.enableScissor(0, HEADER_HEIGHT, this.width, this.height - FOOTER_HEIGHT);
        
        // Render scrollable widgets only
        for (ScrollableWidget sw : scrollableWidgets) {
            if (sw.widget.visible) {
                sw.widget.render(context, mouseX, mouseY, delta);
            }
        }
        
        context.disableScissor();
        
        // Render footer buttons outside scissor
        for (ClickableWidget btn : footerButtons) {
            btn.render(context, mouseX, mouseY, delta);
        }
        
        // Draw scrollbar if needed
        if (maxScroll > 0) {
            int scrollbarX = this.width - SCROLLBAR_WIDTH - 4;
            int scrollbarHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT;
            int thumbHeight = Math.max(20, (int)((float)scrollbarHeight * scrollbarHeight / contentHeight));
            int thumbY = HEADER_HEIGHT + (int)((float)scrollOffset / maxScroll * (scrollbarHeight - thumbHeight));
            
            // Track
            context.fill(scrollbarX, HEADER_HEIGHT, scrollbarX + SCROLLBAR_WIDTH, 
                this.height - FOOTER_HEIGHT, 0x40FFFFFF);
            // Thumb
            context.fill(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, 
                thumbY + thumbHeight, 0xAAFFFFFF);
        }
        
        // Draw tooltips
        for (TooltipArea area : tooltipAreas) {
            int areaY = HEADER_HEIGHT + area.y - scrollOffset;
            if (mouseX >= area.x && mouseX <= area.x + area.width &&
                mouseY >= areaY && mouseY <= areaY + area.height &&
                mouseY >= HEADER_HEIGHT && mouseY < this.height - FOOTER_HEIGHT) {
                context.drawTooltip(this.textRenderer, area.tooltip, mouseX, mouseY);
                break;
            }
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(verticalAmount * SCROLL_SPEED)));
        return true;
    }
    
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        // Check for scrollbar click
        if (button == 0 && maxScroll > 0) {
            int scrollbarX = this.width - SCROLLBAR_WIDTH - 4;
            if (mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH &&
                mouseY >= HEADER_HEIGHT && mouseY < this.height - FOOTER_HEIGHT) {
                isDraggingScrollbar = true;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }
    
    @Override
    public boolean mouseReleased(Click click) {
        int button = click.button();
        if (button == 0) {
            isDraggingScrollbar = false;
        }
        return super.mouseReleased(click);
    }
    
    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double mouseY = click.y();
        int button = click.button();
        if (isDraggingScrollbar && button == 0) {
            int scrollbarHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT;
            float percent = (float)(mouseY - HEADER_HEIGHT) / scrollbarHeight;
            scrollOffset = (int)(percent * maxScroll);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }
    
    // ============================================
    // Helper Classes
    // ============================================
    
    private record ScrollableWidget(net.minecraft.client.gui.widget.ClickableWidget widget, int originalY) {}
    
    private record TooltipArea(int x, int y, int width, int height, List<Text> tooltip) {}
    
    /**
     * Custom integer slider widget.
     */
    private abstract static class IntSlider extends SliderWidget {
        private final int min;
        private final int max;
        
        public IntSlider(int x, int y, int width, int height, Text text, int value, int min, int max) {
            super(x, y, width, height, text, (double)(value - min) / (max - min));
            this.min = min;
            this.max = max;
        }
        
        public int getValue() {
            return (int)(this.value * (max - min) + min);
        }
        
        public void setValue(int value, int min, int max) {
            this.value = (double)(value - min) / (max - min);
            updateMessage();
        }
        
        @Override
        protected abstract void updateMessage();
        
        @Override
        protected abstract void applyValue();
    }
}
