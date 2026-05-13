package com.supremecrafting.table;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

/**
 * Slot subclass that opts out of vanilla's hover-highlight (which is hardcoded
 * to {@code 16x16} in {@link net.minecraft.client.gui.screens.inventory.AbstractContainerScreen}
 * via the static {@code renderSlotHighlight}). The screen draws its own
 * highlight at the current {@code cellSize}.
 */
public class ViewportSlot extends Slot {
    public ViewportSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean isHighlightable() {
        return false;
    }
}
