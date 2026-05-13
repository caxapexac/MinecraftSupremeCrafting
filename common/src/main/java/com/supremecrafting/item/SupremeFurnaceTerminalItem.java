package com.supremecrafting.item;

import com.supremecrafting.SupremeCrafting;
import com.supremecrafting.furnace.BoundFurnace;
import com.supremecrafting.furnace.MultiblockRegions;
import com.supremecrafting.furnace.Region;
import com.supremecrafting.furnace.RegionFurnaceContainer;
import com.supremecrafting.furnace.RegionFurnaceData;
import com.supremecrafting.registry.SCDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Wireless Supreme Furnace Terminal.
 *
 * <p>Bound via right-clicking any wall of a formed furnace — the casing's
 * {@code useItemOn} sees the terminal in the player's hand and writes the
 * region's {@code (dim, uuid)} to the stack's data component.
 *
 * <p>Right-clicked in air, the server resolves the bound dimension + region
 * and opens the vanilla furnace menu against it. Works cross-dim and at any
 * distance — vanilla closes the menu on dimension change anyway.
 */
public class SupremeFurnaceTerminalItem extends Item {
    public SupremeFurnaceTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.consume(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        BoundFurnace bound = stack.get(SCDataComponents.BOUND_FURNACE.get());
        if (bound == null) {
            sp.sendSystemMessage(Component.literal(
                    "Terminal is not bound. Right-click a Supreme Furnace wall to bind."));
            return InteractionResultHolder.fail(stack);
        }

        ServerLevel target = sp.server.getLevel(bound.dim());
        if (target == null) {
            sp.sendSystemMessage(Component.literal("Bound dimension is not loaded."));
            return InteractionResultHolder.fail(stack);
        }
        Region region = MultiblockRegions.get(target).byId(bound.regionId());
        if (region == null) {
            sp.sendSystemMessage(Component.literal("Bound furnace has been disassembled."));
            return InteractionResultHolder.fail(stack);
        }

        sp.openMenu(new SimpleMenuProvider(
                (id, playerInv, p) -> new FurnaceMenu(id, playerInv,
                        new RegionFurnaceContainer(target, region.id()),
                        new RegionFurnaceData(target, region.id())),
                Component.translatable("container." + SupremeCrafting.MOD_ID + ".supreme_furnace")));
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext ctx,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        BoundFurnace bound = stack.get(SCDataComponents.BOUND_FURNACE.get());
        if (bound == null) {
            tooltip.add(Component.literal("Not bound").withStyle(ChatFormatting.GRAY));
        } else {
            String shortId = bound.regionId().toString().substring(0, 8);
            tooltip.add(Component.literal("Bound: " + bound.dim().location() + " · " + shortId)
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
