package com.supremecrafting.furnace;

import com.supremecrafting.SupremeCrafting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-{@link ServerLevel} index of formed Supreme Furnace structures.
 *
 * <p>Holds the authoritative state for each region (currently just bounds; later: inventory,
 * lit flag, smelt progress). A secondary chunk index lets us answer
 * "what region contains this block?" in O(1) without scanning every region.
 */
public class MultiblockRegions extends SavedData {
    private static final String DATA_NAME = SupremeCrafting.MOD_ID + "_multiblocks";

    private final Map<UUID, Region> regions = new HashMap<>();
    private final Map<Long, List<UUID>> chunkIndex = new HashMap<>();

    public static SavedData.Factory<MultiblockRegions> factory() {
        return new SavedData.Factory<>(MultiblockRegions::new, MultiblockRegions::load, null);
    }

    public static MultiblockRegions get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }

    @Nullable
    public Region findContaining(BlockPos pos) {
        long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        List<UUID> ids = chunkIndex.get(key);
        if (ids == null) return null;
        for (UUID id : ids) {
            Region r = regions.get(id);
            if (r != null && r.contains(pos)) return r;
        }
        return null;
    }

    public Region create(BoundingBox bounds, Direction front) {
        Region r = new Region(UUID.randomUUID(), bounds, front);
        regions.put(r.id(), r);
        addToIndex(r);
        setDirty();
        return r;
    }

    public java.util.Collection<Region> all() {
        return regions.values();
    }

    public void remove(UUID id) {
        Region r = regions.remove(id);
        if (r == null) return;
        removeFromIndex(r);
        setDirty();
    }

    private void addToIndex(Region r) {
        forEachChunk(r.bounds(), key ->
                chunkIndex.computeIfAbsent(key, k -> new ArrayList<>()).add(r.id()));
    }

    private void removeFromIndex(Region r) {
        forEachChunk(r.bounds(), key -> {
            List<UUID> list = chunkIndex.get(key);
            if (list == null) return;
            list.remove(r.id());
            if (list.isEmpty()) chunkIndex.remove(key);
        });
    }

    private static void forEachChunk(BoundingBox b, java.util.function.LongConsumer fn) {
        int minCx = b.minX() >> 4;
        int maxCx = b.maxX() >> 4;
        int minCz = b.minZ() >> 4;
        int maxCz = b.maxZ() >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                fn.accept(ChunkPos.asLong(cx, cz));
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Region r : regions.values()) list.add(r.save(registries));
        tag.put("regions", list);
        return tag;
    }

    public static MultiblockRegions load(CompoundTag tag, HolderLookup.Provider registries) {
        MultiblockRegions data = new MultiblockRegions();
        ListTag list = tag.getList("regions", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Region r = Region.load(list.getCompound(i), registries);
            data.regions.put(r.id(), r);
            data.addToIndex(r);
        }
        return data;
    }

    @Nullable
    public Region byId(UUID id) {
        return regions.get(id);
    }
}
