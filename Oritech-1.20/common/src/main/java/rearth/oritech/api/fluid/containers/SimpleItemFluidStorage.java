package rearth.oritech.api.fluid.containers;

import dev.architectury.fluid.FluidStack;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.init.ComponentContent;

import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;

public class SimpleItemFluidStorage extends SimpleFluidStorage {
    
    private final ItemStack itemStack;
    public Consumer<ItemStack> contextCallback;
    
    public SimpleItemFluidStorage(Long capacity, ItemStack itemStack) {
        super(capacity);
        this.itemStack = itemStack;
        var stored = ComponentContent.getFromNbt(itemStack, ComponentContent.storedFluidKey(), FluidStack.CODEC);
        this.setStack(stored != null ? stored : FluidStack.empty());
    }

    @Override
    public void update() {
        super.update();

        if (this.getStack().isEmpty()) {
            var tag = itemStack.getTag();
            if (tag != null) tag.remove(ComponentContent.storedFluidKey());
            return;
        }

        ComponentContent.setToNbt(itemStack, ComponentContent.storedFluidKey(), this.getStack(), FluidStack.CODEC);

        if (contextCallback != null) contextCallback.accept(itemStack);
    }
    
    public SimpleItemFluidStorage withCallback(Consumer<ItemStack> contextCallback) {
        this.contextCallback = contextCallback;
        return this;
    }
}
