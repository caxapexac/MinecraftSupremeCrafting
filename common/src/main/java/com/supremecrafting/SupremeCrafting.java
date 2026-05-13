package com.supremecrafting;

import com.mojang.logging.LogUtils;
import com.supremecrafting.net.SCNetwork;
import com.supremecrafting.registry.SCBlockEntities;
import com.supremecrafting.registry.SCBlocks;
import com.supremecrafting.registry.SCCreativeTabs;
import com.supremecrafting.registry.SCDataComponents;
import com.supremecrafting.registry.SCEntities;
import com.supremecrafting.registry.SCItems;
import com.supremecrafting.registry.SCMenus;
import com.supremecrafting.registry.SCRecipes;
import org.slf4j.Logger;

public final class SupremeCrafting {
    public static final String MOD_ID = "supreme_crafting";
    private static final Logger LOGGER = LogUtils.getLogger();

    private SupremeCrafting() {
    }

    public static void init() {
        LOGGER.info("Initializing Supreme Crafting (common)");
        SCBlocks.BLOCKS.register();
        SCItems.ITEMS.register();
        SCBlockEntities.BLOCK_ENTITY_TYPES.register();
        SCEntities.ENTITY_TYPES.register();
        SCMenus.MENU_TYPES.register();
        SCRecipes.TYPES.register();
        SCRecipes.SERIALIZERS.register();
        SCDataComponents.COMPONENTS.register();
        SCCreativeTabs.CREATIVE_MODE_TABS.register();
        SCNetwork.register();
    }
}
