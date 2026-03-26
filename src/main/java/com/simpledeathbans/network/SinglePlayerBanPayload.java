package com.simpledeathbans.network;

import com.simpledeathbans.SimpleDeathBans;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;*/
//?}

/**
 * Network payload sent from server to client when a player dies in single-player.
 * The client will then properly save the world and disconnect to title screen.
 */
public record SinglePlayerBanPayload(
    int banTier,
    long banDurationMs,
    String timeFormatted
) implements CustomPacketPayload {
    
    //? if >=1.21.11 {
    public static final Identifier PAYLOAD_ID = Identifier.fromNamespaceAndPath(SimpleDeathBans.MOD_ID, "singleplayer_ban");
    //?} else {
    /*public static final ResourceLocation PAYLOAD_ID = ResourceLocation.fromNamespaceAndPath(SimpleDeathBans.MOD_ID, "singleplayer_ban");*/
    //?}
    public static final Type<SinglePlayerBanPayload> ID = new Type<>(PAYLOAD_ID);
    
    public static final StreamCodec<FriendlyByteBuf, SinglePlayerBanPayload> CODEC = StreamCodec.ofMember(
        SinglePlayerBanPayload::write,
        SinglePlayerBanPayload::read
    );
    
    private void write(FriendlyByteBuf buf) {
        buf.writeInt(banTier);
        buf.writeLong(banDurationMs);
        buf.writeUtf(timeFormatted);
    }
    
    private static SinglePlayerBanPayload read(FriendlyByteBuf buf) {
        return new SinglePlayerBanPayload(
            buf.readInt(),
            buf.readLong(),
            buf.readUtf()
        );
    }
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
