package com.supremecrafting.recipe;

import com.supremecrafting.registry.SCRecipes;
import com.supremecrafting.table.SupremeTableInventory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Top-level entry point for finding a recipe match for the Supreme Table.
 *
 * <p>Checks the table's own {@code RecipeType<SupremeCraftingRecipe>} first;
 * falls back to vanilla {@link RecipeType#CRAFTING} when the input bounding
 * box fits in 3×3 — so a 2×2 plank → crafting-table recipe etc. all "just
 * work" in our table. The strict-size match guarantees a 3×3 vanilla recipe
 * can't accidentally match inside an unrelated 81×81 layout.
 */
public final class SupremeCraftingMatcher {
    private SupremeCraftingMatcher() {}

    public static CraftingInput buildInput(SupremeTableInventory inv) {
        if (inv.isEmpty()) return CraftingInput.EMPTY;
        return CraftingInput.of(SupremeTableInventory.WIDTH, SupremeTableInventory.HEIGHT, inv.items());
    }

    /**
     * Find a matching recipe, preferring our type then falling back to vanilla
     * 3×3 crafting. Returns the {@link Recipe} (no holder) — both consumers
     * (result computation, getRemainingItems) just need the recipe.
     */
    public static Optional<Recipe<CraftingInput>> findRecipe(
            CraftingInput input, RecipeManager rm, Level level) {
        if (input.isEmpty()) return Optional.empty();

        Optional<RecipeHolder<SupremeCraftingRecipe>> supreme =
                rm.getRecipeFor(SCRecipes.SUPREME_CRAFTING_TYPE.get(), input, level);
        if (supreme.isPresent()) {
            return Optional.<Recipe<CraftingInput>>of(supreme.get().value());
        }

        if (input.width() <= 3 && input.height() <= 3) {
            Optional<RecipeHolder<CraftingRecipe>> vanilla =
                    rm.getRecipeFor(RecipeType.CRAFTING, input, level);
            if (vanilla.isPresent()) {
                return Optional.<Recipe<CraftingInput>>of(vanilla.get().value());
            }
        }

        return Optional.empty();
    }

    /** Convenience: build the input from inventory and find a match in one call. */
    public static Optional<Recipe<CraftingInput>> findRecipe(
            SupremeTableInventory inv, RecipeManager rm, Level level) {
        return findRecipe(buildInput(inv), rm, level);
    }
}
