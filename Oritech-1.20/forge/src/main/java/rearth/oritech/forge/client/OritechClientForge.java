package rearth.oritech.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;
import rearth.oritech.OritechClient;
import rearth.oritech.client.cablesurfer.ActiveCableRenderer;
import rearth.oritech.client.init.ModRenderers;
import rearth.oritech.client.other.OreFinderRenderer;
import rearth.oritech.client.renderers.BlockOutlineRenderer;
import rearth.oritech.client.renderers.PortalEntityRenderer;
import rearth.oritech.client.renderers.SmallTankItemRenderer;
import rearth.oritech.init.EntitiesContent;

/**
 * Client-side entry point for the Forge 1.20.1 module.
 *
 * <p>In Forge 1.20.1, custom renderer registration and entity layer setup uses
 * {@link EntityRenderersEvent.RegisterRenderers} and {@link EntityRenderersEvent.AddLayers}.
 *
 * <p>There is no {@code RegisterClientExtensionsEvent} in Forge 1.20.1; custom item renderers
 * are provided by overriding {@link net.minecraftforge.client.extensions.common.IClientItemExtensions#initializeClient}
 * on the item itself. Tank items use a wrapper class here registered via item override.
 */
@Mod.EventBusSubscriber(modid = Oritech.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class OritechClientForge {

    /**
     * Called during client-side mod setup.
     * Auto-subscribed because the outer class is annotated with
     * {@code @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)}.
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        OritechClient.initialize();
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        OritechClient.registerRenderers();
        event.registerEntityRenderer(EntitiesContent.PORTAL_ENTITY, PortalEntityRenderer::new);

        for (var entry : ModRenderers.RENDER_LAYERS.entrySet()) {
            ItemBlockRenderTypes.setRenderLayer(entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @SubscribeEvent
    public static void addElytraLayers(EntityRenderersEvent.AddLayers event) {
        // Add to all player skins
        for (var skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer pr) {
                pr.addLayer(new OritechElytraLayer<>(pr, event.getEntityModels()));
            }
        }
        // Add to all humanoid entities
        for (EntityType<?> entityType : event.getEntityTypes()) {
            if (event.getRenderer(entityType) instanceof HumanoidMobRenderer<?, ?> hmr) {
                hmr.addLayer(new OritechElytraLayer(hmr, event.getEntityModels()));
            }
        }
        // Add to armor stands
        if (event.getRenderer(EntityType.ARMOR_STAND) instanceof LivingEntityRenderer<?, ?> ler) {
            ler.addLayer(new OritechElytraLayer(ler, event.getEntityModels()));
        }
    }

    // -------------------------------------------------------------------------
    // Game-bus events (must be on MinecraftForge.EVENT_BUS)
    // -------------------------------------------------------------------------

    @Mod.EventBusSubscriber(modid = Oritech.MOD_ID, value = Dist.CLIENT)
    public static class GameBusEvents {

        @SubscribeEvent
        public static void onWorldRender(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                OreFinderRenderer.doRender(event.getPoseStack(), event.getCamera(), Minecraft.getInstance().renderBuffers().bufferSource());
                ActiveCableRenderer.render(event.getPoseStack(), Minecraft.getInstance().renderBuffers().bufferSource());
            }
        }

        @SubscribeEvent
        public static void onOutlineRender(RenderHighlightEvent.Block event) {
            BlockOutlineRenderer.render(Minecraft.getInstance().level, event.getCamera(), event.getPoseStack(), event.getMultiBufferSource());
        }

        // Mouse click handling is registered via Architectury ClientRawInputEvent.MOUSE_CLICKED_PRE
        // inside OritechClient.initialize(), so we do not duplicate it here.
    }

    // -------------------------------------------------------------------------
    // Custom tank item renderer helpers
    // -------------------------------------------------------------------------

    /**
     * Provides the BEWLR for tank items in Forge 1.20.1.
     * The tank items' {@code initializeClient} override should call
     * {@code consumer.accept(new TankItemExtensions(modelId))}.
     */
    public static class TankItemRenderer extends BlockEntityWithoutLevelRenderer {

        private final SmallTankItemRenderer itemRenderer;

        public TankItemRenderer(ResourceLocation modelId) {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
            this.itemRenderer = new SmallTankItemRenderer(modelId);
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext mode, PoseStack matrices,
                                 MultiBufferSource vertexConsumers, int light, int overlay) {
            itemRenderer.render(stack, mode, matrices, vertexConsumers, light, overlay);
        }
    }

    /**
     * {@link IClientItemExtensions} implementation for tank items.
     *
     * <p>Register this in the item's {@code initializeClient(Consumer<IClientItemExtensions> consumer)}
     * override on {@link BlockContent#SMALL_TANK_ITEM} and {@link BlockContent#CREATIVE_TANK_ITEM}.
     *
     * <p>Example in item class:
     * <pre>{@code
     * @Override
     * public void initializeClient(Consumer<IClientItemExtensions> consumer) {
     *     consumer.accept(new OritechClientForge.TankItemExtensions(Oritech.id("tank_item_model")));
     * }
     * }</pre>
     */
    public static class TankItemExtensions implements IClientItemExtensions {
        private final TankItemRenderer renderer;

        public TankItemExtensions(ResourceLocation modelId) {
            this.renderer = new TankItemRenderer(modelId);
        }

        @Override
        public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return renderer;
        }
    }
}
