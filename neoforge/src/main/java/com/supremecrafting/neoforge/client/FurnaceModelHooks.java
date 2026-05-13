package com.supremecrafting.neoforge.client;

import com.mojang.logging.LogUtils;
import com.supremecrafting.furnace.SupremeFurnaceCasingBlock;
import com.supremecrafting.registry.SCBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.slf4j.Logger;

import java.util.Map;
import java.util.function.Function;

/**
 * Hooks the formed casing's baked model via {@link ModelEvent.ModifyBakingResult}.
 *
 * <p>Runs once per resource reload, on a worker thread. We grab the four
 * sprites we'll render with (via the event's texture getter — safe to capture
 * since sprites are immutable after stitch) and wrap the {@code formed=true}
 * variant's BakedModel in a {@link FormedCasingModel}.
 */
public final class FurnaceModelHooks {
    private static final Logger LOGGER = LogUtils.getLogger();

    private FurnaceModelHooks() {}

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        Function<Material, TextureAtlasSprite> tg = event.getTextureGetter();

        TextureAtlasSprite top = tg.apply(matBlock("furnace_top"));
        TextureAtlasSprite side = tg.apply(matBlock("furnace_side"));
        TextureAtlasSprite front = tg.apply(matBlock("furnace_front"));
        TextureAtlasSprite frontOn = tg.apply(matBlock("furnace_front_on"));
        TextureAtlasSprite interior = tg.apply(matBlock("coal_block"));

        Block[] shellBlocks = {
                SCBlocks.SUPREME_FURNACE_CASING.get(),
                SCBlocks.SUPREME_FURNACE_INPUT_HATCH.get(),
                SCBlocks.SUPREME_FURNACE_OUTPUT_HATCH.get(),
                SCBlocks.SUPREME_FURNACE_FUEL_HATCH.get(),
        };
        int wrapped = 0;
        for (Block b : shellBlocks) {
            for (BlockState state : b.getStateDefinition().getPossibleStates()) {
                if (!state.getValue(SupremeFurnaceCasingBlock.FORMED)) continue;
                ModelResourceLocation mrl = BlockModelShaper.stateToModelLocation(state);
                BakedModel original = models.get(mrl);
                if (original == null) {
                    LOGGER.warn("No baked model for {} — formed casing won't render with furnace texture", mrl);
                    continue;
                }
                models.put(mrl, new FormedCasingModel(original, top, side, front, frontOn, interior));
                wrapped++;
            }
        }
        LOGGER.debug("Supreme Furnace: wrapped {} formed shell-block baked model variant(s)", wrapped);
    }

    private static Material matBlock(String name) {
        return new Material(TextureAtlas.LOCATION_BLOCKS,
                ResourceLocation.fromNamespaceAndPath("minecraft", "block/" + name));
    }
}
