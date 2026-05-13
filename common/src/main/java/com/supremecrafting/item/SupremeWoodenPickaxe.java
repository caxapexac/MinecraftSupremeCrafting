package com.supremecrafting.item;

import com.supremecrafting.SupremeCrafting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;

/** Vanilla wooden pickaxe stats + 100-block reach + 5× model. */
public class SupremeWoodenPickaxe extends PickaxeItem {
    private static final ResourceLocation REACH =
            ResourceLocation.fromNamespaceAndPath(SupremeCrafting.MOD_ID, "supreme_pickaxe_reach");
    private static final ResourceLocation BLOCK_REACH =
            ResourceLocation.fromNamespaceAndPath(SupremeCrafting.MOD_ID, "supreme_pickaxe_block_reach");

    public SupremeWoodenPickaxe(Properties props) {
        super(Tiers.WOOD, props.attributes(SupremeToolAttributes.withReach(
                PickaxeItem.createAttributes(Tiers.WOOD, 1.0F, -2.8F),
                REACH, BLOCK_REACH)));
    }
}
