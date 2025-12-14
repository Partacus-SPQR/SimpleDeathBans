package com.simpledeathbans.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Network payload for syncing config changes from client to server.
 * Server will validate operator permissions before applying.
 */
public record ConfigSyncPayload(
    int baseBanMinutes,
    double banMultiplier,
    int maxBanTier,
    boolean exponentialBanMode,
    boolean enableGhostEcho,
    boolean enableSoulLink,
    double soulLinkDamageShare,
    boolean soulLinkRandomPartner,
    boolean soulLinkTotemSavesAll,
    boolean enableSharedHealth,
    double sharedHealthDamagePercent,
    boolean sharedHealthTotemSavesAll,
    boolean enableMercyCooldown,
    int mercyPlaytimeHours,
    int mercyMovementBlocks,
    int mercyBlockInteractions,
    int mercyCheckIntervalMinutes,
    double pvpBanMultiplier,
    double pveBanMultiplier,
    boolean enableResurrectionAltar
) implements CustomPayload {
    
    public static final CustomPayload.Id<ConfigSyncPayload> ID = 
        new CustomPayload.Id<>(Identifier.of("simpledeathbans", "config_sync"));
    
    public static final PacketCodec<RegistryByteBuf, ConfigSyncPayload> CODEC = 
        new PacketCodec<>() {
            @Override
            public ConfigSyncPayload decode(RegistryByteBuf buf) {
                return new ConfigSyncPayload(
                    buf.readInt(),        // baseBanMinutes
                    buf.readDouble(),     // banMultiplier
                    buf.readInt(),        // maxBanTier
                    buf.readBoolean(),    // exponentialBanMode
                    buf.readBoolean(),    // enableGhostEcho
                    buf.readBoolean(),    // enableSoulLink
                    buf.readDouble(),     // soulLinkDamageShare
                    buf.readBoolean(),    // soulLinkRandomPartner
                    buf.readBoolean(),    // soulLinkTotemSavesAll
                    buf.readBoolean(),    // enableSharedHealth
                    buf.readDouble(),     // sharedHealthDamagePercent
                    buf.readBoolean(),    // sharedHealthTotemSavesAll
                    buf.readBoolean(),    // enableMercyCooldown
                    buf.readInt(),        // mercyPlaytimeHours
                    buf.readInt(),        // mercyMovementBlocks
                    buf.readInt(),        // mercyBlockInteractions
                    buf.readInt(),        // mercyCheckIntervalMinutes
                    buf.readDouble(),     // pvpBanMultiplier
                    buf.readDouble(),     // pveBanMultiplier
                    buf.readBoolean()     // enableResurrectionAltar
                );
            }
            
            @Override
            public void encode(RegistryByteBuf buf, ConfigSyncPayload payload) {
                buf.writeInt(payload.baseBanMinutes);
                buf.writeDouble(payload.banMultiplier);
                buf.writeInt(payload.maxBanTier);
                buf.writeBoolean(payload.exponentialBanMode);
                buf.writeBoolean(payload.enableGhostEcho);
                buf.writeBoolean(payload.enableSoulLink);
                buf.writeDouble(payload.soulLinkDamageShare);
                buf.writeBoolean(payload.soulLinkRandomPartner);
                buf.writeBoolean(payload.soulLinkTotemSavesAll);
                buf.writeBoolean(payload.enableSharedHealth);
                buf.writeDouble(payload.sharedHealthDamagePercent);
                buf.writeBoolean(payload.sharedHealthTotemSavesAll);
                buf.writeBoolean(payload.enableMercyCooldown);
                buf.writeInt(payload.mercyPlaytimeHours);
                buf.writeInt(payload.mercyMovementBlocks);
                buf.writeInt(payload.mercyBlockInteractions);
                buf.writeInt(payload.mercyCheckIntervalMinutes);
                buf.writeDouble(payload.pvpBanMultiplier);
                buf.writeDouble(payload.pveBanMultiplier);
                buf.writeBoolean(payload.enableResurrectionAltar);
            }
        };
    
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
