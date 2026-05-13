package com.supremecrafting.furnace;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Per-{@link ServerLevel} BFS that destroys connected Supreme Furnace shell
 * blocks (casings + hatch variants) a few at a time. Seeded by
 * {@link com.supremecrafting.item.FurnaceDestroyerItem} on player break,
 * driven by the existing {@code TickEvent.SERVER_LEVEL_POST} hook.
 *
 * <p>State is in-memory only — if the server stops mid-cascade, the remaining
 * casings persist as normal blocks. The player just hits one again with the
 * Destroyer to resume.
 */
public final class CasingCascade {
    /** Blocks destroyed per server tick, per level. */
    private static final int MAX_PER_TICK = 100;
    /** Client sync + no neighbour cascades + no drops. */
    private static final int VOID_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private static final Map<ServerLevel, Cascade> CASCADES = new WeakHashMap<>();

    private CasingCascade() {}

    public static void seed(ServerLevel level, BlockPos start) {
        Cascade c = CASCADES.computeIfAbsent(level, k -> new Cascade());
        BlockPos imm = start.immutable();
        // The seed itself is already being destroyed by the player's break —
        // mark it visited so we never re-process it, then enqueue its 6
        // neighbours as the first wave.
        c.visited.add(imm);
        for (Direction d : Direction.values()) {
            BlockPos n = imm.relative(d).immutable();
            if (c.visited.add(n)) c.queue.add(n);
        }
    }

    public static void tick(ServerLevel level) {
        Cascade c = CASCADES.get(level);
        if (c == null) return;
        if (c.queue.isEmpty()) {
            CASCADES.remove(level);
            return;
        }

        int destroyed = 0;
        while (destroyed < MAX_PER_TICK && !c.queue.isEmpty()) {
            BlockPos pos = c.queue.poll();
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof SupremeFurnaceCasingBlock)) continue;

            // Void: clear any container contents so nothing drops, then
            // setBlock-to-air with SUPPRESS_DROPS.
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Container container) container.clearContent();
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), VOID_FLAGS);
            destroyed++;
            for (Direction d : Direction.values()) {
                BlockPos n = pos.relative(d).immutable();
                if (c.visited.add(n)) c.queue.add(n);
            }
        }

        if (c.queue.isEmpty()) CASCADES.remove(level);
    }

    private static final class Cascade {
        final Deque<BlockPos> queue = new ArrayDeque<>();
        final Set<BlockPos> visited = new HashSet<>();
    }
}
