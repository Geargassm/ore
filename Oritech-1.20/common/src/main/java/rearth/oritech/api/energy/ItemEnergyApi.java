package rearth.oritech.api.energy;

import rearth.oritech.util.StackContext;

import java.util.function.Supplier;
// TODO_1_20: DataComponents not available in 1.20.1 - needs NBT conversion
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;

public interface ItemEnergyApi {
    
    void registerForItem(Supplier<Item> itemSupplier);
    
    EnergyApi.EnergyStorage find(StackContext stack);
    
// TODO_1_20: DataComponents not available in 1.20.1 - needs NBT conversion
    DataComponentType<Long> getEnergyComponent();
    
}
