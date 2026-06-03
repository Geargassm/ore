package rearth.oritech.block.entity.augmenter.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ModifierAugment extends Augment {

    private final Attribute targetAttribute;
    private final float amount;
    private final AttributeModifier.Operation operation;
    private final UUID modifierUuid;

    public ModifierAugment(ResourceLocation id, Attribute targetAttribute, AttributeModifier.Operation operation, float amount, boolean toggleable) {
        super(id, toggleable);
        this.targetAttribute = targetAttribute;
        this.amount = amount;
        this.operation = operation;
        this.modifierUuid = UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void activate(Player player) {
        if (targetAttribute == null) return;
        var instance = player.getAttribute(targetAttribute);
        if (instance == null) return;
        if (instance.getModifier(modifierUuid) != null) instance.removeModifier(modifierUuid);
        instance.addPermanentModifier(new AttributeModifier(modifierUuid, id.toString(), amount, operation));
    }

    @Override
    public void deactivate(Player player) {
        if (targetAttribute == null) return;
        var instance = player.getAttribute(targetAttribute);
        if (instance == null) return;
        instance.removeModifier(modifierUuid);
    }

    @Override
    public void refreshServer(Player player) {
        if (targetAttribute == null) return;
        var instance = player.getAttribute(targetAttribute);
        if (instance == null) return;
        if (instance.getModifier(modifierUuid) != null) instance.removeModifier(modifierUuid);
        instance.addPermanentModifier(new AttributeModifier(modifierUuid, id.toString(), amount, operation));
    }

    @Override
    public int refreshInterval() {
        return 3000;
    }
}
