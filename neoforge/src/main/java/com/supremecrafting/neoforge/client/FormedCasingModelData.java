package com.supremecrafting.neoforge.client;

import com.supremecrafting.furnace.FaceMath;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * Per-block model data attached at chunk-bake time on the client.
 *
 * <p>Holds the outward {@link FaceMath.FaceTexel} for each of the 6 world
 * directions (index = {@code Direction.get3DDataValue()}). Entries are
 * {@code null} for inward faces; those still render but with the interior
 * sprite, not a furnace pixel.
 */
public final class FormedCasingModelData {
    public static final ModelProperty<FaceMath.FaceTexel[]> FACE_TEXELS = new ModelProperty<>();
    public static final ModelProperty<Boolean> LIT = new ModelProperty<>();

    private FormedCasingModelData() {}
}
