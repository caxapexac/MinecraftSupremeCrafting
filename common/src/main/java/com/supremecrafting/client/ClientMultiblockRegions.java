package com.supremecrafting.client;

import com.supremecrafting.furnace.Region;
import com.supremecrafting.net.S2CMultiblockSyncPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side mirror of the server's {@link com.supremecrafting.furnace.MultiblockRegions}.
 *
 * <p>Populated exclusively via {@link S2CMultiblockSyncPacket}. Every mutating
 * apply triggers a chunk re-bake over the affected bounds so the
 * {@code FormedCasingModel} on NeoForge picks up the new region info — without
 * this, chunks baked before the sync packet arrived would render with the
 * fallback placeholder model.
 */
@Environment(EnvType.CLIENT)
public final class ClientMultiblockRegions {
    /**
     * Single monitor for both maps. Writes happen on the client main thread
     * (apply() is queued via {@code ctx.queue(...)}); reads happen from chunk
     * render worker threads via {@code BakedModel#getModelData}. Without this
     * lock the worker-side HashMap reads race with main-thread writes — at
     * minimum returning stale data, at worst throwing in the middle of a
     * resize.
     */
    private static final Object LOCK = new Object();
    private static final Map<UUID, Region> REGIONS = new HashMap<>();
    private static final Map<Long, List<UUID>> CHUNK_INDEX = new HashMap<>();

    private ClientMultiblockRegions() {}

    public static void apply(S2CMultiblockSyncPacket packet) {
        List<BoundingBox> dirty = new ArrayList<>(packet.add().size() + 1);
        synchronized (LOCK) {
            switch (packet.op()) {
                case S2CMultiblockSyncPacket.OP_RESET -> {
                    for (Region r : REGIONS.values()) dirty.add(r.bounds());
                    REGIONS.clear();
                    CHUNK_INDEX.clear();
                    for (S2CMultiblockSyncPacket.Entry e : packet.add()) {
                        addEntry(e);
                        dirty.add(e.bounds());
                    }
                }
                case S2CMultiblockSyncPacket.OP_ADD -> {
                    for (S2CMultiblockSyncPacket.Entry e : packet.add()) {
                        addEntry(e);
                        dirty.add(e.bounds());
                    }
                }
                case S2CMultiblockSyncPacket.OP_REMOVE -> {
                    UUID id = packet.remove();
                    if (id != null) {
                        Region r = REGIONS.get(id);
                        if (r != null) dirty.add(r.bounds());
                        removeId(id);
                    }
                }
                default -> { /* unknown op — ignore */ }
            }
        }
        markChunksDirty(dirty);
    }

    public static void clear() {
        List<BoundingBox> dirty;
        synchronized (LOCK) {
            dirty = new ArrayList<>(REGIONS.size());
            for (Region r : REGIONS.values()) dirty.add(r.bounds());
            REGIONS.clear();
            CHUNK_INDEX.clear();
        }
        markChunksDirty(dirty);
    }

    @Nullable
    public static Region findContaining(BlockPos pos) {
        long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        synchronized (LOCK) {
            List<UUID> ids = CHUNK_INDEX.get(key);
            if (ids == null) return null;
            for (UUID id : ids) {
                Region r = REGIONS.get(id);
                if (r != null && r.contains(pos)) return r;
            }
            return null;
        }
    }

    private static void addEntry(S2CMultiblockSyncPacket.Entry e) {
        Region r = new Region(e.id(), e.bounds(), e.front());
        r.setLit(e.lit());
        REGIONS.put(r.id(), r);
        forEachChunk(r, key -> CHUNK_INDEX.computeIfAbsent(key, k -> new ArrayList<>()).add(r.id()));
    }

    /**
     * Flip a region's lit flag and dirty its chunks so the formed-casing
     * model re-bakes with the new front sprite.
     */
    public static void applyLit(UUID id, boolean lit) {
        BoundingBox dirty;
        synchronized (LOCK) {
            Region r = REGIONS.get(id);
            if (r == null || r.lit() == lit) return;
            r.setLit(lit);
            dirty = r.bounds();
        }
        Minecraft.getInstance().levelRenderer.setBlocksDirty(
                dirty.minX(), dirty.minY(), dirty.minZ(),
                dirty.maxX(), dirty.maxY(), dirty.maxZ());
    }

    private static void removeId(UUID id) {
        Region r = REGIONS.remove(id);
        if (r == null) return;
        forEachChunk(r, key -> {
            List<UUID> list = CHUNK_INDEX.get(key);
            if (list == null) return;
            list.remove(id);
            if (list.isEmpty()) CHUNK_INDEX.remove(key);
        });
    }

    private static void forEachChunk(Region r, java.util.function.LongConsumer fn) {
        int minCx = r.bounds().minX() >> 4;
        int maxCx = r.bounds().maxX() >> 4;
        int minCz = r.bounds().minZ() >> 4;
        int maxCz = r.bounds().maxZ() >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                fn.accept(ChunkPos.asLong(cx, cz));
            }
        }
    }

    private static void markChunksDirty(List<BoundingBox> boxes) {
        if (boxes.isEmpty()) return;
        LevelRenderer lr = Minecraft.getInstance().levelRenderer;
        for (BoundingBox b : boxes) {
            lr.setBlocksDirty(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ());
        }
    }
}
