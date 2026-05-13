package com.supremecrafting.registry;

import com.supremecrafting.SupremeCrafting;
import com.supremecrafting.table.SupremeTableBlockEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class SCBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(SupremeCrafting.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<SupremeTableBlockEntity>> SUPREME_TABLE =
            BLOCK_ENTITY_TYPES.register(
                    "supreme_table",
                    () -> BlockEntityType.Builder
                            .of(SupremeTableBlockEntity::new, SCBlocks.SUPREME_TABLE.get())
                            .build(null));

    private SCBlockEntities() {}
}
