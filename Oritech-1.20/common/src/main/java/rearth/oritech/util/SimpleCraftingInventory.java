package rearth.oritech.util;

import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SimpleCraftingInventory implements net.minecraft.world.Container {

    private final int size;
    private final NonNullList<ItemStack> items;

    public SimpleCraftingInventory(ItemStack ... items) {
        this.size = items.length;
        this.items = NonNullList.of(ItemStack.EMPTY, items);
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return slot >= 0 && slot < items.size() ? ContainerHelper.removeItem(items, slot, amount) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot >= 0 && slot < items.size()) {
            ItemStack stack = items.get(slot);
            items.set(slot, ItemStack.EMPTY);
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < items.size()) items.set(slot, stack);
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public void clearContent() { items.replaceAll(e -> ItemStack.EMPTY); }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }
}
