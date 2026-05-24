package rearth.oritech.api.attachment;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.resources.ResourceLocation;
import rearth.oritech.compat.StreamCodec;

import java.util.function.Supplier;

public interface Attachment<A> {

    ResourceLocation identifier();

    Codec<A> persistenceCodec();

    StreamCodec<ByteBuf, A> networkCodec();

    Supplier<A> initializer();
}
