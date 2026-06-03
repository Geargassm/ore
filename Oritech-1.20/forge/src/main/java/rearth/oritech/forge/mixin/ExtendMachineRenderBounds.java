package rearth.oritech.forge.mixin;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import rearth.oritech.client.renderers.AcceleratorControllerRenderer;
import rearth.oritech.client.renderers.LaserArmRenderer;
import rearth.oritech.client.renderers.MachineGantryRenderer;
import rearth.oritech.client.renderers.PowerPoleCableRenderer;
import rearth.oritech.client.renderers.ShrinkerBlockRenderer;

/**
 * Extends the render bounding box of several renderers to cover a very large area
 * so that they are never culled when their actual rendererd geometry extends far from
 * the block entity position.
 *
 * <p>In Forge 1.20.1, {@code BlockEntityRenderer} defines a {@code getRenderBoundingBox(T)}
 * method that we can override via Mixin. We cannot use {@code AABB.INFINITE} because that
 * constant is a NeoForge-only patch, so we provide a practically-infinite AABB instead.
 *
 * <p>Unlike the NeoForge version which uses {@code IBlockEntityRendererExtension}, in Forge
 * 1.20.1 we simply {@link Overwrite} the default method directly on the renderer classes.
 */
@Mixin({AcceleratorControllerRenderer.class, LaserArmRenderer.class, MachineGantryRenderer.class,
        PowerPoleCableRenderer.class, ShrinkerBlockRenderer.class})
public abstract class ExtendMachineRenderBounds<T extends BlockEntity> {

    /**
     * Returns a very large AABB to prevent these renderers from being culled by the frustum.
     * The exact same AABB instance is returned regardless of which block entity is queried.
     */
    @Overwrite
    public AABB getRenderBoundingBox(T blockEntity) {
        // Use an effectively-infinite AABB as a substitute for AABB.INFINITE (NeoForge-only).
        return new AABB(
            -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE,
             Double.MAX_VALUE,  Double.MAX_VALUE,  Double.MAX_VALUE
        );
    }
}
