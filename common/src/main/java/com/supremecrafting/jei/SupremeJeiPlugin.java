package com.supremecrafting.jei;

import com.supremecrafting.SupremeCrafting;
import com.supremecrafting.client.RecipeViewerHooks;
import com.supremecrafting.recipe.SupremeCraftingRecipe;
import com.supremecrafting.registry.SCBlocks;
import com.supremecrafting.registry.SCRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/**
 * JEI integration entrypoint for the Supreme Table.
 * <p>
 * Discovered by JEI's classpath scanner via the {@link JeiPlugin} annotation
 * on both Fabric and NeoForge. Loaded only when JEI is present at runtime —
 * JEI is {@code modCompileOnly} so the rest of the mod still loads cleanly
 * when JEI is absent. {@link Environment} is auto-remapped to NeoForge's
 * {@code @OnlyIn} by Architectury Loom at jar-build time.
 */
@Environment(EnvType.CLIENT)
@JeiPlugin
public class SupremeJeiPlugin implements IModPlugin {
    public static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(SupremeCrafting.MOD_ID, "jei_plugin");

    /**
     * Generic-type cast hack: JEI's {@link RecipeType} keys on the raw class.
     * For parameterised types like {@code RecipeHolder<SupremeCraftingRecipe>}
     * we use the erased {@code RecipeHolder} class plus an unchecked cast.
     */
    @SuppressWarnings("unchecked")
    public static final RecipeType<RecipeHolder<SupremeCraftingRecipe>> RECIPE_TYPE =
            new RecipeType<>(
                    ResourceLocation.fromNamespaceAndPath(SupremeCrafting.MOD_ID, "supreme_crafting"),
                    (Class<RecipeHolder<SupremeCraftingRecipe>>) (Class<?>) RecipeHolder.class);

    /** Captured in {@link #onRuntimeAvailable}; cleared in {@link #onRuntimeUnavailable}. */
    private IJeiRuntime runtime;

    public SupremeJeiPlugin() {
        // Hook fires lazily — runtime may not be ready until a world is joined.
        RecipeViewerHooks.openSupremeRecipes.add(() -> {
            if (runtime != null) {
                runtime.getRecipesGui().showTypes(List.of(RECIPE_TYPE));
            } else {
                throw new IllegalStateException("JEI runtime not yet available");
            }
        });
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime r) {
        this.runtime = r;
    }

    @Override
    public void onRuntimeUnavailable() {
        this.runtime = null;
    }

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new SupremeJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return; // JEI re-runs registration when a world is loaded
        List<RecipeHolder<SupremeCraftingRecipe>> recipes = mc.level.getRecipeManager()
                .getAllRecipesFor(SCRecipes.SUPREME_CRAFTING_TYPE.get())
                .stream()
                .toList();
        registration.addRecipes(RECIPE_TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                VanillaTypes.ITEM_STACK,
                new ItemStack(SCBlocks.SUPREME_TABLE_ITEM.get()),
                RECIPE_TYPE);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                new SupremeJeiTransferHandler(registration.getTransferHelper()),
                RECIPE_TYPE);
    }
}
