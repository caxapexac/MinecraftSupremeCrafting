package com.supremecrafting.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.supremecrafting.registry.SCRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Shaped recipe for the Supreme Table. Strict-size: matches only when the
 * input's bounding box equals the pattern's width × height.
 */
public class SupremeShapedRecipe implements SupremeCraftingRecipe {
    private final SupremeShapedPattern pattern;
    private final ItemStack result;
    private final String group;

    public SupremeShapedRecipe(String group, SupremeShapedPattern pattern, ItemStack result) {
        this.group = group;
        this.pattern = pattern;
        this.result = result;
    }

    public SupremeShapedPattern pattern() { return pattern; }

    @Override
    public String getGroup() { return group; }

    @Override
    public boolean matches(@NotNull CraftingInput input, @NotNull Level level) {
        return pattern.matches(input);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput input, HolderLookup.@NotNull Provider provider) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= pattern.width() && height >= pattern.height();
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider provider) {
        return result;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        return pattern.ingredients();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return SCRecipes.SHAPED.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return SCRecipes.SUPREME_CRAFTING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<SupremeShapedRecipe> {
        public static final MapCodec<SupremeShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                com.mojang.serialization.Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                SupremeShapedPattern.MAP_CODEC.forGetter(r -> r.pattern),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result)
        ).apply(instance, SupremeShapedRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, SupremeShapedRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public @NotNull MapCodec<SupremeShapedRecipe> codec() { return CODEC; }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, SupremeShapedRecipe> streamCodec() { return STREAM_CODEC; }

        private static void toNetwork(RegistryFriendlyByteBuf buf, SupremeShapedRecipe recipe) {
            buf.writeUtf(recipe.group);
            SupremeShapedPattern.STREAM_CODEC.encode(buf, recipe.pattern);
            ItemStack.STREAM_CODEC.encode(buf, recipe.result);
        }

        private static SupremeShapedRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
            String group = buf.readUtf();
            SupremeShapedPattern pattern = SupremeShapedPattern.STREAM_CODEC.decode(buf);
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            return new SupremeShapedRecipe(group, pattern, result);
        }
    }
}
