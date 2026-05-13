package com.supremecrafting.recipe.integration;

import com.supremecrafting.recipe.SupremeShapedPattern;
import com.supremecrafting.recipe.SupremeShapedRecipe;
import com.supremecrafting.recipe.SupremeShapelessRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.List;

/**
 * 2D ingredient grid for recipe-viewer rendering. Width × height with a flat
 * {@code ingredients} list in row-major order (length = width × height).
 * Empty cells use {@link Ingredient#EMPTY}.
 *
 * <p>Used by both EMI and JEI integrations to drive the visual layout — keeps
 * the per-loader plugin classes free of recipe-shape logic.
 */
public record RecipeGridLayout(int width, int height, NonNullList<Ingredient> ingredients) {

    public RecipeGridLayout {
        if (ingredients.size() != width * height) {
            throw new IllegalArgumentException(
                    "ingredients size " + ingredients.size() + " != " + width + "*" + height);
        }
    }

    public Ingredient at(int gx, int gy) {
        return ingredients.get(gx + gy * width);
    }

    /** Visual layout for a Supreme-shaped recipe — same shape as the pattern. */
    public static RecipeGridLayout fromShaped(SupremeShapedPattern pattern) {
        return new RecipeGridLayout(pattern.width(), pattern.height(), pattern.ingredients());
    }

    /** Visual layout for a vanilla shaped recipe (3×3 fallback). */
    public static RecipeGridLayout fromVanillaShaped(ShapedRecipe recipe) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        return new RecipeGridLayout(recipe.getWidth(), recipe.getHeight(), ingredients);
    }

    /**
     * Visual layout for a shapeless recipe — N ingredients arranged in a
     * near-square grid, e.g. 9 → 3×3, 16 → 4×4, 6 → 3×2.
     */
    public static RecipeGridLayout fromShapeless(List<Ingredient> ingredients) {
        int n = ingredients.size();
        if (n == 0) {
            return new RecipeGridLayout(0, 0, NonNullList.create());
        }
        int cols = (int) Math.ceil(Math.sqrt(n));
        int rows = (int) Math.ceil((double) n / cols);
        NonNullList<Ingredient> grid = NonNullList.withSize(cols * rows, Ingredient.EMPTY);
        for (int i = 0; i < n; i++) grid.set(i, ingredients.get(i));
        return new RecipeGridLayout(cols, rows, grid);
    }

    /** Convenience: derive layout from any supported recipe type, returns null if unsupported. */
    public static RecipeGridLayout fromRecipe(Recipe<?> recipe) {
        if (recipe instanceof SupremeShapedRecipe shaped) {
            return fromShaped(shaped.pattern());
        }
        if (recipe instanceof SupremeShapelessRecipe) {
            return fromShapeless(recipe.getIngredients());
        }
        if (recipe instanceof ShapedRecipe vanillaShaped) {
            return fromVanillaShaped(vanillaShaped);
        }
        if (recipe instanceof ShapelessRecipe) {
            return fromShapeless(recipe.getIngredients());
        }
        return null;
    }
}
