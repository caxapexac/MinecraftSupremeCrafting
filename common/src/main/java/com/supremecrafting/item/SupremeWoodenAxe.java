package com.supremecrafting.item;

import com.supremecrafting.SupremeCrafting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Tiers;

/** Vanilla wooden axe stats + 100-block reach + 5× model. */
public class SupremeWoodenAxe extends AxeItem {
    private static final ResourceLocation REACH =
            ResourceLocation.fromNamespaceAndPath(SupremeCrafting.MOD_ID, "supreme_axe_reach");
    private static final ResourceLocation BLOCK_REACH =
            ResourceLocation.fromNamespaceAndPath(SupremeCrafting.MOD_ID, "supreme_axe_block_reach");

    public SupremeWoodenAxe(Properties props) {
        super(Tiers.WOOD, props.attributes(SupremeToolAttributes.withReach(
                AxeItem.createAttributes(Tiers.WOOD, 6.0F, -3.2F),
                REACH, BLOCK_REACH)));
    }
}
