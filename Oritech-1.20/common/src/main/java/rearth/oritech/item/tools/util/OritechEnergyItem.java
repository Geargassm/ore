package rearth.oritech.item.tools.util;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.SimpleEnergyItemStorage;

public interface OritechEnergyItem extends EnergyApi.ItemProvider {

    default long getEnergyCapacity(ItemStack stack) { return 10_000; }

    default long getEnergyMaxInput(ItemStack stack) { return 500; }

    default long getEnergyMaxOutput(ItemStack stack) { return 0; }

    default boolean tryUseEnergy(ItemStack stack, long amount, Player player) {
        RandomSource random = RandomSource.create();
        int unbreakingLevel = getUnbreakingLevel(stack);
        if (unbreakingLevel > 0) {
            amount = amount / (random.nextInt(unbreakingLevel) + 1);
        }
        var storage = getEnergyStorage(stack);
        if (storage instanceof SimpleEnergyItemStorage itemStorage) {
            var extracted = itemStorage.extractIgnoringLimit(amount, false);
            if (extracted > 0) {
                itemStorage.update();
            }
            return extracted == amount;
        }
        return false;
    }

    // In 1.20.1, use EnchantmentHelper instead of DataComponents
    private int getUnbreakingLevel(ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, stack);
    }

    default long getStoredEnergy(ItemStack stack) {
        return getEnergyStorage(stack).getAmount();
    }

    @Override
    default EnergyApi.EnergyStorage getEnergyStorage(ItemStack stack) {
        return new SimpleEnergyItemStorage(getEnergyMaxInput(stack), getEnergyMaxOutput(stack), getEnergyCapacity(stack), stack);
    }
}
