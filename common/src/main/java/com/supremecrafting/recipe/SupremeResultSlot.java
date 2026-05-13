package com.supremecrafting.recipe;

import com.supremecrafting.table.SupremeTableBlockEntity;
import com.supremecrafting.table.SupremeTableInventory;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Result slot for the Supreme Table menu. Mirrors vanilla {@link
 * net.minecraft.world.inventory.ResultSlot} but reads from / writes to the
 * Supreme Table's 81×81 grid instead of a 3×3 {@code CraftingContainer}.
 *
 * <p>{@link #onTake} consumes one of every ingredient that participated in
 * the recipe (using the {@code CraftingInput.Positioned} bbox to find the
 * exact cells), and re-deposits any crafting-remaining items (e.g. empty
 * bucket from water bucket).
 */
public class SupremeResultSlot extends Slot {
    private final Player player;
    private final SupremeTableBlockEntity table;
    private int removeCount;

    public SupremeResultSlot(Player player, SupremeTableBlockEntity table,
                             ResultContainer container, int slot, int x, int y) {
        super(container, slot, x, y);
        this.player = player;
        this.table = table;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFake() {
        return true;
    }

    @Override
    public @NotNull ItemStack remove(int amount) {
        if (this.hasItem()) {
            this.removeCount += Math.min(amount, this.getItem().getCount());
        }
        return super.remove(amount);
    }

    @Override
    protected void onQuickCraft(@NotNull ItemStack stack, int amount) {
        this.removeCount += amount;
    }

    @Override
    protected void checkTakeAchievements(@NotNull ItemStack stack) {
        if (removeCount > 0) {
            stack.onCraftedBy(player.level(), player, removeCount);
        }
        removeCount = 0;
    }

    @Override
    public void onTake(@NotNull Player player, @NotNull ItemStack craftedStack) {
        // Skip client-side predictive consumption: ServerboundContainerClickPacket
        // caps changed-slots at 128 entries, but our recipes can consume hundreds
        // of cells in one click. Server is authoritative; vanilla menu sync
        // catches the client up on the next tick.
        if (player.level().isClientSide) return;

        checkTakeAchievements(craftedStack);

        SupremeTableInventory inv = table.inventory();
        CraftingInput.Positioned positioned = CraftingInput.ofPositioned(
                SupremeTableInventory.WIDTH,
                SupremeTableInventory.HEIGHT,
                inv.items());
        CraftingInput input = positioned.input();
        if (input.isEmpty()) return;

        int left = positioned.left();
        int top = positioned.top();
        // Use the same matcher path as the menu so we get remaining items from the
        // recipe that actually matched (could be ours or vanilla 3×3).
        Optional<Recipe<CraftingInput>> match = SupremeCraftingMatcher.findRecipe(
                input, player.level().getRecipeManager(), player.level());
        NonNullList<ItemStack> remaining = match
                .map(r -> r.getRemainingItems(input))
                .orElse(NonNullList.withSize(input.size(), ItemStack.EMPTY));

        for (int row = 0; row < input.height(); row++) {
            for (int col = 0; col < input.width(); col++) {
                int gridIdx = SupremeTableInventory.indexOf(left + col, top + row);
                ItemStack remainder = remaining.get(col + row * input.width());

                ItemStack cell = inv.get(gridIdx);
                if (!cell.isEmpty()) {
                    ItemStack shrunk = cell.copy();
                    shrunk.shrink(1);
                    table.setItem(gridIdx, shrunk);
                    cell = inv.get(gridIdx);
                }

                if (!remainder.isEmpty()) {
                    ItemStack rem = remainder.copy();
                    if (cell.isEmpty()) {
                        table.setItem(gridIdx, rem);
                    } else if (ItemStack.isSameItemSameComponents(cell, rem)) {
                        rem.grow(cell.getCount());
                        table.setItem(gridIdx, rem);
                    } else if (!player.getInventory().add(rem)) {
                        player.drop(rem, false);
                    }
                }
            }
        }
    }
}
