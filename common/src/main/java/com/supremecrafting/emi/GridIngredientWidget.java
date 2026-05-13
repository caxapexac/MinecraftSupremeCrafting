package com.supremecrafting.emi;

import com.supremecrafting.recipe.integration.RecipeGridLayout;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

/**
 * Renders a {@link RecipeGridLayout} fit-scaled into a square panel. Each
 * non-empty cell draws its {@link EmiIngredient} (which cycles through items
 * for tag ingredients). Hovering a cell returns its tooltip.
 *
 * <p>Per-cell width/height use the same {@code gridLine}-style integer
 * arithmetic as the in-game canvas so neighbouring cells tile pixel-perfectly
 * even at fractional scale.
 */
@Environment(EnvType.CLIENT)
public class GridIngredientWidget extends Widget {
    private final int x;
    private final int y;
    private final int panelW;
    private final int panelH;
    private final RecipeGridLayout layout;
    private final EmiIngredient[] cached;

    public GridIngredientWidget(int x, int y, int panelW, int panelH, RecipeGridLayout layout) {
        this.x = x;
        this.y = y;
        this.panelW = panelW;
        this.panelH = panelH;
        this.layout = layout;
        // Cache EmiIngredient per cell so we don't reallocate every frame.
        int w = layout.width();
        int h = layout.height();
        this.cached = new EmiIngredient[w * h];
        for (int i = 0; i < cached.length; i++) {
            Ingredient ing = layout.ingredients().get(i);
            cached[i] = ing.isEmpty() ? EmiIngredient.of(Ingredient.EMPTY) : EmiIngredient.of(ing);
        }
    }

    @Override
    public Bounds getBounds() {
        return new Bounds(x, y, panelW, panelH);
    }

    private int gridLineX(int gx) {
        return Math.round((float) panelW * gx / layout.width());
    }

    private int gridLineY(int gy) {
        return Math.round((float) panelH * gy / layout.height());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int w = layout.width();
        int h = layout.height();
        for (int gy = 0; gy < h; gy++) {
            int sy = y + gridLineY(gy);
            int cellH = gridLineY(gy + 1) - gridLineY(gy);
            for (int gx = 0; gx < w; gx++) {
                EmiIngredient ing = cached[gx + gy * w];
                if (ing.isEmpty()) continue;
                int sx = x + gridLineX(gx);
                int cellW = gridLineX(gx + 1) - gridLineX(gx);
                float scale = Math.min(cellW, cellH) / 16f;
                g.pose().pushPose();
                g.pose().translate(sx, sy, 0);
                g.pose().scale(scale, scale, 1);
                ing.render(g, 0, 0, delta, EmiIngredient.RENDER_ICON | EmiIngredient.RENDER_AMOUNT);
                g.pose().popPose();
            }
        }
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
        EmiIngredient ing = ingredientAtMouse(mouseX, mouseY);
        if (ing == null || ing.isEmpty()) return List.of();
        return ing.getTooltip();
    }

    private EmiIngredient ingredientAtMouse(int mouseX, int mouseY) {
        int relX = mouseX - x;
        int relY = mouseY - y;
        if (relX < 0 || relX >= panelW || relY < 0 || relY >= panelH) return null;
        int gx = (int) ((long) relX * layout.width() / panelW);
        int gy = (int) ((long) relY * layout.height() / panelH);
        if (gx < 0 || gx >= layout.width() || gy < 0 || gy >= layout.height()) return null;
        return cached[gx + gy * layout.width()];
    }
}
