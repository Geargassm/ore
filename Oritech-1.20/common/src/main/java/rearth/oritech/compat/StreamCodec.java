package rearth.oritech.compat;

import io.netty.buffer.ByteBuf;

import java.util.*;
import java.util.function.*;

/**
 * Compatibility StreamCodec interface that mirrors Minecraft 1.20.5+ StreamCodec API.
 * Used to minimize code changes when backporting to 1.20.1 where StreamCodec does not exist.
 */
public interface StreamCodec<B, V> {

    V decode(B buf);

    void encode(B buf, V value);

    default <O> StreamCodec<B, O> map(Function<? super V, ? extends O> toO, Function<? super O, ? extends V> toV) {
        return new StreamCodec<>() {
            @Override
            public O decode(B buf) {
                return toO.apply(StreamCodec.this.decode(buf));
            }

            @Override
            public void encode(B buf, O value) {
                StreamCodec.this.encode(buf, toV.apply(value));
            }
        };
    }

    default <C extends Collection<V>> StreamCodec<B, C> apply(
            Function<StreamCodec<B, V>, StreamCodec<B, C>> operator) {
        return operator.apply(this);
    }

    static <B, V> StreamCodec<B, V> of(BiConsumer<B, V> encoder, Function<B, V> decoder) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                return decoder.apply(buf);
            }

            @Override
            public void encode(B buf, V value) {
                encoder.accept(buf, value);
            }
        };
    }

    static <B extends ByteBuf, V> StreamCodec<B, V> ofMember(BiConsumer<B, V> encoder, Function<B, V> decoder) {
        return of(encoder, decoder);
    }

    static <B, C1, V> StreamCodec<B, V> composite(
            StreamCodec<? super B, C1> codec1, Function<V, C1> getter1,
            Function<C1, V> factory) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                C1 v1 = codec1.decode(buf);
                return factory.apply(v1);
            }

            @Override
            public void encode(B buf, V value) {
                codec1.encode(buf, getter1.apply(value));
            }
        };
    }

    static <B, C1, C2, V> StreamCodec<B, V> composite(
            StreamCodec<? super B, C1> codec1, Function<V, C1> getter1,
            StreamCodec<? super B, C2> codec2, Function<V, C2> getter2,
            BiFunction<C1, C2, V> factory) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                C1 v1 = codec1.decode(buf);
                C2 v2 = codec2.decode(buf);
                return factory.apply(v1, v2);
            }

            @Override
            public void encode(B buf, V value) {
                codec1.encode(buf, getter1.apply(value));
                codec2.encode(buf, getter2.apply(value));
            }
        };
    }

    static <B, C1, C2, C3, V> StreamCodec<B, V> composite(
            StreamCodec<? super B, C1> codec1, Function<V, C1> getter1,
            StreamCodec<? super B, C2> codec2, Function<V, C2> getter2,
            StreamCodec<? super B, C3> codec3, Function<V, C3> getter3,
            Function3<C1, C2, C3, V> factory) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                C1 v1 = codec1.decode(buf);
                C2 v2 = codec2.decode(buf);
                C3 v3 = codec3.decode(buf);
                return factory.apply(v1, v2, v3);
            }

            @Override
            public void encode(B buf, V value) {
                codec1.encode(buf, getter1.apply(value));
                codec2.encode(buf, getter2.apply(value));
                codec3.encode(buf, getter3.apply(value));
            }
        };
    }

    static <B, C1, C2, C3, C4, V> StreamCodec<B, V> composite(
            StreamCodec<? super B, C1> codec1, Function<V, C1> getter1,
            StreamCodec<? super B, C2> codec2, Function<V, C2> getter2,
            StreamCodec<? super B, C3> codec3, Function<V, C3> getter3,
            StreamCodec<? super B, C4> codec4, Function<V, C4> getter4,
            Function4<C1, C2, C3, C4, V> factory) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                C1 v1 = codec1.decode(buf);
                C2 v2 = codec2.decode(buf);
                C3 v3 = codec3.decode(buf);
                C4 v4 = codec4.decode(buf);
                return factory.apply(v1, v2, v3, v4);
            }

            @Override
            public void encode(B buf, V value) {
                codec1.encode(buf, getter1.apply(value));
                codec2.encode(buf, getter2.apply(value));
                codec3.encode(buf, getter3.apply(value));
                codec4.encode(buf, getter4.apply(value));
            }
        };
    }

    @FunctionalInterface
    interface Function3<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    @FunctionalInterface
    interface Function4<A, B, C, D, R> {
        R apply(A a, B b, C c, D d);
    }
}
