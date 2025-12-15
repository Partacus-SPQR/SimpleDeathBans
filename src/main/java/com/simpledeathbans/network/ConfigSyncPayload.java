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
    int banMultiplierPercent,
    int maxBanTier,
    boolean exponentialBanMode,
    boolean enableGhostEcho,
    boolean enableSoulLink,
    int soulLinkDamageSharePercent,
    boolean soulLinkRandomPartner,
    boolean soulLinkTotemSavesPartner,
    int soulLinkSeverCooldownMinutes,
    int soulLinkSeverBanTierIncrease,
    int soulLinkExPartnerCooldownHours,
    int soulLinkRandomReassignCooldownHours,
    int soulLinkRandomAssignCheckIntervalMinutes,
    int soulLinkCompassMaxUses,
    int soulLinkCompassCooldownMinutes,
    boolean enableSharedHealth,
    int sharedHealthDamagePercent,
    boolean sharedHealthTotemSavesAll,
    boolean enableMercyCooldown,
    int mercyPlaytimeHours,
    int mercyMovementBlocks,
    int mercyBlockInteractions,
    int mercyCheckIntervalMinutes,
    int pvpBanMultiplierPercent,
    int pveBanMultiplierPercent,
    boolean enableResurrectionAltar,
    boolean singlePlayerEnabled
) implements CustomPayload {
    
    public static final CustomPayload.Id<ConfigSyncPayload> ID = 
        new CustomPayload.Id<>(Identifier.of("simpledeathbans", "config_sync"));
    
    public static final PacketCodec<RegistryByteBuf, ConfigSyncPayload> CODEC = 
        new PacketCodec<>() {
            @Override
            public ConfigSyncPayload decode(RegistryByteBuf buf) {
                return new ConfigSyncPayload(
                    buf.readInt(),        // baseBanMinutes
                    buf.readInt(),        // banMultiplierPercent
                    buf.readInt(),        // maxBanTier
                    buf.readBoolean(),    // exponentialBanMode
                    buf.readBoolean(),    // enableGhostEcho
                    buf.readBoolean(),    // enableSoulLink
                    buf.readInt(),        // soulLinkDamageSharePercent
                    buf.readBoolean(),    // soulLinkRandomPartner
                    buf.readBoolean(),    // soulLinkTotemSavesPartner
                    buf.readInt(),        // soulLinkSeverCooldownMinutes
                    buf.readInt(),        // soulLinkSeverBanTierIncrease
                    buf.readInt(),        // soulLinkExPartnerCooldownHours
                    buf.readInt(),        // soulLinkRandomReassignCooldownHours
                    buf.readInt(),        // soulLinkRandomAssignCheckIntervalMinutes
                    buf.readInt(),        // soulLinkCompassMaxUses
                    buf.readInt(),        // soulLinkCompassCooldownMinutes
                    buf.readBoolean(),    // enableSharedHealth
                    buf.readInt(),        // sharedHealthDamagePercent
                    buf.readBoolean(),    // sharedHealthTotemSavesAll
                    buf.readBoolean(),    // enableMercyCooldown
                    buf.readInt(),        // mercyPlaytimeHours
                    buf.readInt(),        // mercyMovementBlocks
                    buf.readInt(),        // mercyBlockInteractions
                    buf.readInt(),        // mercyCheckIntervalMinutes
                    buf.readInt(),        // pvpBanMultiplierPercent
                    buf.readInt(),        // pveBanMultiplierPercent
                    buf.readBoolean(),    // enableResurrectionAltar
                    buf.readBoolean()     // singlePlayerEnabled
                );
            }
            
            @Override
            public void encode(RegistryByteBuf buf, ConfigSyncPayload payload) {
                buf.writeInt(payload.baseBanMinutes);
                buf.writeInt(payload.banMultiplierPercent);
                buf.writeInt(payload.maxBanTier);
                buf.writeBoolean(payload.exponentialBanMode);
                buf.writeBoolean(payload.enableGhostEcho);
                buf.writeBoolean(payload.enableSoulLink);
                buf.writeInt(payload.soulLinkDamageSharePercent);
                buf.writeBoolean(payload.soulLinkRandomPartner);
                buf.writeBoolean(payload.soulLinkTotemSavesPartner);
                buf.writeInt(payload.soulLinkSeverCooldownMinutes);
                buf.writeInt(payload.soulLinkSeverBanTierIncrease);
                buf.writeInt(payload.soulLinkExPartnerCooldownHours);
                buf.writeInt(payload.soulLinkRandomReassignCooldownHours);
                buf.writeInt(payload.soulLinkRandomAssignCheckIntervalMinutes);
                buf.writeInt(payload.soulLinkCompassMaxUses);
                buf.writeInt(payload.soulLinkCompassCooldownMinutes);
                buf.writeBoolean(payload.enableSharedHealth);
                buf.writeInt(payload.sharedHealthDamagePercent);
                buf.writeBoolean(payload.sharedHealthTotemSavesAll);
                buf.writeBoolean(payload.enableMercyCooldown);
                buf.writeInt(payload.mercyPlaytimeHours);
                buf.writeInt(payload.mercyMovementBlocks);
                buf.writeInt(payload.mercyBlockInteractions);
                buf.writeInt(payload.mercyCheckIntervalMinutes);
                buf.writeInt(payload.pvpBanMultiplierPercent);
                buf.writeInt(payload.pveBanMultiplierPercent);
                buf.writeBoolean(payload.enableResurrectionAltar);
                buf.writeBoolean(payload.singlePlayerEnabled);
            }
        };
    
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
