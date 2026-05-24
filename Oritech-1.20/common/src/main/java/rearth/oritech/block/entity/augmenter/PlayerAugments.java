package rearth.oritech.block.entity.augmenter;
import rearth.oritech.api.networking.PacketId;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeManager;
import rearth.oritech.Oritech;
import rearth.oritech.api.attachment.AttachmentApi;
import rearth.oritech.block.entity.augmenter.api.Augment;
import rearth.oritech.init.recipes.RecipeContent;

import java.util.HashMap;
import java.util.Map;

public class PlayerAugments {
    
    public static final Map<ResourceLocation, Augment> allAugments = new HashMap<>();
    
    // this is called after recipe manager init / recipe reload
    public static void loadAllAugments(RecipeManager manager) {
        allAugments.clear();
        manager.getAllRecipesFor(RecipeContent.AUGMENT_DATA).forEach(recipe -> allAugments.put(recipe.id(), recipe.value().createAugment(recipe.id())));
    }
    
    public static void serverTickAugments(ServerPlayer player) {
        
        var data = AttachmentApi.getAttachmentValue(player, Augment.ACTIVE_AUGMENTS_DATA);
        
        for (var augment : allAugments.values()) {
            if (augment.isEnabled(data)) {
                if (player.serverLevel().getGameTime() % augment.refreshInterval() == 0)
                    augment.refreshServer(player);
            }
        }
    }

    public static void refreshActiveAugments(ServerPlayer player) {

        var data = AttachmentApi.getAttachmentValue(player, Augment.ACTIVE_AUGMENTS_DATA);

        for (var augment : allAugments.values()) {
            if (augment.isEnabled(data)) {
                augment.refreshServer(player);
            }
        }
    }
    
    public static void receiveInstallTrigger(AugmentInstallTriggerPacket packet, Player player, Level level) {
        var entity = player.level().getBlockEntity(packet.position);
        
        if (entity instanceof AugmentApplicationEntity modifierEntity) {
            var operation = PlayerAugments.AugmentApplicatorOperation.values()[packet.operationId];
            switch (operation) {
                case RESEARCH -> {
                    modifierEntity.researchAugment(packet.id, player.isCreative(), player);
                }
                case ADD -> {
                    modifierEntity.installAugmentToPlayer(packet.id, player);
                }
                case REMOVE -> {
                    modifierEntity.removeAugmentFromPlayer(packet.id, player);
                }
            }
        }
    }
    
    public static void receivePlayerLoadMachine(LoadPlayerAugmentsToMachinePacket packet, Player player, Level level) {
        var entity = player.level().getBlockEntity(packet.position);
        
        if (entity instanceof AugmentApplicationEntity modifierEntity) {
            modifierEntity.loadResearchesFromPlayer(player);
        }
    }
    
    public static void receiveOpenAugmentScreen(OpenAugmentScreenPacket packet, Player player, Level level) {
        var entity = player.level().getBlockEntity(packet.position);
        
        if (entity instanceof AugmentApplicationEntity modifierEntity && player instanceof ServerPlayer serverPlayer) {
            modifierEntity.screenInvOverride = true;
            MenuRegistry.openExtendedMenu(serverPlayer, modifierEntity);
        }
    }
    
    public static void receiveToggleAugment(AugmentPlayerTogglePacket packet, Player player, Level level) {
        AugmentApplicationEntity.toggleAugmentForPlayer(packet.id, player);
    }
    
    public enum AugmentApplicatorOperation {
        RESEARCH, ADD, REMOVE, NONE, NEEDS_INIT
    }
    
    public record AugmentInstallTriggerPacket(BlockPos position, ResourceLocation id, int operationId) {
        
        public static final PacketId<AugmentInstallTriggerPacket> PACKET_ID = new PacketId<>(Oritech.id("aug_install"));
        
    }
    
    public record LoadPlayerAugmentsToMachinePacket(BlockPos position) {
        
        public static final PacketId<LoadPlayerAugmentsToMachinePacket> PACKET_ID = new PacketId<>(Oritech.id("aug_loadtomachine"));
        
    }
    
    public record OpenAugmentScreenPacket(BlockPos position) {
        
        public static final PacketId<OpenAugmentScreenPacket> PACKET_ID = new PacketId<>(Oritech.id("aug_openscreen"));
        
    }
    
    public record AugmentPlayerTogglePacket(ResourceLocation id) {
        
        public static final PacketId<AugmentPlayerTogglePacket> PACKET_ID = new PacketId<>(Oritech.id("aug_toggle"));
        
    }
}
