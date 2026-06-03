package rearth.oritech.api.networking;

import io.netty.buffer.ByteBuf;
import rearth.oritech.compat.StreamCodec;

/**
 * Fields implementing this interface receive delta or full updates.
 * R is the delta type; T is the full state type.
 */
public interface UpdatableField<T, R> {

    R getDeltaData();

    T getFullData();

    StreamCodec<? extends ByteBuf, R> getDeltaCodec();

    StreamCodec<? extends ByteBuf, T> getFullCodec();

    default boolean useDeltaOnly(SyncType type) {
        return type.equals(SyncType.TICK) || type.equals(SyncType.GUI_TICK) || type.equals(SyncType.SPARSE_TICK);
    }

    void handleFullUpdate(T updatedData);

    void handleDeltaUpdate(R updatedData);
}
