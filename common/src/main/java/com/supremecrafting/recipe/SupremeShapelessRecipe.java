package com.supremecrafting.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.supremecrafting.registry.SCRecipes;
import com.supremecrafting.table.SupremeTableInventory;
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
 * Shapeless recipe for the Supreme Table. Matches when the input's bounding
 * box contains exactly the listed ingredients in any arrangement (any order,
 * any cell positions inside the bbox).
 *
 * <p>Vanilla shapeless caps ingredients at 9 (3×3 fits); we cap at
 * {@link SupremeTableInventory#SIZE} (6561 — i.e. no practical limit).
 */
public class SupremeShapelessRecipe implements SupremeCraftingRecipe {
    private static final int MAX_INGREDIENTS = SupremeTableInventory.SIZE;

    private final String group;
    private final ItemStack result;
    private final NonNullList<Ingredient> ingredients;

    public SupremeShapelessRecipe(String group, ItemStack result, NonNullList<Ingredient> ingredients) {
        this.group = group;
        this.result = result;
        this.ingredients = ingredients;
    }

    @Override
    public String getGroup() { return group; }

    @Override
    public boolean matches(@NotNull CraftingInput input, @NotNull Level level) {
        if (input.ingredientCount() != ingredients.size()) return false;
        if (input.size() == 1 && ingredients.size() == 1) {
            return ingredients.get(0).test(input.getItem(0));
        }
        return input.stackedContents().canCraft(this, null);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput input, HolderLookup.@NotNull Provider provider) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= ingredients.size();
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider provider) {
        return result;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return SCRecipes.SHAPELESS.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return SCRecipes.SUPREME_CRAFTING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<SupremeShapelessRecipe> {
        public static final MapCodec<SupremeShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap(list -> {
                    Ingredient[] arr = list.stream().filter(i -> !i.isEmpty()).toArray(Ingredient[]::new);
                    if (arr.length == 0) {
                        return DataResult.error(() -> "Shapeless recipe needs at least one ingredient");
                    }
                    if (arr.length > MAX_INGREDIENTS) {
                        return DataResult.error(() -> "Shapeless recipe has too many ingredients (max " + MAX_INGREDIENTS + ")");
                    }
                    return DataResult.success(NonNullList.of(Ingredient.EMPTY, arr));
                }, DataResult::success).forGetter(r -> r.ingredients)
        ).apply(instance, SupremeShapelessRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, SupremeShapelessRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public @NotNull MapCodec<SupremeShapelessRecipe> codec() { return CODEC; }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, SupremeShapelessRecipe> streamCodec() { return STREAM_CODEC; }

        private static void toNetwork(RegistryFriendlyByteBuf buf, SupremeShapelessRecipe recipe) {
            buf.writeUtf(recipe.group);
            ItemStack.STREAM_CODEC.encode(buf, recipe.result);
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient ing : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing);
            }
        }

        private static SupremeShapelessRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
            String group = buf.readUtf();
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            int count = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(count, Ingredient.EMPTY);
            ingredients.replaceAll(ignored -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            return new SupremeShapelessRecipe(group, result, ingredients);
        }
    }
}
