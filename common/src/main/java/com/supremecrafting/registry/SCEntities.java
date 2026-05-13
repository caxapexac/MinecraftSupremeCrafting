package com.supremecrafting.registry;

import com.supremecrafting.SupremeCrafting;
import com.supremecrafting.furnace.FurnaceBombEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class SCEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(SupremeCrafting.MOD_ID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<FurnaceBombEntity>> FURNACE_BOMB =
            ENTITY_TYPES.register(
                    "furnace_bomb",
                    () -> EntityType.Builder.<FurnaceBombEntity>of(FurnaceBombEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build(SupremeCrafting.MOD_ID + ":furnace_bomb"));

    private SCEntities() {}
}
