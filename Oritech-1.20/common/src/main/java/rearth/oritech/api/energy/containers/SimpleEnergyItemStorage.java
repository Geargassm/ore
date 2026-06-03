package rearth.oritech.api.energy.containers;

import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;

public class SimpleEnergyItemStorage extends SimpleEnergyStorage {

    private static final String ENERGY_NBT_KEY = "oritech_energy";

    private final ItemStack stack;
    public Consumer<ItemStack> contextCallback;

    public SimpleEnergyItemStorage(long maxInsert, long maxExtract, long capacity, ItemStack stack) {
        super(maxInsert, maxExtract, capacity);
        this.stack = stack;
        var tag = stack.getTag();
        this.setAmount(tag != null ? tag.getLong(ENERGY_NBT_KEY) : 0L);
    }

    @Override
    public void update() {
        super.update();
        stack.getOrCreateTag().putLong(ENERGY_NBT_KEY, getAmount());
        if (contextCallback != null) contextCallback.accept(stack);
    }
    
    public SimpleEnergyItemStorage withCallback(Consumer<ItemStack> contextCallback) {
        this.contextCallback = contextCallback;
        return this;
    }
}
