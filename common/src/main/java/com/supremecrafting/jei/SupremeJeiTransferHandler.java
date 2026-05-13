package com.supremecrafting.jei;

import com.supremecrafting.net.C2STransferRecipePacket;
import com.supremecrafting.recipe.SupremeCraftingRecipe;
import com.supremecrafting.table.SupremeTableMenu;
import dev.architectury.networking.NetworkManager;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * JEI "+" auto-fill handler. Partial-fill: "+" is always enabled, server
 * places what the player has, missing cells stay empty.
 */
@Environment(EnvType.CLIENT)
public class SupremeJeiTransferHandler
        implements IRecipeTransferHandler<SupremeTableMenu, RecipeHolder<SupremeCraftingRecipe>> {

    private final IRecipeTransferHandlerHelper helper;

    public SupremeJeiTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
    }

    @Override
    public Class<? extends SupremeTableMenu> getContainerClass() {
        return SupremeTableMenu.class;
    }

    @Override
    public Optional<MenuType<SupremeTableMenu>> getMenuType() {
        return Optional.of(com.supremecrafting.registry.SCMenus.SUPREME_TABLE_MENU.get());
    }

    @Override
    public RecipeType<RecipeHolder<SupremeCraftingRecipe>> getRecipeType() {
        return SupremeJeiPlugin.RECIPE_TYPE;
    }

    @Override
    @Nullable
    public IRecipeTransferError transferRecipe(
            SupremeTableMenu container,
            RecipeHolder<SupremeCraftingRecipe> holder,
            IRecipeSlotsView recipeSlots,
            Player player,
            boolean maxTransfer,
            boolean doTransfer) {
        if (doTransfer) {
            NetworkManager.sendToServer(new C2STransferRecipePacket(container.tablePos(), holder.id()));
        }
        return null;
    }
}
