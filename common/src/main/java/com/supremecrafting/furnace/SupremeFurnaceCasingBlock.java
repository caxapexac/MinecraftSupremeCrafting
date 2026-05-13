package com.supremecrafting.furnace;

import com.supremecrafting.registry.SCDataComponents;
import com.supremecrafting.registry.SCItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

/**
 * Casing block for the 32^3 Supreme Furnace.
 *
 * <p>One block type for the whole multiblock — no separate "controller". A
 * single {@link #FORMED} blockstate property toggles between the unformed
 * placeholder model and the per-pixel formed model; the broader region
 * geometry (bounds, front direction) lives in {@link MultiblockRegions}
 * server-side and {@link ClientMultiblockRegions} client-side.
 */
public class SupremeFurnaceCasingBlock extends Block {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public SupremeFurnaceCasingBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    public @NotNull BlockState playerWillDestroy(@NotNull Level level, @NotNull BlockPos pos,
                                                 @NotNull BlockState state, @NotNull Player player) {
        if (!level.isClientSide && level instanceof ServerLevel sl) {
            Region r = MultiblockRegions.get(sl).findContaining(pos);
            if (r != null) {
                player.sendSystemMessage(Component.literal("Supreme Furnace disassembled."));
            }
            // Furnace Destroyer: seed the cascade + return AIR so vanilla's
            // downstream playerDestroy sees an air state and drops nothing.
            // Fires in both creative and survival (mineBlock would only fire in
            // survival and after drops have already happened).
            if (player.getMainHandItem().is(SCItems.FURNACE_DESTROYER.get())) {
                CasingCascade.seed(sl, pos);
                super.playerWillDestroy(level, pos, state, player);
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                         @NotNull BlockState newState, boolean moved) {
        // Only disassemble on a real block-type change (avoids re-entry when
        // we flip the FORMED property in bulk during form/disassemble).
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel sl) {
            Region r = MultiblockRegions.get(sl).findContaining(pos);
            if (r != null) {
                FurnaceFormation.disassemble(sl, r);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state,
                                                      @NotNull Level level, @NotNull BlockPos pos,
                                                      @NotNull Player player, @NotNull InteractionHand hand,
                                                      @NotNull BlockHitResult hit) {
        if (stack.is(SCItems.SUPREME_WRENCH.get())) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            if (!(level instanceof ServerLevel sl)) return ItemInteractionResult.FAIL;

            FurnaceFormation.Result result = FurnaceFormation.tryForm(sl, pos, player);
            if (result instanceof FurnaceFormation.Result.Success s) {
                player.sendSystemMessage(Component.literal("Supreme Furnace formed: "
                        + s.region().bounds() + " front=" + s.region().front()));
                return ItemInteractionResult.CONSUME;
            }
            if (result instanceof FurnaceFormation.Result.Failure f) {
                player.sendSystemMessage(Component.literal("Cannot form: " + f.reason()));
            }
            return ItemInteractionResult.FAIL;
        }
        if (stack.is(SCItems.SUPREME_FURNACE_TERMINAL.get())) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            if (!(level instanceof ServerLevel sl)) return ItemInteractionResult.FAIL;
            Region r = MultiblockRegions.get(sl).findContaining(pos);
            if (r == null) {
                player.sendSystemMessage(Component.literal("Not part of a formed Supreme Furnace."));
                return ItemInteractionResult.FAIL;
            }
            stack.set(SCDataComponents.BOUND_FURNACE.get(), new BoundFurnace(sl.dimension(), r.id()));
            player.sendSystemMessage(Component.literal("Terminal bound: " + r.bounds()));
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level,
                                                    @NotNull BlockPos pos, @NotNull Player player,
                                                    @NotNull BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel sl)) return InteractionResult.PASS;
        Region r = MultiblockRegions.get(sl).findContaining(pos);
        if (r == null) {
            player.sendSystemMessage(Component.literal("Not part of a formed structure"));
            return InteractionResult.CONSUME;
        }
        if (player instanceof ServerPlayer sp) {
            sp.openMenu(new SimpleMenuProvider(
                    (id, playerInv, p) -> new FurnaceMenu(id, playerInv,
                            new RegionFurnaceContainer(sl, r.id()),
                            new RegionFurnaceData(sl, r.id())),
                    Component.translatable("container." + com.supremecrafting.SupremeCrafting.MOD_ID + ".supreme_furnace")));
        }
        return InteractionResult.CONSUME;
    }
}
