package rearth.oritech.forge;

import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.client.init.OritechClientConfig;
import rearth.oritech.init.OritechConfig;
import rearth.oritech.init.OritechStartupConfig;
import rearth.oritech.item.tools.util.ArmorEventHandler;

/**
 * Main Forge 1.20.1 mod entry point for Oritech.
 *
 * <p>Key differences from the NeoForge 1.21.1 version:
 * <ul>
 *   <li>No {@code IEventBus} / {@code ModContainer} constructor parameters — use
 *       {@link FMLJavaModLoadingContext#get()} to access the mod event bus.</li>
 *   <li>No {@code DeferredRegister.createDataComponents} — energy component not available
 *       in 1.20.1; energy stored in item NBT via {@code SimpleEnergyItemStorage}.</li>
 *   <li>No {@code RegisterCapabilitiesEvent} — capabilities are registered via
 *       {@link AttachCapabilitiesEvent} on the game event bus.</li>
 *   <li>No {@code RegisterPayloadHandlersEvent} / {@code PayloadRegistrar} — networking
 *       uses Architectury 9.x {@code dev.architectury.networking.NetworkManager}.</li>
 *   <li>No NeoForge fluid type registry — fluid attributes registered in fluid properties.</li>
 * </ul>
 */
@Mod(Oritech.MOD_ID)
public final class OritechModForge {

    public OritechModForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.register(new ModBusEventHandler());

        // Register config specs
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, OritechConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, OritechClientConfig.CLIENT_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.STARTUP, OritechStartupConfig.STARTUP_SPEC);

        // Set up fluid stack codec using Architectury hooks.
        // OPTIONAL_CODEC handles empty FluidStack gracefully (equivalent to NeoForge's OPTIONAL_CODEC).
        NetworkManager.FLUID_STACK_CODEC = net.minecraftforge.fluids.FluidStack.OPTIONAL_CODEC
            .xmap(FluidStackHooksForge::fromForge, FluidStackHooksForge::toForge);
        // Forge 1.20.1 FluidStack has no built-in stream codec, so we create a simple one
        // using FluidStack.writeToPacket / readFromPacket (Forge's network serialization methods).
        NetworkManager.FLUID_STACK_STREAM_CODEC = rearth.oritech.compat.StreamCodec.of(
            (buf, stack) -> FluidStackHooksForge.toForge(stack).writeToPacket(buf),
            buf -> FluidStackHooksForge.fromForge(net.minecraftforge.fluids.FluidStack.readFromPacket(buf))
        );

        Oritech.initialize();
    }

    // -------------------------------------------------------------------------
    // Game event bus — static inner class with @EventBusSubscriber handles registration
    // -------------------------------------------------------------------------

    /**
     * Events fired on the Forge game event bus ({@code MinecraftForge.EVENT_BUS}).
     *
     * <p>{@link AttachCapabilitiesEvent} fires on the game bus, not the mod bus,
     * so capability attachment is handled here along with entity events.
     */
    @Mod.EventBusSubscriber(modid = Oritech.MOD_ID)
    public static class ForgeGameBusEvents {

        @SubscribeEvent
        public static void onEquipmentChanged(LivingEquipmentChangeEvent event) {
            ArmorEventHandler.processEvent(event.getEntity(), event.getSlot(), event.getFrom(), event.getTo());
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onPlayerClone(PlayerEvent.Clone event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                PlayerAugments.refreshActiveAugments(player);
            }
        }

        /**
         * Attaches capabilities to block entities.
         *
         * <p>This replaces {@code RegisterCapabilitiesEvent} from NeoForge 1.21.
         * {@link AttachCapabilitiesEvent} fires on the game bus each time a block entity is loaded,
         * and each impl checks whether the entity matches a registered type before attaching.
         */
        @SubscribeEvent
        public static void onAttachBlockEntityCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
            if (ItemApi.BLOCK instanceof ForgeItemApiImpl forgeApi)
                forgeApi.onAttachCapabilities(event);
            if (FluidApi.BLOCK instanceof ForgeFluidApiImpl forgeApi)
                forgeApi.onAttachBlockEntityCapabilities(event);
            if (EnergyApi.BLOCK instanceof ForgeEnergyApiImpl forgeApi)
                forgeApi.onAttachBlockEntityCapabilities(event);
        }

        /**
         * Attaches capabilities to item stacks (energy-storing items, fluid containers).
         */
        @SubscribeEvent
        public static void onAttachItemStackCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
            if (FluidApi.ITEM instanceof ForgeFluidApiImpl forgeApi)
                forgeApi.onAttachItemStackCapabilities(event);
            if (EnergyApi.ITEM instanceof ForgeEnergyApiImpl forgeApi)
                forgeApi.onAttachItemStackCapabilities(event);
        }
    }

    // -------------------------------------------------------------------------
    // Mod event bus handlers (registration-related)
    // -------------------------------------------------------------------------

    /**
     * Events fired on the mod event bus (registered manually in constructor).
     */
    class ModBusEventHandler {

        @SubscribeEvent
        public void onRegister(RegisterEvent event) {
            var id = event.getRegistryKey().location();

            if (Oritech.EVENT_MAP.containsKey(id)) {
                Oritech.LOGGER.debug(event.getRegistryKey().toString());
                Oritech.EVENT_MAP.get(id).forEach(Runnable::run);
            }

            // NOTE: In Forge 1.20.1 fluid attributes are part of ForgeFlowingFluid.Properties —
            // there is no separate "fluid types" registry event to handle here.
        }
    }
}
