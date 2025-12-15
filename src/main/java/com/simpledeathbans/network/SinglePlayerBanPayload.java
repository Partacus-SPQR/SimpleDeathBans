package com.simpledeathbans.network;

import com.simpledeathbans.SimpleDeathBans;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Network payload sent from server to client when a player dies in single-player.
 * The client will then properly save the world and disconnect to title screen.
 */
public record SinglePlayerBanPayload(
    int banTier,
    long banDurationMs,
    String timeFormatted
) implements CustomPayload {
    
    public static final Identifier IDENTIFIER = Identifier.of(SimpleDeathBans.MOD_ID, "singleplayer_ban");
    public static final Id<SinglePlayerBanPayload> ID = new Id<>(IDENTIFIER);
    
    public static final PacketCodec<PacketByteBuf, SinglePlayerBanPayload> CODEC = PacketCodec.of(
        SinglePlayerBanPayload::write,
        SinglePlayerBanPayload::read
    );
    
    private void write(PacketByteBuf buf) {
        buf.writeInt(banTier);
        buf.writeLong(banDurationMs);
        buf.writeString(timeFormatted);
    }
    
    private static SinglePlayerBanPayload read(PacketByteBuf buf) {
        return new SinglePlayerBanPayload(
            buf.readInt(),
            buf.readLong(),
            buf.readString()
        );
    }
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
