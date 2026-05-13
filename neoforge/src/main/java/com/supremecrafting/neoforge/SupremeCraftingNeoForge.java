package com.supremecrafting.neoforge;

import com.supremecrafting.SupremeCrafting;
import com.supremecrafting.client.SupremeTableRenderer;
import com.supremecrafting.client.SupremeTableScreen;
import com.supremecrafting.neoforge.client.FurnaceModelHooks;
import com.supremecrafting.registry.SCBlockEntities;
import com.supremecrafting.registry.SCEntities;
import com.supremecrafting.registry.SCMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(SupremeCrafting.MOD_ID)
public final class SupremeCraftingNeoForge {
    public SupremeCraftingNeoForge(IEventBus modBus) {
        SupremeCrafting.init();

        modBus.addListener((RegisterCapabilitiesEvent event) -> HatchCapabilities.onRegisterCapabilities(event));

        if (FMLEnvironment.dist == Dist.CLIENT) {
            // Register screen factories directly on RegisterMenuScreensEvent — see
            // CLAUDE.md note on the Architectury helper's bus-ordering race here.
            modBus.addListener((RegisterMenuScreensEvent event) -> {
                event.register(SCMenus.SUPREME_TABLE_MENU.get(), SupremeTableScreen::new);
            });
            modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
                event.registerBlockEntityRenderer(SCBlockEntities.SUPREME_TABLE.get(),
                        SupremeTableRenderer::new);
                event.registerEntityRenderer(SCEntities.FURNACE_BOMB.get(),
                        net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
            });
            modBus.addListener(FurnaceModelHooks::onModifyBakingResult);
        }
    }
}
