package rearth.oritech;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.api.attachment.Attachment;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;
import rearth.oritech.api.networking.PacketId;
import rearth.oritech.compat.StreamCodec;

import java.util.ServiceLoader;
import java.util.function.Consumer;

public interface OritechPlatform {

    OritechPlatform INSTANCE = ServiceLoader.load(OritechPlatform.class)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Failed to load platform service."));

    // Network — sends a packet with the given writer producing the payload bytes
    void sendBlockHandle(BlockEntity blockEntity, PacketId<?> packetId, Consumer<FriendlyByteBuf> writer);

    void sendPlayerHandle(ServerPlayer player, PacketId<?> packetId, Consumer<FriendlyByteBuf> writer);

    void sendToServer(PacketId<?> packetId, Consumer<FriendlyByteBuf> writer);

    <T> void registerToClient(PacketId<T> packetId, StreamCodec<FriendlyByteBuf, T> codec, TriConsumer<T, Level, Player> consumer);

    <T> void registerToServer(PacketId<T> packetId, StreamCodec<FriendlyByteBuf, T> codec, TriConsumer<T, Player, Level> consumer);

    // Attachment
    <T> void register(Attachment<T> attachment);

    <T> boolean hasAttachment(LivingEntity entity, Attachment<T> attachment);

    <T> T getAttachmentValue(LivingEntity entity, Attachment<T> attachment);

    <T> void setAttachment(LivingEntity entity, Attachment<T> attachment, T value);

    <T> void removeAttachment(LivingEntity entity, Attachment<T> attachment);

    // FakeMachinePlayer
    ServerPlayer create(ServerLevel world, GameProfile profile, SimpleInventoryStorage inventory);

    void resetCapabilities(ServerLevel world, BlockPos pos);

    /**
     * Fires a BlockEvent.BreakEvent / PlayerBlockBreakEvents.BEFORE and returns whether it was allowed.
     */
    boolean canPlayerBreakBlock(Level level, BlockPos pos, BlockState state, @NotNull Player player);

    /**
     * Fires a LivingDamageEvent.Pre or Fabric equivalent and returns whether the attack is allowed.
     */
    boolean canAttackBeDone(Level level, LivingEntity target, float amount, DamageSource damageSource);
}
