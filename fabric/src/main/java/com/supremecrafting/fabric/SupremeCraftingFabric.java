package com.supremecrafting.fabric;

import net.fabricmc.api.ModInitializer;

import com.supremecrafting.SupremeCrafting;

public final class SupremeCraftingFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SupremeCrafting.init();
    }
}
