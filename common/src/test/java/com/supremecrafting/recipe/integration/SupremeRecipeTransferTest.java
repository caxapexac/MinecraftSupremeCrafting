package com.supremecrafting.recipe.integration;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SupremeRecipeTransfer#findMissing(RecipeGridLayout, List)}
 * — the pure variant introduced specifically so we can test the partial-fill
 * quoting logic without spinning up a real {@link net.minecraft.world.entity.player.Player}.
 *
 * <p>The contract: given a layout and a player-inventory item list, return
 * the cell indices the player can't currently fill. One inventory stack of
 * N counts as N reservable items (one per cell).
 */
class SupremeRecipeTransferTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static RecipeGridLayout layout3CobbleRow() {
        // 3-wide × 1-tall, all cobble.
        NonNullList<Ingredient> ings = NonNullList.create();
        for (int i = 0; i < 3; i++) ings.add(Ingredient.of(Items.COBBLESTONE));
        return new RecipeGridLayout(3, 1, ings);
    }

    private static List<ItemStack> inv(ItemStack... items) {
        // Mutable list — findMissing doesn't mutate but the contract takes List<ItemStack>.
        List<ItemStack> list = new ArrayList<>(items.length);
        for (ItemStack s : items) list.add(s);
        return list;
    }

    @Test
    void emptyInventoryAllCellsMissing() {
        RecipeGridLayout layout = layout3CobbleRow();
        var result = SupremeRecipeTransfer.findMissing(layout, inv());
        assertNotNull(result);
        assertEquals(List.of(0, 1, 2), result.cellIndices());
        assertEquals("missing ingredients", result.reason());
    }

    @Test
    void exactlyEnoughItemsReturnsNull() {
        // Single stack of 3 cobble covers all 3 cells.
        var result = SupremeRecipeTransfer.findMissing(
                layout3CobbleRow(),
                inv(new ItemStack(Items.COBBLESTONE, 3)));
        assertNull(result);
    }

    @Test
    void surplusItemsReturnsNull() {
        // Stack of 64 cobble — plenty.
        var result = SupremeRecipeTransfer.findMissing(
                layout3CobbleRow(),
                inv(new ItemStack(Items.COBBLESTONE, 64)));
        assertNull(result);
    }

    @Test
    void onlyTwoItemsLeavesLastCellMissing() {
        var result = SupremeRecipeTransfer.findMissing(
                layout3CobbleRow(),
                inv(new ItemStack(Items.COBBLESTONE, 2)));
        assertNotNull(result);
        assertEquals(List.of(2), result.cellIndices());
    }

    @Test
    void itemsAcrossMultipleSlotsArePooled() {
        // Two stacks of 1 + 2 → 3 cobble total, enough for the 3 cells.
        var result = SupremeRecipeTransfer.findMissing(
                layout3CobbleRow(),
                inv(new ItemStack(Items.COBBLESTONE, 1), new ItemStack(Items.COBBLESTONE, 2)));
        assertNull(result);
    }

    @Test
    void wrongItemDoesNotSatisfyIngredient() {
        // 64 diamonds doesn't satisfy a cobble ingredient.
        var result = SupremeRecipeTransfer.findMissing(
                layout3CobbleRow(),
                inv(new ItemStack(Items.DIAMOND, 64)));
        assertNotNull(result);
        assertEquals(List.of(0, 1, 2), result.cellIndices());
    }

    @Test
    void emptyCellsInLayoutAreSkipped() {
        // 3-wide × 1-tall: cobble, EMPTY, cobble. Only 2 cells need filling.
        NonNullList<Ingredient> ings = NonNullList.create();
        ings.add(Ingredient.of(Items.COBBLESTONE));
        ings.add(Ingredient.EMPTY);
        ings.add(Ingredient.of(Items.COBBLESTONE));
        RecipeGridLayout layout = new RecipeGridLayout(3, 1, ings);

        // Only 1 cobble → cell 0 fills, cell 2 misses.
        var result = SupremeRecipeTransfer.findMissing(
                layout, inv(new ItemStack(Items.COBBLESTONE, 1)));
        assertNotNull(result);
        assertEquals(List.of(2), result.cellIndices());
    }

    @Test
    void mixedIngredientsPartiallyMissing() {
        // Layout: [cobble, gold, diamond]. Player has cobble + gold.
        NonNullList<Ingredient> ings = NonNullList.create();
        ings.add(Ingredient.of(Items.COBBLESTONE));
        ings.add(Ingredient.of(Items.GOLD_INGOT));
        ings.add(Ingredient.of(Items.DIAMOND));
        RecipeGridLayout layout = new RecipeGridLayout(3, 1, ings);

        var result = SupremeRecipeTransfer.findMissing(
                layout,
                inv(new ItemStack(Items.COBBLESTONE), new ItemStack(Items.GOLD_INGOT)));
        assertNotNull(result);
        assertEquals(List.of(2), result.cellIndices()); // diamond cell missing
    }

    @Test
    void reservationPreventsDoubleCountingSameStack() {
        // Layout needs 5 cobble. Player has stack of 3. → 3 fill, last 2 miss.
        NonNullList<Ingredient> ings = NonNullList.create();
        for (int i = 0; i < 5; i++) ings.add(Ingredient.of(Items.COBBLESTONE));
        RecipeGridLayout layout = new RecipeGridLayout(5, 1, ings);

        var result = SupremeRecipeTransfer.findMissing(
                layout, inv(new ItemStack(Items.COBBLESTONE, 3)));
        assertNotNull(result);
        assertEquals(List.of(3, 4), result.cellIndices());
    }
}
