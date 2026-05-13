package com.supremecrafting.table;

import com.supremecrafting.registry.SCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for the 81x81 Supreme Table.
 *
 * <p>Implements {@link Container} with full size 6561 — the menu wires every
 * grid cell as a vanilla {@link net.minecraft.world.inventory.Slot} and lets
 * vanilla menu sync replicate state to the client. Pan/zoom is purely a
 * client-side rendering concern.
 */
public class SupremeTableBlockEntity extends BlockEntity implements Container, MenuProvider {
    private static final String NBT_INVENTORY = "Inventory";

    private final SupremeTableInventory inventory = new SupremeTableInventory();
    /**
     * Bumped on every mutation to {@link #inventory}. Open menus compare this
     * to their last-seen value to skip redundant recipe re-computation when
     * nothing actually changed — saves rebuilding {@link
     * net.minecraft.world.item.crafting.CraftingInput} (a 6561-cell bbox scan
     * + alloc) every server tick.
     */
    private long modVersion;

    public SupremeTableBlockEntity(BlockPos pos, BlockState state) {
        super(SCBlockEntities.SUPREME_TABLE.get(), pos, state);
    }

    public SupremeTableInventory inventory() {
        return inventory;
    }

    public long modVersion() {
        return modVersion;
    }

    private void bumpModVersion() {
        modVersion++;
    }

    // ---------- Container ----------

    @Override
    public int getContainerSize() {
        return SupremeTableInventory.SIZE;
    }

    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return inventory.get(slot);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        ItemStack current = inventory.get(slot);
        if (current.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        int taken = Math.min(amount, current.getCount());
        ItemStack out = current.copy();
        out.setCount(taken);
        ItemStack remaining = current.copy();
        remaining.setCount(current.getCount() - taken);
        inventory.set(slot, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
        bumpModVersion();
        setChanged();
        return out;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = inventory.get(slot);
        if (!stack.isEmpty()) {
            inventory.set(slot, ItemStack.EMPTY);
            bumpModVersion();
        }
        return stack;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        inventory.set(slot, stack);
        bumpModVersion();
        setChanged();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        inventory.clear();
        bumpModVersion();
        setChanged();
    }

    // ---------- MenuProvider ----------

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container." + com.supremecrafting.SupremeCrafting.MOD_ID + ".supreme_table");
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInv, @NotNull Player player) {
        return new SupremeTableMenu(containerId, playerInv, this);
    }

    // ---------- Persistence ----------

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(NBT_INVENTORY, inventory.save(registries));
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(NBT_INVENTORY, Tag.TAG_LIST)) {
            ListTag list = tag.getList(NBT_INVENTORY, Tag.TAG_COMPOUND);
            inventory.load(list, registries);
        } else {
            inventory.clear();
        }
        bumpModVersion();
    }
}
