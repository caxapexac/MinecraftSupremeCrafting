package com.supremecrafting.neoforge.client;

import com.supremecrafting.client.ClientMultiblockRegions;
import com.supremecrafting.furnace.FaceMath;
import com.supremecrafting.furnace.MultiblockFace;
import com.supremecrafting.furnace.Region;
import com.supremecrafting.furnace.SupremeFurnaceCasingBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.List;

/**
 * Replaces the formed casing's quads with vanilla furnace texels.
 *
 * <p>One quad per outward block-face, UV-mapped to the corresponding pixel of
 * {@code furnace_top}/{@code furnace_side}/{@code furnace_front}. Inward faces
 * render with {@code coal_block} so the interior of the multiblock looks like
 * a dark void rather than a hole into nothing.
 */
public class FormedCasingModel extends BakedModelWrapper<BakedModel> {
    private static final FaceBakery FACE_BAKERY = new FaceBakery();
    private static final Vector3f FROM = new Vector3f(0, 0, 0);
    private static final Vector3f TO = new Vector3f(16, 16, 16);

    private final TextureAtlasSprite topSprite;
    private final TextureAtlasSprite sideSprite;
    private final TextureAtlasSprite frontSprite;
    private final TextureAtlasSprite frontOnSprite;
    private final TextureAtlasSprite interiorSprite;

    public FormedCasingModel(BakedModel original,
                             TextureAtlasSprite top, TextureAtlasSprite side,
                             TextureAtlasSprite front, TextureAtlasSprite frontOn,
                             TextureAtlasSprite interior) {
        super(original);
        this.topSprite = top;
        this.sideSprite = side;
        this.frontSprite = front;
        this.frontOnSprite = frontOn;
        this.interiorSprite = interior;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        Region r = ClientMultiblockRegions.findContaining(pos);
        if (r == null) return modelData;
        FaceMath.FaceTexel[] texels = FaceMath.perDirection(pos, r);
        return modelData.derive()
                .with(FormedCasingModelData.FACE_TEXELS, texels)
                .with(FormedCasingModelData.LIT, r.lit())
                .build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData extraData, @Nullable RenderType renderType) {
        if (state == null || !state.getValue(SupremeFurnaceCasingBlock.FORMED) || side == null) {
            return super.getQuads(state, side, rand, extraData, renderType);
        }
        FaceMath.FaceTexel[] texels = extraData.get(FormedCasingModelData.FACE_TEXELS);
        if (texels == null) {
            return super.getQuads(state, side, rand, extraData, renderType);
        }
        FaceMath.FaceTexel t = texels[side.get3DDataValue()];
        if (t == null) {
            return Collections.singletonList(buildQuad(side, interiorSprite, 0, 0, 16, 16));
        }
        Boolean lit = extraData.get(FormedCasingModelData.LIT);
        TextureAtlasSprite sprite = spriteFor(t.face(), Boolean.TRUE.equals(lit));
        return Collections.singletonList(buildQuad(side, sprite, t.u(), t.v(), t.u() + 1, t.v() + 1));
    }

    /**
     * Vanilla {@code getQuads(state, side, rand)} is what
     * {@link BakedModel#getQuads(BlockState, Direction, RandomSource)} delegates
     * to — chunk renderer goes through the NeoForge five-arg version, but block
     * particles and a few other paths still call this. Forward to the same
     * logic with EMPTY data so we degrade to the placeholder rather than
     * spitting out the original's quads.
     */
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    private TextureAtlasSprite spriteFor(MultiblockFace face, boolean lit) {
        return switch (face) {
            case TOP, BOTTOM -> topSprite;
            case FRONT -> lit ? frontOnSprite : frontSprite;
            case BACK, LEFT, RIGHT -> sideSprite;
        };
    }

    private static BakedQuad buildQuad(Direction side, TextureAtlasSprite sprite,
                                       float u0, float v0, float u1, float v1) {
        BlockFaceUV uv = new BlockFaceUV(new float[]{u0, v0, u1, v1}, 0);
        BlockElementFace face = new BlockElementFace(null, -1, "", uv);
        return FACE_BAKERY.bakeQuad(FROM, TO, face, sprite, side,
                BlockModelRotation.X0_Y0, null, true);
    }
}
