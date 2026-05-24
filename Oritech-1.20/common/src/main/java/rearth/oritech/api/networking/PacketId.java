package rearth.oritech.api.networking;

import net.minecraft.resources.ResourceLocation;

/**
 * Replacement for Minecraft 1.20.5+ {@code CustomPacketPayload.Type<T>}.
 * Holds the packet channel ResourceLocation; the codec is provided separately.
 */
public record PacketId<T>(ResourceLocation id) {
    public PacketId(ResourceLocation id) {
        this.id = id;
    }
}
