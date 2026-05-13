package com.supremecrafting.recipe;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic tests for {@link SupremeShapelessRecipe#matches}. Uses
 * {@code null} for the {@code Level} argument — the implementation doesn't
 * dereference it for shapeless matching.
 */
class SupremeShapelessRecipeTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static SupremeShapelessRecipe recipe(Ingredient... ingredients) {
        NonNullList<Ingredient> list = NonNullList.create();
        for (Ingredient i : ingredients) list.add(i);
        return new SupremeShapelessRecipe("", new ItemStack(Items.STICK), list);
    }

    private static CraftingInput inputOf(ItemStack... stacks) {
        // Single-row crafting input — shapeless matcher uses StackedContents
        // which ignores spatial arrangement.
        NonNullList<ItemStack> grid = NonNullList.withSize(stacks.length, ItemStack.EMPTY);
        for (int i = 0; i < stacks.length; i++) grid.set(i, stacks[i]);
        return CraftingInput.of(stacks.length, 1, grid);
    }

    @Test
    void singleIngredientFastPath() {
        SupremeShapelessRecipe r = recipe(Ingredient.of(Items.COBBLESTONE));
        assertTrue(r.matches(inputOf(new ItemStack(Items.COBBLESTONE)), null));
    }

    @Test
    void singleIngredientWrongItemRejected() {
        SupremeShapelessRecipe r = recipe(Ingredient.of(Items.COBBLESTONE));
        assertFalse(r.matches(inputOf(new ItemStack(Items.DIAMOND)), null));
    }

    @Test
    void exactMatch_threeIngredientsInOrder() {
        SupremeShapelessRecipe r = recipe(
                Ingredient.of(Items.COBBLESTONE),
                Ingredient.of(Items.GOLD_INGOT),
                Ingredient.of(Items.DIAMOND));
        CraftingInput input = inputOf(
                new ItemStack(Items.COBBLESTONE),
                new ItemStack(Items.GOLD_INGOT),
                new ItemStack(Items.DIAMOND));
        assertTrue(r.matches(input, null));
    }

    @Test
    void anyPermutationMatches() {
        // Same ingredients in reverse order → still matches.
        SupremeShapelessRecipe r = recipe(
                Ingredient.of(Items.COBBLESTONE),
                Ingredient.of(Items.GOLD_INGOT),
                Ingredient.of(Items.DIAMOND));
        CraftingInput input = inputOf(
                new ItemStack(Items.DIAMOND),
                new ItemStack(Items.COBBLESTONE),
                new ItemStack(Items.GOLD_INGOT));
        assertTrue(r.matches(input, null));
    }

    @Test
    void mismatchedItemRejected() {
        SupremeShapelessRecipe r = recipe(
                Ingredient.of(Items.COBBLESTONE),
                Ingredient.of(Items.GOLD_INGOT));
        CraftingInput input = inputOf(
                new ItemStack(Items.COBBLESTONE),
                new ItemStack(Items.IRON_INGOT)); // gold expected, iron given
        assertFalse(r.matches(input, null));
    }

    @Test
    void wrongCountRejected() {
        // Recipe wants 2, input has 3.
        SupremeShapelessRecipe r = recipe(
                Ingredient.of(Items.COBBLESTONE),
                Ingredient.of(Items.GOLD_INGOT));
        CraftingInput input = inputOf(
                new ItemStack(Items.COBBLESTONE),
                new ItemStack(Items.GOLD_INGOT),
                new ItemStack(Items.GOLD_INGOT));
        assertFalse(r.matches(input, null));
    }

    @Test
    void duplicateIngredientNeedsDuplicateInput() {
        // Recipe needs 2 cobble; input has 1 cobble + 1 diamond → reject.
        SupremeShapelessRecipe r = recipe(
                Ingredient.of(Items.COBBLESTONE),
                Ingredient.of(Items.COBBLESTONE));
        CraftingInput input = inputOf(
                new ItemStack(Items.COBBLESTONE),
                new ItemStack(Items.DIAMOND));
        assertFalse(r.matches(input, null));
    }

    @Test
    void duplicateIngredientWithDuplicateInputMatches() {
        SupremeShapelessRecipe r = recipe(
                Ingredient.of(Items.COBBLESTONE),
                Ingredient.of(Items.COBBLESTONE));
        CraftingInput input = inputOf(
                new ItemStack(Items.COBBLESTONE),
                new ItemStack(Items.COBBLESTONE));
        assertTrue(r.matches(input, null));
    }

    @Test
    void canCraftInDimensions() {
        SupremeShapelessRecipe r = recipe(
                Ingredient.of(Items.COBBLESTONE),
                Ingredient.of(Items.COBBLESTONE),
                Ingredient.of(Items.COBBLESTONE),
                Ingredient.of(Items.COBBLESTONE));
        // 4 ingredients need at least 4 cells.
        assertTrue(r.canCraftInDimensions(2, 2));   // 4 cells, exact fit
        assertTrue(r.canCraftInDimensions(9, 9));   // plenty of room
        assertFalse(r.canCraftInDimensions(2, 1));  // 2 cells, not enough
        assertFalse(r.canCraftInDimensions(1, 3));  // 3 cells, not enough
    }
}
