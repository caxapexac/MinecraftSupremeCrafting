package com.supremecrafting.neoforge;

import com.supremecrafting.furnace.MultiblockRegions;
import com.supremecrafting.furnace.Region;
import com.supremecrafting.furnace.SupremeFurnaceHatchBlock;
import com.supremecrafting.registry.SCBlocks;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Registers per-block {@link net.neoforged.neoforge.items.IItemHandler}
 * providers for the three hatch blocks. No BE involved — the provider closes
 * over the region's UUID via {@link MultiblockRegions#findContaining}.
 */
public final class HatchCapabilities {
    private HatchCapabilities() {}

    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.ItemHandler.BLOCK,
                (level, pos, state, be, side) -> {
                    if (!(level instanceof ServerLevel sl)) return null;
                    if (!(state.getBlock() instanceof SupremeFurnaceHatchBlock hatch)) return null;
                    Region r = MultiblockRegions.get(sl).findContaining(pos);
                    if (r == null) return null;
                    return new HatchItemHandler(sl, r.id(), hatch.role());
                },
                SCBlocks.SUPREME_FURNACE_INPUT_HATCH.get(),
                SCBlocks.SUPREME_FURNACE_OUTPUT_HATCH.get(),
                SCBlocks.SUPREME_FURNACE_FUEL_HATCH.get());
    }
}
