package com.supremecrafting.recipe.integration;

import com.mojang.logging.LogUtils;
import com.supremecrafting.table.SupremeTableBlockEntity;
import com.supremecrafting.table.SupremeTableInventory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared "+" auto-fill logic for the recipe-viewer plugins.
 *
 * <p>Partial-fill semantics:
 * <ul>
 *   <li>Items outside the recipe's bbox (placed at grid origin (0, 0)) are
 *       moved back to the player's inventory — strict-size matching needs a
 *       clean bounding box.</li>
 *   <li>Inside the bbox: cells already holding a matching ingredient are
 *       left alone (so re-clicking "+" tops up missing cells without
 *       disturbing what's already there); cells holding the wrong item are
 *       moved back to inv; empty cells try to pull a match from inv.</li>
 *   <li>Cells the recipe wants empty (shaped pattern gaps) are cleared.</li>
 *   <li>If the player can't supply some ingredient, that cell is left empty
 *       — the result slot stays empty until the player tops up and re-fills.</li>
 * </ul>
 *
 * <p>This is naturally anti-cheat: we only consume what's actually in the
 * player's inventory. The viewer-side validation can therefore be relaxed
 * (always enable "+"); the server-side handler just executes.
 */
public final class SupremeRecipeTransfer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private SupremeRecipeTransfer() {}

    /**
     * Diagnostic only — returns the cells the player can't currently supply.
     * Not used as a gate (partial fill is always allowed); kept for tooltips
     * / future UI hints.
     *
     * <p>Thin wrapper around {@link #findMissing(RecipeGridLayout, List)} —
     * the heavy lifting lives there so unit tests don't need a real Player.
     */
    public static MissingIngredients findMissing(Player player, Recipe<CraftingInput> recipe) {
        RecipeGridLayout layout = RecipeGridLayout.fromRecipe(recipe);
        if (layout == null) return new MissingIngredients(List.of(), "unsupported recipe shape");
        return findMissing(layout, player.getInventory().items);
    }

    /**
     * Pure-data variant: given a recipe's grid layout and the player's
     * inventory items (read-only), return which cells can't be filled. One
     * inventory item slot covers one cell — items are reserved 1-by-1 so a
     * single stack of N is enough for N cells.
     *
     * <p>Returns {@code null} if every cell can be filled.
     */
    public static MissingIngredients findMissing(RecipeGridLayout layout, List<ItemStack> playerItems) {
        int[] reserved = new int[playerItems.size()];
        List<Integer> missingCells = new ArrayList<>();
        for (int gy = 0; gy < layout.height(); gy++) {
            for (int gx = 0; gx < layout.width(); gx++) {
                Ingredient ing = layout.at(gx, gy);
                if (ing.isEmpty()) continue;
                if (!reserveOneFromItems(playerItems, ing, reserved)) {
                    missingCells.add(gx + gy * layout.width());
                }
            }
        }
        return missingCells.isEmpty() ? null : new MissingIngredients(missingCells, "missing ingredients");
    }

    /**
     * Server-side: align the table's grid to the recipe layout, taking what
     * we can from the player's inventory and leaving missing cells empty.
     */
    public static void executeTransfer(SupremeTableBlockEntity be, Player player, Recipe<CraftingInput> recipe) {
        RecipeGridLayout layout = RecipeGridLayout.fromRecipe(recipe);
        if (layout == null) return;
        SupremeTableInventory tableInv = be.inventory();
        Inventory inv = player.getInventory();

        int bboxW = layout.width();
        int bboxH = layout.height();
        // Center the recipe in the 81×81 grid. The engine matches/consumes on the
        // bbox regardless of where it sits (CraftingInput auto-shrinks; SupremeResultSlot
        // reads positioned.left/top), so centering is purely cosmetic — looks nicer.
        int offX = (SupremeTableInventory.WIDTH - bboxW) / 2;
        int offY = (SupremeTableInventory.HEIGHT - bboxH) / 2;
        int placed = 0;
        int skippedAlreadyCorrect = 0;
        int leftEmpty = 0;

        // 1. Clear items outside the bbox — strict-size matching demands a clean rectangle.
        for (int gridIdx = 0; gridIdx < SupremeTableInventory.SIZE; gridIdx++) {
            int gx = SupremeTableInventory.xOf(gridIdx);
            int gy = SupremeTableInventory.yOf(gridIdx);
            if (gx >= offX && gx < offX + bboxW && gy >= offY && gy < offY + bboxH) continue;
            ItemStack stack = tableInv.get(gridIdx);
            if (!stack.isEmpty()) {
                returnToInvOrDrop(player, inv, stack.copy());
                be.setItem(gridIdx, ItemStack.EMPTY);
            }
        }

        // 2. Align each cell inside the bbox to the recipe's expected ingredient.
        for (int gy = 0; gy < bboxH; gy++) {
            for (int gx = 0; gx < bboxW; gx++) {
                int gridIdx = SupremeTableInventory.indexOf(gx + offX, gy + offY);
                Ingredient target = layout.at(gx, gy);
                ItemStack current = tableInv.get(gridIdx);
                if (target.isEmpty()) {
                    if (!current.isEmpty()) {
                        returnToInvOrDrop(player, inv, current.copy());
                        be.setItem(gridIdx, ItemStack.EMPTY);
                    }
                    continue;
                }
                if (!current.isEmpty() && target.test(current)) {
                    skippedAlreadyCorrect++;
                    continue; // already correct — leave it (partial-fill top-up case)
                }
                if (!current.isEmpty()) {
                    returnToInvOrDrop(player, inv, current.copy());
                    be.setItem(gridIdx, ItemStack.EMPTY);
                }
                ItemStack picked = takeOneMatching(inv, target);
                if (!picked.isEmpty()) {
                    be.setItem(gridIdx, picked);
                    placed++;
                } else {
                    leftEmpty++;
                }
            }
        }
        LOGGER.debug("[transfer] placed={} skipped(already-ok)={} leftEmpty={} bbox={}x{}",
                placed, skippedAlreadyCorrect, leftEmpty, bboxW, bboxH);
    }

    /** True iff at least one item matching {@code ingredient} is reservable from {@code items}. */
    private static boolean reserveOneFromItems(List<ItemStack> items, Ingredient ingredient, int[] reserved) {
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) continue;
            int available = stack.getCount() - reserved[slot];
            if (available <= 0) continue;
            if (!ingredient.test(stack)) continue;
            reserved[slot]++;
            return true;
        }
        return false;
    }

    /** Remove one matching item from inv, return the picked single-stack. */
    private static ItemStack takeOneMatching(Inventory inv, Ingredient ingredient) {
        for (int slot = 0; slot < inv.items.size(); slot++) {
            ItemStack stack = inv.items.get(slot);
            if (stack.isEmpty()) continue;
            if (!ingredient.test(stack)) continue;
            ItemStack picked = stack.copy();
            picked.setCount(1);
            stack.shrink(1);
            return picked;
        }
        return ItemStack.EMPTY;
    }

    private static void returnToInvOrDrop(Player player, Inventory inv, ItemStack stack) {
        if (!inv.add(stack)) {
            player.drop(stack, false);
        }
    }

    public record MissingIngredients(List<Integer> cellIndices, String reason) {
    }
}
