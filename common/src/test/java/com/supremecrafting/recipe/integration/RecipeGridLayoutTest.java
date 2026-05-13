package com.supremecrafting.recipe.integration;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic tests for {@link RecipeGridLayout}. Covers the constructor
 * invariant, indexing, the {@code fromShapeless} near-square sizing math,
 * the recipe-type dispatcher, and unsupported-recipe fallthrough.
 */
class RecipeGridLayoutTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static NonNullList<Ingredient> ings(Ingredient... ingredients) {
        NonNullList<Ingredient> list = NonNullList.create();
        for (Ingredient i : ingredients) list.add(i);
        return list;
    }

    private static Ingredient cobble() { return Ingredient.of(Items.COBBLESTONE); }

    @Test
    void constructorRejectsSizeMismatch() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new RecipeGridLayout(3, 3, ings(cobble())));
        assertTrue(ex.getMessage().contains("!= 3*3"));
    }

    @Test
    void atReturnsRowMajor() {
        Ingredient c = cobble();
        Ingredient g = Ingredient.of(Items.GOLD_INGOT);
        // 2-wide × 3-tall grid: row 0 = [c, g], row 1 = [g, c], row 2 = [c, c]
        NonNullList<Ingredient> grid = ings(c, g, g, c, c, c);
        RecipeGridLayout layout = new RecipeGridLayout(2, 3, grid);
        assertEquals(c, layout.at(0, 0));
        assertEquals(g, layout.at(1, 0));
        assertEquals(g, layout.at(0, 1));
        assertEquals(c, layout.at(1, 1));
        assertEquals(c, layout.at(0, 2));
        assertEquals(c, layout.at(1, 2));
    }

    @Test
    void fromShapelessSingleIngredient() {
        RecipeGridLayout l = RecipeGridLayout.fromShapeless(List.of(cobble()));
        assertEquals(1, l.width());
        assertEquals(1, l.height());
    }

    @Test
    void fromShapelessFourIngredientsIs2x2() {
        RecipeGridLayout l = RecipeGridLayout.fromShapeless(
                List.of(cobble(), cobble(), cobble(), cobble()));
        assertEquals(2, l.width());
        assertEquals(2, l.height());
    }

    @Test
    void fromShapelessSixIngredientsIs3x2() {
        // 6 → ceil(sqrt(6))=3 cols, ceil(6/3)=2 rows
        RecipeGridLayout l = RecipeGridLayout.fromShapeless(
                List.of(cobble(), cobble(), cobble(), cobble(), cobble(), cobble()));
        assertEquals(3, l.width());
        assertEquals(2, l.height());
    }

    @Test
    void fromShapelessNineIngredientsIs3x3() {
        List<Ingredient> nine = java.util.Collections.nCopies(9, cobble());
        RecipeGridLayout l = RecipeGridLayout.fromShapeless(nine);
        assertEquals(3, l.width());
        assertEquals(3, l.height());
    }

    @Test
    void fromShapelessTenIngredientsIs4x3WithTrailingEmpty() {
        // 10 → ceil(sqrt(10))=4 cols, ceil(10/4)=3 rows. Last 2 cells empty.
        List<Ingredient> ten = java.util.Collections.nCopies(10, cobble());
        RecipeGridLayout l = RecipeGridLayout.fromShapeless(ten);
        assertEquals(4, l.width());
        assertEquals(3, l.height());
        // First 10 cells populated, last 2 empty.
        for (int i = 0; i < 10; i++) {
            assertFalse(l.ingredients().get(i).isEmpty(), "cell " + i + " should be populated");
        }
        assertTrue(l.ingredients().get(10).isEmpty());
        assertTrue(l.ingredients().get(11).isEmpty());
    }

    @Test
    void fromShapelessEmptyListReturnsZeroSized() {
        RecipeGridLayout l = RecipeGridLayout.fromShapeless(List.of());
        assertEquals(0, l.width());
        assertEquals(0, l.height());
    }

    @Test
    void fromRecipeDispatchesVanillaShapeless() {
        // Vanilla shapeless recipe → routes through fromShapeless().
        net.minecraft.world.item.crafting.Recipe<?> vanilla = new ShapelessRecipe(
                "", net.minecraft.world.item.crafting.CraftingBookCategory.MISC,
                new ItemStack(Items.STICK), ings(cobble(), cobble(), cobble(), cobble()));
        RecipeGridLayout l = RecipeGridLayout.fromRecipe(vanilla);
        assertNotNull(l);
        assertEquals(2, l.width());
        assertEquals(2, l.height());
    }

    @Test
    void fromRecipeReturnsNullForUnsupportedRecipe() {
        // Anonymous Recipe<?> not matching any branch → null. We can't
        // synthesize one without a vanilla MC RecipeType, so verify via a
        // mock object that doesn't subclass any handled type. Simplest:
        // a lambda-style anonymous class.
        net.minecraft.world.item.crafting.Recipe<?> unknown =
                new UnknownRecipeStub();
        assertNull(RecipeGridLayout.fromRecipe(unknown));
    }

    /** Bare-minimum Recipe<?> impl that isn't any of the handled subtypes. */
    private static class UnknownRecipeStub implements net.minecraft.world.item.crafting.Recipe<net.minecraft.world.item.crafting.CraftingInput> {
        @Override public boolean matches(net.minecraft.world.item.crafting.CraftingInput in, net.minecraft.world.level.Level l) { return false; }
        @Override public ItemStack assemble(net.minecraft.world.item.crafting.CraftingInput in, net.minecraft.core.HolderLookup.Provider p) { return ItemStack.EMPTY; }
        @Override public boolean canCraftInDimensions(int w, int h) { return false; }
        @Override public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider p) { return ItemStack.EMPTY; }
        @Override public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer() { return null; }
        @Override public net.minecraft.world.item.crafting.RecipeType<?> getType() { return null; }
    }
}
