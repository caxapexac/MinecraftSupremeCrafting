package com.supremecrafting.furnace;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * Pure math for mapping a (block-position, world-direction) pair inside a
 * formed {@link Region} to a vanilla furnace texel.
 *
 * <p>Convention:
 * <ul>
 *   <li>Vanilla texture is 16×16; multiblock face is {@code size} blocks wide.
 *       Divisor is {@code size/16} (32 → 2, 64 → 4, 128 → 8). One vanilla pixel
 *       maps to a {@code (size/16)²} cluster of multiblock blocks.</li>
 *   <li>Horizontal faces: {@code v=0} at top ({@code y=maxY}); {@code u}
 *       increases along {@code worldDir.getClockWise()} — i.e. left-to-right
 *       looking at the face from outside.</li>
 *   <li>Top/bottom faces: {@code u} along {@code x}, {@code v} along {@code z}.
 *       Vanilla {@code furnace_top} is rotationally symmetric so we don't
 *       orient it against {@code front}.</li>
 * </ul>
 */
public final class FaceMath {
    private FaceMath() {}

    public record FaceTexel(MultiblockFace face, int u, int v) {}

    /**
     * For each world {@link Direction}, return either the {@link FaceTexel} the
     * block's face shows (when that face is outward) or {@code null} when it
     * faces into the hollow interior. Indexed by
     * {@code Direction.get3DDataValue()} — array length 6.
     */
    public static FaceTexel[] perDirection(BlockPos pos, Region region) {
        FaceTexel[] out = new FaceTexel[6];
        BoundingBox b = region.bounds();
        Direction front = region.front();
        int divisor = region.size() / 16;
        for (Direction d : Direction.values()) {
            if (!isOutward(pos, b, d)) continue;
            MultiblockFace mbf = multiblockFaceFor(d, front);
            int[] uv = texelOnFace(pos, b, d, divisor);
            out[d.get3DDataValue()] = new FaceTexel(mbf, uv[0], uv[1]);
        }
        return out;
    }

    /** {@code true} if {@code d} points out of the bounding box from {@code pos}. */
    public static boolean isOutward(BlockPos pos, BoundingBox b, Direction d) {
        return switch (d) {
            case UP    -> pos.getY() == b.maxY();
            case DOWN  -> pos.getY() == b.minY();
            case NORTH -> pos.getZ() == b.minZ();
            case SOUTH -> pos.getZ() == b.maxZ();
            case EAST  -> pos.getX() == b.maxX();
            case WEST  -> pos.getX() == b.minX();
        };
    }

    public static MultiblockFace multiblockFaceFor(Direction worldDir, Direction front) {
        if (worldDir == Direction.UP) return MultiblockFace.TOP;
        if (worldDir == Direction.DOWN) return MultiblockFace.BOTTOM;
        if (worldDir == front) return MultiblockFace.FRONT;
        if (worldDir == front.getOpposite()) return MultiblockFace.BACK;
        if (worldDir == front.getCounterClockWise()) return MultiblockFace.LEFT;
        return MultiblockFace.RIGHT; // == front.getClockWise()
    }

    private static int[] texelOnFace(BlockPos pos, BoundingBox b, Direction worldDir, int divisor) {
        if (worldDir.getAxis() == Direction.Axis.Y) {
            int u = (pos.getX() - b.minX()) / divisor;
            int v = (pos.getZ() - b.minZ()) / divisor;
            return new int[]{u, v};
        }
        int v = 15 - (pos.getY() - b.minY()) / divisor;
        Direction right = worldDir.getClockWise();
        int u = switch (right) {
            case EAST  -> (pos.getX() - b.minX()) / divisor;
            case WEST  -> (b.maxX() - pos.getX()) / divisor;
            case SOUTH -> (pos.getZ() - b.minZ()) / divisor;
            case NORTH -> (b.maxZ() - pos.getZ()) / divisor;
            default    -> throw new IllegalStateException("non-horizontal right? " + right);
        };
        return new int[]{u, v};
    }

    /**
     * Looks up the texel for a single direction. Convenience for callers that
     * only need one face — avoids allocating the full {@code FaceTexel[6]} array.
     */
    @Nullable
    public static FaceTexel texelFor(BlockPos pos, Region region, Direction worldDir) {
        if (!isOutward(pos, region.bounds(), worldDir)) return null;
        MultiblockFace mbf = multiblockFaceFor(worldDir, region.front());
        int[] uv = texelOnFace(pos, region.bounds(), worldDir, region.size() / 16);
        return new FaceTexel(mbf, uv[0], uv[1]);
    }
}
