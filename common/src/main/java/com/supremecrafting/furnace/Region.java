package com.supremecrafting.furnace;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Objects;
import java.util.UUID;

/**
 * A formed Supreme Furnace.
 *
 * <p>Identity + geometry are immutable; smelt state is one int ({@link #litTime},
 * a fuel-tick reservoir) plus the 3-slot inventory. The layout
 * {@code 0 = input, 1 = fuel, 2 = output} matches vanilla so we can reuse
 * vanilla {@code FurnaceMenu} unchanged.
 *
 * <p>{@link #lit} is cached "smelt fired this tick" — drives the front-face
 * glow on the client. Recomputed every tick in {@link FurnaceTick}.
 */
public class Region {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_COUNT = 3;

    private final UUID id;
    private final BoundingBox bounds;
    private final Direction front;

    private boolean lit;
    /** Banked fuel-ticks — single source of truth for fuel state. */
    private int litTime;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public Region(UUID id, BoundingBox bounds, Direction front) {
        if (front.getAxis().isVertical()) {
            throw new IllegalArgumentException("front must be horizontal, got " + front);
        }
        this.id = id;
        this.bounds = bounds;
        this.front = front;
    }

    public UUID id() { return id; }
    public BoundingBox bounds() { return bounds; }
    public Direction front() { return front; }

    public boolean lit() { return lit; }
    public void setLit(boolean lit) { this.lit = lit; }

    public int litTime() { return litTime; }
    public void setLitTime(int v) { this.litTime = Math.max(0, v); }

    public NonNullList<ItemStack> items() { return items; }

    /** Cube edge length — 32, 64, or 128. Derived from bounds (always a cube for valid regions). */
    public int size() {
        return bounds.getXSpan();
    }

    /**
     * Items smelted per tick when fully fueled — scales with volume:
     * 32³ → 1, 64³ → 8, 128³ → 64. Same value as {@code (size/32)³}.
     */
    public int throughput() {
        int s = size() / 32;
        return s * s * s;
    }

    public boolean contains(BlockPos pos) {
        return bounds.isInside(pos);
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putIntArray("bounds", new int[]{
                bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ()
        });
        tag.putByte("front", (byte) front.get2DDataValue());
        tag.putInt("litTime", litTime);
        tag.put("items", ContainerHelper.saveAllItems(new CompoundTag(), items, registries).getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND));
        return tag;
    }

    public static Region load(CompoundTag tag, HolderLookup.Provider registries) {
        UUID id = tag.getUUID("id");
        int[] b = tag.getIntArray("bounds");
        Direction front = Direction.from2DDataValue(tag.getByte("front"));
        Region r = new Region(id, new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]), front);
        // Migration: prefer the new key, fall back to legacy "fuelTicks".
        if (tag.contains("litTime", net.minecraft.nbt.Tag.TAG_INT)) {
            r.litTime = tag.getInt("litTime");
        } else if (tag.contains("fuelTicks", net.minecraft.nbt.Tag.TAG_INT)) {
            r.litTime = tag.getInt("fuelTicks");
        }
        // lit is "smelted this tick" — recomputed by FurnaceTick. Default false on load.
        if (tag.contains("items", net.minecraft.nbt.Tag.TAG_LIST)) {
            CompoundTag itemsWrap = new CompoundTag();
            itemsWrap.put("Items", tag.getList("items", net.minecraft.nbt.Tag.TAG_COMPOUND));
            ContainerHelper.loadAllItems(itemsWrap, r.items, registries);
        }
        return r;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Region r && Objects.equals(id, r.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
