package com.supremecrafting.net;

import com.supremecrafting.SupremeCrafting;
import com.supremecrafting.furnace.Region;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server → client multiblock region sync: RESET (clear + reload all),
 * ADD (one new region), REMOVE (one UUID). The smaller-bandwidth
 * {@link S2CMultiblockLitPacket} handles incremental {@code lit} flips
 * that happen during smelting.
 */
public record S2CMultiblockSyncPacket(byte op, List<Entry> add, @Nullable UUID remove)
        implements CustomPacketPayload {
    public static final byte OP_RESET = 0;
    public static final byte OP_ADD = 1;
    public static final byte OP_REMOVE = 2;

    public static final CustomPacketPayload.Type<S2CMultiblockSyncPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SupremeCrafting.MOD_ID, "multiblock_sync"));

    public static final StreamCodec<FriendlyByteBuf, S2CMultiblockSyncPacket> STREAM_CODEC =
            StreamCodec.of(S2CMultiblockSyncPacket::encode, S2CMultiblockSyncPacket::decode);

    public record Entry(UUID id, BoundingBox bounds, Direction front, boolean lit) {
        public static Entry of(Region r) {
            return new Entry(r.id(), r.bounds(), r.front(), r.lit());
        }
    }

    public static S2CMultiblockSyncPacket reset(List<Region> regions) {
        List<Entry> entries = new ArrayList<>(regions.size());
        for (Region r : regions) entries.add(Entry.of(r));
        return new S2CMultiblockSyncPacket(OP_RESET, entries, null);
    }

    public static S2CMultiblockSyncPacket add(Region region) {
        return new S2CMultiblockSyncPacket(OP_ADD, List.of(Entry.of(region)), null);
    }

    public static S2CMultiblockSyncPacket remove(UUID id) {
        return new S2CMultiblockSyncPacket(OP_REMOVE, List.of(), id);
    }

    private static void encode(FriendlyByteBuf buf, S2CMultiblockSyncPacket p) {
        buf.writeByte(p.op);
        if (p.op == OP_REMOVE) {
            //noinspection DataFlowIssue
            buf.writeUUID(p.remove);
            return;
        }
        buf.writeVarInt(p.add.size());
        for (Entry e : p.add) {
            buf.writeUUID(e.id);
            buf.writeInt(e.bounds.minX());
            buf.writeInt(e.bounds.minY());
            buf.writeInt(e.bounds.minZ());
            buf.writeInt(e.bounds.maxX());
            buf.writeInt(e.bounds.maxY());
            buf.writeInt(e.bounds.maxZ());
            buf.writeByte(e.front.get2DDataValue());
            buf.writeBoolean(e.lit);
        }
    }

    private static S2CMultiblockSyncPacket decode(FriendlyByteBuf buf) {
        byte op = buf.readByte();
        if (op == OP_REMOVE) {
            return new S2CMultiblockSyncPacket(op, List.of(), buf.readUUID());
        }
        int n = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            UUID id = buf.readUUID();
            int x0 = buf.readInt(), y0 = buf.readInt(), z0 = buf.readInt();
            int x1 = buf.readInt(), y1 = buf.readInt(), z1 = buf.readInt();
            Direction front = Direction.from2DDataValue(buf.readByte());
            boolean lit = buf.readBoolean();
            entries.add(new Entry(id, new BoundingBox(x0, y0, z0, x1, y1, z1), front, lit));
        }
        return new S2CMultiblockSyncPacket(op, entries, null);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
