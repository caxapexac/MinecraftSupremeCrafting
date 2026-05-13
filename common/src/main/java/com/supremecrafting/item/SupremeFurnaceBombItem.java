package com.supremecrafting.item;

import com.supremecrafting.furnace.FurnaceBombEntity;
import com.supremecrafting.registry.SCItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Thrown like a snowball; on hit, the projectile destroys and voids every
 * block in a Supreme Furnace-sized volume (32 / 64 / 128) and attempts to
 * form the furnace there. See {@link FurnaceBombEntity}.
 */
public class SupremeFurnaceBombItem extends Item {
    public SupremeFurnaceBombItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5f,
                0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
        if (!level.isClientSide) {
            FurnaceBombEntity bomb = new FurnaceBombEntity(level, player);
            bomb.setItem(stack);
            bomb.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 1.5f, 1.0f);
            level.addFreshEntity(bomb);
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext ctx,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        int size = sizeForItem(stack.getItem());
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Builds a " + size + "³ Supreme Furnace")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("where it lands.")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("⚠ DESTROYS AND VOIDS BLOCKS ⚠")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Every block inside the target cube is")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("erased without drops, including chests,")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("spawners, and anything else in the way.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.empty());
    }

    private static int sizeForItem(net.minecraft.world.item.Item item) {
        if (item == SCItems.SUPREME_FURNACE_BOMB_T2.get()) return 64;
        if (item == SCItems.SUPREME_FURNACE_BOMB_T3.get()) return 128;
        return 32;
    }
}
