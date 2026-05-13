package com.supremecrafting.item;

import net.minecraft.world.item.Item;

/**
 * Form/inspect tool for Supreme multiblocks. The block handles the actual
 * interaction in its {@code useItemOn} — this item is just the "key" the
 * casing checks for. See
 * {@link com.supremecrafting.furnace.SupremeFurnaceCasingBlock#useItemOn}.
 */
public class SupremeWrenchItem extends Item {
    public SupremeWrenchItem(Properties properties) {
        super(properties);
    }
}
