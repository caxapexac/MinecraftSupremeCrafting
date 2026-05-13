package com.supremecrafting.registry;

import com.supremecrafting.SupremeCrafting;
import com.supremecrafting.furnace.BoundFurnace;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

public final class SCDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(SupremeCrafting.MOD_ID, Registries.DATA_COMPONENT_TYPE);

    /** Stored on a Supreme Furnace Terminal item to remember which furnace it's bound to. */
    public static final RegistrySupplier<DataComponentType<BoundFurnace>> BOUND_FURNACE =
            COMPONENTS.register(
                    "bound_furnace",
                    () -> DataComponentType.<BoundFurnace>builder()
                            .persistent(BoundFurnace.CODEC)
                            .networkSynchronized(BoundFurnace.STREAM_CODEC)
                            .build());

    private SCDataComponents() {}
}
