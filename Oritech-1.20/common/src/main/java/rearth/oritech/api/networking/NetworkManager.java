package rearth.oritech.api.networking;
import rearth.oritech.api.networking.PacketId;

import com.mojang.serialization.Codec;
import dev.architectury.fluid.FluidStack;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import rearth.oritech.Oritech;
import rearth.oritech.OritechPlatform;
import rearth.oritech.block.base.entity.ExpandableEnergyStorageBlockEntity;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.entity.accelerator.AcceleratorControllerBlockEntity;
import rearth.oritech.block.entity.addons.InventoryProxyAddonBlockEntity;
import rearth.oritech.block.entity.addons.RedstoneAddonBlockEntity;
import rearth.oritech.block.entity.arcane.EnchanterBlockEntity;
import rearth.oritech.block.entity.arcane.EnchantmentCatalystBlockEntity;
import rearth.oritech.block.entity.arcane.SpawnerControllerBlockEntity;
import rearth.oritech.block.entity.augmenter.AugmentApplicationEntity;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.block.entity.interaction.LaserArmBlockEntity;
import rearth.oritech.block.entity.interaction.ShrinkerBlockEntity;
import rearth.oritech.block.entity.pipes.ItemFilterBlockEntity;
import rearth.oritech.block.entity.pipes.ItemPipeInterfaceEntity;
import rearth.oritech.block.entity.processing.TaintedRefineryBlockEntity;
import rearth.oritech.client.ui.OritechScreenHandler;
import rearth.oritech.compat.ByteBufCodecs;
import rearth.oritech.compat.StreamCodec;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.item.tools.PortableLaserItem;
import rearth.oritech.item.tools.armor.JetpackItem;
import rearth.oritech.util.ServerZiplineHandler;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;

public class NetworkManager {

    private static final Map<Type, StreamCodec<? extends ByteBuf, ?>> AUTO_CODECS = new HashMap<>();
    private static final Map<Integer, List<Field>> CACHED_FIELDS = new HashMap<>();

    // Fluid stack codecs — set by loader module during init
    public static Codec<FluidStack> FLUID_STACK_CODEC;
    public static StreamCodec<FriendlyByteBuf, FluidStack> FLUID_STACK_STREAM_CODEC;

    // --- Sending helpers ---

    public static void sendBlockHandle(BlockEntity blockEntity, MessagePayload message) {
        OritechPlatform.INSTANCE.sendBlockHandle(blockEntity, MessagePayload.TYPE,
            buf -> MessagePayload.CODEC.encode(buf, message));
    }

    public static void sendPlayerHandle(MessagePayload message, ServerPlayer player) {
        OritechPlatform.INSTANCE.sendPlayerHandle(player, MessagePayload.TYPE,
            buf -> MessagePayload.CODEC.encode(buf, message));
    }

    public static void sendToServer(PacketId<?> packetId, Consumer<FriendlyByteBuf> writer) {
        OritechPlatform.INSTANCE.sendToServer(packetId, writer);
    }

    public static void sendNearby(ServerLevel level, Vec3 pos, double radius, MessagePayload message) {
        double rSq = radius * radius;
        for (var player : level.players()) {
            if (player.distanceToSqr(pos.x, pos.y, pos.z) < rSq) {
                sendPlayerHandle(message, player);
            }
        }
    }

    public static <T> void registerToClient(PacketId<T> id, StreamCodec<FriendlyByteBuf, T> codec, TriConsumer<T, Level, Player> consumer) {
        OritechPlatform.INSTANCE.registerToClient(id, codec, consumer);
    }

    public static <T> void registerToServer(PacketId<T> id, StreamCodec<FriendlyByteBuf, T> codec, TriConsumer<T, Player, Level> consumer) {
        OritechPlatform.INSTANCE.registerToServer(id, codec, consumer);
    }

