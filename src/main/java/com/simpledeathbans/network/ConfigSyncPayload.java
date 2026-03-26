package com.simpledeathbans.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;*/
//?}

/**
 * Network payload for syncing config changes from client to server.
 * Server will validate operator permissions before applying.
 */
public record ConfigSyncPayload(
    boolean enableDeathBans,
    int baseBanMinutes,
    int banMultiplierPercent,
    int maxBanTier,
    boolean exponentialBanMode,
    boolean enableGhostEcho,
    boolean enableSoulLink,
    int soulLinkDamageSharePercent,
    boolean soulLinkShareHunger,
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
    boolean sharedHealthShareHunger,
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
) implements CustomPacketPayload {
    
    //? if >=1.21.11 {
    public static final CustomPacketPayload.Type<ConfigSyncPayload> ID = 
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("simpledeathbans", "config_sync"));
    //?} else {
    /*public static final CustomPacketPayload.Type<ConfigSyncPayload> ID = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("simpledeathbans", "config_sync"));*/
    //?}
    
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPayload> CODEC = 
        new StreamCodec<>() {
            @Override
            public ConfigSyncPayload decode(RegistryFriendlyByteBuf buf) {
                return new ConfigSyncPayload(
                    buf.readBoolean(),    // enableDeathBans
                    buf.readInt(),        // baseBanMinutes
                    buf.readInt(),        // banMultiplierPercent
                    buf.readInt(),        // maxBanTier
                    buf.readBoolean(),    // exponentialBanMode
                    buf.readBoolean(),    // enableGhostEcho
                    buf.readBoolean(),    // enableSoulLink
                    buf.readInt(),        // soulLinkDamageSharePercent
                    buf.readBoolean(),    // soulLinkShareHunger
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
                    buf.readBoolean(),    // sharedHealthShareHunger
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
            public void encode(RegistryFriendlyByteBuf buf, ConfigSyncPayload payload) {
                buf.writeBoolean(payload.enableDeathBans);
                buf.writeInt(payload.baseBanMinutes);
                buf.writeInt(payload.banMultiplierPercent);
                buf.writeInt(payload.maxBanTier);
                buf.writeBoolean(payload.exponentialBanMode);
                buf.writeBoolean(payload.enableGhostEcho);
                buf.writeBoolean(payload.enableSoulLink);
                buf.writeInt(payload.soulLinkDamageSharePercent);
                buf.writeBoolean(payload.soulLinkShareHunger);
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
                buf.writeBoolean(payload.sharedHealthShareHunger);
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
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
