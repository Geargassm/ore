package rearth.oritech.block.entity.addons;

import net.minecraft.server.level.ServerLevel;
import rearth.oritech.OritechPlatform;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.MachineAddonController;
import rearth.oritech.util.MachineAddonProvider;

import java.util.HashSet;
import net.minecraft.core.BlockPos;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class AddonBlockEntity extends BlockEntity implements MachineAddonProvider {
    
    public static final HashSet<MachineAddonController> pendingInits = new HashSet<>();
    public static void completeInits() {
        for (var controller : pendingInits) {
            controller.initAddons();
        }
        
        pendingInits.clear();
    }
    
    private BlockPos controllerPos = BlockPos.ZERO;
    
    public AddonBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ADDON_ENTITY, pos, state);
    }
    
    public AddonBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    @Override
    public void setControllerPos(BlockPos pos) {
        if (!controllerPos.equals(pos) && level instanceof ServerLevel serverLevel)
            OritechPlatform.INSTANCE.resetCapabilities(serverLevel, pos);
        controllerPos = pos;
    }
    
    @Override
    public BlockPos getControllerPos() {
        return controllerPos;
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putInt("controller_x", controllerPos.getX());
        nbt.putInt("controller_y", controllerPos.getY());
        nbt.putInt("controller_z", controllerPos.getZ());
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt) {
        super.loadAdditional(nbt);
        controllerPos = new BlockPos(nbt.getInt("controller_x"), nbt.getInt("controller_y"), nbt.getInt("controller_z"));
    }
}