    @SuppressWarnings("unchecked")
    public static <T> StreamCodec<FriendlyByteBuf, T> autoCodec(Class<T> type) {
        return (StreamCodec<FriendlyByteBuf, T>) getAutoCodec(type);
    }

    public static void registerDefaultCodecs() {
        registerCodec(ByteBufCodecs.INT, Integer.class, int.class);
        registerCodec(ByteBufCodecs.VAR_LONG, Long.class, long.class);
        registerCodec(ByteBufCodecs.FLOAT, Float.class, float.class);
        registerCodec(ByteBufCodecs.BOOL, Boolean.class, boolean.class);
        registerCodec(ByteBufCodecs.DOUBLE, Double.class, double.class);
        registerCodec(ByteBufCodecs.BYTE, Byte.class, byte.class);
        registerCodec(ByteBufCodecs.SHORT, Short.class, short.class);
        registerCodec(ByteBufCodecs.STRING_UTF8, String.class);
        registerCodec(ByteBufCodecs.RESOURCE_LOCATION, ResourceLocation.class);
        registerCodec(ByteBufCodecs.BLOCK_POS, BlockPos.class);
        registerCodec(ByteBufCodecs.ITEM_STACK, ItemStack.class);
        registerCodec(VEC2I_PACKED_CODEC, Vector2i.class);
        registerCodec(VEC3D_PACKET_CODEC, Vec3.class);
        registerCodec(SIMPLE_BLOCK_STATE_PACKET_CODEC, BlockState.class);
        registerCodec(FLUID_STACK_STREAM_CODEC, FluidStack.class);
        registerCodec(ByteBufCodecs.COMPOUND_TAG, CompoundTag.class);
        registerCodec(ItemFilterBlockEntity.FilterData.PACKET_CODEC, ItemFilterBlockEntity.FilterData.class);
        registerCodec(OritechRecipeType.PACKET_CODEC, OritechRecipe.class);
        registerCodec(LaserArmBlockEntity.LASER_TARGET_PACKET_CODEC, LivingEntity.class);
        registerCodec(AugmentApplicationEntity.ResearchState.PACKET_CODEC, AugmentApplicationEntity.ResearchState.class);
    }

    public static <T> void registerCodec(StreamCodec<? extends ByteBuf, T> codec, Type... classes) {
        for (var clazz : classes)
            AUTO_CODECS.put(clazz, codec);
    }

