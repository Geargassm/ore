package rearth.oritech.init;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Replacement for the 1.20.5+ DataComponentType system.
 * In 1.20.1, item state is stored in ItemStack NBT tags.
 */
public class ComponentContent {

    private static final String IS_AOE_ACTIVE_KEY = "oritech.is_aoe_active";
    private static final String TARGET_POSITION_KEY = "oritech.target_position";
    private static final String STORED_FLUID_KEY = "oritech.stored_fluid";
    private static final String ADDON_DATA_KEY = "oritech.addon_data";

    // --- IS_AOE_ACTIVE ---

    public static boolean getIsAoeActive(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(IS_AOE_ACTIVE_KEY);
    }

    public static void setIsAoeActive(ItemStack stack, boolean value) {
        stack.getOrCreateTag().putBoolean(IS_AOE_ACTIVE_KEY, value);
    }

    public static boolean hasIsAoeActive(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(IS_AOE_ACTIVE_KEY);
    }

    // --- TARGET_POSITION ---

    @Nullable
    public static BlockPos getTargetPosition(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TARGET_POSITION_KEY)) return null;
        long packed = tag.getLong(TARGET_POSITION_KEY);
        return BlockPos.of(packed);
    }

    public static void setTargetPosition(ItemStack stack, @Nullable BlockPos pos) {
        if (pos == null) {
            CompoundTag tag = stack.getTag();
            if (tag != null) tag.remove(TARGET_POSITION_KEY);
        } else {
            stack.getOrCreateTag().putLong(TARGET_POSITION_KEY, pos.asLong());
        }
    }

    public static boolean hasTargetPosition(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TARGET_POSITION_KEY);
    }

    // --- STORED_FLUID (stored as CompoundTag via Codec) ---

    @Nullable
    public static <T> T getFromNbt(ItemStack stack, String key, Codec<T> codec) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(key)) return null;
        return codec.parse(NbtOps.INSTANCE, tag.get(key)).result().orElse(null);
    }

    public static <T> void setToNbt(ItemStack stack, String key, T value, Codec<T> codec) {
        var result = codec.encodeStart(NbtOps.INSTANCE, value).result();
        result.ifPresent(nbt -> stack.getOrCreateTag().put(key, nbt));
    }

    public static boolean hasNbtKey(ItemStack stack, String key) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(key);
    }

    public static String storedFluidKey() { return STORED_FLUID_KEY; }
    public static String addonDataKey()   { return ADDON_DATA_KEY; }

    private ComponentContent() {}
}
