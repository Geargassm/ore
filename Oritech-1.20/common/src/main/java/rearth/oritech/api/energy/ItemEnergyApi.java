package rearth.oritech.api.energy;

import rearth.oritech.util.StackContext;

import java.util.function.Supplier;
import net.minecraft.world.item.Item;

public interface ItemEnergyApi {

    void registerForItem(Supplier<Item> itemSupplier);

    EnergyApi.EnergyStorage find(StackContext stack);

    // In 1.20.1 energy is stored in item NBT, not a data component.
    // Use SimpleEnergyItemStorage which reads/writes the "oritech_energy" NBT key.
}
