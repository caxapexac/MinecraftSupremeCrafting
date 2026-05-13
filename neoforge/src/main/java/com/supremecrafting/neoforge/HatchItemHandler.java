package com.supremecrafting.neoforge;

import com.supremecrafting.furnace.HatchRole;
import com.supremecrafting.furnace.MultiblockRegions;
import com.supremecrafting.furnace.Region;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * NeoForge {@link IItemHandler} that exposes one slot of a {@link Region}'s
 * 3-slot inventory, role-filtered for the kind of hatch it's bound to.
 *
 * <p>Lookup is by UUID every call — handler instances stay valid across
 * disassembly (they just return empty/refuse), and survive Region instance
 * replacement on chunk reloads.
 */
public class HatchItemHandler implements IItemHandler {
    private final ServerLevel level;
    private final UUID regionId;
    private final HatchRole role;

    public HatchItemHandler(ServerLevel level, UUID regionId, HatchRole role) {
        this.level = level;
        this.regionId = regionId;
        this.role = role;
    }

    @Nullable
    private Region region() {
        return MultiblockRegions.get(level).byId(regionId);
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        Region r = region();
        return r == null ? ItemStack.EMPTY : r.items().get(role.slotIndex());
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (role == HatchRole.OUTPUT) return stack;
        if (!isItemValid(slot, stack)) return stack;
        Region r = region();
        if (r == null) return stack;

        NonNullList<ItemStack> items = r.items();
        ItemStack current = items.get(role.slotIndex());
        int max = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
        if (current.isEmpty()) {
            int n = Math.min(stack.getCount(), max);
            if (!simulate) {
                ItemStack placed = stack.copyWithCount(n);
                items.set(role.slotIndex(), placed);
                MultiblockRegions.get(level).setDirty();
            }
            return n == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - n);
        }
        if (!ItemStack.isSameItemSameComponents(current, stack)) return stack;
        int space = max - current.getCount();
        if (space <= 0) return stack;
        int n = Math.min(stack.getCount(), space);
        if (!simulate) {
            current.grow(n);
            MultiblockRegions.get(level).setDirty();
        }
        return n == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - n);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (role != HatchRole.OUTPUT) return ItemStack.EMPTY;
        Region r = region();
        if (r == null) return ItemStack.EMPTY;
        ItemStack current = r.items().get(role.slotIndex());
        if (current.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        int n = Math.min(amount, current.getCount());
        ItemStack out = current.copyWithCount(n);
        if (!simulate) {
            current.shrink(n);
            MultiblockRegions.get(level).setDirty();
        }
        return out;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return switch (role) {
            case INPUT -> true;
            case OUTPUT -> false;
            case FUEL -> AbstractFurnaceBlockEntity.isFuel(stack) || stack.is(Items.BUCKET);
        };
    }
}
