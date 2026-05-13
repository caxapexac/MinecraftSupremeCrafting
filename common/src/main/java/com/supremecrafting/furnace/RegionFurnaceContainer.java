package com.supremecrafting.furnace;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Adapts a {@link Region}'s 3-slot inventory to {@link Container}, so vanilla
 * {@code FurnaceMenu} can drive it as if it were a vanilla furnace BE.
 *
 * <p>Resolves the region by UUID from the level's
 * {@link MultiblockRegions} on each access — region instances can be replaced
 * during chunk reloads, but the UUID is stable. {@code stillValid} also uses
 * this to disconnect the menu cleanly if the region got disassembled while
 * open.
 */
public class RegionFurnaceContainer implements Container {
    private final ServerLevel level;
    private final UUID regionId;

    public RegionFurnaceContainer(ServerLevel level, UUID regionId) {
        this.level = level;
        this.regionId = regionId;
    }

    private Region region() {
        return MultiblockRegions.get(level).byId(regionId);
    }

    @Override
    public int getContainerSize() {
        return Region.SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        Region r = region();
        if (r == null) return true;
        for (ItemStack s : r.items()) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        Region r = region();
        return r == null ? ItemStack.EMPTY : r.items().get(slot);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        Region r = region();
        if (r == null) return ItemStack.EMPTY;
        ItemStack taken = net.minecraft.world.ContainerHelper.removeItem(r.items(), slot, amount);
        if (!taken.isEmpty()) setChanged();
        return taken;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        Region r = region();
        if (r == null) return ItemStack.EMPTY;
        return net.minecraft.world.ContainerHelper.takeItem(r.items(), slot);
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        Region r = region();
        if (r == null) return;
        r.items().set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override
    public void setChanged() {
        MultiblockRegions.get(level).setDirty();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        // Permissive: the region itself is the source of truth. The terminal
        // can open this menu from any distance / dimension; vanilla closes the
        // menu on dimension change anyway, and walking away is the user's choice.
        return region() != null;
    }

    @Override
    public void clearContent() {
        Region r = region();
        if (r == null) return;
        r.items().clear();
        setChanged();
    }

    /**
     * Mirror the vanilla furnace's per-slot input rules so quick-move ({@code Shift-click})
     * routes items the same way the vanilla furnace menu does.
     */
    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (slot == Region.SLOT_OUTPUT) return false;
        if (slot == Region.SLOT_INPUT) return true;
        // fuel slot — bucket allowed (for wet-sponge → water bucket trick) or
        // burnable fuel
        ItemStack current = getItem(Region.SLOT_FUEL);
        if (AbstractFurnaceBlockEntity.isFuel(stack)) return true;
        return stack.is(net.minecraft.world.item.Items.BUCKET) && !current.is(net.minecraft.world.item.Items.BUCKET);
    }

}
