package com.supremecrafting.item;

import com.supremecrafting.SupremeCrafting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tiers;

/** Vanilla wooden shovel stats + 100-block reach + 5× model. */
public class SupremeWoodenShovel extends ShovelItem {
    private static final ResourceLocation REACH =
            ResourceLocation.fromNamespaceAndPath(SupremeCrafting.MOD_ID, "supreme_shovel_reach");
    private static final ResourceLocation BLOCK_REACH =
            ResourceLocation.fromNamespaceAndPath(SupremeCrafting.MOD_ID, "supreme_shovel_block_reach");

    public SupremeWoodenShovel(Properties props) {
        super(Tiers.WOOD, props.attributes(SupremeToolAttributes.withReach(
                ShovelItem.createAttributes(Tiers.WOOD, 1.5F, -3.0F),
                REACH, BLOCK_REACH)));
    }
}
