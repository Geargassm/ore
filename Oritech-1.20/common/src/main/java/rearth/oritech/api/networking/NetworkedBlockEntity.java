package rearth.oritech.api.networking;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.Oritech;

/**
 * Base class for block entities that sync data over the network.
 * When implementing and block has a GUI, call {@code sendUpdate(SyncType.GUI_OPEN)} in saveExtraData().
 */
public abstract class NetworkedBlockEntity extends BlockEntity implements BlockEntityTicker<NetworkedBlockEntity> {

    private boolean networkDirty = false;
    private boolean needsInitialUpdate = false;
    private long lastSentTickUpdate = 0;

    public NetworkedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        if (world.isClientSide) {
            clientTick(world, pos, state, blockEntity);
            return;
        }

        serverTick(world, pos, state, blockEntity);

        var time = world.getGameTime();

        if ((time + this.worldPosition.asLong()) % getSparseUpdateInterval() == 0)
            sendUpdate(SyncType.SPARSE_TICK);

        if (networkDirty && time >= lastSentTickUpdate + getTickUpdateInterval()) {
            networkDirty = false;
            sendUpdate(SyncType.TICK);
            lastSentTickUpdate = time;
        }
        if (needsInitialUpdate) {
            needsInitialUpdate = false;
            sendUpdate(SyncType.INITIAL);
        }
    }

    public abstract void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity);

    public void clientTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {}

    public int getSparseUpdateInterval() { return 100; }

    public int getTickUpdateInterval() { return 4; }

    @Override
    public void setChanged() {
        setChanged(false);
    }

    public void setChanged(boolean updateComparator) {
        if (this.level != null) {
            setChanged(this.level, this.worldPosition, this.getBlockState());
            if (updateComparator)
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        networkDirty = true;
    }

    public void preNetworkUpdate(SyncType type) {}

    public void sendUpdate(SyncType type) {
        if (level == null) {
            Oritech.LOGGER.warn("unable to send update: World is null.");
            return;
        }
        preNetworkUpdate(type);
        var usedBuf = new FriendlyByteBuf(Unpooled.buffer());
        var fieldCount = NetworkManager.encodeFields(this, type, usedBuf, level);
        if (fieldCount == 0) return;
        NetworkManager.sendBlockHandle(this,
            new NetworkManager.MessagePayload(worldPosition, BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(getType()), type, usedBuf.array()));
    }

    public void sendUpdate(SyncType type, ServerPlayer player) {
        if (level == null) {
            Oritech.LOGGER.warn("unable to send player update: World is null.");
            return;
        }
        preNetworkUpdate(type);
        var usedBuf = new FriendlyByteBuf(Unpooled.buffer());
        var fieldCount = NetworkManager.encodeFields(this, type, usedBuf, level);
        if (fieldCount == 0) return;
        NetworkManager.sendPlayerHandle(
            new NetworkManager.MessagePayload(worldPosition, BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(getType()), type, usedBuf.array()),
            player);
    }

    @Override
    public CompoundTag getUpdateTag() {
        needsInitialUpdate = true;
        return super.getUpdateTag();
    }

    // 1.20.1 uses saveAdditional(CompoundTag) without HolderLookup.Provider
    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
    }
}
