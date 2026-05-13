package com.supremecrafting.registry;

import com.supremecrafting.SupremeCrafting;
import com.supremecrafting.recipe.SupremeCraftingRecipe;
import com.supremecrafting.recipe.SupremeShapedRecipe;
import com.supremecrafting.recipe.SupremeShapelessRecipe;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class SCRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(SupremeCrafting.MOD_ID, Registries.RECIPE_TYPE);

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(SupremeCrafting.MOD_ID, Registries.RECIPE_SERIALIZER);

    public static final RegistrySupplier<RecipeType<SupremeCraftingRecipe>> SUPREME_CRAFTING_TYPE =
            TYPES.register("supreme_crafting", () -> new RecipeType<SupremeCraftingRecipe>() {
                private final String name =
                        ResourceLocation.fromNamespaceAndPath(SupremeCrafting.MOD_ID, "supreme_crafting").toString();
                @Override
                public String toString() { return name; }
            });

    public static final RegistrySupplier<RecipeSerializer<SupremeShapedRecipe>> SHAPED =
            SERIALIZERS.register("supreme_shaped", SupremeShapedRecipe.Serializer::new);

    public static final RegistrySupplier<RecipeSerializer<SupremeShapelessRecipe>> SHAPELESS =
            SERIALIZERS.register("supreme_shapeless", SupremeShapelessRecipe.Serializer::new);

    private SCRecipes() {}
}
