package com.supremecrafting.table;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * Storage backend for the 81x81 Supreme Table.
 *
 * <p>Vanilla {@link net.minecraft.world.Container} and the default content-sync
 * packet assume slot counts that fit comfortably in a byte. 6561 doesn't, so
 * this class deliberately does not implement {@code Container} — the menu
 * (1d) will expose only a viewport's worth of vanilla {@code Slot} objects
 * that read/write through this storage by stable {@code int} index.
 *
 * <p>Serialization is sparse: empty stacks are not written. Slot indices are
 * stored as {@code int}s.
 */
public final class SupremeTableInventory {
    public static final int WIDTH = 81;
    public static final int HEIGHT = 81;
    public static final int SIZE = WIDTH * HEIGHT; // 6561

    private static final String NBT_SLOT = "Slot";
    private static final String NBT_ITEM = "Item";

    private final NonNullList<ItemStack> stacks = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public int size() {
        return SIZE;
    }

    public ItemStack get(int index) {
        checkIndex(index);
        return stacks.get(index);
    }

    public void set(int index, ItemStack stack) {
        checkIndex(index);
        stacks.set(index, stack == null ? ItemStack.EMPTY : stack);
    }

    public ItemStack get(int x, int y) {
        return get(indexOf(x, y));
    }

    public void set(int x, int y, ItemStack stack) {
        set(indexOf(x, y), stack);
    }

    public boolean isEmpty() {
        for (ItemStack s : stacks) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

    public int countNonEmpty() {
        int n = 0;
        for (ItemStack s : stacks) {
            if (!s.isEmpty()) n++;
        }
        return n;
    }

    public void clear() {
        for (int i = 0; i < SIZE; i++) {
            stacks.set(i, ItemStack.EMPTY);
        }
    }

    public static int indexOf(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
            throw new IndexOutOfBoundsException("coords out of range: x=" + x + " y=" + y);
        }
        return x + y * WIDTH;
    }

    public static int xOf(int index) {
        checkIndex(index);
        return index % WIDTH;
    }

    public static int yOf(int index) {
        checkIndex(index);
        return index / WIDTH;
    }

    private static void checkIndex(int index) {
        if (index < 0 || index >= SIZE) {
            throw new IndexOutOfBoundsException("slot index out of range: " + index);
        }
    }

    /**
     * Sparse save: only non-empty stacks are written, each tagged with its
     * {@code int} slot index. Stable across reorderings of the underlying list.
     */
    public ListTag save(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (int i = 0; i < SIZE; i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt(NBT_SLOT, i);
            entry.put(NBT_ITEM, stack.save(registries));
            list.add(entry);
        }
        return list;
    }

    /**
     * Loads from a {@link ListTag} produced by {@link #save}. Clears existing
     * contents first. Out-of-range slot indices in the tag are skipped (logged
     * as ignored at the call site if desired) — never thrown — to allow for
     * future shrinking of the grid without corrupting saves.
     */
    public void load(ListTag list, HolderLookup.Provider registries) {
        clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getInt(NBT_SLOT);
            if (slot < 0 || slot >= SIZE) continue;
            stacks.set(slot, ItemStack.parseOptional(registries, entry.getCompound(NBT_ITEM)));
        }
    }

    /**
     * Backing list of all 6561 stacks. Caller must not mutate returned list
     * directly — use {@link #set} instead. Exposed for the recipe matcher,
     * which feeds the list to {@code CraftingInput.of(WIDTH, HEIGHT, items)}.
     */
    public NonNullList<ItemStack> items() {
        return stacks;
    }

    /** Test-only alias kept for clarity in existing tests. */
    NonNullList<ItemStack> rawStacksForTesting() {
        return stacks;
    }

    static {
        // Sanity: NBT_ITEM constant is referenced from load to keep symbols consistent.
        if (Tag.TAG_COMPOUND == 0) throw new IllegalStateException("NBT API changed");
    }
}