    @SuppressWarnings("unchecked")
    public static void init() {
        registerDefaultCodecs();

        registerToServer(ItemFilterBlockEntity.ItemFilterPayload.FILTER_PACKET_ID, ItemFilterBlockEntity.ItemFilterPayload.PACKET_CODEC, (p, player, level) -> ItemFilterBlockEntity.handleClientUpdate(p, player, level));
        registerToServer(EnchanterBlockEntity.SelectEnchantingPacket.PACKET_ID, autoCodec(EnchanterBlockEntity.SelectEnchantingPacket.class), (p, player, level) -> EnchanterBlockEntity.receiveEnchantmentSelection(p, player, level));
        registerToServer(RedstoneAddonBlockEntity.RedstoneAddonServerUpdate.PACKET_ID, autoCodec(RedstoneAddonBlockEntity.RedstoneAddonServerUpdate.class), (p, player, level) -> RedstoneAddonBlockEntity.receiveOnServer(p, player, level));
        registerToServer(PortableLaserItem.LaserPlayerUsePacket.PACKET_ID, autoCodec(PortableLaserItem.LaserPlayerUsePacket.class), (p, player, level) -> PortableLaserItem.receiveUsePacket(p, player, level));
        registerToServer(ServerZiplineHandler.ZiplinePlayerUsePacket.PACKET_ID, autoCodec(ServerZiplineHandler.ZiplinePlayerUsePacket.class), (p, player, level) -> ServerZiplineHandler.onZipLineTickUseEvent(p, player, level));
        registerToServer(MachineBlockEntity.InventoryInputModeSelectorPacket.PACKET_ID, autoCodec(MachineBlockEntity.InventoryInputModeSelectorPacket.class), (p, player, level) -> MachineBlockEntity.receiveCycleModePacket(p, player, level));
        registerToServer(InventoryProxyAddonBlockEntity.InventoryProxySlotSelectorPacket.PACKET_ID, autoCodec(InventoryProxyAddonBlockEntity.InventoryProxySlotSelectorPacket.class), (p, player, level) -> InventoryProxyAddonBlockEntity.receiveSlotSelection(p, player, level));
        registerToServer(JetpackItem.JetpackUsageUpdatePacket.PACKET_ID, autoCodec(JetpackItem.JetpackUsageUpdatePacket.class), (p, player, level) -> JetpackItem.receiveUsagePacket(p, player, level));
        registerToServer(PlayerAugments.AugmentInstallTriggerPacket.PACKET_ID, autoCodec(PlayerAugments.AugmentInstallTriggerPacket.class), (p, player, level) -> PlayerAugments.receiveInstallTrigger(p, player, level));
        registerToServer(PlayerAugments.LoadPlayerAugmentsToMachinePacket.PACKET_ID, autoCodec(PlayerAugments.LoadPlayerAugmentsToMachinePacket.class), (p, player, level) -> PlayerAugments.receivePlayerLoadMachine(p, player, level));
        registerToServer(PlayerAugments.OpenAugmentScreenPacket.PACKET_ID, autoCodec(PlayerAugments.OpenAugmentScreenPacket.class), (p, player, level) -> PlayerAugments.receiveOpenAugmentScreen(p, player, level));
        registerToServer(PlayerAugments.AugmentPlayerTogglePacket.PACKET_ID, autoCodec(PlayerAugments.AugmentPlayerTogglePacket.class), (p, player, level) -> PlayerAugments.receiveToggleAugment(p, player, level));
        registerToServer(ShrinkerBlockEntity.ShrinkerPlayerUsePacket.PACKET_ID, autoCodec(ShrinkerBlockEntity.ShrinkerPlayerUsePacket.class), (p, player, level) -> ShrinkerBlockEntity.onPlayerUse(p, player, level));
        registerToServer(OritechScreenHandler.FluidContainerInteractionPacket.PACKET_ID, autoCodec(OritechScreenHandler.FluidContainerInteractionPacket.class), (p, player, level) -> OritechScreenHandler.handleFluidContainerInteraction(p, player, level));
        registerToServer(TaintedRefineryBlockEntity.RefineryTankSelectorPacket.PACKET_ID, autoCodec(TaintedRefineryBlockEntity.RefineryTankSelectorPacket.class), (p, player, level) -> TaintedRefineryBlockEntity.handleTankPacket(p, player, level));
        registerToServer(ExpandableEnergyStorageBlockEntity.StorageLimitPacket.PACKET_ID, autoCodec(ExpandableEnergyStorageBlockEntity.StorageLimitPacket.class), (p, player, level) -> ExpandableEnergyStorageBlockEntity.handleLimitPacket(p, player, level));

        registerToClient(MessagePayload.TYPE, MessagePayload.CODEC, (msg, level, player) -> receiveMessage(msg, level));
        registerToClient(ItemPipeInterfaceEntity.RenderStackData.PIPE_ITEMS_ID, autoCodec(ItemPipeInterfaceEntity.RenderStackData.class), (p, level, player) -> ItemPipeInterfaceEntity.receiveVisualItemsPacket(p, level, player));
        registerToClient(EnchantmentCatalystBlockEntity.CatalystSyncPacket.PACKET_ID, autoCodec(EnchantmentCatalystBlockEntity.CatalystSyncPacket.class), (p, level, player) -> EnchantmentCatalystBlockEntity.receiveUpdatePacket(p, level, player));
        registerToClient(SpawnerControllerBlockEntity.SpawnerSyncPacket.PACKET_ID, autoCodec(SpawnerControllerBlockEntity.SpawnerSyncPacket.class), (p, level, player) -> SpawnerControllerBlockEntity.receiveUpdatePacket(p, level, player));
        registerToClient(RedstoneAddonBlockEntity.RedstoneAddonClientUpdate.PACKET_ID, autoCodec(RedstoneAddonBlockEntity.RedstoneAddonClientUpdate.class), (p, level, player) -> RedstoneAddonBlockEntity.receiveOnClient(p, level, player));
        registerToClient(AcceleratorControllerBlockEntity.ParticleRenderTrail.PACKET_ID, autoCodec(AcceleratorControllerBlockEntity.ParticleRenderTrail.class), (p, level, player) -> AcceleratorControllerBlockEntity.receiveTrail(p, level, player));
        registerToClient(AcceleratorControllerBlockEntity.LastEventPacket.PACKET_ID, autoCodec(AcceleratorControllerBlockEntity.LastEventPacket.class), (p, level, player) -> AcceleratorControllerBlockEntity.receiveEvent(p, level, player));
    }

