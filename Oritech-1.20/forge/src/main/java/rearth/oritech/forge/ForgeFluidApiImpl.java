package rearth.oritech.forge;

import com.google.auto.service.AutoService;
import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.BlockFluidApi;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.ItemFluidApi;
import rearth.oritech.api.fluid.containers.DelegatingFluidStorage;
import rearth.oritech.api.fluid.containers.SimpleItemFluidStorage;
import rearth.oritech.api.lookup.BlockLookupCache;
import rearth.oritech.util.StackContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@AutoService({BlockFluidApi.class, ItemFluidApi.class})
public class ForgeFluidApiImpl implements BlockFluidApi, ItemFluidApi {

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
                if (!(entity instanceof FluidApi.BlockProvider)) continue;
                var provider = (FluidApi.BlockProvider) entity;
                event.addCapability(
                    new ResourceLocation("oritech", "fluid_block"),
                    new ICapabilityProvider() {
                        @Override
                        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                            if (cap == ForgeCapabilities.FLUID_HANDLER) {
                                var storage = provider.getFluidStorage(side);
                                if (storage == null) return LazyOptional.empty();
                                IFluidHandler handler = wrapFluidStorage(storage);
                                if (handler == null) return LazyOptional.empty();
                                return LazyOptional.of(() -> handler).cast();
                            }
                            return LazyOptional.empty();
                        }
                    }
                );
                return;
            }
        }
    }

    /**
     * Called from {@link OritechModForge.ForgeGameBusEvents#onAttachItemStackCapabilities}.
     */
    public void onAttachItemStackCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
        var stack = event.getObject();
        for (var supplied : registeredItems) {
            if (stack.getItem() == supplied.get()) {
                if (!(stack.getItem() instanceof FluidApi.ItemProvider)) continue;
                var itemProvider = (FluidApi.ItemProvider) stack.getItem();
                final var capturedStack = stack;
                event.addCapability(
                    new ResourceLocation("oritech", "fluid_item"),
                    new ICapabilityProvider() {
                        @Override
                        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                            if (cap == ForgeCapabilities.FLUID_HANDLER_ITEM) {
                                var storage = itemProvider.getFluidStorage(capturedStack);
                                if (storage == null) return LazyOptional.empty();
                                if (!(storage instanceof FluidApi.SingleSlotStorage singleSlot)) return LazyOptional.empty();
                                return LazyOptional.of(() -> new FluidContainerItemWrapper(singleSlot, capturedStack)).cast();
                            }
                            return LazyOptional.empty();
                        }
                    }
                );
                return;
            }
        }
    }

    @Nullable
    private static IFluidHandler wrapFluidStorage(FluidApi.FluidStorage storage) {
        if (storage instanceof FluidApi.MultiSlotStorage multiSlot) {
            return new MultiSlotStorageWrapper(multiSlot);
        } else if (storage instanceof FluidApi.SingleSlotStorage singleSlot) {
            return new SingleSlotContainerStorageWrapper(singleSlot);
        } else if (storage instanceof DelegatingFluidStorage delegating) {
            return new DelegatingContainerStorageWrapper(delegating);
        }
        Oritech.LOGGER.error("Unable to wrap fluid storage of type: {}", storage.getClass());
        return null;
    }

    // -------------------------------------------------------------------------
    // BlockFluidApi — find / createCache
    // -------------------------------------------------------------------------

    @Override
    public FluidApi.FluidStorage find(Level world, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity entity,
                                      @Nullable Direction direction) {
        BlockEntity be = entity != null ? entity : world.getBlockEntity(pos);
        if (be == null) return null;
        LazyOptional<IFluidHandler> opt = be.getCapability(ForgeCapabilities.FLUID_HANDLER, direction);
        return opt.map(handler -> unwrapFluidHandler(handler)).orElse(null);
    }

    @Override
    public FluidApi.FluidStorage find(Level world, BlockPos pos, @Nullable Direction direction) {
        return find(world, pos, null, null, direction);
    }

    @Override
    public BlockLookupCache<FluidApi.FluidStorage> createCache(Level world, BlockPos pos, @Nullable Direction direction) {
        return BlockLookupCache.of(() -> find(world, pos, direction));
    }

    // -------------------------------------------------------------------------
    // ItemFluidApi — find
    // -------------------------------------------------------------------------

    @Override
    public FluidApi.FluidStorage find(StackContext stack) {
        if (stack.getValue().getCount() > 1) return null;
        LazyOptional<IFluidHandlerItem> opt = stack.getValue().getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
        return opt.map(handler -> {
            if (handler instanceof SingleSlotContainerStorageWrapper wrapper) {
                if (wrapper.container instanceof SimpleItemFluidStorage itemContainer) {
                    return (FluidApi.FluidStorage) itemContainer.withCallback(ignored -> stack.sync());
                }
                return (FluidApi.FluidStorage) wrapper.container;
            }
            return (FluidApi.FluidStorage) new NeoforgeItemStorageWrapper(handler, stack);
        }).orElse(null);
    }

    @Nullable
    private static FluidApi.FluidStorage unwrapFluidHandler(@Nullable IFluidHandler handler) {
        if (handler == null) return null;
        if (handler instanceof SingleSlotContainerStorageWrapper wrapper) return wrapper.container;
        if (handler instanceof MultiSlotStorageWrapper wrapper) return wrapper.container;
        if (handler instanceof DelegatingContainerStorageWrapper wrapper) return wrapper.container;
        return new NeoforgeStorageWrapper(handler);
    }

    // -------------------------------------------------------------------------
    // Inner wrappers — external mods → Oritech
    // -------------------------------------------------------------------------

    /** Wraps an external {@link IFluidHandler} as an Oritech {@link FluidApi.FluidStorage}. */
    public static class NeoforgeStorageWrapper extends FluidApi.FluidStorage {

        private final IFluidHandler storage;

        public NeoforgeStorageWrapper(IFluidHandler storage) {
            this.storage = storage;
        }

        @Override
        public long insert(FluidStack toInsert, boolean simulate) {
            var action = simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
            return storage.fill(FluidStackHooksForge.toForge(toInsert), action);
        }

        @Override
        public long extract(FluidStack toExtract, boolean simulate) {
            var action = simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
            return storage.drain(FluidStackHooksForge.toForge(toExtract), action).getAmount();
        }

        @Override
        public List<FluidStack> getContent() {
            var content = new ArrayList<FluidStack>();
            for (int i = 0; i < storage.getTanks(); i++) {
                content.add(FluidStackHooksForge.fromForge(storage.getFluidInTank(i)));
            }
            return content;
        }

        @Override
        public void update() {
        }

        @Override
        public long getCapacity() {
            Oritech.LOGGER.warn("tried to access capacity of external container");
            return 0L;
        }
    }

    /** Wraps an external {@link IFluidHandlerItem} with StackContext for sync. */
    public static class NeoforgeItemStorageWrapper extends NeoforgeStorageWrapper {

        private final StackContext stack;
        private final IFluidHandlerItem handler;

        public NeoforgeItemStorageWrapper(IFluidHandlerItem storage, StackContext stack) {
            super(storage);
            this.stack = stack;
            this.handler = storage;
        }

        @Override
        public void update() {
            super.update();
            stack.setValue(handler.getContainer());
            stack.sync();
        }
    }

    // -------------------------------------------------------------------------
    // Inner wrappers — Oritech → Forge (exposed to other mods)
    // -------------------------------------------------------------------------

    /** Wraps an Oritech {@link FluidApi.SingleSlotStorage} as a Forge {@link IFluidHandler}. */
    public static class SingleSlotContainerStorageWrapper implements IFluidHandler {

        public final FluidApi.SingleSlotStorage container;

        public static @Nullable SingleSlotContainerStorageWrapper of(@Nullable FluidApi.SingleSlotStorage container) {
            if (container == null) return null;
            return new SingleSlotContainerStorageWrapper(container);
        }

        public SingleSlotContainerStorageWrapper(FluidApi.SingleSlotStorage container) {
            this.container = container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            return FluidStackHooksForge.toForge(container.getStack());
        }

        @Override
        public int getTankCapacity(int tank) {
            return (int) Math.min(container.getCapacity(), Integer.MAX_VALUE);
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull net.minecraftforge.fluids.FluidStack fluidStack) {
            return true;
        }

        @Override
        public int fill(@NotNull net.minecraftforge.fluids.FluidStack fluidStack, @NotNull FluidAction action) {
            int result = (int) container.insert(FluidStackHooksForge.fromForge(fluidStack), action.simulate());
            if (result > 0 && action.execute()) container.update();
            return result;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(@NotNull net.minecraftforge.fluids.FluidStack fluidStack, @NotNull FluidAction action) {
            long extracted = container.extract(FluidStackHooksForge.fromForge(fluidStack), action.simulate());
            if (extracted > 0 && action.execute()) container.update();
            return new net.minecraftforge.fluids.FluidStack(fluidStack.getFluid(), (int) extracted);
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(int maxDrain, @NotNull FluidAction action) {
            var toDrain = container.getStack().copyWithAmount(maxDrain);
            long extracted = container.extract(toDrain, action.simulate());
            if (extracted > 0 && action.execute()) container.update();
            return new net.minecraftforge.fluids.FluidStack(container.getStack().getFluid(), (int) extracted);
        }
    }

    /** Wraps an Oritech {@link FluidApi.MultiSlotStorage} as a Forge {@link IFluidHandler}. */
    public static class MultiSlotStorageWrapper implements IFluidHandler {

        public final FluidApi.MultiSlotStorage container;

        public static @Nullable MultiSlotStorageWrapper of(@Nullable FluidApi.MultiSlotStorage container) {
            if (container == null) return null;
            return new MultiSlotStorageWrapper(container);
        }

        public MultiSlotStorageWrapper(FluidApi.MultiSlotStorage container) {
            this.container = container;
        }

        @Override
        public int getTanks() {
            return container.getSlotCount();
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            return FluidStackHooksForge.toForge(container.getStack(tank));
        }

        @Override
        public int getTankCapacity(int tank) {
            return (int) Math.min(container.getCapacity(), Integer.MAX_VALUE);
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull net.minecraftforge.fluids.FluidStack fluidStack) {
            return true;
        }

        @Override
        public int fill(@NotNull net.minecraftforge.fluids.FluidStack fluidStack, @NotNull FluidAction action) {
            int result = (int) container.insert(FluidStackHooksForge.fromForge(fluidStack), action.simulate());
            if (result > 0 && action.execute()) container.update();
            return result;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(@NotNull net.minecraftforge.fluids.FluidStack fluidStack, @NotNull FluidAction action) {
            long extracted = container.extract(FluidStackHooksForge.fromForge(fluidStack), action.simulate());
            if (extracted > 0 && action.execute()) container.update();
            return new net.minecraftforge.fluids.FluidStack(fluidStack.getFluid(), (int) extracted);
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(int maxDrain, @NotNull FluidAction action) {
            // Drain from slot 0 as a representative slot.
            var toDrain = container.getStack(0).copyWithAmount(maxDrain);
            long extracted = container.extract(toDrain, action.simulate());
            if (extracted > 0 && action.execute()) container.update();
            return new net.minecraftforge.fluids.FluidStack(container.getStack(0).getFluid(), (int) extracted);
        }
    }

    /** Wraps an Oritech {@link DelegatingFluidStorage} as a Forge {@link IFluidHandler}. */
    public static class DelegatingContainerStorageWrapper implements IFluidHandler {

        private final DelegatingFluidStorage container;

        public static @Nullable DelegatingContainerStorageWrapper of(@Nullable DelegatingFluidStorage container) {
            if (container == null) return null;
            return new DelegatingContainerStorageWrapper(container);
        }

        private DelegatingContainerStorageWrapper(DelegatingFluidStorage container) {
            this.container = container;
        }

        @Override
        public int getTanks() {
            var content = container.getContent();
            return content == null ? 0 : content.size();
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            var content = container.getContent();
            if (content == null || tank >= content.size()) return net.minecraftforge.fluids.FluidStack.EMPTY;
            return FluidStackHooksForge.toForge(content.get(tank));
        }

        @Override
        public int getTankCapacity(int tank) {
            return (int) Math.min(container.getCapacity(), Integer.MAX_VALUE);
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull net.minecraftforge.fluids.FluidStack fluidStack) {
            return true;
        }

        @Override
        public int fill(@NotNull net.minecraftforge.fluids.FluidStack fluidStack, @NotNull FluidAction action) {
            int result = (int) container.insert(FluidStackHooksForge.fromForge(fluidStack), action.simulate());
            if (result > 0 && action.execute()) container.update();
            return result;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(@NotNull net.minecraftforge.fluids.FluidStack fluidStack, @NotNull FluidAction action) {
            long extracted = container.extract(FluidStackHooksForge.fromForge(fluidStack), action.simulate());
            if (extracted > 0 && action.execute()) container.update();
            return new net.minecraftforge.fluids.FluidStack(fluidStack.getFluid(), (int) extracted);
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(int maxDrain, @NotNull FluidAction action) {
            var content = container.getContent();
            if (content == null || content.isEmpty()) return net.minecraftforge.fluids.FluidStack.EMPTY;
            var last = content.get(content.size() - 1);
            long extracted = container.extract(last.copyWithAmount(maxDrain), action.simulate());
            if (extracted > 0 && action.execute()) container.update();
            return new net.minecraftforge.fluids.FluidStack(last.getFluid(), (int) extracted);
        }
    }

    /** Wraps a single-slot Oritech container as a Forge {@link IFluidHandlerItem}. */
    public static class FluidContainerItemWrapper extends SingleSlotContainerStorageWrapper implements IFluidHandlerItem {

        private final ItemStack stack;

        public static @Nullable FluidContainerItemWrapper of(@Nullable FluidApi.SingleSlotStorage container, @Nullable ItemStack stack) {
            if (container == null || stack == null || stack.isEmpty()) return null;
            return new FluidContainerItemWrapper(container, stack);
        }

        public FluidContainerItemWrapper(FluidApi.SingleSlotStorage container, ItemStack stack) {
            super(container);
            this.stack = stack;
        }

        @Override
        public @NotNull ItemStack getContainer() {
            return stack;
        }
    }
}
