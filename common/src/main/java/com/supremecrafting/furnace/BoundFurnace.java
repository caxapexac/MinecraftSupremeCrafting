package com.supremecrafting.furnace;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * What the Supreme Furnace Terminal stores when bound: the dimension key and
 * UUID of a formed {@link Region}. UUID alone would be unique in practice, but
 * dimension lets the server look up the right {@code ServerLevel} in O(1).
 */
public record BoundFurnace(ResourceKey<Level> dim, UUID regionId) {
    public static final Codec<BoundFurnace> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dim").forGetter(BoundFurnace::dim),
            UUIDUtil.CODEC.fieldOf("region_id").forGetter(BoundFurnace::regionId)
    ).apply(inst, BoundFurnace::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BoundFurnace> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceKey.streamCodec(Registries.DIMENSION), BoundFurnace::dim,
                    ByteBufCodecs.fromCodec(UUIDUtil.CODEC), BoundFurnace::regionId,
                    BoundFurnace::new);
}
