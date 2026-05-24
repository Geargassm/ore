package rearth.oritech.api.networking;

import net.minecraft.network.FriendlyByteBuf;
import rearth.oritech.compat.StreamCodec;

public enum SyncType {

    INITIAL, TICK, SPARSE_TICK, GUI_TICK, GUI_OPEN, CUSTOM;

    public static StreamCodec<FriendlyByteBuf, SyncType> PACKET_CODEC = new StreamCodec<>() {
        @Override
        public SyncType decode(FriendlyByteBuf buf) {
            return SyncType.values()[buf.readUnsignedShort()];
        }

        @Override
        public void encode(FriendlyByteBuf buf, SyncType value) {
            buf.writeShort(value.ordinal());
        }
    };
}
