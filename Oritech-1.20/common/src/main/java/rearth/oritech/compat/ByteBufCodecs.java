package rearth.oritech.compat;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.IntFunction;

/**
 * Compatibility class mirroring Minecraft 1.20.5+ ByteBufCodecs using FriendlyByteBuf.
 */
public final class ByteBufCodecs {

    public static final StreamCodec<FriendlyByteBuf, Integer> INT =
        StreamCodec.of(FriendlyByteBuf::writeInt, FriendlyByteBuf::readInt);

    public static final StreamCodec<FriendlyByteBuf, Integer> VAR_INT =
        StreamCodec.of(FriendlyByteBuf::writeVarInt, FriendlyByteBuf::readVarInt);

    public static final StreamCodec<FriendlyByteBuf, Long> VAR_LONG =
        StreamCodec.of(FriendlyByteBuf::writeVarLong, FriendlyByteBuf::readVarLong);

    public static final StreamCodec<FriendlyByteBuf, Long> LONG =
        StreamCodec.of(FriendlyByteBuf::writeLong, FriendlyByteBuf::readLong);

    public static final StreamCodec<FriendlyByteBuf, Float> FLOAT =
        StreamCodec.of(FriendlyByteBuf::writeFloat, FriendlyByteBuf::readFloat);

    public static final StreamCodec<FriendlyByteBuf, Double> DOUBLE =
        StreamCodec.of(FriendlyByteBuf::writeDouble, FriendlyByteBuf::readDouble);

    public static final StreamCodec<FriendlyByteBuf, Boolean> BOOL =
        StreamCodec.of(FriendlyByteBuf::writeBoolean, FriendlyByteBuf::readBoolean);

    public static final StreamCodec<FriendlyByteBuf, Byte> BYTE =
        StreamCodec.of((buf, v) -> buf.writeByte(v), buf -> buf.readByte());

    public static final StreamCodec<FriendlyByteBuf, Short> SHORT =
        StreamCodec.of((buf, v) -> buf.writeShort(v), buf -> (short) buf.readShort());

    public static final StreamCodec<FriendlyByteBuf, String> STRING_UTF8 =
        StreamCodec.of((buf, s) -> buf.writeUtf(s), buf -> buf.readUtf(32767));

    public static final StreamCodec<FriendlyByteBuf, byte[]> BYTE_ARRAY =
        StreamCodec.of(FriendlyByteBuf::writeByteArray, FriendlyByteBuf::readByteArray);

    public static final StreamCodec<FriendlyByteBuf, CompoundTag> COMPOUND_TAG =
        StreamCodec.of(FriendlyByteBuf::writeNbt, buf -> {
            CompoundTag tag = buf.readNbt();
            return tag != null ? tag : new CompoundTag();
        });

    public static final StreamCodec<FriendlyByteBuf, ItemStack> ITEM_STACK =
        StreamCodec.of(FriendlyByteBuf::writeItem, FriendlyByteBuf::readItem);

    public static final StreamCodec<FriendlyByteBuf, ResourceLocation> RESOURCE_LOCATION =
        StreamCodec.of(FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::readResourceLocation);

    public static final StreamCodec<FriendlyByteBuf, BlockPos> BLOCK_POS =
        StreamCodec.of((buf, pos) -> buf.writeBlockPos(pos), FriendlyByteBuf::readBlockPos);

    // ---- Collection helpers ----

    public static <B extends ByteBuf, V> StreamCodec.Function<StreamCodec<B, V>, StreamCodec<B, List<V>>> list() {
        return elementCodec -> new StreamCodec<>() {
            @Override
            public List<V> decode(B buf) {
                int size = ((FriendlyByteBuf) buf).readVarInt();
                List<V> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(elementCodec.decode(buf));
                }
                return list;
            }

            @Override
            public void encode(B buf, List<V> value) {
                ((FriendlyByteBuf) buf).writeVarInt(value.size());
                for (V v : value) {
                    elementCodec.encode(buf, v);
                }
            }
        };
    }

    public static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(
            IntFunction<C> factory, StreamCodec<B, V> elementCodec) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buf) {
                int size = ((FriendlyByteBuf) buf).readVarInt();
                C coll = factory.apply(size);
                for (int i = 0; i < size; i++) {
                    coll.add(elementCodec.decode(buf));
                }
                return coll;
            }

            @Override
            public void encode(B buf, C value) {
                ((FriendlyByteBuf) buf).writeVarInt(value.size());
                for (V v : value) {
                    elementCodec.encode(buf, v);
                }
            }
        };
    }

    public static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(
            IntFunction<M> factory, StreamCodec<B, K> keyCodec, StreamCodec<B, V> valueCodec) {
        return new StreamCodec<>() {
            @Override
            public M decode(B buf) {
                int size = ((FriendlyByteBuf) buf).readVarInt();
                M map = factory.apply(size);
                for (int i = 0; i < size; i++) {
                    K key = keyCodec.decode(buf);
                    V val = valueCodec.decode(buf);
                    map.put(key, val);
                }
                return map;
            }

            @Override
            public void encode(B buf, M value) {
                ((FriendlyByteBuf) buf).writeVarInt(value.size());
                for (Map.Entry<K, V> entry : value.entrySet()) {
                    keyCodec.encode(buf, entry.getKey());
                    valueCodec.encode(buf, entry.getValue());
                }
            }
        };
    }

    @FunctionalInterface
    public interface Function<A, B> {
        B apply(A a);
    }

    private ByteBufCodecs() {}
}
