package com.simpledeathbans.config;

import com.simpledeathbans.SimpleDeathBans;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Fallback config screen using vanilla widgets when Cloth Config is not available.
 */
public class FallbackConfigScreen extends Screen {
    private final Screen parent;
    private ModConfig config;
    
    // Layout constants
    private static final int HEADER_HEIGHT = 35;
    private static final int FOOTER_HEIGHT = 35;
    private static final int ROW_HEIGHT = 24;
    private static final int WIDGET_WIDTH = 180;
    private static final int RESET_BTN_WIDTH = 40;
    private static final int SPACING = 4;
    private static final int SCROLL_SPEED = 10;
    private static final int SCROLLBAR_WIDTH = 6;
    
    // Scroll state
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private int contentHeight = 0;
    private boolean isDraggingScrollbar = false;
    private int scrollbarDragOffset = 0;
    
    // Tooltip tracking
    private record TooltipEntry(int x, int y, int width, int height, String tooltip) {}
    private final List<TooltipEntry> tooltips = new ArrayList<>();
    
    // Track scrollable widgets with their original Y positions
    private record WidgetEntry(ClickableWidget widget, int originalY) {}
    private final List<WidgetEntry> scrollableWidgets = new ArrayList<>();
    
    // Track footer buttons (non-scrollable)
    private final List<ClickableWidget> footerButtons = new ArrayList<>();
    
    // Working copies of config values
    private int baseBanMinutes;
    private double banMultiplier;
    private int maxBanTier;
    private boolean exponentialBanMode;
    private boolean enableSoulLink;
    private double soulLinkDamageShare;
    private boolean enableSharedHealth;
    private double sharedHealthDamagePercent;
    private boolean sharedHealthTotemSavesAll;
    private int mercyPlaytimeHours;
    private int mercyMovementBlocks;
    private int mercyBlockInteractions;
    private int mercyCheckIntervalMinutes;
    private double pvpBanMultiplier;
    private double pveBanMultiplier;
    private boolean enableGhostEcho;
    private boolean enableResurrectionAltar;
    
    public FallbackConfigScreen(Screen parent) {
        super(Text.translatable("config.simpledeathbans.title"));
        this.parent = parent;
        
        // Initialize working copies - handle client-side where getInstance might be null
        if (SimpleDeathBans.getInstance() != null && SimpleDeathBans.getInstance().getConfig() != null) {
            this.config = SimpleDeathBans.getInstance().getConfig();
        } else {
            this.config = ModConfig.load();
        }
        
        this.baseBanMinutes = config.baseBanMinutes;
        this.banMultiplier = config.banMultiplier;
        this.maxBanTier = config.maxBanTier;
        this.exponentialBanMode = config.exponentialBanMode;
        this.enableSoulLink = config.enableSoulLink;
        this.soulLinkDamageShare = config.soulLinkDamageShare;
        this.enableSharedHealth = config.enableSharedHealth;
        this.sharedHealthDamagePercent = config.sharedHealthDamagePercent;
        this.sharedHealthTotemSavesAll = config.sharedHealthTotemSavesAll;
        this.mercyPlaytimeHours = config.mercyPlaytimeHours;
        this.mercyMovementBlocks = config.mercyMovementBlocks;
        this.mercyBlockInteractions = config.mercyBlockInteractions;
        this.mercyCheckIntervalMinutes = config.mercyCheckIntervalMinutes;
        this.pvpBanMultiplier = config.pvpBanMultiplier;
        this.pveBanMultiplier = config.pveBanMultiplier;
        this.enableGhostEcho = config.enableGhostEcho;
        this.enableResurrectionAltar = config.enableResurrectionAltar;
    }
    
    @Override
    protected void init() {
        super.init();
        tooltips.clear();
        scrollableWidgets.clear();
        footerButtons.clear();
        
        int centerX = this.width / 2;
        int totalWidth = WIDGET_WIDTH + SPACING + RESET_BTN_WIDTH;
        int widgetX = centerX - totalWidth / 2;
        int resetX = widgetX + WIDGET_WIDTH + SPACING;
        int y = HEADER_HEIGHT;
        
        // Count options for scroll calculation (18 options total)
        int numberOfOptions = 18;
        contentHeight = numberOfOptions * ROW_HEIGHT;
        int contentAreaHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT;
        maxScrollOffset = Math.max(0, contentHeight - contentAreaHeight);
        scrollOffset = Math.min(scrollOffset, maxScrollOffset);
        
        // === GENERAL SETTINGS ===
        
        // Base Ban Minutes
        addTooltip(widgetX, y, totalWidth, 20, "Base ban time in minutes per tier. Default: 1");
        IntSlider baseBanSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20, 
            "Base Ban Time", baseBanMinutes, 1, 60, val -> baseBanMinutes = val);
        addScrollableWidget(baseBanSlider, y);
        addScrollableWidget(createResetButton(resetX, y, () -> {
            baseBanMinutes = 1;
            baseBanSlider.setValue(1);
        }), y);
        y += ROW_HEIGHT;
        
