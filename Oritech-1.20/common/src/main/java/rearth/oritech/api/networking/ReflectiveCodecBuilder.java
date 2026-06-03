package rearth.oritech.api.networking;

import net.minecraft.network.FriendlyByteBuf;
import rearth.oritech.compat.ByteBufCodecs;
import rearth.oritech.compat.StreamCodec;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashMap;

import static rearth.oritech.api.networking.NetworkManager.getAutoCodec;

public class ReflectiveCodecBuilder {

    public static <E extends Enum<E>> StreamCodec<FriendlyByteBuf, E> createForEnum(Class<E> enumClass) {
        return new StreamCodec<>() {
            @Override
            public void encode(FriendlyByteBuf buf, E value) {
                buf.writeShort(value.ordinal());
            }

            @Override
            public E decode(FriendlyByteBuf buf) {
                return enumClass.getEnumConstants()[buf.readShort()];
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T extends Record> StreamCodec<FriendlyByteBuf, T> create(Class<T> recordClass) {
        if (!recordClass.isRecord()) {
            throw new IllegalArgumentException(recordClass.getName() + " is not a record type.");
        }

        RecordComponent[] recordComponents = recordClass.getRecordComponents();
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();

        var accessors = new ArrayList<MethodHandle>(recordComponents.length);
        var componentCodecs = new ArrayList<StreamCodec<FriendlyByteBuf, ?>>(recordComponents.length);
        Class<?>[] componentTypes = new Class<?>[recordComponents.length];

        for (int i = 0; i < recordComponents.length; i++) {
            var component = recordComponents[i];
            componentTypes[i] = component.getType();
            try {
                accessors.add(lookup.unreflect(component.getAccessor()));
                var listCandidate = NetworkManager.getListType(component.getGenericType());
                var mapCandidate = NetworkManager.getMapType(component.getGenericType());
                if (listCandidate.isPresent()) {
                    var codec = getAutoCodec((Class<?>) listCandidate.get()).apply(ByteBufCodecs.list());
                    if (codec == null)
                        throw new RuntimeException("Failed to get codec for: " + component.getName());
                    componentCodecs.add(codec);
                } else if (mapCandidate.isPresent()) {
                    var keyCodec = getAutoCodec((Class<?>) mapCandidate.get().getA());
                    var valueCodec = getAutoCodec((Class<?>) mapCandidate.get().getB());
                    componentCodecs.add(ByteBufCodecs.map(HashMap::new, keyCodec, valueCodec));
                } else {
                    var codec = getAutoCodec(component.getType());
                    if (codec == null)
                        throw new RuntimeException("Failed to get codec for: " + component.getName());
                    componentCodecs.add(codec);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to unreflect accessor for: " + component.getName(), e);
            }
        }

        java.lang.reflect.Constructor<T> ctor;
        try {
            ctor = recordClass.getDeclaredConstructor(componentTypes);
            ctor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Failed to find canonical constructor for: " + recordClass.getName(), e);
        }

        return new StreamCodec<>() {
            @Override
            @SuppressWarnings("rawtypes")
            public T decode(FriendlyByteBuf buf) {
                Object[] args = new Object[componentCodecs.size()];
                for (int i = 0; i < componentCodecs.size(); i++) {
                    args[i] = ((StreamCodec) componentCodecs.get(i)).decode(buf);
                }
                try {
                    return ctor.newInstance(args);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to construct record: " + recordClass.getName(), e);
                }
            }

            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public void encode(FriendlyByteBuf buf, T value) {
                for (int i = 0; i < accessors.size(); i++) {
                    try {
                        Object fieldValue = accessors.get(i).invoke(value);
                        ((StreamCodec) componentCodecs.get(i)).encode(buf, fieldValue);
                    } catch (Throwable e) {
                        throw new RuntimeException("Failed to encode record component at index " + i, e);
                    }
                }
            }
        };
    }
}
