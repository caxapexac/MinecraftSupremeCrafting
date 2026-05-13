package com.supremecrafting.table;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SupremeTableInventoryTest {

    private static HolderLookup.Provider registries;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    void sizeIs6561() {
        assertEquals(81 * 81, SupremeTableInventory.SIZE);
        assertEquals(SupremeTableInventory.SIZE, new SupremeTableInventory().size());
    }

    @Test
    void freshInventoryIsAllEmpty() {
        SupremeTableInventory inv = new SupremeTableInventory();
        assertTrue(inv.isEmpty());
        assertEquals(0, inv.countNonEmpty());
        for (int i = 0; i < SupremeTableInventory.SIZE; i++) {
            assertTrue(inv.get(i).isEmpty(), "slot " + i + " should be empty");
        }
    }

    @Test
    void coordIndexRoundTrip() {
        for (int y = 0; y < SupremeTableInventory.HEIGHT; y++) {
            for (int x = 0; x < SupremeTableInventory.WIDTH; x++) {
                int idx = SupremeTableInventory.indexOf(x, y);
                assertEquals(x, SupremeTableInventory.xOf(idx));
                assertEquals(y, SupremeTableInventory.yOf(idx));
            }
        }
    }

    @Test
    void outOfRangeAccessThrows() {
        SupremeTableInventory inv = new SupremeTableInventory();
        assertThrows(IndexOutOfBoundsException.class, () -> inv.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> inv.get(SupremeTableInventory.SIZE));
        assertThrows(IndexOutOfBoundsException.class, () -> SupremeTableInventory.indexOf(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> SupremeTableInventory.indexOf(0, 81));
    }

    @Test
    void setAndGetByIndex() {
        SupremeTableInventory inv = new SupremeTableInventory();
        inv.set(42, new ItemStack(Items.STONE, 5));
        assertFalse(inv.get(42).isEmpty());
        assertEquals(Items.STONE, inv.get(42).getItem());
        assertEquals(5, inv.get(42).getCount());
        assertEquals(1, inv.countNonEmpty());
    }

    @Test
    void setNullCoercesToEmpty() {
        SupremeTableInventory inv = new SupremeTableInventory();
        inv.set(0, new ItemStack(Items.STONE));
        inv.set(0, null);
        assertTrue(inv.get(0).isEmpty());
    }

    @Test
    void clearWipesEverything() {
        SupremeTableInventory inv = new SupremeTableInventory();
        inv.set(0, new ItemStack(Items.STONE));
        inv.set(SupremeTableInventory.SIZE - 1, new ItemStack(Items.DIRT));
        inv.clear();
        assertTrue(inv.isEmpty());
    }

    @Test
    void saveSkipsEmptySlotsAndKeepsOrder() {
        SupremeTableInventory inv = new SupremeTableInventory();
        inv.set(10, new ItemStack(Items.STONE));
        inv.set(2000, new ItemStack(Items.DIRT));
        inv.set(SupremeTableInventory.SIZE - 1, new ItemStack(Items.GOLD_INGOT));

        ListTag list = inv.save(registries);

        assertEquals(3, list.size(), "only non-empty slots should be in tag");
        assertEquals(10, list.getCompound(0).getInt("Slot"));
        assertEquals(2000, list.getCompound(1).getInt("Slot"));
        assertEquals(SupremeTableInventory.SIZE - 1, list.getCompound(2).getInt("Slot"));
    }

    @Test
    void emptyInventorySavesEmptyList() {
        SupremeTableInventory inv = new SupremeTableInventory();
        ListTag list = inv.save(registries);
        assertEquals(0, list.size());
    }

    @Test
    void roundTripPreservesAllNonEmptySlots() {
        SupremeTableInventory original = new SupremeTableInventory();
        original.set(0, new ItemStack(Items.STONE, 1));
        original.set(80, new ItemStack(Items.DIRT, 17));   // top-right corner
        original.set(81, new ItemStack(Items.GOLD_INGOT)); // start of row 1
        original.set(SupremeTableInventory.SIZE - 1, new ItemStack(Items.DIAMOND, 64));

        ListTag list = original.save(registries);

        SupremeTableInventory loaded = new SupremeTableInventory();
        loaded.set(500, new ItemStack(Items.STICK)); // ensure load() clears prior state
        loaded.load(list, registries);

        assertEquals(4, loaded.countNonEmpty());
        assertTrue(ItemStack.matches(loaded.get(0), original.get(0)));
        assertTrue(ItemStack.matches(loaded.get(80), original.get(80)));
        assertTrue(ItemStack.matches(loaded.get(81), original.get(81)));
        assertTrue(ItemStack.matches(loaded.get(SupremeTableInventory.SIZE - 1),
                original.get(SupremeTableInventory.SIZE - 1)));
        assertTrue(loaded.get(500).isEmpty(), "load() should have cleared pre-existing slots");
    }

    @Test
    void loadIgnoresOutOfRangeSlots() {
        SupremeTableInventory inv = new SupremeTableInventory();
        inv.set(5, new ItemStack(Items.STONE));
        ListTag list = inv.save(registries);
        // Inject a malformed entry pointing at an impossible slot.
        net.minecraft.nbt.CompoundTag bad = new net.minecraft.nbt.CompoundTag();
        bad.putInt("Slot", SupremeTableInventory.SIZE + 100);
        bad.put("Item", new ItemStack(Items.DIRT).save(registries));
        list.add(bad);

        SupremeTableInventory loaded = new SupremeTableInventory();
        loaded.load(list, registries);
        assertEquals(1, loaded.countNonEmpty());
        assertEquals(Items.STONE, loaded.get(5).getItem());
    }
}