        // Ban Multiplier
        addTooltip(widgetX, y, totalWidth, 20, "Multiplier for ban time. Default: 1.0");
        DoubleSlider banMultSlider = new DoubleSlider(widgetX, y, WIDGET_WIDTH, 20,
            "Ban Multiplier", banMultiplier, 0.1, 10.0, val -> banMultiplier = val);
        addScrollableWidget(banMultSlider, y);
        addScrollableWidget(createResetButton(resetX, y, () -> {
            banMultiplier = 1.0;
            banMultSlider.setValue(1.0);
        }), y);
        y += ROW_HEIGHT;
        
        // Max Ban Tier (clamped for display, stored as very large)
        addTooltip(widgetX, y, totalWidth, 20, "Max tier (100+ = infinite). Use 100 for unlimited. Default: Infinite");
        int displayMaxTier = maxBanTier >= 100 ? 100 : maxBanTier;
        IntSlider maxTierSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            "Max Ban Tier", displayMaxTier, 1, 100, val -> maxBanTier = val >= 100 ? Integer.MAX_VALUE : val);
        addScrollableWidget(maxTierSlider, y);
        addScrollableWidget(createResetButton(resetX, y, () -> {
            maxBanTier = Integer.MAX_VALUE;
            maxTierSlider.setValue(100);
        }), y);
        y += ROW_HEIGHT;
        
        // Exponential Ban Mode
        addTooltip(widgetX, y, totalWidth, 20, "Use doubling (1,2,4,8,16...) instead of linear. Default: OFF");
        ButtonWidget expModeBtn = createToggleButton(widgetX, y, "Exponential Mode", exponentialBanMode, val -> exponentialBanMode = val);
        addScrollableWidget(expModeBtn, y);
        addScrollableWidget(createResetButton(resetX, y, () -> {
            exponentialBanMode = false;
            updateToggleButton(expModeBtn, "Exponential Mode", false);
        }), y);
        y += ROW_HEIGHT;
        
        // Enable Ghost Echo
        addTooltip(widgetX, y, totalWidth, 20, "Enable lightning strike and custom death message. Default: ON");
        ButtonWidget ghostEchoBtn = createToggleButton(widgetX, y, "Ghost Echo", enableGhostEcho, val -> enableGhostEcho = val);
        addScrollableWidget(ghostEchoBtn, y);
        addScrollableWidget(createResetButton(resetX, y, () -> {
            enableGhostEcho = true;
            updateToggleButton(ghostEchoBtn, "Ghost Echo", true);
        }), y);
        y += ROW_HEIGHT;
        
        // === SOUL LINK SETTINGS ===
        
        // Enable Soul Link
        addTooltip(widgetX, y, totalWidth, 20, "Enable Soul Link feature. Default: OFF");
        ButtonWidget soulLinkBtn = createToggleButton(widgetX, y, "Soul Link", enableSoulLink, val -> enableSoulLink = val);
        addScrollableWidget(soulLinkBtn, y);
        addScrollableWidget(createResetButton(resetX, y, () -> {
            enableSoulLink = false;
            updateToggleButton(soulLinkBtn, "Soul Link", false);
        }), y);
        y += ROW_HEIGHT;
        
        // Soul Link Damage Share
        addTooltip(widgetX, y, totalWidth, 20, "Damage shared to soul partner (1.0 = 0.5 hearts). Default: 1.0");
        DoubleSlider soulDmgSlider = new DoubleSlider(widgetX, y, WIDGET_WIDTH, 20,
            "Damage Share", soulLinkDamageShare, 0.0, 10.0, val -> soulLinkDamageShare = val);
        addScrollableWidget(soulDmgSlider, y);
        addScrollableWidget(createResetButton(resetX, y, () -> {
            soulLinkDamageShare = 1.0;
            soulDmgSlider.setValue(1.0);
        }), y);
        y += ROW_HEIGHT;
        
        // === SHARED HEALTH SETTINGS ===
        
        // Enable Shared Health
        addTooltip(widgetX, y, totalWidth, 20, "Server-wide damage sharing (all players share health). Default: OFF");
        ButtonWidget sharedHealthBtn = createToggleButton(widgetX, y, "Shared Health", enableSharedHealth, val -> enableSharedHealth = val);
        addScrollableWidget(sharedHealthBtn, y);
        addScrollableWidget(createResetButton(resetX, y, () -> {
            enableSharedHealth = false;
            updateToggleButton(sharedHealthBtn, "Shared Health", false);
        }), y);
        y += ROW_HEIGHT;
        
        // Shared Health Damage Percent
        addTooltip(widgetX, y, totalWidth, 20, "Percent of damage shared to all (1.0 = 100%). Default: 1.0");
        DoubleSlider sharedDmgSlider = new DoubleSlider(widgetX, y, WIDGET_WIDTH, 20,
            "Shared Damage %", sharedHealthDamagePercent, 0.0, 2.0, val -> sharedHealthDamagePercent = val);
        addScrollableWidget(sharedDmgSlider, y);
        addScrollableWidget(createResetButton(resetX, y, () -> {
            sharedHealthDamagePercent = 1.0;
            sharedDmgSlider.setValue(1.0);
        }), y);
        y += ROW_HEIGHT;
        
        // Shared Health Totem Saves All
        addTooltip(widgetX, y, totalWidth, 20, "Any player's totem can save all players from death. Default: ON");
        ButtonWidget totemSavesBtn = createToggleButton(widgetX, y, "Totem Saves All", sharedHealthTotemSavesAll, val -> sharedHealthTotemSavesAll = val);
        addScrollableWidget(totemSavesBtn, y);
        addScrollableWidget(createResetButton(resetX, y, () -> {
            sharedHealthTotemSavesAll = true;
            updateToggleButton(totemSavesBtn, "Totem Saves All", true);
        }), y);
        y += ROW_HEIGHT;
        
        // === MERCY COOLDOWN SETTINGS ===
        
        // Mercy Playtime Hours
        addTooltip(widgetX, y, totalWidth, 20, "Real playtime hours (not AFK) without deaths to reduce ban tier. Default: 24");
        IntSlider mercyHoursSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            "Mercy Hours", mercyPlaytimeHours, 1, 2500, val -> mercyPlaytimeHours = val);
        addScrollableWidget(mercyHoursSlider, y);
        addScrollableWidget(createResetButton(resetX, y, () -> {
            mercyPlaytimeHours = 24;
            mercyHoursSlider.setValue(24);
        }), y);
        y += ROW_HEIGHT;
        
        // Mercy Movement Blocks
        addTooltip(widgetX, y, totalWidth, 20, "Blocks moved in check interval to count as active. Default: 50");
        IntSlider mercyMoveSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            "Mercy Movement", mercyMovementBlocks, 0, 1000, val -> mercyMovementBlocks = val);
        addScrollableWidget(mercyMoveSlider, y);
        addScrollableWidget(createResetButton(resetX, y, () -> {
            mercyMovementBlocks = 50;
            mercyMoveSlider.setValue(50);
        }), y);
        y += ROW_HEIGHT;
        
        // Mercy Block Interactions
        addTooltip(widgetX, y, totalWidth, 20, "Block interactions in check interval to count as active. Default: 20");
        IntSlider mercyBlockSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            "Block Interactions", mercyBlockInteractions, 0, 1000, val -> mercyBlockInteractions = val);
        addScrollableWidget(mercyBlockSlider, y);
        addScrollableWidget(createResetButton(resetX, y, () -> {
            mercyBlockInteractions = 20;
            mercyBlockSlider.setValue(20);
        }), y);
        y += ROW_HEIGHT;
        
        // Mercy Check Interval
        addTooltip(widgetX, y, totalWidth, 20, "Minutes between activity checks. Default: 15");
        IntSlider mercyIntervalSlider = new IntSlider(widgetX, y, WIDGET_WIDTH, 20,
            "Check Interval", mercyCheckIntervalMinutes, 1, 60, val -> mercyCheckIntervalMinutes = val);
        addScrollableWidget(mercyIntervalSlider, y);
        addScrollableWidget(createResetButton(resetX, y, () -> {
            mercyCheckIntervalMinutes = 15;
            mercyIntervalSlider.setValue(15);
        }), y);
        y += ROW_HEIGHT;
        
        // === PVP SETTINGS ===
        
        // PvP Ban Multiplier
        addTooltip(widgetX, y, totalWidth, 20, "Ban time multiplier for PvP deaths. Default: 0.5");
        DoubleSlider pvpMultSlider = new DoubleSlider(widgetX, y, WIDGET_WIDTH, 20,
            "PvP Multiplier", pvpBanMultiplier, 0.0, 5.0, val -> pvpBanMultiplier = val);
        addScrollableWidget(pvpMultSlider, y);
        addScrollableWidget(createResetButton(resetX, y, () -> {
            pvpBanMultiplier = 0.5;
            pvpMultSlider.setValue(0.5);
        }), y);
        y += ROW_HEIGHT;
        
        // PvE Ban Multiplier
        addTooltip(widgetX, y, totalWidth, 20, "Ban time multiplier for PvE deaths. Default: 1.0");
        DoubleSlider pveMultSlider = new DoubleSlider(widgetX, y, WIDGET_WIDTH, 20,
            "PvE Multiplier", pveBanMultiplier, 0.0, 5.0, val -> pveBanMultiplier = val);
        addScrollableWidget(pveMultSlider, y);
        addScrollableWidget(createResetButton(resetX, y, () -> {
            pveBanMultiplier = 1.0;
            pveMultSlider.setValue(1.0);
        }), y);
        y += ROW_HEIGHT;
        
        // === ALTAR SETTINGS ===
        
        // Enable Resurrection Altar
        addTooltip(widgetX, y, totalWidth, 20, "Enable Resurrection Altar feature. Default: ON");
        ButtonWidget altarBtn = createToggleButton(widgetX, y, "Resurrection Altar", enableResurrectionAltar, val -> enableResurrectionAltar = val);
        addScrollableWidget(altarBtn, y);
        addScrollableWidget(createResetButton(resetX, y, () -> {
            enableResurrectionAltar = true;
            updateToggleButton(altarBtn, "Resurrection Altar", true);
        }), y);
        
        // === FOOTER BUTTONS ===
        int bottomY = this.height - FOOTER_HEIGHT + 7;
        int bottomButtonWidth = 76;
        int totalBottomWidth = bottomButtonWidth * 3 + 8;
        int bottomStartX = this.width / 2 - totalBottomWidth / 2;
        
        // Save & Close button
        ButtonWidget saveBtn = ButtonWidget.builder(Text.literal("Save & Close"), button -> {
            saveConfig();
            this.client.setScreen(parent);
        }).dimensions(bottomStartX, bottomY, bottomButtonWidth, 20).build();
        this.addDrawableChild(saveBtn);
        footerButtons.add(saveBtn);
        
        // Keybinds button
        ButtonWidget keybindsBtn = ButtonWidget.builder(Text.translatable("controls.keybinds"), button -> {
            this.client.setScreen(new KeybindsScreen(this, this.client.options));
        }).dimensions(bottomStartX + bottomButtonWidth + 4, bottomY, bottomButtonWidth, 20).build();
        this.addDrawableChild(keybindsBtn);
        footerButtons.add(keybindsBtn);
        
        // Cancel button
        ButtonWidget cancelBtn = ButtonWidget.builder(Text.translatable("gui.cancel"), button -> {
            this.client.setScreen(parent);
        }).dimensions(bottomStartX + bottomButtonWidth * 2 + 8, bottomY, bottomButtonWidth, 20).build();
        this.addDrawableChild(cancelBtn);
        footerButtons.add(cancelBtn);
        
        // Update widget positions based on initial scroll
        updateWidgetPositions();
    }
    
    private void addScrollableWidget(ClickableWidget widget, int originalY) {
        this.addDrawableChild(widget);
        scrollableWidgets.add(new WidgetEntry(widget, originalY));
    }
    
    private ButtonWidget createToggleButton(int x, int y, String label, boolean initialValue, java.util.function.Consumer<Boolean> setter) {
        final boolean[] value = {initialValue};
        ButtonWidget btn = ButtonWidget.builder(getToggleText(label, value[0]), button -> {
            value[0] = !value[0];
            setter.accept(value[0]);
            button.setMessage(getToggleText(label, value[0]));
        }).dimensions(x, y, WIDGET_WIDTH, 20).build();
        return btn;
    }
    
    private void updateToggleButton(ButtonWidget button, String label, boolean value) {
        button.setMessage(getToggleText(label, value));
    }
    
    private Text getToggleText(String label, boolean value) {
        return Text.literal(label + ": ")
            .append(value ? Text.literal("ON").styled(s -> s.withColor(0x55FF55)) 
                          : Text.literal("OFF").styled(s -> s.withColor(0xFF5555)));
    }
    
    private ButtonWidget createResetButton(int x, int y, Runnable action) {
        return ButtonWidget.builder(Text.literal("↺"), button -> action.run())
            .dimensions(x, y, RESET_BTN_WIDTH, 20).build();
    }
    
    private void updateWidgetPositions() {
        for (WidgetEntry entry : scrollableWidgets) {
            entry.widget.setY(entry.originalY - scrollOffset);
        }
    }
    
    private void addTooltip(int x, int y, int width, int height, String tooltip) {
        tooltips.add(new TooltipEntry(x, y, width, height, tooltip));
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY > HEADER_HEIGHT && mouseY < this.height - FOOTER_HEIGHT) {
            scrollOffset -= (int)(verticalAmount * SCROLL_SPEED);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
            updateWidgetPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        
        if (button == 0 && maxScrollOffset > 0) {
            int scrollbarX = this.width - SCROLLBAR_WIDTH - 4;
            int scrollbarTrackTop = HEADER_HEIGHT;
            int scrollbarTrackBottom = this.height - FOOTER_HEIGHT;
            
            if (mouseX >= scrollbarX && mouseX <= this.width - 2 &&
                mouseY >= scrollbarTrackTop && mouseY <= scrollbarTrackBottom) {
                
                int trackHeight = scrollbarTrackBottom - scrollbarTrackTop;
                int thumbHeight = Math.max(20, trackHeight * trackHeight / (maxScrollOffset + trackHeight));
                int thumbY = scrollbarTrackTop + (int)((trackHeight - thumbHeight) * ((float)scrollOffset / maxScrollOffset));
                
                if (mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                    isDraggingScrollbar = true;
                    scrollbarDragOffset = (int)(mouseY - thumbY);
                } else {
                    int clickOffset = (int)mouseY - scrollbarTrackTop - thumbHeight / 2;
                    float scrollPercent = (float)clickOffset / (trackHeight - thumbHeight);
                    scrollOffset = (int)(scrollPercent * maxScrollOffset);
                    scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset));
                    isDraggingScrollbar = true;
                    scrollbarDragOffset = thumbHeight / 2;
                    updateWidgetPositions();
                }
                return true;
            }
        }
        return super.mouseClicked(click, doubleClick);
    }
    
    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double mouseY = click.y();
        int button = click.button();
        
        if (isDraggingScrollbar && button == 0 && maxScrollOffset > 0) {
            int scrollbarTrackTop = HEADER_HEIGHT;
            int scrollbarTrackBottom = this.height - FOOTER_HEIGHT;
            int trackHeight = scrollbarTrackBottom - scrollbarTrackTop;
            int thumbHeight = Math.max(20, trackHeight * trackHeight / (maxScrollOffset + trackHeight));
            
            int thumbY = (int)mouseY - scrollbarDragOffset - scrollbarTrackTop;
            float scrollPercent = (float)thumbY / (trackHeight - thumbHeight);
            scrollOffset = (int)(scrollPercent * maxScrollOffset);
            scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset));
            updateWidgetPositions();
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }
    
    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) {
            isDraggingScrollbar = false;
        }
        return super.mouseReleased(click);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark background
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        
        // Title in header area (fixed)
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        
        // Enable scissor to clip scrollable content ONLY
        int scissorTop = HEADER_HEIGHT;
        int scissorBottom = this.height - FOOTER_HEIGHT;
        context.enableScissor(0, scissorTop, this.width, scissorBottom);
        
        // Render ONLY scrollable widgets (not footer buttons)
        for (WidgetEntry entry : scrollableWidgets) {
            entry.widget.render(context, mouseX, mouseY, delta);
        }
        
        // Disable scissor
        context.disableScissor();
        
        // Render footer buttons OUTSIDE scissor
        for (ClickableWidget button : footerButtons) {
            button.render(context, mouseX, mouseY, delta);
        }
        
        // Draw scrollbar if needed
        if (maxScrollOffset > 0) {
            int scrollbarX = this.width - SCROLLBAR_WIDTH - 4;
            int trackHeight = scissorBottom - scissorTop;
            int thumbHeight = Math.max(20, trackHeight * trackHeight / (maxScrollOffset + trackHeight));
            int thumbY = scissorTop + (int)((trackHeight - thumbHeight) * ((float)scrollOffset / maxScrollOffset));
            
            // Track background
            context.fill(scrollbarX, scissorTop, scrollbarX + SCROLLBAR_WIDTH, scissorBottom, 0x40FFFFFF);
            // Thumb
            context.fill(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xFFAAAAAA);
        }
        
        // Draw scroll indicators
        if (scrollOffset > 0) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("▲"), this.width / 2, scissorTop + 2, 0xAAAAAA);
        }
        if (scrollOffset < maxScrollOffset) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("▼"), this.width / 2, scissorBottom - 12, 0xAAAAAA);
        }
        
        // Draw tooltips LAST (after scissor disabled)
        if (mouseY > HEADER_HEIGHT && mouseY < this.height - FOOTER_HEIGHT) {
            for (TooltipEntry entry : tooltips) {
                int adjustedY = entry.y - scrollOffset;
                if (mouseX >= entry.x && mouseX < entry.x + entry.width &&
                    mouseY >= adjustedY && mouseY < adjustedY + 20) {
                    context.drawTooltip(this.textRenderer, Text.literal(entry.tooltip), mouseX, mouseY);
                    break;
                }
            }
        }
    }
    
    @Override
    public void close() {
        this.client.setScreen(parent);
    }
    
    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.isEscape()) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    private void saveConfig() {
        config.baseBanMinutes = baseBanMinutes;
        config.banMultiplier = banMultiplier;
        config.maxBanTier = maxBanTier;
        config.exponentialBanMode = exponentialBanMode;
        config.enableSoulLink = enableSoulLink;
        config.soulLinkDamageShare = soulLinkDamageShare;
        config.enableSharedHealth = enableSharedHealth;
        config.sharedHealthDamagePercent = sharedHealthDamagePercent;
        config.sharedHealthTotemSavesAll = sharedHealthTotemSavesAll;
        config.mercyPlaytimeHours = mercyPlaytimeHours;
        config.mercyMovementBlocks = mercyMovementBlocks;
        config.mercyBlockInteractions = mercyBlockInteractions;
        config.mercyCheckIntervalMinutes = mercyCheckIntervalMinutes;
        config.pvpBanMultiplier = pvpBanMultiplier;
        config.pveBanMultiplier = pveBanMultiplier;
        config.enableGhostEcho = enableGhostEcho;
        config.enableResurrectionAltar = enableResurrectionAltar;
        config.save();
    }
    
    // Integer slider widget
    private static class IntSlider extends SliderWidget {
        private final String label;
        private final int min;
        private final int max;
        private final java.util.function.Consumer<Integer> setter;
        
        public IntSlider(int x, int y, int width, int height, String label, int value, int min, int max, java.util.function.Consumer<Integer> setter) {
            super(x, y, width, height, Text.literal(label + ": " + value), (value - min) / (double)(max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
        }
        
        @Override
        protected void updateMessage() {
            int value = (int)(this.value * (max - min) + min);
            this.setMessage(Text.literal(label + ": " + value));
        }
        
        @Override
        protected void applyValue() {
            int value = (int)(this.value * (max - min) + min);
            setter.accept(value);
        }
        
        public void setValue(int newValue) {
            this.value = (newValue - min) / (double)(max - min);
            updateMessage();
            applyValue();
        }
    }
    
    // Double slider widget
    private static class DoubleSlider extends SliderWidget {
        private final String label;
        private final double min;
        private final double max;
        private final java.util.function.Consumer<Double> setter;
        
        public DoubleSlider(int x, int y, int width, int height, String label, double value, double min, double max, java.util.function.Consumer<Double> setter) {
            super(x, y, width, height, Text.literal(label + ": " + String.format("%.1f", value)), (value - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
        }
        
        @Override
        protected void updateMessage() {
            double value = this.value * (max - min) + min;
            this.setMessage(Text.literal(label + ": " + String.format("%.1f", value)));
        }
        
        @Override
        protected void applyValue() {
            double value = this.value * (max - min) + min;
            setter.accept(value);
        }
        
        public void setValue(double newValue) {
            this.value = (newValue - min) / (max - min);
            updateMessage();
            applyValue();
        }
    }
}
