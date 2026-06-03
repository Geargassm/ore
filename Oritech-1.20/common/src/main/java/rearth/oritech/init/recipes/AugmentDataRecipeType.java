package rearth.oritech.init.recipes;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import rearth.oritech.compat.ByteBufCodecs;
import rearth.oritech.compat.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import rearth.oritech.util.SizedIngredient;

public class AugmentDataRecipeType implements RecipeSerializer<AugmentDataRecipe>, RecipeType<AugmentDataRecipe> {
    
    public static final MapCodec<AugmentDataRecipe> AUGMENT_DATA_RECIPE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      ResourceLocation.CODEC.xmap(identifier1 -> (AugmentDataRecipeType) BuiltInRegistries.RECIPE_TYPE.get(identifier1), AugmentDataRecipeType::getIdentifier).fieldOf("type").forGetter(AugmentDataRecipe::getOriType),
      Codec.BOOL.fieldOf("toggleable").forGetter(AugmentDataRecipe::isToggleable),
      SizedIngredient.CODEC.codec().listOf().fieldOf("researchCost").forGetter(AugmentDataRecipe::getResearchCost),
      SizedIngredient.CODEC.codec().listOf().fieldOf("applyCost").forGetter(AugmentDataRecipe::getApplyCost),
      ResourceLocation.CODEC.listOf().fieldOf("requirements").forGetter(AugmentDataRecipe::getRequirements),
      ResourceLocation.CODEC.fieldOf("requiredStation").forGetter(AugmentDataRecipe::getRequiredStation),
      Codec.INT.fieldOf("uiX").forGetter(AugmentDataRecipe::getUiX),
      Codec.INT.fieldOf("uiY").forGetter(AugmentDataRecipe::getUiY),
      Codec.INT.fieldOf("time").forGetter(AugmentDataRecipe::getTime),
      Codec.LONG.fieldOf("rfCost").forGetter(AugmentDataRecipe::getRfCost),
      Codec.either(Codec.either(AugmentDataRecipe.EffectDefinition.CODEC, AugmentDataRecipe.ModifierDefinition.CODEC), AugmentDataRecipe.CustomAugmentDefinition.CODEC).fieldOf("effect").forGetter(AugmentDataRecipe::getDefinition)
    ).apply(instance, AugmentDataRecipe::new));
    
    public static final StreamCodec<FriendlyByteBuf, AugmentDataRecipe> PACKET_CODEC = StreamCodec.of(
      (buf, recipe) -> {
          ByteBufCodecs.RESOURCE_LOCATION.encode(buf, recipe.getOriType().getIdentifier());
          buf.writeBoolean(recipe.isToggleable());
          SizedIngredient.PACKET_CODEC.apply(ByteBufCodecs.list()).encode(buf, recipe.getResearchCost());
          SizedIngredient.PACKET_CODEC.apply(ByteBufCodecs.list()).encode(buf, recipe.getApplyCost());
          ByteBufCodecs.RESOURCE_LOCATION.apply(ByteBufCodecs.list()).encode(buf, recipe.getRequirements());
          ByteBufCodecs.RESOURCE_LOCATION.encode(buf, recipe.getRequiredStation());
          buf.writeInt(recipe.getUiX());
          buf.writeInt(recipe.getUiY());
          buf.writeInt(recipe.getTime());
          buf.writeLong(recipe.getRfCost());
          var def = recipe.getDefinition();
          if (def.right().isPresent()) {
              buf.writeByte(2);
              ByteBufCodecs.RESOURCE_LOCATION.encode(buf, def.right().get().customAugmentId());
          } else if (def.left().get().right().isPresent()) {
              buf.writeByte(1);
              var mod = def.left().get().right().get();
              ByteBufCodecs.RESOURCE_LOCATION.encode(buf, mod.entityAttributeId());
              buf.writeInt(mod.attributeOperationType());
              buf.writeFloat(mod.amount());
          } else {
              buf.writeByte(0);
              var eff = def.left().get().left().get();
              ByteBufCodecs.RESOURCE_LOCATION.encode(buf, eff.potionEffectId());
              buf.writeInt(eff.effectStrength());
          }
      },
      buf -> {
          var type = (AugmentDataRecipeType) BuiltInRegistries.RECIPE_TYPE.get(ByteBufCodecs.RESOURCE_LOCATION.decode(buf));
          var toggleable = buf.readBoolean();
          var researchCost = SizedIngredient.PACKET_CODEC.apply(ByteBufCodecs.list()).decode(buf);
          var applyCost = SizedIngredient.PACKET_CODEC.apply(ByteBufCodecs.list()).decode(buf);
          var requirements = ByteBufCodecs.RESOURCE_LOCATION.apply(ByteBufCodecs.list()).decode(buf);
          var requiredStation = ByteBufCodecs.RESOURCE_LOCATION.decode(buf);
          var uiX = buf.readInt();
          var uiY = buf.readInt();
          var time = buf.readInt();
          var rfCost = buf.readLong();
          var kind = buf.readByte();
          Either<Either<AugmentDataRecipe.EffectDefinition, AugmentDataRecipe.ModifierDefinition>, AugmentDataRecipe.CustomAugmentDefinition> effect;
          if (kind == 2) {
              effect = Either.right(new AugmentDataRecipe.CustomAugmentDefinition(ByteBufCodecs.RESOURCE_LOCATION.decode(buf)));
          } else if (kind == 1) {
              effect = Either.left(Either.right(new AugmentDataRecipe.ModifierDefinition(ByteBufCodecs.RESOURCE_LOCATION.decode(buf), buf.readInt(), buf.readFloat())));
          } else {
              effect = Either.left(Either.left(new AugmentDataRecipe.EffectDefinition(ByteBufCodecs.RESOURCE_LOCATION.decode(buf), buf.readInt())));
          }
          return new AugmentDataRecipe(type, toggleable, researchCost, applyCost, requirements, requiredStation, uiX, uiY, time, rfCost, effect);
      }
    );
    
    private final ResourceLocation identifier;
    
    public ResourceLocation getIdentifier() {
        return identifier;
    }
    
    public AugmentDataRecipeType(ResourceLocation identifier) {
        this.identifier = identifier;
    }
    
    @Override
    public AugmentDataRecipe fromJson(ResourceLocation id, com.google.gson.JsonObject json) {
        return AUGMENT_DATA_RECIPE_CODEC.codec().parse(com.mojang.serialization.JsonOps.INSTANCE, json)
            .getOrThrow(false, err -> {});
    }

    @Override
    public AugmentDataRecipe fromNetwork(ResourceLocation id, net.minecraft.network.FriendlyByteBuf buf) {
        return PACKET_CODEC.decode(buf);
    }

    @Override
    public void toNetwork(net.minecraft.network.FriendlyByteBuf buf, AugmentDataRecipe recipe) {
        PACKET_CODEC.encode(buf, recipe);
    }
}
