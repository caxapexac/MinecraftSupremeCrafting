package com.supremecrafting.fabric.client;

import com.supremecrafting.client.SupremeTableRenderer;
import com.supremecrafting.client.SupremeTableScreen;
import com.supremecrafting.registry.SCBlockEntities;
import com.supremecrafting.registry.SCMenus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class SupremeCraftingFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(SCMenus.SUPREME_TABLE_MENU.get(), SupremeTableScreen::new);
        BlockEntityRenderers.register(SCBlockEntities.SUPREME_TABLE.get(), SupremeTableRenderer::new);
    }
}
