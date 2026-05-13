package com.supremecrafting.item;

import com.supremecrafting.SupremeCrafting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

/** Vanilla wooden sword stats + 100-block reach + 5× model. */
public class SupremeWoodenSword extends SwordItem {
    private static final ResourceLocation REACH =
            ResourceLocation.fromNamespaceAndPath(SupremeCrafting.MOD_ID, "supreme_sword_reach");
    private static final ResourceLocation BLOCK_REACH =
            ResourceLocation.fromNamespaceAndPath(SupremeCrafting.MOD_ID, "supreme_sword_block_reach");

    public SupremeWoodenSword(Properties props) {
        super(Tiers.WOOD, props.attributes(SupremeToolAttributes.withReach(
                SwordItem.createAttributes(Tiers.WOOD, 3, -2.4F),
                REACH, BLOCK_REACH)));
    }
}
