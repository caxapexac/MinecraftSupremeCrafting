package com.supremecrafting.item;

import com.supremecrafting.SupremeCrafting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Tiers;

/** Vanilla wooden hoe stats + 100-block reach + 5× model. */
public class SupremeWoodenHoe extends HoeItem {
    private static final ResourceLocation REACH =
            ResourceLocation.fromNamespaceAndPath(SupremeCrafting.MOD_ID, "supreme_hoe_reach");
    private static final ResourceLocation BLOCK_REACH =
            ResourceLocation.fromNamespaceAndPath(SupremeCrafting.MOD_ID, "supreme_hoe_block_reach");

    public SupremeWoodenHoe(Properties props) {
        super(Tiers.WOOD, props.attributes(SupremeToolAttributes.withReach(
                HoeItem.createAttributes(Tiers.WOOD, 0.0F, -3.0F),
                REACH, BLOCK_REACH)));
    }
}
