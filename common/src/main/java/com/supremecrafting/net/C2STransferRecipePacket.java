package com.supremecrafting.net;

import com.supremecrafting.SupremeCrafting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client → server: "+" was clicked in JEI for the supplied recipe; please
 * fill the open Supreme Table at {@code tablePos} with its ingredients.
 *
 * <p>Server re-validates the player has the items + the menu is open before
 * applying the transfer (anti-cheat). EMI uses its own protocol so doesn't
 * need this packet.
 */
public record C2STransferRecipePacket(BlockPos tablePos, ResourceLocation recipeId)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2STransferRecipePacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SupremeCrafting.MOD_ID, "transfer_recipe"));

    public static final StreamCodec<FriendlyByteBuf, C2STransferRecipePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, C2STransferRecipePacket::tablePos,
            ByteBufCodecs.fromCodec(ResourceLocation.CODEC), C2STransferRecipePacket::recipeId,
            C2STransferRecipePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
