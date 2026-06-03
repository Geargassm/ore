package rearth.oritech.item.tools;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.init.OritechStartupConfig;
import rearth.oritech.init.SoundContent;
import rearth.oritech.item.tools.harvesting.ChainsawItem;
import rearth.oritech.item.tools.util.OritechEnergyItem;
import rearth.oritech.util.TooltipHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ElectricMaceItem extends Item implements OritechEnergyItem {

    private static final UUID BASE_ATTACK_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private static final UUID BASE_ATTACK_SPEED_UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    public static final Map<Long, Runnable> PENDING_LIGHTNING_HITS = new HashMap<>();

    public ElectricMaceItem(Properties settings) {
        super(settings);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 8, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -3.4, AttributeModifier.Operation.ADDITION));
            return builder.build();
        }
        return super.getDefaultAttributeModifiers(slot);
    }

    private boolean canSmashAttack(LivingEntity attacker) {
        return attacker.fallDistance > 0.0F && !attacker.onGround();
    }

    private float calculateFallDamageBonus(LivingEntity attacker) {
        float fallDist = attacker.fallDistance;
        float damage;
        if (fallDist <= 3.0F) {
            damage = getAttackDamage() * fallDist;
        } else if (fallDist <= 8.0F) {
            damage = getAttackDamage() * 4 + 4.0F * (fallDist - 3.0F);
        } else {
            damage = getAttackDamage() * 6 + fallDist - 8.0F;
        }
        return damage;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        var bonus = 0f;

        var usedEnergy = tryUseEnergy(stack, OritechStartupConfig.electricMace.energyUsage.get(), null);
        if (usedEnergy && canSmashAttack(attacker)) {
            attacker.level().playSound(null, target.blockPosition(), SoundContent.ELECTRIC_SHOCK, SoundSource.PLAYERS);
            attacker.resetFallDistance();
            bonus = calculateFallDamageBonus(attacker);
        }

        if (attacker instanceof Player player && attacker.level() instanceof ServerLevel serverWorld) {
            if (player.getCooldowns().isOnCooldown(this))
                return true;

            player.getCooldowns().addCooldown(this, 40);
            createLightningAttack(serverWorld, player, target, stack, (int) (getAttackDamage() / 2f + bonus / 2f));
        }

        return true;
    }

    private void createLightningAttack(ServerLevel world, Player attacker, LivingEntity target, ItemStack stack, int damage) {

        var usedEnergy = tryUseEnergy(stack, OritechStartupConfig.electricMace.energyUsage.get() * OritechStartupConfig.electricMace.lightningCostMultiplier.get(), null);
        if (usedEnergy) {

            var playerPos = attacker.getEyePosition();
            var targetPos = target.getEyePosition();
            var offset = targetPos.subtract(playerPos);
            var up = new Vec3(0, 1, 0);
            var cross = offset.cross(up).normalize();
            var pos = targetPos.add(cross.scale(14)).offsetRandom(world.random, 3).add(0, 7, 0);

            createLightningBolt(world, pos, target.getEyePosition().offsetRandom(world.random, 0.1f), 10, 0.8f, 4, ParticleTypes.ENCHANTED_HIT, 0.35f, 2, 0);

            for (int i = 1; i <= 5; i++) {
                final var ownPos = targetPos.add(cross.yRot(i * 90).scale(14)).offsetRandom(world.random, 5).add(0, 7, 0);
                PENDING_LIGHTNING_HITS.put(world.getGameTime() + 10 * i, () -> {
                    createLightningBolt(world, ownPos, target.getEyePosition().offsetRandom(world.random, 0.1f), 10, 0.8f, 4, ParticleTypes.ENCHANTED_HIT, 0.35f, 2, 0);
                    target.hurtTime = 0;
                    target.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.LIGHTNING_BOLT), attacker), damage);
                });
            }
        }
    }

    public static void processLightningEvents(Level world) {
        var toRemove = new ArrayList<Long>();
        for (var entry : PENDING_LIGHTNING_HITS.entrySet()) {
            var key = entry.getKey();
            if (world.getGameTime() > key) {
                var event = entry.getValue();
                event.run();
                toRemove.add(key);
            }
        }

        toRemove.forEach(PENDING_LIGHTNING_HITS::remove);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag type) {
        var text = Component.translatable("tooltip.oritech.energy_indicator", TooltipHelper.getEnergyText(this.getStoredEnergy(stack)), TooltipHelper.getEnergyText(this.getEnergyCapacity(stack)));
        tooltip.add(text.withStyle(ChatFormatting.GOLD));

        var showExtra = Screen.hasControlDown();

        if (showExtra) {
            tooltip.add(Component.translatable("tooltip.oritech.electric_mace").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
            tooltip.add(Component.translatable("tooltip.oritech.electric_mace.1").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        } else {
            tooltip.add(Component.translatable("tooltip.oritech.item_extra_info").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        }
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
        return Math.round((getStoredEnergy(stack) * 100f / this.getEnergyCapacity(stack)) * ChainsawItem.BAR_STEP_COUNT) / 100;
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
        return OritechStartupConfig.electricMace.energyCapacity.get();
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return getEnergyCapacity(stack) / 10;
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

    private int getAttackDamage() {
        return 8;
    }

    /**
     * Creates a lightning effect between two points.
     */
    public static void createLightningBolt(ServerLevel level, Vec3 startPos, Vec3 endPos,
                                           int mainSegments, double jitterAmount,
                                           double particlesPerMeter, ParticleOptions particleEffect,
                                           float branchChance, int maxBranchDepth, int currentBranchDepth) {
        var random = level.getRandom();
        var direction = endPos.subtract(startPos);
        double totalDistance = direction.length();

        if (totalDistance < 0.1) {
            level.sendParticles(particleEffect, startPos.x, startPos.y, startPos.z, 5, 0.1, 0.1, 0.1, 0.05);
            return;
        }

        var segmentVector = direction.normalize().scale(totalDistance / mainSegments);
        var previousPoint = startPos;

        if (currentBranchDepth == 0) {
            level.playSound(null, endPos.x, endPos.y, endPos.z, SoundContent.ELECTRIC_SHOCK, SoundSource.PLAYERS, 0.8f, 0.5f + level.random.nextFloat() * 0.8f);
        }

        for (int i = 0; i < mainSegments; i++) {
            Vec3 currentTargetPoint;
            if (i < mainSegments - 1) {
                currentTargetPoint = startPos.add(segmentVector.scale(i + 1));
                currentTargetPoint = currentTargetPoint.add(
                  (random.nextDouble() - 0.5) * 2 * jitterAmount,
                  (random.nextDouble() - 0.5) * 2 * jitterAmount,
                  (random.nextDouble() - 0.5) * 2 * jitterAmount
                );
            } else {
                currentTargetPoint = endPos;
            }

            spawnParticlesAlongSegment(level, previousPoint, currentTargetPoint, particlesPerMeter, particleEffect);

            if (currentBranchDepth < maxBranchDepth && random.nextFloat() < branchChance && i < mainSegments - 1) {
                var branchEndOffset = new Vec3(
                  (random.nextDouble() - 0.5) * totalDistance * 0.3,
                  (random.nextDouble() - 0.5) * totalDistance * 0.3,
                  (random.nextDouble() - 0.5) * totalDistance * 0.3
                );
                var branchEnd = currentTargetPoint.add(branchEndOffset);
                createLightningBolt(level, currentTargetPoint, branchEnd,
                  Math.max(1, mainSegments / 2), jitterAmount * 0.7,
                  particlesPerMeter * 0.7, particleEffect,
                  branchChance * 0.5f, maxBranchDepth, currentBranchDepth + 1);
            }
            previousPoint = currentTargetPoint;
        }
    }

    private static void spawnParticlesAlongSegment(ServerLevel level, Vec3 p1, Vec3 p2, double particlesPerMeter, ParticleOptions particleEffect) {
        var segment = p2.subtract(p1);
        var length = segment.length();
        if (length < 0.01) return;

        var unit = segment.normalize();
        var numParticles = Math.max(1, (int) (length * particlesPerMeter));

        for (int i = 0; i < numParticles; i++) {
            double progress = (double) i / (double) numParticles;
            var particlePos = p1.add(unit.scale(length * progress));
            level.sendParticles(particleEffect, particlePos.x, particlePos.y, particlePos.z,
              1, 0, 0, 0, 0.0D);
        }
    }
}