    public static void receiveMessage(MessagePayload message, Level world) {
        var receivedBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(message.message()));
        var receiverEntity = world.getBlockEntity(message.pos());
        var receiverType = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(message.targetEntityType());
        if (receiverEntity != null && receiverType != null && receiverType.equals(receiverEntity.getType())) {
            decodeFields(receiverEntity, message.syncType(), receivedBuf, world);
            if (receiverEntity instanceof NetworkedEventHandler networkedBlock) {
                networkedBlock.onNetworkUpdated();
            }
        } else {
            Oritech.LOGGER.debug("Unable to start decoding for block entity type {} at {}. Target Mismatch!", receiverType, message.pos());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static int encodeFields(Object target, SyncType type, ByteBuf byteBuf, @Nullable Level world) {
        var fields = getCachedFields(target, type);
        int encodedCount = 0;
        for (var field : fields) {
            try {
                if (UpdatableField.class.isAssignableFrom(field.getType())) {
                    var fieldInstance = ((UpdatableField) field.get(target));
                    var deltaOnly = fieldInstance.useDeltaOnly(type);
                    var dataToSend = deltaOnly ? fieldInstance.getDeltaData() : fieldInstance;
                    var codec = deltaOnly ? fieldInstance.getDeltaCodec() : fieldInstance.getFullCodec();
                    if (codec instanceof WorldPacketCodec worldPacketCodec) {
                        worldPacketCodec.encode(byteBuf, dataToSend, world);
                    } else {
                        codec.encode(byteBuf, dataToSend);
                    }
                } else {
                    var codec = getAutoCodec(field);
                    var value = field.get(target);
                    if (codec instanceof WorldPacketCodec worldPacketCodec) {
                        worldPacketCodec.encode(byteBuf, value, world);
                    } else {
                        codec.encode(byteBuf, value);
                    }
                }
                encodedCount++;
            } catch (Exception ex) {
                Oritech.LOGGER.warn("failed to encode field: {}", field.getName(), ex);
            }
        }
        return encodedCount;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void decodeFields(Object target, SyncType type, ByteBuf byteBuf, Level world) {
        var fields = getCachedFields(target, type);
        for (var field : fields) {
            try {
                if (UpdatableField.class.isAssignableFrom(field.getType())) {
                    var fieldInstance = ((UpdatableField) field.get(target));
                    var deltaOnly = fieldInstance.useDeltaOnly(type);
                    var codec = deltaOnly ? fieldInstance.getDeltaCodec() : fieldInstance.getFullCodec();
                    Object value;
                    if (codec instanceof WorldPacketCodec worldPacketCodec) {
                        value = worldPacketCodec.decode(byteBuf, world);
                    } else {
                        value = codec.decode(byteBuf);
                    }
                    if (deltaOnly) {
                        fieldInstance.handleDeltaUpdate(value);
                    } else {
                        fieldInstance.handleFullUpdate(value);
                    }
                } else {
                    var codec = getAutoCodec(field);
                    Object value;
                    if (codec instanceof WorldPacketCodec worldPacketCodec) {
                        value = worldPacketCodec.decode(byteBuf, world);
                    } else {
                        value = codec.decode(byteBuf);
                    }
                    field.set(target, value);
                }
            } catch (Exception ex) {
                Oritech.LOGGER.warn("failed to decode field: {}", field.getName(), ex);
            }
        }
    }

    private static @NotNull List<Field> getCachedFields(Object target, SyncType type) {
        var key = target.getClass().hashCode() + type.hashCode();
        return CACHED_FIELDS.computeIfAbsent(key, elem -> getSyncFields(target, type));
    }

    private static @NotNull List<Field> getSyncFields(Object target, SyncType type) {
        var fields = new ArrayList<>(Arrays.asList(target.getClass().getDeclaredFields()));
        var superClass = target.getClass().getSuperclass();
        while (superClass != null) {
            fields.addAll(Arrays.asList(superClass.getDeclaredFields()));
            superClass = superClass.getSuperclass();
        }
        var filteredFields = new ArrayList<Field>();
        fields.stream()
            .filter(field -> hasSyncType(field.getAnnotation(SyncField.class), type))
            .forEachOrdered(field -> {
                field.setAccessible(true);
                filteredFields.add(field);
            });
        if (target instanceof AdditionalNetworkingProvider additionalNetworkingProvider) {
            var addedFields = additionalNetworkingProvider.additionalSyncedFields(type);
            addedFields.forEach(field -> {
                field.setAccessible(true);
                filteredFields.add(field);
            });
        }
        return filteredFields;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static StreamCodec getAutoCodec(Class<?> type) {
        if (!AUTO_CODECS.containsKey(type)) {
            if (type.isRecord()) {
                Oritech.LOGGER.debug("creating reflective codec for: " + type);
                var computedCodec = ReflectiveCodecBuilder.create((Class<? extends Record>) type);
                AUTO_CODECS.put(type, computedCodec);
                return computedCodec;
            } else if (type.isEnum()) {
                Oritech.LOGGER.debug("creating reflective enum codec for: " + type);
                var computedCodec = ReflectiveCodecBuilder.createForEnum((Class<? extends Enum>) type);
                AUTO_CODECS.put(type, computedCodec);
                return computedCodec;
            }
        }
        if (!AUTO_CODECS.containsKey(type)) {
            Oritech.LOGGER.error("No codec defined for: {}", type);
        }
        return AUTO_CODECS.get(type);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static StreamCodec getAutoCodec(Field field) {
        var listType = getListType(field.getGenericType());
        if (listType.isPresent()) {
            var listTypeCodec = getAutoCodec((Class<?>) listType.get());
            return listTypeCodec.apply(ByteBufCodecs.list());
        }
        var setType = getSetType(field.getGenericType());
        if (setType.isPresent()) {
            var setTypeCodec = getAutoCodec((Class<?>) setType.get());
            return setTypeCodec.apply(toSet());
        }
        var mapType = getMapType(field.getGenericType());
        if (mapType.isPresent()) {
            var keyCodec = getAutoCodec((Class<?>) mapType.get().getA());
            var valueCodec = getAutoCodec((Class<?>) mapType.get().getB());
            return ByteBufCodecs.map(HashMap::new, keyCodec, valueCodec);
        }
        return getAutoCodec(field.getType());
    }

    public static Optional<Type> getListType(Type type) {
        if (type instanceof ParameterizedType pType) {
            var rawType = (Class<?>) pType.getRawType();
            if (rawType instanceof Class && List.class.isAssignableFrom(rawType)) {
                return Optional.of(pType.getActualTypeArguments()[0]);
            }
        }
        return Optional.empty();
    }

    public static Optional<Type> getSetType(Type type) {
        if (type instanceof ParameterizedType pType) {
            var rawType = (Class<?>) pType.getRawType();
            if (rawType instanceof Class && Set.class.isAssignableFrom(rawType)) {
                return Optional.of(pType.getActualTypeArguments()[0]);
            }
        }
        return Optional.empty();
    }

    public static Optional<Tuple<Type, Type>> getMapType(Type type) {
        if (type instanceof ParameterizedType pType) {
            var rawType = (Class<?>) pType.getRawType();
            if (rawType instanceof Class && Map.class.isAssignableFrom(rawType)) {
                var typeArgs = pType.getActualTypeArguments();
                return Optional.of(new Tuple<>(typeArgs[0], typeArgs[1]));
            }
        }
        return Optional.empty();
    }

    private static boolean hasSyncType(SyncField annotation, SyncType type) {
        if (annotation == null) return false;
        for (var value : annotation.value()) {
            if (value.equals(type)) return true;
        }
        return false;
    }

    // ---- Packet record ----

    public record MessagePayload(BlockPos pos, ResourceLocation targetEntityType, SyncType syncType, byte[] message) {

        public static final PacketId<MessagePayload> TYPE = new PacketId<>(Oritech.id("generic"));

        public static final StreamCodec<FriendlyByteBuf, MessagePayload> CODEC = new StreamCodec<>() {
            @Override
            public MessagePayload decode(FriendlyByteBuf buf) {
                var pos = buf.readBlockPos();
                var entityType = buf.readResourceLocation();
                var syncType = SyncType.PACKET_CODEC.decode(buf);
                var bytes = buf.readByteArray();
                return new MessagePayload(pos, entityType, syncType, bytes);
            }

            @Override
            public void encode(FriendlyByteBuf buf, MessagePayload value) {
                buf.writeBlockPos(value.pos);
                buf.writeResourceLocation(value.targetEntityType);
                SyncType.PACKET_CODEC.encode(buf, value.syncType);
                buf.writeByteArray(value.message);
            }
        };
    }

    static <B extends ByteBuf, V> java.util.function.Function<StreamCodec<B, V>, StreamCodec<B, Set<V>>> toSet() {
        return (codec) -> ByteBufCodecs.collection(HashSet::new, codec);
    }

    public static StreamCodec<FriendlyByteBuf, BlockState> SIMPLE_BLOCK_STATE_PACKET_CODEC = new StreamCodec<>() {
        @Override
        public BlockState decode(FriendlyByteBuf buf) {
            return BuiltInRegistries.BLOCK.get(buf.readResourceLocation()).defaultBlockState();
        }

        @Override
        public void encode(FriendlyByteBuf buf, BlockState value) {
            buf.writeResourceLocation(BuiltInRegistries.BLOCK.getKey(value.getBlock()));
        }
    };

    public static StreamCodec<FriendlyByteBuf, Vector2i> VEC2I_PACKED_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, Vector2i::x,
        ByteBufCodecs.INT, Vector2i::y,
        Vector2i::new
    );

    @SuppressWarnings("unchecked")
    public static <K, V> StreamCodec<FriendlyByteBuf, HashMap<K, V>> createMapCodec(Class<K> keyType, Class<V> valueType) {
        return ByteBufCodecs.map(HashMap::new, getAutoCodec(keyType), getAutoCodec(valueType));
    }

    public static StreamCodec<FriendlyByteBuf, Vec3> VEC3D_PACKET_CODEC = new StreamCodec<>() {
        @Override
        public Vec3 decode(FriendlyByteBuf buf) {
            return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }

        @Override
        public void encode(FriendlyByteBuf buf, Vec3 value) {
            buf.writeDouble(value.x);
            buf.writeDouble(value.y);
            buf.writeDouble(value.z);
        }
    };
}
