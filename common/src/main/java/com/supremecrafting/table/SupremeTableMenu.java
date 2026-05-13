package com.supremecrafting.table;

import com.supremecrafting.recipe.SupremeCraftingMatcher;
import com.supremecrafting.recipe.SupremeResultSlot;
import com.supremecrafting.registry.SCMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Full 81×81 menu over the Supreme Table. {@link Slot#index} == grid index for
 * 0..6560; the next 36 slots are the player inventory; the final slot is the
 * result slot.
 */
public class SupremeTableMenu extends AbstractContainerMenu {
    public static final int SLOT_PX = 18;

    /** Screen position (Slot.x-space) of the result slot — fixed in sidebar. */
    public static final int RESULT_SLOT_X = 329;
    public static final int RESULT_SLOT_Y = 118;

    public static final int RESULT_SLOT_INDEX = SupremeTableInventory.SIZE + 36;

    private final BlockPos tablePos;
    private final Container backingContainer;
    private final Inventory playerInv;
    private final ResultContainer resultContainer = new ResultContainer();
    /** Last BE modVersion the result was computed against; -1 forces first compute. */
    private long lastResultModVersion = -1L;

    public SupremeTableMenu(int containerId, Inventory playerInv, SupremeTableBlockEntity be) {
        this(containerId, playerInv, be.getBlockPos(), be);
    }

    public SupremeTableMenu(int containerId, Inventory playerInv, BlockPos pos, Container container) {
        super(SCMenus.SUPREME_TABLE_MENU.get(), containerId);
        this.tablePos = pos;
        this.backingContainer = container;
        this.playerInv = playerInv;

        addGridSlots();
        addPlayerInventory(playerInv);
        addResultSlot(playerInv.player);

        container.startOpen(playerInv.player);
    }

    public static SupremeTableMenu fromNetwork(int windowId, Inventory playerInv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Container container = resolveClientContainer(playerInv.player.level(), pos);
        return new SupremeTableMenu(windowId, playerInv, pos, container);
    }

    private static Container resolveClientContainer(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SupremeTableBlockEntity table) {
            return table;
        }
        return new SimpleContainer(SupremeTableInventory.SIZE);
    }

    private void addGridSlots() {
        for (int i = 0; i < SupremeTableInventory.SIZE; i++) {
            addSlot(new ViewportSlot(backingContainer, i, 0, 0));
        }
    }

    private void addPlayerInventory(Inventory playerInv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 0, 0));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 0, 0));
        }
    }

    private void addResultSlot(Player player) {
        if (backingContainer instanceof SupremeTableBlockEntity be) {
            addSlot(new SupremeResultSlot(player, be, resultContainer, 0, RESULT_SLOT_X, RESULT_SLOT_Y));
        } else {
            // Defensive client fallback — non-functional crafting but slot exists for layout.
            addSlot(new Slot(resultContainer, 0, RESULT_SLOT_X, RESULT_SLOT_Y));
        }
    }

    public BlockPos tablePos() {
        return tablePos;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return backingContainer.stillValid(player);
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        backingContainer.stopOpen(player);
    }

    /**
     * Server-side: recompute the result only when the BE's mod version has
     * changed since last tick. Skipping the recompute when nothing changed
     * matters in big modpacks — building the {@code CraftingInput} alone
     * scans all 6561 cells.
     */
    @Override
    public void broadcastChanges() {
        Player player = playerInv.player;
        Level level = player.level();
        if (!level.isClientSide && backingContainer instanceof SupremeTableBlockEntity be) {
            long current = be.modVersion();
            if (current != lastResultModVersion) {
                lastResultModVersion = current;
                recomputeResult(level, be);
            }
        }
        super.broadcastChanges();
    }

    private void recomputeResult(Level level, SupremeTableBlockEntity be) {
        CraftingInput input = SupremeCraftingMatcher.buildInput(be.inventory());
        Optional<Recipe<CraftingInput>> match =
                SupremeCraftingMatcher.findRecipe(input, level.getRecipeManager(), level);
        ItemStack result = match
                .map(r -> r.assemble(input, level.registryAccess()))
                .orElse(ItemStack.EMPTY);
        if (!ItemStack.matches(resultContainer.getItem(0), result)) {
            resultContainer.setItem(0, result);
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(index);
        if (!source.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = source.getItem();
        ItemStack result = sourceStack.copy();
        int gridEnd = SupremeTableInventory.SIZE;
        int playerInvEnd = gridEnd + 36;

        if (index == RESULT_SLOT_INDEX) {
            // Result → player inv. Vanilla pattern: try to move, then onTake.
            if (!moveItemStackTo(sourceStack, gridEnd, playerInvEnd, true)) return ItemStack.EMPTY;
            source.onQuickCraft(sourceStack, result);
            source.onTake(player, sourceStack);
        } else if (index < gridEnd) {
            // Grid → player inv.
            if (!moveItemStackTo(sourceStack, gridEnd, playerInvEnd, true)) return ItemStack.EMPTY;
        } else {
            // Player inv → grid.
            if (!moveItemStackTo(sourceStack, 0, gridEnd, false)) return ItemStack.EMPTY;
        }

        if (sourceStack.isEmpty()) {
            source.set(ItemStack.EMPTY);
        } else {
            source.setChanged();
        }
        return result;
    }
}
