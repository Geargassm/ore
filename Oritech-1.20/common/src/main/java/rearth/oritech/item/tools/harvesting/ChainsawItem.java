package rearth.oritech.item.tools.harvesting;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.entity.interaction.TreefellerBlockEntity;
import rearth.oritech.init.OritechConfig;
import rearth.oritech.init.OritechStartupConfig;
import rearth.oritech.item.tools.util.OritechEnergyItem;

import java.util.List;

public class ChainsawItem extends AxeItem implements OritechEnergyItem {
    
    public static final int BAR_STEP_COUNT = 13;
    
    public ChainsawItem(Tier toolMaterial, Item.Properties settings) {
        super(toolMaterial, 5f, -2.4f, settings);
        // In 1.20.1, AxeItem already handles axe-like breaking via its built-in Item subclass logic.
        // The Tool/Rule/DataComponents system doesn't exist in 1.20.1.
    }
    
    // this overrides the fabric specific extensions
    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }
    
    public boolean allowContinuingBlockBreaking(Player player, ItemStack oldStack, ItemStack newStack) {
        return true;
    }
    
    // this overrides the neoforge specific extensions
    public boolean shouldCauseReequipAnimation(@NotNull ItemStack oldStack, @NotNull ItemStack newStack, boolean slotChanged) {
        return false;
    }
    
    public boolean shouldCauseBlockBreakReset(@NotNull ItemStack oldStack, @NotNull ItemStack newStack) {
        return false;
    }
    
    private long getEnergyUsageMultiplier() {
        return OritechStartupConfig.chainSaw.energyUsage.get();
    }
    
    @Override
    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity miner) {
        
        if (!(miner instanceof Player player)) return true;
        
        var amount = state.getBlock().defaultDestroyTime() * getEnergyUsageMultiplier();
        amount = Math.min(amount, this.getStoredEnergy(stack));
        
        var energySuccess = this.tryUseEnergy(stack, (long) amount, player);
        
        if (!world.isClientSide && miner.isShiftKeyDown() && energySuccess && OritechConfig.chainsawTreeCutting.get()) {
            var startPos = pos.above();
            var startState = world.getBlockState(startPos);
            if (startState.is(BlockTags.LOGS)) {
                var treeBlocks = TreefellerBlockEntity.getTreeBlocks(startPos, world);
                PromethiumAxeItem.pendingBlocks.addAll(treeBlocks.stream().map(elem -> new PromethiumAxeItem.PendingBlock(world, elem, stack)).toList());
                
                var extraEnergyUsed = treeBlocks.size() * getEnergyUsageMultiplier() / 2;
                this.tryUseEnergy(stack, (long) extraEnergyUsed, player);
            }
        }
        
        return energySuccess;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag type) {
        var text = Component.translatable("tooltip.oritech.energy_indicator", this.getStoredEnergy(stack), this.getEnergyCapacity(stack));
        tooltip.add(text.withStyle(ChatFormatting.GOLD));
        
        if (OritechConfig.chainsawTreeCutting.get())
            tooltip.add(Component.translatable("tooltip.oritech.promethium_axe").withStyle(ChatFormatting.DARK_GRAY));
    }
    
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        var enoughEnergy = getStoredEnergy(stack) >= state.getBlock().defaultDestroyTime() * getEnergyUsageMultiplier();
        var multiplier = enoughEnergy ? 1 : 0.1f;
        return super.getDestroySpeed(stack, state) * multiplier;
    }
    
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        return false;
    }
    
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }
    
    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round((getStoredEnergy(stack) * 100f / this.getEnergyCapacity(stack)) * BAR_STEP_COUNT) / 100;
    }
    
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }
    
    @Override
    public int getBarColor(ItemStack stack) {
        return 0xff7007;
    }
    
    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return OritechStartupConfig.chainSaw.energyCapacity.get();
    }
    
    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return OritechStartupConfig.chainSaw.chargeSpeed.get();
    }
}
