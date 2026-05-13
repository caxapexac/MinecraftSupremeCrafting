package com.supremecrafting.furnace;

import com.supremecrafting.net.S2CMultiblockLitPacket;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import java.util.Optional;

/**
 * Per-region furnace tick — accumulator model with size-based batch throughput.
 *
 * <p>Each tick smelts up to {@link Region#throughput()} items in one batch
 * (32³ → 1, 64³ → 8, 128³ → 64). Actual batch is
 * {@code min(throughput, banked/FUEL_PER_ITEM, input.count, output.room)} so
 * shortages on any of fuel / input / output room reduce the batch gracefully.
 *
 * <p>{@link Region#litTime()} is a banked fuel-tick reservoir. Refilled one
 * fuel item at a time from the slot while {@code litTime &lt; FUEL_PER_ITEM},
 * then drained {@code batch × FUEL_PER_ITEM} on smelt. Any positive-burn fuel
 * contributes (sticks, planks, etc.) — 8 sticks bank up to one smelt.
 *
 * <p>{@link Region#lit()} = "smelted this tick" — drives the front-face glow.
 * GUI flame reads {@code min(litTime, FUEL_PER_ITEM) / FUEL_PER_ITEM}.
 */
public final class FurnaceTick {
    /** Vanilla cook-cost (200) × 4 = 800 fuel-ticks per smelt. */
    public static final int FUEL_PER_ITEM = 200 * 4;

    private FurnaceTick() {}

    public static void tickAll(ServerLevel level) {
        MultiblockRegions regions = MultiblockRegions.get(level);
        for (Region r : new java.util.ArrayList<>(regions.all())) {
            tick(level, r, regions);
        }
    }

    private static void tick(ServerLevel level, Region r, MultiblockRegions regions) {
        boolean wasLit = r.lit();
        boolean changed = false;
        boolean cookedThisTick = false;

        NonNullList<ItemStack> items = r.items();
        ItemStack input = items.get(Region.SLOT_INPUT);
        RecipeHolder<SmeltingRecipe> recipe = input.isEmpty() ? null : findRecipe(level, input);

        if (recipe != null) {
            ItemStack result = recipe.value().getResultItem(level.registryAccess());
            if (!result.isEmpty()) {
                int outRoom = outputRoom(items, result);
                if (outRoom > 0) {
                    // 1. Top up the bank until it can cover a full throughput
                    //    batch — without this the loop stops at one smelt's
                    //    worth and 64³ would still only smelt 1/tick.
                    int neededForFullBatch = r.throughput() * FUEL_PER_ITEM;
                    while (r.litTime() < neededForFullBatch && !items.get(Region.SLOT_FUEL).isEmpty()) {
                        ItemStack fuel = items.get(Region.SLOT_FUEL);
                        int burn = burnDuration(fuel);
                        if (burn <= 0) break;
                        consumeOneFuel(items, fuel);
                        r.setLitTime(r.litTime() + burn);
                        changed = true;
                    }

                    // 2. Batch size limited by fuel, input, output, and throughput.
                    int byFuel = r.litTime() / FUEL_PER_ITEM;
                    int batch = Math.min(r.throughput(),
                            Math.min(byFuel, Math.min(input.getCount(), outRoom)));
                    if (batch > 0) {
                        burnBatch(level.registryAccess(), recipe, items, batch);
                        r.setLitTime(r.litTime() - batch * FUEL_PER_ITEM);
                        cookedThisTick = true;
                        changed = true;
                    }
                }
            }
        }

        if (cookedThisTick != r.lit()) {
            r.setLit(cookedThisTick);
            changed = true;
        }
        if (wasLit != r.lit()) broadcastLit(level, r);
        if (changed) regions.setDirty();
    }

    private static int outputRoom(NonNullList<ItemStack> items, ItemStack result) {
        ItemStack out = items.get(Region.SLOT_OUTPUT);
        if (out.isEmpty()) return result.getMaxStackSize();
        if (!ItemStack.isSameItemSameComponents(out, result)) return 0;
        return out.getMaxStackSize() - out.getCount();
    }

    private static void burnBatch(RegistryAccess registries, RecipeHolder<SmeltingRecipe> recipe,
                                  NonNullList<ItemStack> items, int batch) {
        ItemStack input = items.get(Region.SLOT_INPUT);
        ItemStack result = recipe.value().getResultItem(registries);
        ItemStack out = items.get(Region.SLOT_OUTPUT);
        if (out.isEmpty()) {
            items.set(Region.SLOT_OUTPUT, result.copyWithCount(batch));
        } else {
            out.grow(batch);
        }
        // Wet sponge → water bucket (single-shot like vanilla; fires only once per batch).
        if (input.is(Blocks.WET_SPONGE.asItem())
                && items.get(Region.SLOT_FUEL).is(Items.BUCKET)) {
            items.set(Region.SLOT_FUEL, new ItemStack(Items.WATER_BUCKET));
        }
        input.shrink(batch);
    }

    private static void consumeOneFuel(NonNullList<ItemStack> items, ItemStack fuel) {
        Item fuelItem = fuel.getItem();
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            Item remain = fuelItem.getCraftingRemainingItem();
            items.set(Region.SLOT_FUEL, remain == null ? ItemStack.EMPTY : new ItemStack(remain));
        }
    }

    private static RecipeHolder<SmeltingRecipe> findRecipe(ServerLevel level, ItemStack input) {
        RecipeManager rm = level.getRecipeManager();
        Optional<RecipeHolder<SmeltingRecipe>> opt =
                rm.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level);
        return opt.orElse(null);
    }

    private static int burnDuration(ItemStack fuel) {
        if (fuel.isEmpty()) return 0;
        return AbstractFurnaceBlockEntity.getFuel().getOrDefault(fuel.getItem(), 0);
    }

    private static void broadcastLit(ServerLevel level, Region r) {
        S2CMultiblockLitPacket packet = new S2CMultiblockLitPacket(r.id(), r.lit());
        for (ServerPlayer sp : level.players()) {
            NetworkManager.sendToPlayer(sp, packet);
        }
    }
}
