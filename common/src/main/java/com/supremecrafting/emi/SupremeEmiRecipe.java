package com.supremecrafting.emi;

import com.supremecrafting.recipe.integration.RecipeGridLayout;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

/**
 * EMI recipe wrapper for shaped + shapeless supreme recipes. Layout is
 * fit-scaled into a {@link #GRID_PANEL_SIZE}×{@link #GRID_PANEL_SIZE} square,
 * followed by an arrow and a result slot. No pan/zoom inside the panel —
 * EMI's recipe screen consumes scroll for paging; if a recipe is too dense
 * to read at fit-scale, the player can hover individual cells for tooltips.
 */
@Environment(EnvType.CLIENT)
public class SupremeEmiRecipe extends BasicEmiRecipe {
    /** Side of the input grid square in pixels. */
    public static final int GRID_PANEL_SIZE = 144;
    public static final int ARROW_WIDTH = 24;
    public static final int RESULT_SLOT_SIZE = 18;
    public static final int H_GAP = 8;

    public static final int PANEL_WIDTH = GRID_PANEL_SIZE + H_GAP + ARROW_WIDTH + H_GAP + RESULT_SLOT_SIZE;
    public static final int PANEL_HEIGHT = GRID_PANEL_SIZE;

    private final RecipeGridLayout layout;

    public RecipeGridLayout layout() { return layout; }

    private SupremeEmiRecipe(EmiRecipeCategory category, RecipeHolder<?> holder,
                             RecipeGridLayout layout, ItemStack result) {
        super(category, holder.id(), PANEL_WIDTH, PANEL_HEIGHT);
        this.layout = layout;

        for (Ingredient ing : layout.ingredients()) {
            if (!ing.isEmpty()) {
                inputs.add(EmiIngredient.of(ing));
            }
        }
        outputs.add(EmiStack.of(result));
    }

    /** Returns null if recipe shape can't be derived (forward-compat with future types). */
    public static @Nullable SupremeEmiRecipe create(EmiRecipeCategory category, RecipeHolder<?> holder) {
        Recipe<?> recipe = holder.value();
        RecipeGridLayout layout = RecipeGridLayout.fromRecipe(recipe);
        if (layout == null) return null;
        // getResultItem may need a HolderLookup.Provider for some recipes;
        // both shaped and shapeless return their stored result without using it.
        ItemStack result = recipe.getResultItem(HolderLookup.Provider.create(java.util.stream.Stream.empty()));
        if (result.isEmpty()) return null;
        return new SupremeEmiRecipe(category, holder, layout, result);
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.add(new GridIngredientWidget(0, 0, GRID_PANEL_SIZE, GRID_PANEL_SIZE, layout));
        int arrowX = GRID_PANEL_SIZE + H_GAP;
        int centerY = (GRID_PANEL_SIZE - 17) / 2;
        widgets.addFillingArrow(arrowX, centerY, 1000);
        int slotX = arrowX + ARROW_WIDTH + H_GAP;
        int slotY = (GRID_PANEL_SIZE - RESULT_SLOT_SIZE) / 2;
        widgets.addSlot(outputs.get(0), slotX, slotY).recipeContext(this);
    }
}
