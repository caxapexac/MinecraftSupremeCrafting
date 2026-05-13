package com.supremecrafting.furnace;

/**
 * Which Region slot a hatch reads/writes. Routes capability traffic in
 * {@code HatchItemHandler}: {@link #INPUT} accepts inserts and feeds slot 0;
 * {@link #FUEL} accepts only burnables (or empty buckets, for the wet-sponge
 * water-bucket interaction) and feeds slot 1; {@link #OUTPUT} only allows
 * extraction from slot 2.
 */
public enum HatchRole {
    INPUT(Region.SLOT_INPUT),
    FUEL(Region.SLOT_FUEL),
    OUTPUT(Region.SLOT_OUTPUT);

    private final int slotIndex;

    HatchRole(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public int slotIndex() {
        return slotIndex;
    }
}
