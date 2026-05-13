package com.supremecrafting.emi;

import com.supremecrafting.SupremeCrafting;
import com.supremecrafting.client.RecipeViewerHooks;
import com.supremecrafting.recipe.SupremeCraftingRecipe;
import com.supremecrafting.registry.SCBlocks;
import com.supremecrafting.registry.SCRecipes;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * EMI integration entrypoint for the Supreme Table.
 *
 * <p>Cross-loader: on Fabric the {@code emi} entrypoint in
 * {@code fabric.mod.json} loads this class; on NeoForge the
 * {@link EmiEntrypoint} annotation is what EMI's classpath scanner picks up.
 * EMI is {@code modCompileOnly} so the rest of the mod still loads cleanly
 * when EMI is absent.
 *
 * <p>{@link Environment} is auto-remapped to NeoForge's {@code @OnlyIn} by
 * Architectury Loom at jar-build time, so a single annotation works on both.
 */
@Environment(EnvType.CLIENT)
@EmiEntrypoint
public class SupremeEmiPlugin implements EmiPlugin {

    public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(SupremeCrafting.MOD_ID, "supreme_crafting"),
            EmiStack.of(SCBlocks.SUPREME_TABLE_ITEM.get()));

    public SupremeEmiPlugin() {
        RecipeViewerHooks.openSupremeRecipes.add(() -> EmiApi.displayRecipeCategory(CATEGORY));
    }

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(CATEGORY);
        registry.addWorkstation(CATEGORY, EmiStack.of(SCBlocks.SUPREME_TABLE_ITEM.get()));
        registry.addRecipeHandler(
                com.supremecrafting.registry.SCMenus.SUPREME_TABLE_MENU.get(),
                new SupremeEmiRecipeHandler());

        // Iterate every recipe of our type and wrap it for EMI. Both shaped
        // and shapeless variants implement SupremeCraftingRecipe.
        for (RecipeHolder<SupremeCraftingRecipe> holder
                : registry.getRecipeManager().getAllRecipesFor(SCRecipes.SUPREME_CRAFTING_TYPE.get())) {
            SupremeEmiRecipe wrapped = SupremeEmiRecipe.create(CATEGORY, holder);
            if (wrapped != null) {
                registry.addRecipe(wrapped);
            }
        }
    }
}
