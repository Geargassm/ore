package rearth.oritech.api.networking;

import net.minecraft.resources.ResourceLocation;
import rearth.oritech.compat.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Replacement for Minecraft 1.20.5+ CustomPacketPayload.Type&lt;T&gt;.
 * Combines a packet ResourceLocation ID with its codec.
 */
public record PacketId<T>(ResourceLocation id, StreamCodec<FriendlyByteBuf, T> codec) {

    public PacketId(ResourceLocation id, StreamCodec<FriendlyByteBuf, T> codec) {
        this.id = id;
        this.codec = codec;
    }
}
