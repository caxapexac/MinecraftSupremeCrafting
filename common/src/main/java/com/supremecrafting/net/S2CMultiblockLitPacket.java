package com.supremecrafting.net;

import com.supremecrafting.SupremeCrafting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Server → client: a region's {@code lit} flag changed. Cheap (~17 bytes) and
 * broadcast on every burn-state flip, which is rare enough during smelting
 * that we don't need to batch.
 */
public record S2CMultiblockLitPacket(UUID id, boolean lit) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2CMultiblockLitPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SupremeCrafting.MOD_ID, "multiblock_lit"));

    public static final StreamCodec<FriendlyByteBuf, S2CMultiblockLitPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, S2CMultiblockLitPacket::id,
                    ByteBufCodecs.BOOL, S2CMultiblockLitPacket::lit,
                    S2CMultiblockLitPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
