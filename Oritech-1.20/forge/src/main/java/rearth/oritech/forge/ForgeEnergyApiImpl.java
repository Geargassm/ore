package rearth.oritech.forge;

import com.google.auto.service.AutoService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.energy.BlockEnergyApi;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.ItemEnergyApi;
import rearth.oritech.api.energy.containers.SimpleEnergyItemStorage;
import rearth.oritech.api.lookup.BlockLookupCache;
import rearth.oritech.util.StackContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// TODO: DataComponentType<Long> getEnergyComponent() — 1.20.1 has no DataComponents.
//   Energy for items is stored in ItemStack NBT under the tag "energy" (see SimpleEnergyItemStorage).
//   If the common module calls getEnergyComponent() for display purposes we return null here.

@AutoService({BlockEnergyApi.class, ItemEnergyApi.class})
public class ForgeEnergyApiImpl implements BlockEnergyApi, ItemEnergyApi {

    private final List<Supplier<BlockEntityType<?>>> registeredBlockEntities = new ArrayList<>();
    private final List<Supplier<Item>> registeredItems = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    @Override
    public void registerBlockEntity(Supplier<BlockEntityType<?>> typeSupplier) {
        registeredBlockEntities.add(typeSupplier);
    }

    @Override
    public void registerForItem(Supplier<Item> itemSupplier) {
        registeredItems.add(itemSupplier);
    }

