package com.supremecrafting.registry;

import com.supremecrafting.SupremeCrafting;
import com.supremecrafting.item.SupremeWoodenAxe;
import com.supremecrafting.item.SupremeWoodenHoe;
import com.supremecrafting.item.SupremeWoodenPickaxe;
import com.supremecrafting.item.SupremeWoodenShovel;
import com.supremecrafting.item.FurnaceDestroyerItem;
import com.supremecrafting.item.SupremeFurnaceBombItem;
import com.supremecrafting.item.SupremeFurnaceTerminalItem;
import com.supremecrafting.item.SupremeWoodenSword;
import com.supremecrafting.item.SupremeWrenchItem;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;

public final class SCItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(SupremeCrafting.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> SUPREME_WOODEN_SWORD = ITEMS.register(
            "supreme_wooden_sword",
            () -> new SupremeWoodenSword(new Item.Properties()));

    public static final RegistrySupplier<Item> SUPREME_WOODEN_PICKAXE = ITEMS.register(
            "supreme_wooden_pickaxe",
            () -> new SupremeWoodenPickaxe(new Item.Properties()));

    public static final RegistrySupplier<Item> SUPREME_WOODEN_AXE = ITEMS.register(
            "supreme_wooden_axe",
            () -> new SupremeWoodenAxe(new Item.Properties()));

    public static final RegistrySupplier<Item> SUPREME_WOODEN_SHOVEL = ITEMS.register(
            "supreme_wooden_shovel",
            () -> new SupremeWoodenShovel(new Item.Properties()));

    public static final RegistrySupplier<Item> SUPREME_WOODEN_HOE = ITEMS.register(
            "supreme_wooden_hoe",
            () -> new SupremeWoodenHoe(new Item.Properties()));

    public static final RegistrySupplier<Item> SUPREME_WRENCH = ITEMS.register(
            "supreme_wrench",
            () -> new SupremeWrenchItem(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> SUPREME_FURNACE_TERMINAL = ITEMS.register(
            "supreme_furnace_terminal",
            () -> new SupremeFurnaceTerminalItem(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> SUPREME_FURNACE_BOMB_T1 = ITEMS.register(
            "supreme_furnace_bomb_t1",
            () -> new SupremeFurnaceBombItem(new Item.Properties()));
    public static final RegistrySupplier<Item> SUPREME_FURNACE_BOMB_T2 = ITEMS.register(
            "supreme_furnace_bomb_t2",
            () -> new SupremeFurnaceBombItem(new Item.Properties()));
    public static final RegistrySupplier<Item> SUPREME_FURNACE_BOMB_T3 = ITEMS.register(
            "supreme_furnace_bomb_t3",
            () -> new SupremeFurnaceBombItem(new Item.Properties()));

    public static final RegistrySupplier<Item> FURNACE_DESTROYER = ITEMS.register(
            "furnace_destroyer",
            () -> new FurnaceDestroyerItem(Tiers.IRON, new Item.Properties().durability(500)));

    private SCItems() {}
}
