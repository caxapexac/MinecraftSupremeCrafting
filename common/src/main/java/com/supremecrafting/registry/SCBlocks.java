package com.supremecrafting.registry;

import com.supremecrafting.SupremeCrafting;
import com.supremecrafting.furnace.HatchRole;
import com.supremecrafting.furnace.SupremeFurnaceCasingBlock;
import com.supremecrafting.furnace.SupremeFurnaceHatchBlock;
import com.supremecrafting.table.SupremeTableBlock;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class SCBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(SupremeCrafting.MOD_ID, Registries.BLOCK);

    public static final RegistrySupplier<Block> SUPREME_TABLE = BLOCKS.register(
            "supreme_table",
            () -> new SupremeTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(2.5f, 3.0f)
                    .sound(SoundType.WOOD)
                    // The BlockEntityRenderer animates the model in space; the
                    // block must not occlude or block-light, otherwise neighbours
                    // cull their adjacent faces and the BER renders dark.
                    .noOcclusion()));

    public static final RegistrySupplier<Item> SUPREME_TABLE_ITEM = SCItems.ITEMS.register(
            "supreme_table",
            () -> new BlockItem(SUPREME_TABLE.get(), new Item.Properties()));

    public static final RegistrySupplier<Block> SUPREME_FURNACE_CASING = BLOCKS.register(
            "supreme_furnace_casing",
            () -> new SupremeFurnaceCasingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Item> SUPREME_FURNACE_CASING_ITEM = SCItems.ITEMS.register(
            "supreme_furnace_casing",
            () -> new BlockItem(SUPREME_FURNACE_CASING.get(), new Item.Properties()));

    private static BlockBehaviour.Properties hatchProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(3.0f, 6.0f)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    public static final RegistrySupplier<Block> SUPREME_FURNACE_INPUT_HATCH = BLOCKS.register(
            "supreme_furnace_input_hatch",
            () -> new SupremeFurnaceHatchBlock(hatchProps(), HatchRole.INPUT));
    public static final RegistrySupplier<Item> SUPREME_FURNACE_INPUT_HATCH_ITEM = SCItems.ITEMS.register(
            "supreme_furnace_input_hatch",
            () -> new BlockItem(SUPREME_FURNACE_INPUT_HATCH.get(), new Item.Properties()));

    public static final RegistrySupplier<Block> SUPREME_FURNACE_OUTPUT_HATCH = BLOCKS.register(
            "supreme_furnace_output_hatch",
            () -> new SupremeFurnaceHatchBlock(hatchProps(), HatchRole.OUTPUT));
    public static final RegistrySupplier<Item> SUPREME_FURNACE_OUTPUT_HATCH_ITEM = SCItems.ITEMS.register(
            "supreme_furnace_output_hatch",
            () -> new BlockItem(SUPREME_FURNACE_OUTPUT_HATCH.get(), new Item.Properties()));

    public static final RegistrySupplier<Block> SUPREME_FURNACE_FUEL_HATCH = BLOCKS.register(
            "supreme_furnace_fuel_hatch",
            () -> new SupremeFurnaceHatchBlock(hatchProps(), HatchRole.FUEL));
    public static final RegistrySupplier<Item> SUPREME_FURNACE_FUEL_HATCH_ITEM = SCItems.ITEMS.register(
            "supreme_furnace_fuel_hatch",
            () -> new BlockItem(SUPREME_FURNACE_FUEL_HATCH.get(), new Item.Properties()));

    private SCBlocks() {}
}
