package com.simpledeathbans.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.simpledeathbans.SimpleDeathBans;
import com.simpledeathbans.data.BanDataManager;
import com.simpledeathbans.data.SoulLinkManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.arguments.EntityArgument;
//? if >=1.21.11
import net.minecraft.server.permissions.Permission;
//? if >=1.21.11
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

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
            var mainCommand = Commands.literal("simpledeathbans")
                .requires(ModCommands::hasOperatorPermission)
                .then(Commands.literal("reload")
                    .executes(ModCommands::reloadConfig))
                .then(Commands.literal("settier")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("tier", IntegerArgumentType.integer(0, 100))
                            .executes(ModCommands::setTier))))
                .then(Commands.literal("gettier")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ModCommands::getTier)))
                .then(Commands.literal("unban")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ModCommands::unbanPlayer)))
                .then(Commands.literal("unbanbyname")
                    .then(Commands.argument("playername", StringArgumentType.word())
                        .executes(ModCommands::unbanPlayerByName)))
                .then(Commands.literal("clearbans")
                    .executes(ModCommands::clearAllBans))
                .then(Commands.literal("listbans")
                    .executes(ModCommands::listBans))
                .then(Commands.literal("soullink")
                    .then(Commands.literal("toggle")
                        .executes(ModCommands::toggleSoulLink))
                    .then(Commands.literal("set")
                        .then(Commands.argument("player1", EntityArgument.player())
                            .then(Commands.argument("player2", EntityArgument.player())
                                .executes(ModCommands::setSoulLink))))
                    .then(Commands.literal("clear")
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(ModCommands::clearSoulLink)))
                    .then(Commands.literal("status")
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(ModCommands::getSoulLinkStatus))))
                .then(Commands.literal("sharedhealth")
                    .then(Commands.literal("toggle")
                        .executes(ModCommands::toggleSharedHealth))
                    .then(Commands.literal("status")
                        .executes(ModCommands::getSharedHealthStatus)));
            
            dispatcher.register(mainCommand);
            
            // Alias: /sdb
            dispatcher.register(Commands.literal("sdb")
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
    private static boolean hasOperatorPermission(CommandSourceStack source) {
        //? if >=1.21.11 {
        // 1.21.11+: Use new Permission/PermissionLevel API
        return source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.OWNERS));
        //?} else {
        /*// Pre-1.21.11: Use hasPermission(4)
        return source.hasPermission(4);
        *///?}
    }
    
    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        SimpleDeathBans.getInstance().reloadConfig();
        context.getSource().sendSuccess(
            () -> Component.translatable("simpledeathbans.command.reload")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)),
            true
        );
        return Command.SINGLE_SUCCESS;
    }
    
    private static int setTier(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            int tier = IntegerArgumentType.getInteger(context, "tier");
            
            BanDataManager banManager = SimpleDeathBans.getInstance().getBanDataManager();
            if (banManager == null) {
                context.getSource().sendFailure(Component.literal("Ban manager not initialized"));
                return 0;
            }
            
            banManager.setTier(player.getUUID(), player.getName().getString(), tier);
            String playerName = player.getName().getString();
            
            context.getSource().sendSuccess(
                () -> Component.translatable("simpledeathbans.command.settier", playerName, tier)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)),
                true
            );
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int getTier(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            
            BanDataManager banManager = SimpleDeathBans.getInstance().getBanDataManager();
            if (banManager == null) {
                context.getSource().sendFailure(Component.literal("Ban manager not initialized"));
                return 0;
            }
            
            int tier = banManager.getTier(player.getUUID());
            String playerName = player.getName().getString();
            
            context.getSource().sendSuccess(
                () -> Component.translatable("simpledeathbans.command.gettier", playerName, tier)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)),
                false
            );
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int unbanPlayer(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            
            BanDataManager banManager = SimpleDeathBans.getInstance().getBanDataManager();
            if (banManager == null) {
                context.getSource().sendFailure(Component.literal("Ban manager not initialized"));
                return 0;
            }
            
            boolean success = banManager.unbanPlayer(player.getUUID());
            String playerName = player.getName().getString();
            
            if (success) {
                context.getSource().sendSuccess(
                    () -> Component.translatable("simpledeathbans.command.unban", playerName)
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)),
                    true
                );
            } else {
                context.getSource().sendSuccess(
                    () -> Component.literal(playerName + " is not currently banned")
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)),
                    false
                );
            }
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int unbanPlayerByName(CommandContext<CommandSourceStack> context) {
        try {
            String playerName = StringArgumentType.getString(context, "playername");
            
            BanDataManager banManager = SimpleDeathBans.getInstance().getBanDataManager();
            if (banManager == null) {
                context.getSource().sendFailure(Component.literal("Ban manager not initialized"));
                return 0;
            }
            
            boolean success = banManager.unbanPlayerByName(playerName);
            
            if (success) {
                context.getSource().sendSuccess(
                    () -> Component.translatable("simpledeathbans.command.unban", playerName)
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)),
                    true
                );
            } else {
                context.getSource().sendSuccess(
                    () -> Component.literal(playerName + " is not currently banned")
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)),
                    false
                );
            }
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int clearAllBans(CommandContext<CommandSourceStack> context) {
        BanDataManager banManager = SimpleDeathBans.getInstance().getBanDataManager();
        if (banManager == null) {
            context.getSource().sendFailure(Component.literal("Ban manager not initialized"));
            return 0;
        }
        
        banManager.clearAllBans();
        context.getSource().sendSuccess(
            () -> Component.translatable("simpledeathbans.command.clearbans")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)),
            true
        );
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int listBans(CommandContext<CommandSourceStack> context) {
        BanDataManager banManager = SimpleDeathBans.getInstance().getBanDataManager();
        if (banManager == null) {
            context.getSource().sendFailure(Component.literal("Ban manager not initialized"));
            return 0;
        }
        
        var bannedPlayers = banManager.getAllBannedPlayers();
        
        if (bannedPlayers.isEmpty()) {
            context.getSource().sendSuccess(
                () -> Component.literal("No players are currently banned.")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)),
                false
            );
            return Command.SINGLE_SUCCESS;
        }
        
        context.getSource().sendSuccess(
            () -> Component.literal("=== Banned Players ===")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)),
            false
        );
        
        for (BanDataManager.BanEntry entry : bannedPlayers) {
            String info = String.format("• %s - Tier %d - %s remaining", 
                entry.playerName(), 
                entry.banTier(), 
                entry.getRemainingTimeFormatted());
            context.getSource().sendSuccess(
                () -> Component.literal(info)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)),
                false
            );
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int setSoulLink(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player1 = EntityArgument.getPlayer(context, "player1");
            ServerPlayer player2 = EntityArgument.getPlayer(context, "player2");
            
            SoulLinkManager soulLinkManager = SimpleDeathBans.getInstance().getSoulLinkManager();
            if (soulLinkManager == null) {
                context.getSource().sendFailure(Component.literal("Soul link manager not initialized"));
                return 0;
            }
            
            soulLinkManager.setLink(player1.getUUID(), player2.getUUID());
            
            String p1Name = player1.getName().getString();
            String p2Name = player2.getName().getString();
            
            context.getSource().sendSuccess(
                () -> Component.translatable("simpledeathbans.command.soullink.set", p1Name, p2Name)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)),
                true
            );
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int clearSoulLink(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            
            SoulLinkManager soulLinkManager = SimpleDeathBans.getInstance().getSoulLinkManager();
            if (soulLinkManager == null) {
                context.getSource().sendFailure(Component.literal("Soul link manager not initialized"));
                return 0;
            }
            
            boolean success = soulLinkManager.clearLink(player.getUUID());
            String playerName = player.getName().getString();
            
            if (success) {
                context.getSource().sendSuccess(
                    () -> Component.translatable("simpledeathbans.command.soullink.clear", playerName)
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)),
                    true
                );
            } else {
                context.getSource().sendSuccess(
                    () -> Component.translatable("simpledeathbans.command.soullink.none", playerName)
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)),
                    false
                );
            }
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int toggleSoulLink(CommandContext<CommandSourceStack> context) {
        var config = SimpleDeathBans.getInstance().getConfig();
        if (config == null) {
            context.getSource().sendFailure(Component.literal("Config not initialized"));
            return 0;
        }
        
        // Check if trying to enable Soul Link while Shared Health is active
        if (!config.enableSoulLink && config.enableSharedHealth) {
            // Broadcast server message
            Component serverMsg = Component.literal("\u00a7k><\u00a7r ")
                .append(Component.literal("Must disable Shared Health before enabling Soul Link.").withStyle(ChatFormatting.RED))
                .append(Component.literal(" \u00a7k><\u00a7r"));
            context.getSource().sendSuccess(() -> serverMsg, true);
            return 0;
        }
        
        // Toggle the setting
        config.enableSoulLink = !config.enableSoulLink;
        
        // Save the config
        SimpleDeathBans.getInstance().saveConfig();
        
        // Broadcast server-wide message when Soul Link is enabled
        if (config.enableSoulLink) {
            Component serverMsg = Component.literal("\u00a7k><\u00a7r ")
                .append(Component.literal("Soul Link has been enabled.").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" \u00a7k><\u00a7r"));
            context.getSource().getServer().getPlayerList().broadcastSystemMessage(serverMsg, false);
        }
        
        String status = config.enableSoulLink ? "ENABLED" : "DISABLED";
        ChatFormatting color = config.enableSoulLink ? ChatFormatting.GREEN : ChatFormatting.RED;
        
        context.getSource().sendSuccess(
            () -> Component.literal("Soul Link feature is now ")
                .append(Component.literal(status).withStyle(color, ChatFormatting.BOLD)),
            true
        );
        
        if (config.enableSoulLink) {
            context.getSource().sendSuccess(
                () -> Component.literal("Players will be auto-assigned soul partners on join.").withStyle(ChatFormatting.GRAY),
                false
            );
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int getSoulLinkStatus(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            
            SoulLinkManager soulLinkManager = SimpleDeathBans.getInstance().getSoulLinkManager();
            if (soulLinkManager == null) {
                context.getSource().sendFailure(Component.literal("Soul link manager not initialized"));
                return 0;
            }
            
            String playerName = player.getName().getString();
            var partnerOpt = soulLinkManager.getPartner(player.getUUID());
            
            if (partnerOpt.isPresent()) {
                var partnerUuid = partnerOpt.get();
                ServerPlayer partner = context.getSource().getServer().getPlayerList().getPlayer(partnerUuid);
                String partnerName = partner != null ? partner.getName().getString() : "Unknown (Offline)";
                
                context.getSource().sendSuccess(
                    () -> Component.translatable("simpledeathbans.command.soullink.status", playerName, partnerName)
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)),
                    false
                );
            } else {
                context.getSource().sendSuccess(
                    () -> Component.translatable("simpledeathbans.command.soullink.none", playerName)
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)),
                    false
                );
            }
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int toggleSharedHealth(CommandContext<CommandSourceStack> context) {
        var config = SimpleDeathBans.getInstance().getConfig();
        if (config == null) {
            context.getSource().sendFailure(Component.literal("Config not initialized"));
            return 0;
        }
        
        boolean wasEnablingSharedHealth = !config.enableSharedHealth;
        boolean soulLinkWasOn = config.enableSoulLink;
        
        // Toggle the setting
        config.enableSharedHealth = !config.enableSharedHealth;
        
        // MUTUAL EXCLUSIVITY: If enabling Shared Health, disable Soul Link
        if (config.enableSharedHealth && soulLinkWasOn) {
            config.enableSoulLink = false;
            // Broadcast two separate server-wide messages
            Component msg1 = Component.literal("\u00a7k><\u00a7r ")
                .append(Component.literal("Soul Link has been disabled.").withStyle(ChatFormatting.RED))
                .append(Component.literal(" \u00a7k><\u00a7r"));
            context.getSource().getServer().getPlayerList().broadcastSystemMessage(msg1, false);
            
            Component msg2 = Component.literal("\u00a7k><\u00a7r ")
                .append(Component.literal("Shared Health has been enabled.").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" \u00a7k><\u00a7r"));
            context.getSource().getServer().getPlayerList().broadcastSystemMessage(msg2, false);
        } else if (wasEnablingSharedHealth) {
            // Just enabling Shared Health (Soul Link wasn't on)
            Component serverMsg = Component.literal("\u00a7k><\u00a7r ")
                .append(Component.literal("Shared Health has been enabled.").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" \u00a7k><\u00a7r"));
            context.getSource().getServer().getPlayerList().broadcastSystemMessage(serverMsg, false);
        }
        
        // Save the config
        SimpleDeathBans.getInstance().saveConfig();
        
        String status = config.enableSharedHealth ? "ENABLED" : "DISABLED";
        ChatFormatting color = config.enableSharedHealth ? ChatFormatting.GREEN : ChatFormatting.RED;
        
        context.getSource().sendSuccess(
            () -> Component.literal("Shared Health feature is now ")
                .append(Component.literal(status).withStyle(color, ChatFormatting.BOLD)),
            true
        );
        
        if (config.enableSharedHealth) {
            context.getSource().sendSuccess(
                () -> Component.literal("⚠ ALL players now share damage! Any totem can save everyone.").withStyle(ChatFormatting.GOLD),
                false
            );
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int getSharedHealthStatus(CommandContext<CommandSourceStack> context) {
        var config = SimpleDeathBans.getInstance().getConfig();
        if (config == null) {
            context.getSource().sendFailure(Component.literal("Config not initialized"));
            return 0;
        }
        
        String status = config.enableSharedHealth ? "ENABLED" : "DISABLED";
        ChatFormatting color = config.enableSharedHealth ? ChatFormatting.GREEN : ChatFormatting.RED;
        
        context.getSource().sendSuccess(
            () -> Component.literal("Shared Health: ")
                .append(Component.literal(status).withStyle(color, ChatFormatting.BOLD))
                .append(Component.literal("\nDamage Share: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal((int)(config.sharedHealthDamagePercent * 100) + "%").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("\nTotem Saves All: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(config.sharedHealthTotemSavesAll ? "Yes" : "No").withStyle(ChatFormatting.YELLOW)),
            false
        );
        
        return Command.SINGLE_SUCCESS;
    }
}
