package com.supremecrafting.registry;

import com.supremecrafting.SupremeCrafting;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class SCCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(SupremeCrafting.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup." + SupremeCrafting.MOD_ID))
                    .icon(() -> new ItemStack(SCBlocks.SUPREME_TABLE_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(SCBlocks.SUPREME_TABLE_ITEM.get());
                        output.accept(SCBlocks.SUPREME_FURNACE_CASING_ITEM.get());
                        output.accept(SCBlocks.SUPREME_FURNACE_INPUT_HATCH_ITEM.get());
                        output.accept(SCBlocks.SUPREME_FURNACE_OUTPUT_HATCH_ITEM.get());
                        output.accept(SCBlocks.SUPREME_FURNACE_FUEL_HATCH_ITEM.get());
                        output.accept(SCItems.SUPREME_WRENCH.get());
                        output.accept(SCItems.SUPREME_FURNACE_TERMINAL.get());
                        output.accept(SCItems.SUPREME_FURNACE_BOMB_T1.get());
                        output.accept(SCItems.SUPREME_FURNACE_BOMB_T2.get());
                        output.accept(SCItems.SUPREME_FURNACE_BOMB_T3.get());
                        output.accept(SCItems.FURNACE_DESTROYER.get());
                        output.accept(SCItems.SUPREME_WOODEN_SWORD.get());
                        output.accept(SCItems.SUPREME_WOODEN_PICKAXE.get());
                        output.accept(SCItems.SUPREME_WOODEN_AXE.get());
                        output.accept(SCItems.SUPREME_WOODEN_SHOVEL.get());
                        output.accept(SCItems.SUPREME_WOODEN_HOE.get());
                    })
                    .build());

    private SCCreativeTabs() {}
}
