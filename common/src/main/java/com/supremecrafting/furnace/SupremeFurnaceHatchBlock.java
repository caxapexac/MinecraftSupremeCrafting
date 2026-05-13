package com.supremecrafting.furnace;

import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Casing variant that exposes an {@link HatchRole}-filtered item handler to
 * automation. Extends {@link SupremeFurnaceCasingBlock} so it inherits form/
 * disassemble/menu-open behavior — externally it's just another shell block.
 * Capability registration (NeoForge-side) reads {@link #role()} to wire the
 * right slot routing.
 */
public class SupremeFurnaceHatchBlock extends SupremeFurnaceCasingBlock {
    private final HatchRole role;

    public SupremeFurnaceHatchBlock(BlockBehaviour.Properties properties, HatchRole role) {
        super(properties);
        this.role = role;
    }

    public HatchRole role() {
        return role;
    }
}
