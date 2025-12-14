package com.simpledeathbans.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.data.BanDataManager;
import com.simpledeathbans.data.SoulLinkManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
//? if >=1.21.11
import net.minecraft.command.permission.Permission;
//? if >=1.21.11
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Registers all admin commands for the mod.
 * All commands require Operator Level 4.
 * 
 * Commands available:
 * - /simpledeathbans reload - Reloads configuration
 * - /simpledeathbans settier <player> <tier> - Sets a player's ban tier
 * - /simpledeathbans gettier <player> - Gets a player's current ban tier
 * - /simpledeathbans unban <player> - Unbans a player
 * - /simpledeathbans clearbans - Clears all bans
 * - /simpledeathbans listbans - Lists all banned players
 * - /simpledeathbans soullink set <player1> <player2> - Creates a soul link
 * - /simpledeathbans soullink clear <player> - Removes a soul link
 * - /simpledeathbans soullink status <player> - Shows soul link status
 * - /sdb - Alias for /simpledeathbans
 */
public class ModCommands {
    
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Main command: /simpledeathbans
            var mainCommand = CommandManager.literal("simpledeathbans")
                .requires(ModCommands::hasOperatorPermission)
                .then(CommandManager.literal("reload")
                    .executes(ModCommands::reloadConfig))
                .then(CommandManager.literal("settier")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.argument("tier", IntegerArgumentType.integer(0, 100))
                            .executes(ModCommands::setTier))))
                .then(CommandManager.literal("gettier")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(ModCommands::getTier)))
                .then(CommandManager.literal("unban")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(ModCommands::unbanPlayer)))
                .then(CommandManager.literal("unbanbyname")
                    .then(CommandManager.argument("playername", StringArgumentType.word())
                        .executes(ModCommands::unbanPlayerByName)))
                .then(CommandManager.literal("clearbans")
                    .executes(ModCommands::clearAllBans))
                .then(CommandManager.literal("listbans")
                    .executes(ModCommands::listBans))
                .then(CommandManager.literal("soullink")
                    .then(CommandManager.literal("toggle")
                        .executes(ModCommands::toggleSoulLink))
                    .then(CommandManager.literal("set")
                        .then(CommandManager.argument("player1", EntityArgumentType.player())
                            .then(CommandManager.argument("player2", EntityArgumentType.player())
                                .executes(ModCommands::setSoulLink))))
                    .then(CommandManager.literal("clear")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .executes(ModCommands::clearSoulLink)))
                    .then(CommandManager.literal("status")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .executes(ModCommands::getSoulLinkStatus))))
                .then(CommandManager.literal("sharedhealth")
                    .then(CommandManager.literal("toggle")
                        .executes(ModCommands::toggleSharedHealth))
                    .then(CommandManager.literal("status")
                        .executes(ModCommands::getSharedHealthStatus)));
            
            dispatcher.register(mainCommand);
            
            // Alias: /sdb
            dispatcher.register(CommandManager.literal("sdb")
                .requires(ModCommands::hasOperatorPermission)
                .redirect(dispatcher.getRoot().getChild("simpledeathbans"))
            );
        });
    }
    
    /**
     * Check if source has operator permission level 4 (OWNERS level)
     * Console and command blocks always have permission.
     * Players must have operator level 4.
     */
    private static boolean hasOperatorPermission(ServerCommandSource source) {
        //? if >=1.21.11 {
        // 1.21.11+: Use new Permission/PermissionLevel API
        return source.getPermissions().hasPermission(new Permission.Level(PermissionLevel.OWNERS));
        //?} else {
        /*// Pre-1.21.11: Use hasPermissionLevel(4)
        return source.hasPermissionLevel(4);
        *///?}
    }
    
    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        SimpleDeathBans.getInstance().reloadConfig();
        context.getSource().sendFeedback(
            () -> Text.translatable("simpledeathbans.command.reload")
                .setStyle(Style.EMPTY.withColor(Formatting.GREEN)),
            true
        );
        return Command.SINGLE_SUCCESS;
    }
    
    private static int setTier(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
            int tier = IntegerArgumentType.getInteger(context, "tier");
            
            BanDataManager banManager = SimpleDeathBans.getInstance().getBanDataManager();
            if (banManager == null) {
                context.getSource().sendError(Text.literal("Ban manager not initialized"));
                return 0;
            }
            
            banManager.setTier(player.getUuid(), player.getName().getString(), tier);
            String playerName = player.getName().getString();
            
            context.getSource().sendFeedback(
                () -> Text.translatable("simpledeathbans.command.settier", playerName, tier)
                    .setStyle(Style.EMPTY.withColor(Formatting.GREEN)),
                true
            );
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int getTier(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
            
            BanDataManager banManager = SimpleDeathBans.getInstance().getBanDataManager();
            if (banManager == null) {
                context.getSource().sendError(Text.literal("Ban manager not initialized"));
                return 0;
            }
            
            int tier = banManager.getTier(player.getUuid());
            String playerName = player.getName().getString();
            
            context.getSource().sendFeedback(
                () -> Text.translatable("simpledeathbans.command.gettier", playerName, tier)
                    .setStyle(Style.EMPTY.withColor(Formatting.YELLOW)),
                false
            );
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int unbanPlayer(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
            
            BanDataManager banManager = SimpleDeathBans.getInstance().getBanDataManager();
            if (banManager == null) {
                context.getSource().sendError(Text.literal("Ban manager not initialized"));
                return 0;
            }
            
            boolean success = banManager.unbanPlayer(player.getUuid());
            String playerName = player.getName().getString();
            
            if (success) {
                context.getSource().sendFeedback(
                    () -> Text.translatable("simpledeathbans.command.unban", playerName)
                        .setStyle(Style.EMPTY.withColor(Formatting.GREEN)),
                    true
                );
            } else {
                context.getSource().sendFeedback(
                    () -> Text.literal(playerName + " is not currently banned")
                        .setStyle(Style.EMPTY.withColor(Formatting.YELLOW)),
                    false
                );
            }
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int unbanPlayerByName(CommandContext<ServerCommandSource> context) {
        try {
            String playerName = StringArgumentType.getString(context, "playername");
            
            BanDataManager banManager = SimpleDeathBans.getInstance().getBanDataManager();
            if (banManager == null) {
                context.getSource().sendError(Text.literal("Ban manager not initialized"));
                return 0;
            }
            
            boolean success = banManager.unbanPlayerByName(playerName);
            
            if (success) {
                context.getSource().sendFeedback(
                    () -> Text.translatable("simpledeathbans.command.unban", playerName)
                        .setStyle(Style.EMPTY.withColor(Formatting.GREEN)),
                    true
                );
            } else {
                context.getSource().sendFeedback(
                    () -> Text.literal(playerName + " is not currently banned")
                        .setStyle(Style.EMPTY.withColor(Formatting.YELLOW)),
                    false
                );
            }
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int clearAllBans(CommandContext<ServerCommandSource> context) {
        BanDataManager banManager = SimpleDeathBans.getInstance().getBanDataManager();
        if (banManager == null) {
            context.getSource().sendError(Text.literal("Ban manager not initialized"));
            return 0;
        }
        
        banManager.clearAllBans();
        context.getSource().sendFeedback(
            () -> Text.translatable("simpledeathbans.command.clearbans")
                .setStyle(Style.EMPTY.withColor(Formatting.GREEN)),
            true
        );
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int listBans(CommandContext<ServerCommandSource> context) {
        BanDataManager banManager = SimpleDeathBans.getInstance().getBanDataManager();
        if (banManager == null) {
            context.getSource().sendError(Text.literal("Ban manager not initialized"));
            return 0;
        }
        
        var bannedPlayers = banManager.getAllBannedPlayers();
        
        if (bannedPlayers.isEmpty()) {
            context.getSource().sendFeedback(
                () -> Text.literal("No players are currently banned.")
                    .setStyle(Style.EMPTY.withColor(Formatting.YELLOW)),
                false
            );
            return Command.SINGLE_SUCCESS;
        }
        
        context.getSource().sendFeedback(
            () -> Text.literal("=== Banned Players ===")
                .setStyle(Style.EMPTY.withColor(Formatting.GOLD)),
            false
        );
        
        for (BanDataManager.BanEntry entry : bannedPlayers) {
            String info = String.format("• %s - Tier %d - %s remaining", 
                entry.playerName(), 
                entry.banTier(), 
                entry.getRemainingTimeFormatted());
            context.getSource().sendFeedback(
                () -> Text.literal(info)
                    .setStyle(Style.EMPTY.withColor(Formatting.GRAY)),
                false
            );
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int setSoulLink(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player1");
            ServerPlayerEntity player2 = EntityArgumentType.getPlayer(context, "player2");
            
            SoulLinkManager soulLinkManager = SimpleDeathBans.getInstance().getSoulLinkManager();
            if (soulLinkManager == null) {
                context.getSource().sendError(Text.literal("Soul link manager not initialized"));
                return 0;
            }
            
            soulLinkManager.setLink(player1.getUuid(), player2.getUuid());
            
            String p1Name = player1.getName().getString();
            String p2Name = player2.getName().getString();
            
            context.getSource().sendFeedback(
                () -> Text.translatable("simpledeathbans.command.soullink.set", p1Name, p2Name)
                    .setStyle(Style.EMPTY.withColor(Formatting.GREEN)),
                true
            );
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int clearSoulLink(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
            
            SoulLinkManager soulLinkManager = SimpleDeathBans.getInstance().getSoulLinkManager();
            if (soulLinkManager == null) {
                context.getSource().sendError(Text.literal("Soul link manager not initialized"));
                return 0;
            }
            
            boolean success = soulLinkManager.clearLink(player.getUuid());
            String playerName = player.getName().getString();
            
            if (success) {
                context.getSource().sendFeedback(
                    () -> Text.translatable("simpledeathbans.command.soullink.clear", playerName)
                        .setStyle(Style.EMPTY.withColor(Formatting.GREEN)),
                    true
                );
            } else {
                context.getSource().sendFeedback(
                    () -> Text.translatable("simpledeathbans.command.soullink.none", playerName)
                        .setStyle(Style.EMPTY.withColor(Formatting.YELLOW)),
                    false
                );
            }
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int toggleSoulLink(CommandContext<ServerCommandSource> context) {
        var config = SimpleDeathBans.getInstance().getConfig();
        if (config == null) {
            context.getSource().sendError(Text.literal("Config not initialized"));
            return 0;
        }
        
        // Toggle the setting
        config.enableSoulLink = !config.enableSoulLink;
        
        // Save the config
        SimpleDeathBans.getInstance().saveConfig();
        
        String status = config.enableSoulLink ? "ENABLED" : "DISABLED";
        Formatting color = config.enableSoulLink ? Formatting.GREEN : Formatting.RED;
        
        context.getSource().sendFeedback(
            () -> Text.literal("Soul Link feature is now ")
                .append(Text.literal(status).formatted(color, Formatting.BOLD)),
            true
        );
        
        if (config.enableSoulLink) {
            context.getSource().sendFeedback(
                () -> Text.literal("Players will be auto-assigned soul partners on join.").formatted(Formatting.GRAY),
                false
            );
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int getSoulLinkStatus(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
            
            SoulLinkManager soulLinkManager = SimpleDeathBans.getInstance().getSoulLinkManager();
            if (soulLinkManager == null) {
                context.getSource().sendError(Text.literal("Soul link manager not initialized"));
                return 0;
            }
            
            String playerName = player.getName().getString();
            var partnerOpt = soulLinkManager.getPartner(player.getUuid());
            
            if (partnerOpt.isPresent()) {
                var partnerUuid = partnerOpt.get();
                ServerPlayerEntity partner = context.getSource().getServer().getPlayerManager().getPlayer(partnerUuid);
                String partnerName = partner != null ? partner.getName().getString() : "Unknown (Offline)";
                
                context.getSource().sendFeedback(
                    () -> Text.translatable("simpledeathbans.command.soullink.status", playerName, partnerName)
                        .setStyle(Style.EMPTY.withColor(Formatting.YELLOW)),
                    false
                );
            } else {
                context.getSource().sendFeedback(
                    () -> Text.translatable("simpledeathbans.command.soullink.none", playerName)
                        .setStyle(Style.EMPTY.withColor(Formatting.YELLOW)),
                    false
                );
            }
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int toggleSharedHealth(CommandContext<ServerCommandSource> context) {
        var config = SimpleDeathBans.getInstance().getConfig();
        if (config == null) {
            context.getSource().sendError(Text.literal("Config not initialized"));
            return 0;
        }
        
        // Toggle the setting
        config.enableSharedHealth = !config.enableSharedHealth;
        
        // MUTUAL EXCLUSIVITY: If enabling Shared Health, disable Soul Link
        if (config.enableSharedHealth && config.enableSoulLink) {
            config.enableSoulLink = false;
            context.getSource().sendFeedback(
                () -> Text.literal("⚠ Soul Link has been DISABLED - Shared Health takes priority!")
                    .formatted(Formatting.RED, Formatting.BOLD),
                true
            );
        }
        
        // Save the config
        SimpleDeathBans.getInstance().saveConfig();
        
        String status = config.enableSharedHealth ? "ENABLED" : "DISABLED";
        Formatting color = config.enableSharedHealth ? Formatting.GREEN : Formatting.RED;
        
        context.getSource().sendFeedback(
            () -> Text.literal("Shared Health feature is now ")
                .append(Text.literal(status).formatted(color, Formatting.BOLD)),
            true
        );
        
        if (config.enableSharedHealth) {
            context.getSource().sendFeedback(
                () -> Text.literal("⚠ ALL players now share damage! Any totem can save everyone.").formatted(Formatting.GOLD),
                false
            );
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int getSharedHealthStatus(CommandContext<ServerCommandSource> context) {
        var config = SimpleDeathBans.getInstance().getConfig();
        if (config == null) {
            context.getSource().sendError(Text.literal("Config not initialized"));
            return 0;
        }
        
        String status = config.enableSharedHealth ? "ENABLED" : "DISABLED";
        Formatting color = config.enableSharedHealth ? Formatting.GREEN : Formatting.RED;
        
        context.getSource().sendFeedback(
            () -> Text.literal("Shared Health: ")
                .append(Text.literal(status).formatted(color, Formatting.BOLD))
                .append(Text.literal("\nDamage Share: ").formatted(Formatting.GRAY))
                .append(Text.literal((int)(config.sharedHealthDamagePercent * 100) + "%").formatted(Formatting.YELLOW))
                .append(Text.literal("\nTotem Saves All: ").formatted(Formatting.GRAY))
                .append(Text.literal(config.sharedHealthTotemSavesAll ? "Yes" : "No").formatted(Formatting.YELLOW)),
            false
        );
        
        return Command.SINGLE_SUCCESS;
    }
}
