package rearth.oritech.forge;

import com.google.auto.service.AutoService;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.level.BlockEvent;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;
import rearth.oritech.OritechPlatform;
import rearth.oritech.api.attachment.Attachment;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;
import rearth.oritech.api.networking.PacketId;
import rearth.oritech.compat.StreamCodec;
import rearth.oritech.util.forge.FakeMachinePlayerImpl;

import java.util.function.Consumer;

@AutoService(OritechPlatform.class)
public class OritechPlatformForge implements OritechPlatform {

    // -------------------------------------------------------------------------
    // Networking — Architectury 9.x NetworkManager
    // -------------------------------------------------------------------------

    @Override
    public void sendBlockHandle(BlockEntity blockEntity, PacketId<?> packetId, Consumer<FriendlyByteBuf> writer) {
        if (!(blockEntity.getLevel() instanceof ServerLevel serverLevel)) return;
        var chunkPos = new ChunkPos(blockEntity.getBlockPos());
        // chunkMap.getPlayers(chunkPos, boundaryOnly=false) returns all players tracking this chunk.
        for (ServerPlayer player : serverLevel.getChunkSource().chunkMap.getPlayers(chunkPos, false)) {
            sendPlayerHandle(player, packetId, writer);
        }
    }

    @Override
    public void sendPlayerHandle(ServerPlayer player, PacketId<?> packetId, Consumer<FriendlyByteBuf> writer) {
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        writer.accept(buf);
        dev.architectury.networking.NetworkManager.sendToPlayer(player, packetId.id(), buf);
    }

    @Override
    public void sendToServer(PacketId<?> packetId, Consumer<FriendlyByteBuf> writer) {
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        writer.accept(buf);
        dev.architectury.networking.NetworkManager.sendToServer(packetId.id(), buf);
    }

    @Override
    public <T> void registerToClient(PacketId<T> packetId, StreamCodec<FriendlyByteBuf, T> codec, TriConsumer<T, Level, Player> consumer) {
        dev.architectury.networking.NetworkManager.registerReceiver(
            dev.architectury.networking.NetworkManager.Side.S2C,
            packetId.id(),
            (buf, ctx) -> {
                T decoded = codec.decode(buf);
                ctx.queue(() -> {
                    Player player = ctx.getPlayer();
                    consumer.accept(decoded, player.level, player);
                });
            }
        );
    }

    @Override
    public <T> void registerToServer(PacketId<T> packetId, StreamCodec<FriendlyByteBuf, T> codec, TriConsumer<T, Player, Level> consumer) {
        dev.architectury.networking.NetworkManager.registerReceiver(
            dev.architectury.networking.NetworkManager.Side.C2S,
            packetId.id(),
            (buf, ctx) -> {
                T decoded = codec.decode(buf);
                ctx.queue(() -> {
                    Player player = ctx.getPlayer();
                    consumer.accept(decoded, player, player.level);
                });
            }
        );
    }

    // -------------------------------------------------------------------------
    // Attachment — NBT-based persistence on entity.getPersistentData()
    // -------------------------------------------------------------------------

    /**
     * Root key in the entity's {@link net.minecraft.world.entity.Entity#getPersistentData()}
     * under which all Oritech attachment data is stored.
     */
    private static final String ATTACHMENT_ROOT_KEY = "oritech_attachments";

    /**
     * Returns (and lazily creates) the sub-CompoundTag used to store Oritech attachment data.
     * For Forge 1.20.1, we use {@code entity.getPersistentData()} which is available on all
     * {@link net.minecraft.world.entity.Entity} subclasses and survives death when data is
     * copied in {@code PlayerEvent.Clone}.
     *
     * <p>Note: {@code PlayerEvent.Clone} in {@link OritechModForge.ForgeGameBusEvents} already
     * triggers {@code PlayerAugments.refreshActiveAugments}, which re-reads the NBT after cloning.
     * The persistent data is automatically copied on respawn by Forge for the non-new-respawn path.
     */
    private static CompoundTag getAttachmentRoot(LivingEntity entity) {
        CompoundTag root = entity.getPersistentData();
        if (!root.contains(ATTACHMENT_ROOT_KEY, 10 /* TAG_Compound */)) {
            root.put(ATTACHMENT_ROOT_KEY, new CompoundTag());
        }
        return root.getCompound(ATTACHMENT_ROOT_KEY);
    }

    @Override
    public <T> void register(Attachment<T> attachment) {
        // No-op in Forge 1.20.1 — there is no AttachmentType registry.
        // Data is stored directly in entity persistent NBT via getPersistentData().
        Oritech.LOGGER.debug("Registering attachment (NBT-backed): {}", attachment.identifier());
    }

    @Override
    public <T> boolean hasAttachment(LivingEntity entity, Attachment<T> attachment) {
        return getAttachmentRoot(entity).contains(attachment.identifier().toString());
    }

    @Override
    public <T> T getAttachmentValue(LivingEntity entity, Attachment<T> attachment) {
        var root = getAttachmentRoot(entity);
        var key = attachment.identifier().toString();
        if (!root.contains(key)) {
            return attachment.initializer().get();
        }
        var tag = root.get(key);
        return attachment.persistenceCodec()
            .parse(NbtOps.INSTANCE, tag)
            .result()
            .orElseGet(attachment.initializer());
    }

    @Override
    public <T> void setAttachment(LivingEntity entity, Attachment<T> attachment, T value) {
        var root = getAttachmentRoot(entity);
        attachment.persistenceCodec()
            .encodeStart(NbtOps.INSTANCE, value)
            .result()
            .ifPresent(tag -> root.put(attachment.identifier().toString(), tag));
        // Ensure the root is re-linked in case it was just created.
        entity.getPersistentData().put(ATTACHMENT_ROOT_KEY, root);
    }

    @Override
    public <T> void removeAttachment(LivingEntity entity, Attachment<T> attachment) {
        getAttachmentRoot(entity).remove(attachment.identifier().toString());
    }

    // -------------------------------------------------------------------------
    // FakeMachinePlayer
    // -------------------------------------------------------------------------

    @Override
    public ServerPlayer create(ServerLevel world, GameProfile profile, SimpleInventoryStorage inventory) {
        return FakeMachinePlayerImpl.create(world, profile, inventory);
    }

    @Override
    public void resetCapabilities(ServerLevel world, BlockPos pos) {
        // Forge 1.20.1 has no per-block-position capability cache invalidation API.
        // LazyOptional handles its own invalidation lifecycle; the block entity is responsible
        // for calling LazyOptional.invalidate() when it is removed.
        // TODO: If needed, iterate block entity capability providers and call invalidate().
    }

    // -------------------------------------------------------------------------
    // Block break / attack checks
    // -------------------------------------------------------------------------

    @Override
    public boolean canPlayerBreakBlock(Level level, BlockPos pos, BlockState state, @NotNull Player player) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return true;
        }
        var event = new BlockEvent.BreakEvent(serverLevel, pos, state, player);
        MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    @Override
    public boolean canAttackBeDone(Level level, LivingEntity target, float amount, DamageSource damageSource) {
        if (level.isClientSide() || !(level instanceof ServerLevel)) {
            return true;
        }
        // In Forge 1.20.1 there is no LivingIncomingDamageEvent/DamageContainer.
        // Use LivingAttackEvent which fires before damage is applied and can be cancelled.
        var event = new LivingAttackEvent(target, damageSource, amount);
        MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }
}
