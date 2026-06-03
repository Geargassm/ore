package rearth.oritech.api.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.compat.StreamCodec;

public interface WorldPacketCodec<I, T> extends StreamCodec<I, T> {

    @Override
    default T decode(I buf) {
        Oritech.LOGGER.warn("Using non-world variant of world packet codec for decode");
        return decode(buf, null);
    }

    @Override
    default void encode(I buf, T value) {
        Oritech.LOGGER.warn("Using non-world variant of world packet codec for encode");
        encode(buf, value, null);
    }

    T decode(I buf, @Nullable Level world);

    void encode(I buf, T value, @Nullable Level world);
}
