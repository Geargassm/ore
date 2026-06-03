package rearth.oritech.init.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.fluid.FluidStack;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.util.FluidIngredient;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import rearth.oritech.compat.ByteBufCodecs;
import rearth.oritech.compat.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public class OritechRecipeType implements RecipeSerializer<OritechRecipe>, RecipeType<OritechRecipe> {
    
    public static final Codec<FluidStack> FLUID_STACK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(FluidStack::getFluid),
        Codec.LONG.optionalFieldOf("amount", FluidStack.bucketAmount()).forGetter(FluidStack::getAmount)
    ).apply(instance, FluidStack::create));
    
    public static final MapCodec<OritechRecipe> ORI_RECIPE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      Codec.INT.optionalFieldOf("time", 60).forGetter(OritechRecipe::getTime),
      Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter(OritechRecipe::getInputs),
      ItemStack.CODEC.listOf().fieldOf("results").forGetter(OritechRecipe::getResults),
      ResourceLocation.CODEC.xmap(identifier1 -> (OritechRecipeType) BuiltInRegistries.RECIPE_TYPE.get(identifier1), OritechRecipeType::getIdentifier).fieldOf("type").forGetter(OritechRecipe::getOriType),
      FluidIngredient.CODEC.optionalFieldOf("fluidInput", FluidIngredient.EMPTY).forGetter(OritechRecipe::getFluidInput),
      FLUID_STACK_CODEC.listOf().optionalFieldOf("fluidOutputs", List.of()).forGetter(OritechRecipe::getFluidOutputs)
    ).apply(instance, OritechRecipe::new));
    
    static final StreamCodec<FriendlyByteBuf, Ingredient> INGREDIENT_STREAM_CODEC =
        StreamCodec.of((buf, ing) -> ing.toNetwork(buf), Ingredient::fromNetwork);

    static final StreamCodec<FriendlyByteBuf, List<ItemStack>> ITEM_LIST_STREAM_CODEC =
        ByteBufCodecs.ITEM_STACK.apply(ByteBufCodecs.list());

    public static final StreamCodec<FriendlyByteBuf, OritechRecipe> PACKET_CODEC = StreamCodec.composite(
      ByteBufCodecs.INT, OritechRecipe::getTime,
      INGREDIENT_STREAM_CODEC.apply(ByteBufCodecs.list()), OritechRecipe::getInputs,
      ITEM_LIST_STREAM_CODEC, OritechRecipe::getResults,
      ByteBufCodecs.RESOURCE_LOCATION.map(identifier1 -> (OritechRecipeType) BuiltInRegistries.RECIPE_TYPE.get(identifier1), OritechRecipeType::getIdentifier), OritechRecipe::getOriType,
      FluidIngredient.PACKET_CODEC, OritechRecipe::getFluidInput,
      NetworkManager.FLUID_STACK_STREAM_CODEC.apply(ByteBufCodecs.list()), OritechRecipe::getFluidOutputs,
      OritechRecipe::new
    );
    
    private final ResourceLocation identifier;
    
    public ResourceLocation getIdentifier() {
        return identifier;
    }
    
    public OritechRecipeType(ResourceLocation identifier) {
        this.identifier = identifier;
    }
    
    @Override
    public OritechRecipe fromJson(ResourceLocation id, com.google.gson.JsonObject json) {
        return ORI_RECIPE_CODEC.codec().parse(com.mojang.serialization.JsonOps.INSTANCE, json)
            .getOrThrow(false, err -> {});
    }

    @Override
    public OritechRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        return PACKET_CODEC.decode(buf);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, OritechRecipe recipe) {
        PACKET_CODEC.encode(buf, recipe);
    }
    
    @Override
    public String toString() {
        return "OritechRecipeType{" +
                 "identifier=" + identifier +
                 '}';
    }
}
