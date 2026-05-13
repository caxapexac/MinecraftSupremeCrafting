package com.supremecrafting.furnace;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.ContainerData;

import java.util.UUID;

/**
 * Adapts a {@link Region} to vanilla's 4-int {@link ContainerData} contract
 * used by {@code AbstractFurnaceMenu} to drive the flame + progress widgets.
 * Indices: {@code [0] litTime, [1] litDuration, [2] cookingProgress,
 * [3] cookingTotalTime}.
 *
 * <p>We only have one piece of fuel state (the bank). Fixing
 * {@code litDuration} to {@link FurnaceTick#FUEL_PER_ITEM} makes the flame
 * widget read as "progress to next smelt" — clamping {@code litTime} to the
 * same value prevents overflow past 100%. The cook arrow is hidden (smelts
 * are instantaneous so there's no progress to show).
 */
public class RegionFurnaceData implements ContainerData {
    private final ServerLevel level;
    private final UUID regionId;

    public RegionFurnaceData(ServerLevel level, UUID regionId) {
        this.level = level;
        this.regionId = regionId;
    }

    @Override
    public int get(int i) {
        Region r = MultiblockRegions.get(level).byId(regionId);
        if (r == null) return 0;
        return switch (i) {
            case 0 -> Math.min(r.litTime(), FurnaceTick.FUEL_PER_ITEM);
            case 1 -> FurnaceTick.FUEL_PER_ITEM;
            case 2 -> 0;
            case 3 -> 1;
            default -> 0;
        };
    }

    @Override
    public void set(int i, int v) {
        // server-authoritative — ignore
    }

    @Override
    public int getCount() {
        return 4;
    }
}
