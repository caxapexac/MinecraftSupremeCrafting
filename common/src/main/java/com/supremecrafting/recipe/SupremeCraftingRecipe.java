package com.supremecrafting.recipe;

import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;

/**
 * Marker interface for recipes crafted at the Supreme Table. Re-uses vanilla
 * {@link CraftingInput} as the input type so vanilla recipes (in step 2d) can
 * match the same input without a custom adapter.
 */
public interface SupremeCraftingRecipe extends Recipe<CraftingInput> {
}
