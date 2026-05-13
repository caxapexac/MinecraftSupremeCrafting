package com.supremecrafting.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.supremecrafting.table.SupremeTableBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Animates the Supreme Table in space — translation jitter on three axes plus
 * a slow rotational wobble. The block is otherwise normal-looking, but it
 * shimmers continuously to communicate "this isn't a regular crafting table".
 *
 * <p>The block itself returns {@code RenderShape.INVISIBLE} so vanilla's chunk
 * baker doesn't draw a static copy underneath us — the BER is now the sole
 * source of the block's visual.
 */
@Environment(EnvType.CLIENT)
public class SupremeTableRenderer implements BlockEntityRenderer<SupremeTableBlockEntity> {

    private static final float TRANSLATE_AMP = 0.04F;
    private static final float ROTATE_AMP_DEG = 4.0F;

    public SupremeTableRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(SupremeTableBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        // All math in double — float precision at gameTime+blockPosHash magnitudes
        // is too coarse and freezes the animation (frame-to-frame increments get
        // rounded away). Cast to float only at the end.
        // Phase per-block via a small hash so adjacent tables don't oscillate in lockstep.
        double phase = (be.getBlockPos().hashCode() & 0xFF) * 0.0245;
        double t = (level.getGameTime() + partialTick) / 20.0 + phase;

        float dx = (float) (TRANSLATE_AMP * Math.sin(t * 7.0));
        float dy = (float) (TRANSLATE_AMP * Math.sin(t * 11.0));
        float dz = (float) (TRANSLATE_AMP * Math.cos(t * 5.0));
        float yaw = (float) (ROTATE_AMP_DEG * Math.sin(t * 4.0));

        pose.pushPose();
        // Rotate around the block's centre.
        pose.translate(0.5F + dx, 0.5F + dy, 0.5F + dz);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-0.5F, -0.5F, -0.5F);

        BlockState state = be.getBlockState();
        // Bypass renderSingleBlock — that method short-circuits when the
        // block's render shape is INVISIBLE (which it is for us, by design,
        // so vanilla's chunk baker doesn't draw a static copy underneath).
        // Drive the model renderer directly so we still get geometry.
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = dispatcher.getBlockModel(state);
        dispatcher.getModelRenderer().renderModel(
                pose.last(),
                buffer.getBuffer(ItemBlockRenderTypes.getRenderType(state, false)),
                state, model, 1.0F, 1.0F, 1.0F, light, overlay);

        pose.popPose();
    }
}