    /**
     * Called from {@link OritechModForge.ForgeGameBusEvents#onAttachBlockEntityCapabilities}.
     */
    public void onAttachBlockEntityCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        var entity = event.getObject();
        for (var supplied : registeredBlockEntities) {
            if (supplied.get().isInstance(entity)) {
                if (!(entity instanceof EnergyApi.BlockProvider)) continue;
                var provider = (EnergyApi.BlockProvider) entity;
                event.addCapability(
                    new ResourceLocation("oritech", "energy_" + registeredBlockEntities.indexOf(supplied)),
                    new ICapabilityProvider() {
                        @Override
                        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                            if (cap == ForgeCapabilities.ENERGY) {
                                var storage = provider.getEnergyStorage(side);
                                if (storage == null) return LazyOptional.empty();
                                return LazyOptional.of(() -> new ContainerStorageWrapper(storage)).cast();
                            }
                            return LazyOptional.empty();
                        }
                    }
                );
                return; // only attach once
            }
        }
    }

    /**
     * Called from {@link OritechModForge.ForgeGameBusEvents#onAttachItemStackCapabilities}.
     */
    public void onAttachItemStackCapabilities(AttachCapabilitiesEvent<net.minecraft.world.item.ItemStack> event) {
        var stack = event.getObject();
        for (var supplied : registeredItems) {
            if (stack.getItem() == supplied.get()) {
                if (!(stack.getItem() instanceof EnergyApi.ItemProvider)) continue;
                var itemProvider = (EnergyApi.ItemProvider) stack.getItem();
                final var capturedStack = stack;
                event.addCapability(
                    new ResourceLocation("oritech", "item_energy"),
                    new ICapabilityProvider() {
                        @Override
                        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                            if (cap == ForgeCapabilities.ENERGY) {
                                var storage = itemProvider.getEnergyStorage(capturedStack);
                                if (storage == null) return LazyOptional.empty();
                                return LazyOptional.of(() -> new ContainerStorageWrapper(storage)).cast();
                            }
                            return LazyOptional.empty();
                        }
                    }
                );
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // BlockEnergyApi — find / createCache
    // -------------------------------------------------------------------------

    @Override
    public EnergyApi.EnergyStorage find(Level world, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity entity,
                                        @Nullable Direction direction) {
        BlockEntity be = entity != null ? entity : world.getBlockEntity(pos);
        if (be == null) return null;
        LazyOptional<IEnergyStorage> opt = be.getCapability(ForgeCapabilities.ENERGY, direction);
        return opt.map(storage -> {
            if (storage instanceof ContainerStorageWrapper wrapper) return wrapper.container;
            return (EnergyApi.EnergyStorage) new ForgeStorageWrapper(storage);
        }).orElse(null);
    }

    @Override
    public EnergyApi.EnergyStorage find(Level world, BlockPos pos, @Nullable Direction direction) {
        return find(world, pos, null, null, direction);
    }

    @Override
    public BlockLookupCache<EnergyApi.EnergyStorage> createCache(Level world, BlockPos pos, @Nullable Direction direction) {
        // Forge 1.20.1 has no BlockCapabilityCache — use a simple supplier-based cache via BlockLookupCache.of.
        return BlockLookupCache.of(() -> find(world, pos, direction));
    }

    // -------------------------------------------------------------------------
    // ItemEnergyApi — find
    // -------------------------------------------------------------------------

    @Override
    public EnergyApi.EnergyStorage find(StackContext stack) {
        if (stack.getValue().getCount() > 1) return null;
        LazyOptional<IEnergyStorage> opt = stack.getValue().getCapability(ForgeCapabilities.ENERGY);
        return opt.map(storage -> {
            if (storage instanceof ContainerStorageWrapper wrapper) {
                if (wrapper.container instanceof SimpleEnergyItemStorage itemStorage) {
                    return (EnergyApi.EnergyStorage) itemStorage.withCallback(ignored -> stack.sync());
                }
                return (EnergyApi.EnergyStorage) wrapper.container;
            }
            return (EnergyApi.EnergyStorage) new ForgeStorageWrapper(storage);
        }).orElse(null);
    }

    @Override
    @Nullable
    public net.minecraft.core.component.DataComponentType<Long> getEnergyComponent() {
        // TODO: DataComponentType does not exist in 1.20.1.
        // Energy for items is stored as NBT directly (tag: "energy") by SimpleEnergyItemStorage.
        // Return null; callers that use this for display should guard against null.
        return null;
    }

    // -------------------------------------------------------------------------
    // Inner wrappers
    // -------------------------------------------------------------------------

    /**
     * Wraps an external (other-mod) {@link IEnergyStorage} as an Oritech {@link EnergyApi.EnergyStorage}.
     * Note: {@link IEnergyStorage} uses {@code int}; values are clamped to {@link Integer#MAX_VALUE}.
     */
    public static class ForgeStorageWrapper extends EnergyApi.EnergyStorage {

        public final IEnergyStorage storage;

        public ForgeStorageWrapper(IEnergyStorage storage) {
            this.storage = storage;
        }

        @Override
        public long insert(long maxAmount, boolean simulate) {
            return storage.receiveEnergy((int) Math.min(maxAmount, Integer.MAX_VALUE), simulate);
        }

        @Override
        public long extract(long maxAmount, boolean simulate) {
            return storage.extractEnergy((int) Math.min(maxAmount, Integer.MAX_VALUE), simulate);
        }

        @Override
        public long getAmount() {
            return storage.getEnergyStored();
        }

        @Override
        public long getCapacity() {
            return storage.getMaxEnergyStored();
        }

        @Override
        public void setAmount(long amount) {
            // IEnergyStorage does not expose setAmount; no-op.
        }

        @Override
        public void update() {
            // no-op for external storages
        }

        @Override
        public boolean supportsInsertion() {
            return storage.canReceive();
        }

        @Override
        public boolean supportsExtraction() {
            return storage.canExtract();
        }
    }

    /**
     * Wraps an Oritech {@link EnergyApi.EnergyStorage} as a Forge {@link IEnergyStorage} so that
     * other mods can interact with Oritech machines/items via the Forge capability API.
     */
    public static class ContainerStorageWrapper implements IEnergyStorage {

        public final EnergyApi.EnergyStorage container;

        public static @Nullable ContainerStorageWrapper of(@Nullable EnergyApi.EnergyStorage container) {
            if (container == null) return null;
            return new ContainerStorageWrapper(container);
        }

        public ContainerStorageWrapper(EnergyApi.EnergyStorage container) {
            this.container = container;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int inserted = (int) Math.min(container.insert(maxReceive, simulate), Integer.MAX_VALUE);
            if (!simulate && inserted > 0) container.update();
            return inserted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = (int) Math.min(container.extract(maxExtract, simulate), Integer.MAX_VALUE);
            if (!simulate && extracted > 0) container.update();
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(container.getAmount(), Integer.MAX_VALUE);
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(container.getCapacity(), Integer.MAX_VALUE);
        }

        @Override
        public boolean canExtract() {
            return container.supportsExtraction();
        }

        @Override
        public boolean canReceive() {
            return container.supportsInsertion();
        }
    }
}
