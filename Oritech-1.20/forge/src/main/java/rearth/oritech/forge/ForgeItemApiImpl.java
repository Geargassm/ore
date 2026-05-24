package rearth.oritech.forge;

import com.google.auto.service.AutoService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.item.BlockItemApi;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.lookup.BlockLookupCache;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@AutoService({BlockItemApi.class})
public class ForgeItemApiImpl implements BlockItemApi {

    private final List<Supplier<BlockEntityType<?>>> registeredBlockEntities = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    @Override
    public void registerBlockEntity(Supplier<BlockEntityType<?>> typeSupplier) {
        registeredBlockEntities.add(typeSupplier);
    }

    /**
     * Called from {@link OritechModForge.ModBusEventHandler#onAttachBlockEntityCapabilities}.
     */
    public void onAttachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        var entity = event.getObject();
        for (var supplied : registeredBlockEntities) {
            if (supplied.get().isInstance(entity)) {
                if (!(entity instanceof ItemApi.BlockProvider)) continue;
                var provider = (ItemApi.BlockProvider) entity;
                event.addCapability(
                    new ResourceLocation("oritech", "item_handler"),
                    new ICapabilityProvider() {
                        @Override
                        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                            if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
                                var storage = provider.getInventoryStorage(side);
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
    // BlockItemApi — find / createCache
    // -------------------------------------------------------------------------

    @Override
    public ItemApi.InventoryStorage find(Level world, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity entity,
                                         @Nullable Direction direction) {
        BlockEntity be = entity != null ? entity : world.getBlockEntity(pos);
        if (be == null) return null;
        LazyOptional<IItemHandler> opt = be.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction);
        return opt.map(handler -> {
            if (handler instanceof ContainerStorageWrapper wrapper) return wrapper.container;
            return (ItemApi.InventoryStorage) new ForgeStorageWrapper(handler);
        }).orElse(null);
    }

    @Override
    public ItemApi.InventoryStorage find(Level world, BlockPos pos, @Nullable Direction direction) {
        return find(world, pos, null, null, direction);
    }

    @Override
    public BlockLookupCache<ItemApi.InventoryStorage> createCache(Level world, BlockPos pos, @Nullable Direction direction) {
        return BlockLookupCache.of(() -> find(world, pos, direction));
    }

    // -------------------------------------------------------------------------
    // Inner wrappers
    // -------------------------------------------------------------------------

    /**
     * Wraps an external (other-mod) {@link IItemHandler} as an Oritech {@link ItemApi.InventoryStorage}.
     */
    public static class ForgeStorageWrapper implements ItemApi.InventoryStorage {

        private final IItemHandler container;

        public ForgeStorageWrapper(IItemHandler container) {
            this.container = container;
        }

        @Override
        public int insert(ItemStack inserted, boolean simulate) {
            return inserted.getCount() - ItemHandlerHelper.insertItem(container, inserted, simulate).getCount();
        }

        @Override
        public int insertToSlot(ItemStack inserted, int slot, boolean simulate) {
            return inserted.getCount() - container.insertItem(slot, inserted, simulate).getCount();
        }

        @Override
        public int extract(ItemStack extracted, boolean simulate) {
            int total = 0;
            for (int i = 0; i < container.getSlots(); i++) {
                var available = container.getStackInSlot(i);
                if (ItemStack.isSameItemSameTags(available, extracted)) {
                    total += container.extractItem(i, extracted.getCount() - total, simulate).getCount();
                    if (total >= extracted.getCount()) break;
                }
            }
            return total;
        }

        @Override
        public int extractFromSlot(ItemStack extracted, int slot, boolean simulate) {
            return container.extractItem(slot, extracted.getCount(), simulate).getCount();
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (container instanceof IItemHandlerModifiable handler) {
                handler.setStackInSlot(slot, stack);
            } else {
                Oritech.LOGGER.error("Unable to set stack in slot: {}, stack is: {}", slot, stack);
            }
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return container.getStackInSlot(slot);
        }

        @Override
        public int getSlotCount() {
            return container.getSlots();
        }

        @Override
        public int getSlotLimit(int slot) {
            return container.getSlotLimit(slot);
        }

        @Override
        public void update() {
            // no-op for external storages
        }
    }

    /**
     * Wraps an Oritech {@link ItemApi.InventoryStorage} as a Forge {@link IItemHandlerModifiable}
     * so other mods can interact with Oritech block inventories.
     */
    public static class ContainerStorageWrapper implements IItemHandlerModifiable {

        public final ItemApi.InventoryStorage container;

        public static @Nullable ContainerStorageWrapper of(@Nullable ItemApi.InventoryStorage storage) {
            if (storage == null) return null;
            return new ContainerStorageWrapper(storage);
        }

        public ContainerStorageWrapper(ItemApi.InventoryStorage container) {
            this.container = container;
        }

        @Override
        public int getSlots() {
            return container.getSlotCount();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return container.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            int inserted = container.insertToSlot(stack, slot, simulate);
            if (inserted > 0 && !simulate) container.update();
            return stack.copyWithCount(stack.getCount() - inserted);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            var taken = container.getStackInSlot(slot).copyWithCount(amount);
            int extracted = container.extractFromSlot(taken, slot, simulate);
            if (extracted > 0 && !simulate) container.update();
            return taken.copyWithCount(extracted);
        }

        @Override
        public int getSlotLimit(int slot) {
            return container.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return container.insertToSlot(stack, slot, true) > 0;
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            container.setStackInSlot(slot, stack);
            container.update();
        }
    }
}
