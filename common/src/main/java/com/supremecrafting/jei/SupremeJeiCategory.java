package com.supremecrafting.jei;

import com.supremecrafting.SupremeCrafting;
import com.supremecrafting.recipe.SupremeCraftingRecipe;
import com.supremecrafting.recipe.integration.RecipeGridLayout;
import com.supremecrafting.registry.SCBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * JEI category for Supreme Crafting recipes.
 *
 * <p>Layout mirrors EMI: input grid (fit-scaled into a 144×144 square) +
 * arrow + result slot. The grid is drawn manually in {@link #draw} so we
 * don't pay the cost of 6561 JEI slot widgets — instead we
 * {@link IRecipeLayoutBuilder#addInvisibleIngredients add invisible
 * ingredients} for searchability and handle hover tooltips ourselves.
 */
@Environment(EnvType.CLIENT)
public class SupremeJeiCategory implements IRecipeCategory<RecipeHolder<SupremeCraftingRecipe>> {
    public static final int GRID_PANEL_SIZE = 144;
    public static final int ARROW_WIDTH = 24;
    public static final int RESULT_SLOT_SIZE = 18;
    public static final int H_GAP = 8;

    public static final int PANEL_WIDTH = GRID_PANEL_SIZE + H_GAP + ARROW_WIDTH + H_GAP + RESULT_SLOT_SIZE;
    public static final int PANEL_HEIGHT = GRID_PANEL_SIZE;

    private static final int RESULT_SLOT_X = GRID_PANEL_SIZE + H_GAP + ARROW_WIDTH + H_GAP;
    private static final int RESULT_SLOT_Y = (GRID_PANEL_SIZE - RESULT_SLOT_SIZE) / 2;
    private static final int ARROW_X = GRID_PANEL_SIZE + H_GAP;
    private static final int ARROW_Y = (GRID_PANEL_SIZE - 17) / 2;

    private final IDrawable icon;
    private final IDrawable arrow;

    public SupremeJeiCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(new ItemStack(SCBlocks.SUPREME_TABLE_ITEM.get()));
        this.arrow = helper.createDrawable(
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/gui/container/anvil.png"),
                0, 0, 0, 0); // placeholder — overridden by drawing the JEI built-in arrow below
    }

    @Override
    public RecipeType<RecipeHolder<SupremeCraftingRecipe>> getRecipeType() {
        return SupremeJeiPlugin.RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.category." + SupremeCrafting.MOD_ID + ".supreme_crafting");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return PANEL_WIDTH;
    }

    @Override
    public int getHeight() {
        return PANEL_HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<SupremeCraftingRecipe> holder, IFocusGroup focuses) {
        SupremeCraftingRecipe recipe = holder.value();
        // Output: visible slot in the sidebar.
        builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_SLOT_X, RESULT_SLOT_Y)
                .addItemStack(recipe.getResultItem(net.minecraft.core.HolderLookup.Provider.create(java.util.stream.Stream.empty())));

        // Inputs: register every ingredient as searchable but invisible —
        // we draw the grid ourselves in draw().
        var acceptor = builder.addInvisibleIngredients(RecipeIngredientRole.INPUT);
        for (Ingredient ing : recipe.getIngredients()) {
            if (!ing.isEmpty()) {
                acceptor.addIngredients(ing);
            }
        }
    }

    @Override
    public void draw(RecipeHolder<SupremeCraftingRecipe> holder, IRecipeSlotsView slotsView,
                     GuiGraphics g, double mouseX, double mouseY) {
        RecipeGridLayout layout = RecipeGridLayout.fromRecipe(holder.value());
        if (layout == null) return;
        renderGrid(g, layout, 0, 0, GRID_PANEL_SIZE, GRID_PANEL_SIZE);
        renderArrow(g, ARROW_X, ARROW_Y);
    }

    private static void renderGrid(GuiGraphics g, RecipeGridLayout layout, int x, int y, int panelW, int panelH) {
        int w = layout.width();
        int h = layout.height();
        for (int gy = 0; gy < h; gy++) {
            int sy = y + Math.round((float) panelH * gy / h);
            int cellH = Math.round((float) panelH * (gy + 1) / h) - Math.round((float) panelH * gy / h);
            for (int gx = 0; gx < w; gx++) {
                Ingredient ing = layout.at(gx, gy);
                if (ing.isEmpty()) continue;
                ItemStack[] items = ing.getItems();
                if (items.length == 0) continue;
                int sx = x + Math.round((float) panelW * gx / w);
                int cellW = Math.round((float) panelW * (gx + 1) / w) - Math.round((float) panelW * gx / w);
                float scale = Math.min(cellW, cellH) / 16f;
                // Cycle through tag entries so multi-item ingredients show all options.
                ItemStack stack = items[(int) ((System.currentTimeMillis() / 1000) % items.length)];
                g.pose().pushPose();
                g.pose().translate(sx, sy, 0);
                g.pose().scale(scale, scale, 1);
                g.renderItem(stack, 0, 0);
                g.pose().popPose();
            }
        }
    }

    private static void renderArrow(GuiGraphics g, int x, int y) {
        // Simple right-pointing arrow drawn with rectangles.
        int color = 0xFF8B8B8B;
        g.fill(x, y + 7, x + ARROW_WIDTH - 6, y + 10, color);
        for (int i = 0; i < 6; i++) {
            g.fill(x + ARROW_WIDTH - 6 - i, y + 4 + i, x + ARROW_WIDTH - 5 - i, y + 13 - i, color);
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, RecipeHolder<SupremeCraftingRecipe> holder,
                           IRecipeSlotsView slotsView, double mouseX, double mouseY) {
        RecipeGridLayout layout = RecipeGridLayout.fromRecipe(holder.value());
        if (layout == null) return;
        if (mouseX < 0 || mouseY < 0 || mouseX >= GRID_PANEL_SIZE || mouseY >= GRID_PANEL_SIZE) return;
        int gx = (int) (mouseX * layout.width() / GRID_PANEL_SIZE);
        int gy = (int) (mouseY * layout.height() / GRID_PANEL_SIZE);
        if (gx < 0 || gx >= layout.width() || gy < 0 || gy >= layout.height()) return;
        Ingredient ing = layout.at(gx, gy);
        if (ing.isEmpty()) return;
        ItemStack[] items = ing.getItems();
        if (items.length == 0) return;
        ItemStack stack = items[(int) ((System.currentTimeMillis() / 1000) % items.length)];
        tooltip.add(stack.getHoverName());
    }
}
