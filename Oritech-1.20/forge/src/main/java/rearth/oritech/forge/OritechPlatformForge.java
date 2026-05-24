package rearth.oritech.forge;

import com.google.auto.service.AutoService;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
import rearth.oritech.Oritech;
import rearth.oritech.OritechPlatform;
import rearth.oritech.api.attachment.Attachment;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;
import rearth.oritech.util.forge.FakeMachinePlayerImpl;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@AutoService(OritechPlatform.class)
public class OritechPlatformForge implements OritechPlatform {

    // -------------------------------------------------------------------------
    // Networking
    // -------------------------------------------------------------------------

    @Override
    public void sendRawToBlockClients(BlockEntity blockEntity, ResourceLocation id, Consumer<FriendlyByteBuf> writer) {
        if (blockEntity.getLevel() instanceof ServerLevel serverLevel) {
            var chunkPos = new ChunkPos(blockEntity.getBlockPos());
            for (ServerPlayer player : serverLevel.getChunkSource().chunkMap.getPlayers(chunkPos, false)) {
                sendRawToPlayer(player, id, writer);
            }
        }
    }

    @Override
    public void sendRawToPlayer(ServerPlayer player, ResourceLocation id, Consumer<FriendlyByteBuf> writer) {
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        writer.accept(buf);
        dev.architectury.networking.NetworkManager.sendToPlayer(player, id, buf);
    }

    @Override
    public void sendRawToServer(ResourceLocation id, Consumer<FriendlyByteBuf> writer) {
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        writer.accept(buf);
        dev.architectury.networking.NetworkManager.sendToServer(id, buf);
    }

    @Override
    public <T> void registerToClient(ResourceLocation id, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Level> handler) {
        dev.architectury.networking.NetworkManager.registerReceiver(
            dev.architectury.networking.NetworkManager.Side.S2C, id,
            (buf, ctx) -> {
                T decoded = decoder.apply(buf.copy());
                ctx.queue(() -> {
                    Level level = ctx.getPlayer().level;
                    handler.accept(decoded, level);
                });
            }
        );
    }

    @Override
    public <T> void registerToServer(ResourceLocation id, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Player> handler) {
        dev.architectury.networking.NetworkManager.registerReceiver(
            dev.architectury.networking.NetworkManager.Side.C2S, id,
            (buf, ctx) -> {
                T decoded = decoder.apply(buf.copy());
                ctx.queue(() -> {
                    Player player = ctx.getPlayer();
                    handler.accept(decoded, player);
                });
            }
        );
    }

    // -------------------------------------------------------------------------
    // Attachment — NBT-based persistence on player.getPersistentData()
    // -------------------------------------------------------------------------

    /**
     * Returns the CompoundTag used to store attachment data for the given entity.
     * Uses player.getPersistentData() with a sub-tag named "oritech_attachments".
     * For non-player entities we use a capability-independent field via EntityData,
     * but since attachments are primarily used on players we fall back to player.getPersistentData().
     */
    private static final String ATTACHMENT_ROOT_KEY = "oritech_attachments";

    private static CompoundTag getAttachmentRoot(LivingEntity entity) {
        // Use getPersistentData() which survives death (via PlayerEvent.Clone) when copyOnDeath is true.
        // For 1.20.1 Forge, only Players have getPersistentData(); for other LivingEntities we store
        // on the entity's persistentData (available via Entity#getPersistentData()).
        CompoundTag root = entity.getPersistentData();
        if (!root.contains(ATTACHMENT_ROOT_KEY, 10 /* TAG_COMPOUND */)) {
            root.put(ATTACHMENT_ROOT_KEY, new CompoundTag());
        }
        return root.getCompound(ATTACHMENT_ROOT_KEY);
    }

    @Override
    public <T> void register(Attachment<T> attachment) {
        // No-op: Forge 1.20.1 has no AttachmentType registry.
        // Data is stored directly in NBT via getPersistentData().
        Oritech.LOGGER.debug("Registered attachment (NBT-based): {}", attachment.identifier());
    }

    @Override
    public <T> boolean hasAttachment(LivingEntity entity, Attachment<T> attachment) {
        var root = getAttachmentRoot(entity);
        return root.contains(attachment.identifier().toString());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAttachmentValue(LivingEntity entity, Attachment<T> attachment) {
        var root = getAttachmentRoot(entity);
        var key = attachment.identifier().toString();
        if (!root.contains(key)) {
            return attachment.initializer().get();
        }
        var tag = root.get(key);
        var result = attachment.persistenceCodec().parse(NbtOps.INSTANCE, tag);
        return result.result().orElseGet(attachment.initializer());
    }

    @Override
    public <T> void setAttachment(LivingEntity entity, Attachment<T> attachment, T value) {
        var root = getAttachmentRoot(entity);
        var key = attachment.identifier().toString();
        var encoded = attachment.persistenceCodec().encodeStart(NbtOps.INSTANCE, value);
        encoded.result().ifPresent(tag -> root.put(key, tag));
        // Write root back — CompoundTag is mutable so it's already updated in place, but we
        // call put to ensure the sub-tag is registered if it was newly created.
        entity.getPersistentData().put(ATTACHMENT_ROOT_KEY, root);
    }

    @Override
    public <T> void removeAttachment(LivingEntity entity, Attachment<T> attachment) {
        var root = getAttachmentRoot(entity);
        root.remove(attachment.identifier().toString());
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
        // In Forge 1.20.1, capability caches are not per-position. Notify listeners if needed.
        // TODO: If block entity capability caches need invalidation, iterate AttachCapabilitiesEvent
        //  providers and call LazyOptional#invalidate on them. For now this is a no-op.
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    @Override
    public boolean canPlayerBreakBlock(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
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
        // In Forge 1.20.1 there is no LivingIncomingDamageEvent/DamageContainer — use LivingAttackEvent.
        var event = new LivingAttackEvent(target, damageSource, amount);
        MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }
}
