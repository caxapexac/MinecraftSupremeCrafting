package com.supremecrafting.item;

import com.supremecrafting.furnace.SupremeFurnaceCasingBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Pickaxe that voids and propagates. The cascade + drop suppression is
 * actually wired in {@link SupremeFurnaceCasingBlock#playerWillDestroy} —
 * fires in both creative and survival, before any drops would happen.
 *
 * <p>This class only customizes mining speed (fast on shell blocks) and the
 * warning tooltip.
 */
public class FurnaceDestroyerItem extends PickaxeItem {
    public FurnaceDestroyerItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public float getDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState state) {
        if (state.getBlock() instanceof SupremeFurnaceCasingBlock) {
            return 100.0f;
        }
        return super.getDestroySpeed(stack, state);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext ctx,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("⚠ VOIDS MINED FURNACE BLOCKS ⚠")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Furnace shell blocks break without")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("dropping, and the chain spreads to")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("every connected casing.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.empty());
    }
}
