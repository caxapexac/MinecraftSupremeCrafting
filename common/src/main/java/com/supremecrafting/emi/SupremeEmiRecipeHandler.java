package com.supremecrafting.emi;

import com.supremecrafting.net.C2STransferRecipePacket;
import com.supremecrafting.recipe.integration.RecipeGridLayout;
import com.supremecrafting.table.SupremeTableInventory;
import com.supremecrafting.table.SupremeTableMenu;
import dev.architectury.networking.NetworkManager;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * EMI "+" auto-fill handler. Reuses EMI's built-in {@link StandardRecipeHandler}
 * fill protocol for missing-ingredient highlighting; the actual fill is sent
 * via our own packet so the server can do partial fills.
 */
@Environment(EnvType.CLIENT)
public class SupremeEmiRecipeHandler implements StandardRecipeHandler<SupremeTableMenu> {

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe instanceof SupremeEmiRecipe;
    }

    /**
     * Always allow + when the recipe is supported — server does partial fill.
     * EMI's missing-ingredient highlight (red overlay) still fires through
     * the default {@link StandardRecipeHandler#render} path independently.
     */
    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<SupremeTableMenu> context) {
        return supportsRecipe(recipe);
    }

    /**
     * Override the default {@link StandardRecipeHandler#craft} which uses
     * EMI's built-in filler — that path returns null and bails when the
     * player can't supply every ingredient, blocking partial fill. We send
     * our own C2S packet instead; the server does the partial work.
     */
    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<SupremeTableMenu> context) {
        if (!(recipe instanceof SupremeEmiRecipe sr)) return false;
        SupremeTableMenu menu = context.getScreenHandler();
        if (sr.getId() == null) return false;
        NetworkManager.sendToServer(new C2STransferRecipePacket(menu.tablePos(), sr.getId()));
        return true;
    }

    @Override
    public List<Slot> getInputSources(SupremeTableMenu menu) {
        List<Slot> sources = new ArrayList<>(36);
        // Player inventory slots are added after the 6561 grid slots.
        for (int i = SupremeTableInventory.SIZE; i < SupremeTableInventory.SIZE + 36; i++) {
            sources.add(menu.slots.get(i));
        }
        return sources;
    }

    @Override
    public List<Slot> getCraftingSlots(SupremeTableMenu menu) {
        // Default impl required when no recipe context is given (rare path).
        // Return all grid slots; the recipe-specific overload below is what
        // EMI actually uses for filling.
        List<Slot> slots = new ArrayList<>(SupremeTableInventory.SIZE);
        for (int i = 0; i < SupremeTableInventory.SIZE; i++) {
            slots.add(menu.slots.get(i));
        }
        return slots;
    }

    @Override
    public List<Slot> getCraftingSlots(EmiRecipe recipe, SupremeTableMenu menu) {
        if (!(recipe instanceof SupremeEmiRecipe sr)) return List.of();
        RecipeGridLayout layout = sr.layout();
        List<Slot> slots = new ArrayList<>();
        // Iterate row-major over the bbox; only non-empty cells correspond to
        // inputs (matching SupremeEmiRecipe's filtered inputs population).
        // Place at grid origin (0, 0).
        for (int gy = 0; gy < layout.height(); gy++) {
            for (int gx = 0; gx < layout.width(); gx++) {
                if (!layout.at(gx, gy).isEmpty()) {
                    int gridIndex = SupremeTableInventory.indexOf(gx, gy);
                    slots.add(menu.slots.get(gridIndex));
                }
            }
        }
        return slots;
    }

    @Override
    public @Nullable Slot getOutputSlot(SupremeTableMenu menu) {
        return menu.slots.get(SupremeTableMenu.RESULT_SLOT_INDEX);
    }
}
