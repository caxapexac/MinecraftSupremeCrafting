package com.supremecrafting.furnace;

import com.supremecrafting.registry.SCBlocks;
import com.supremecrafting.registry.SCEntities;
import com.supremecrafting.registry.SCItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

/**
 * Thrown Furnace Bomb. On hit it <b>destroys and voids</b> every block in a
 * {@code size}-cube volume with the landing block as the bottom-center shell
 * cell, then calls {@link FurnaceFormation#tryForm}. Container contents are
 * cleared before the destroy so nothing drops — the cube becomes empty space
 * lined with casings, ready to form.
 */
public class FurnaceBombEntity extends ThrowableItemProjectile {
    /** Flag set used for every destroy: client sync, no neighbour updates, no drops. */
    private static final int FORCE_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    public FurnaceBombEntity(EntityType<? extends FurnaceBombEntity> type, Level level) {
        super(type, level);
    }

    public FurnaceBombEntity(Level level, LivingEntity thrower) {
        super(SCEntities.FURNACE_BOMB.get(), thrower, level);
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return SCItems.SUPREME_FURNACE_BOMB_T1.get();
    }

    public int size() {
        Item item = getItem().getItem();
        if (item == SCItems.SUPREME_FURNACE_BOMB_T2.get()) return 64;
        if (item == SCItems.SUPREME_FURNACE_BOMB_T3.get()) return 128;
        return 32;
    }

    @Override
    protected void onHit(@NotNull HitResult result) {
        super.onHit(result);
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;
        BlockPos landing = blockPosition();
        placeShellAndForm(sl, landing, size());
        discard();
    }

    private void placeShellAndForm(ServerLevel level, BlockPos landing, int size) {
        int halfSize = size / 2;
        int minX = landing.getX() - halfSize;
        int maxX = minX + size - 1;
        int minY = landing.getY();
        int maxY = minY + size - 1;
        int minZ = landing.getZ() - halfSize;
        int maxZ = minZ + size - 1;

        BlockState casing = SCBlocks.SUPREME_FURNACE_CASING.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        // 1. Shell — force casing on every cell of the six faces.
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                forceSet(level, m.set(x, minY, z), casing);
                forceSet(level, m.set(x, maxY, z), casing);
            }
        }
        for (int y = minY + 1; y < maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                forceSet(level, m.set(x, y, minZ), casing);
                forceSet(level, m.set(x, y, maxZ), casing);
            }
            for (int z = minZ + 1; z < maxZ; z++) {
                forceSet(level, m.set(minX, y, z), casing);
                forceSet(level, m.set(maxX, y, z), casing);
            }
        }

        // 2. Interior — void anything that isn't already air.
        for (int x = minX + 1; x < maxX; x++) {
            for (int y = minY + 1; y < maxY; y++) {
                for (int z = minZ + 1; z < maxZ; z++) {
                    m.set(x, y, z);
                    if (level.getBlockState(m).isAir()) continue;
                    forceSet(level, m, air);
                }
            }
        }

        // 3. Form. Landing pos is on the bottom face — a valid shell cell.
        Entity ownerEntity = getOwner();
        Player owner = ownerEntity instanceof Player p ? p : null;
        FurnaceFormation.Result result = FurnaceFormation.tryForm(level, landing, owner);
        if (owner != null) {
            if (result instanceof FurnaceFormation.Result.Success s) {
                owner.sendSystemMessage(Component.literal("Supreme Furnace formed: "
                        + s.region().bounds() + " front=" + s.region().front()));
            } else if (result instanceof FurnaceFormation.Result.Failure f) {
                owner.sendSystemMessage(Component.literal("Furnace Bomb form failed: " + f.reason()));
            }
        }
    }

    /**
     * Replace a single block, clearing any container contents first so nothing
     * drops. Combined with {@link Block#UPDATE_SUPPRESS_DROPS} this guarantees
     * a true void — no items, no XP, no neighbour observers.
     */
    private static void forceSet(ServerLevel level, BlockPos pos, BlockState target) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof Container container) {
            container.clearContent();
        }
        level.setBlock(pos, target, FORCE_FLAGS);
    }
}
