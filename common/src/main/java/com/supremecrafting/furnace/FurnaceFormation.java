package com.supremecrafting.furnace;

import com.supremecrafting.net.S2CMultiblockSyncPacket;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Forms / disassembles hollow Supreme Furnace structures. Three valid cube
 * sizes: 32, 64, 128. The size is detected from the flood-fill result; the
 * furnace's per-tick throughput scales with volume (see {@link Region#throughput()}).
 *
 * <p>Form algorithm (wrench right-click on any casing block):
 * <ol>
 *   <li>Flood-fill the connected casing component starting from the clicked pos
 *       (6-axis adjacency), capped to {@link #FLOOD_CAP}.</li>
 *   <li>Reject unless the bounding box is a cube of one of {@link #VALID_SIZES}
 *       and the component size matches the expected hollow-shell count.</li>
 *   <li>Reject unless every interior position is air.</li>
 *   <li>Flip {@code FORMED} on every shell cell; register a new {@link Region};
 *       broadcast to clients.</li>
 * </ol>
 */
public final class FurnaceFormation {
    public static final int[] VALID_SIZES = {32, 64, 128};
    public static final int MAX_SIZE = 128;
    /** Largest valid shell count (128^3 - 126^3 = 96,776). Plus a small buffer. */
    private static final int FLOOD_CAP = shellCount(MAX_SIZE) + 1024;

    /** Flag 2 (UPDATE_CLIENTS) + flag 8 (UPDATE_KNOWN_SHAPE) — re-send to clients without cascading neighbour updates. */
    private static final int BLOCK_FLIP_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private FurnaceFormation() {}

    public sealed interface Result {
        record Success(Region region) implements Result {}
        record Failure(String reason) implements Result {}
    }

    public static int shellCount(int size) {
        int inner = size - 2;
        return size * size * size - inner * inner * inner;
    }

    private static boolean isValidSize(int size) {
        for (int s : VALID_SIZES) if (s == size) return true;
        return false;
    }

    public static Result tryForm(ServerLevel level, BlockPos start, @Nullable Player formingPlayer) {
        MultiblockRegions regions = MultiblockRegions.get(level);
        if (regions.findContaining(start) != null) {
            return new Result.Failure("this block is already part of a formed structure");
        }
        if (!isShell(level.getBlockState(start))) {
            return new Result.Failure("not a shell block");
        }

        Set<BlockPos> casingSet = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        BlockPos startImm = start.immutable();
        queue.add(startImm);
        casingSet.add(startImm);
        int minX = start.getX(), maxX = minX;
        int minY = start.getY(), maxY = minY;
        int minZ = start.getZ(), maxZ = minZ;

        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d).immutable();
                if (casingSet.contains(n)) continue;
                if (!isShell(level.getBlockState(n))) continue;
                casingSet.add(n);
                if (casingSet.size() > FLOOD_CAP) {
                    return new Result.Failure("connected casing exceeds " + FLOOD_CAP + " blocks");
                }
                queue.add(n);
                if (n.getX() < minX) minX = n.getX();
                if (n.getX() > maxX) maxX = n.getX();
                if (n.getY() < minY) minY = n.getY();
                if (n.getY() > maxY) maxY = n.getY();
                if (n.getZ() < minZ) minZ = n.getZ();
                if (n.getZ() > maxZ) maxZ = n.getZ();
            }
        }

        int xs = maxX - minX + 1;
        int ys = maxY - minY + 1;
        int zs = maxZ - minZ + 1;
        if (xs != ys || ys != zs) {
            return new Result.Failure("bounding box is " + xs + "x" + ys + "x" + zs + ", must be a cube");
        }
        int size = xs;
        if (!isValidSize(size)) {
            return new Result.Failure("size " + size + " not one of 32, 64, 128");
        }
        int expectedShell = shellCount(size);
        if (casingSet.size() != expectedShell) {
            return new Result.Failure("found " + casingSet.size() + " casing blocks, expected "
                    + expectedShell + " (shell incomplete or has extras)");
        }

        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = minX + 1; x < maxX; x++) {
            for (int y = minY + 1; y < maxY; y++) {
                for (int z = minZ + 1; z < maxZ; z++) {
                    m.set(x, y, z);
                    if (!level.getBlockState(m).isAir()) {
                        return new Result.Failure("interior must be empty at "
                                + m.getX() + "," + m.getY() + "," + m.getZ());
                    }
                }
            }
        }

        Direction front = (formingPlayer != null)
                ? formingPlayer.getDirection().getOpposite()
                : Direction.NORTH;

        BoundingBox bounds = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        Region region = regions.create(bounds, front);

        flipShell(level, bounds, true);
        broadcastAdd(level, region);
        return new Result.Success(region);
    }

    public static void disassemble(ServerLevel level, Region region) {
        flipShell(level, region.bounds(), false);
        MultiblockRegions.get(level).remove(region.id());
        broadcastRemove(level, region.id());
    }

    /**
     * Flips {@code FORMED} on every shell cell of the bounds — exactly
     * {@code shellCount(size)} setBlock calls, no interior iteration.
     */
    private static void flipShell(ServerLevel level, BoundingBox b, boolean formed) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int minX = b.minX(), maxX = b.maxX();
        int minY = b.minY(), maxY = b.maxY();
        int minZ = b.minZ(), maxZ = b.maxZ();

        // Bottom + top faces — full XZ planes.
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                flipCell(level, m.set(x, minY, z), formed);
                flipCell(level, m.set(x, maxY, z), formed);
            }
        }
        // Vertical sides — only the ring at each Y level, avoiding the bottom/top rows.
        for (int y = minY + 1; y < maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                flipCell(level, m.set(x, y, minZ), formed);
                flipCell(level, m.set(x, y, maxZ), formed);
            }
            for (int z = minZ + 1; z < maxZ; z++) {
                flipCell(level, m.set(minX, y, z), formed);
                flipCell(level, m.set(maxX, y, z), formed);
            }
        }
    }

    private static void flipCell(ServerLevel level, BlockPos pos, boolean formed) {
        BlockState s = level.getBlockState(pos);
        if (!isShell(s)) return;
        BlockState target = s.setValue(SupremeFurnaceCasingBlock.FORMED, formed);
        if (s != target) {
            level.setBlock(pos, target, BLOCK_FLIP_FLAGS);
        }
    }

    /** Any casing or hatch (all subclasses of {@link SupremeFurnaceCasingBlock}). */
    private static boolean isShell(BlockState s) {
        return s.getBlock() instanceof SupremeFurnaceCasingBlock;
    }

    private static void broadcastAdd(ServerLevel level, Region region) {
        S2CMultiblockSyncPacket packet = S2CMultiblockSyncPacket.add(region);
        forEachPlayer(level, sp -> NetworkManager.sendToPlayer(sp, packet));
    }

    private static void broadcastRemove(ServerLevel level, java.util.UUID id) {
        S2CMultiblockSyncPacket packet = S2CMultiblockSyncPacket.remove(id);
        forEachPlayer(level, sp -> NetworkManager.sendToPlayer(sp, packet));
    }

    private static void forEachPlayer(ServerLevel level, java.util.function.Consumer<ServerPlayer> fn) {
        List<ServerPlayer> players = level.players();
        for (ServerPlayer sp : players) fn.accept(sp);
    }
}
