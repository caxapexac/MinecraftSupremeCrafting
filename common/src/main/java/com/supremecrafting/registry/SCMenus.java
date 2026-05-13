package com.supremecrafting.registry;

import com.supremecrafting.SupremeCrafting;
import com.supremecrafting.table.SupremeTableMenu;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public final class SCMenus {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(SupremeCrafting.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<SupremeTableMenu>> SUPREME_TABLE_MENU =
            MENU_TYPES.register(
                    "supreme_table",
                    () -> MenuRegistry.ofExtended(SupremeTableMenu::fromNetwork));

    private SCMenus() {}
}
