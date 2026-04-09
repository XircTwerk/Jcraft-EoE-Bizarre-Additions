package net.arna.jcraft.client.rendering.handler;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.NonNull;
import net.arna.jcraft.client.rendering.skybox.CrimsonSkyBoxCool;
import net.arna.jcraft.client.rendering.skybox.SkyBoxManager;
import net.arna.jcraft.common.util.BlockInfo;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class CrimsonShaderHandler extends StandShaderHandler {
    public static final CrimsonShaderHandler INSTANCE = new CrimsonShaderHandler();
    public long effectLength = 0;
    public List<BlockInfo> list = new ArrayList<>();

    @Override
    public void onWorldRendered(final @NonNull PoseStack matrices, final @NonNull Camera camera, final float tickDelta, final long nanoTime) {
        if (renderingEffect) {
            final Level world = camera.getEntity().level();
            if (list.isEmpty()) {
                list = JUtils.collectBlockInfo(world, camera.getBlockPosition(), 8);
            }
            final BlockRenderDispatcher manager = Minecraft.getInstance().getBlockRenderer();


            final MultiBufferSource.BufferSource consumer = Minecraft.getInstance().renderBuffers().bufferSource();
            for (final BlockInfo info : list) {
                matrices.pushPose();
                matrices.translate(info.pos().getX() - camera.getPosition().x,
                        info.pos().getY() - camera.getPosition().y,
                        info.pos().getZ() - camera.getPosition().z);

                manager.getModelRenderer().tesselateBlock(
                        world, manager.getBlockModel(info.state()),
                        info.state(), info.pos(), matrices,
                        consumer.getBuffer(ItemBlockRenderTypes.getChunkRenderType(info.state())),
                        true, RandomSource.create(), info.state().getSeed(info.pos()),
                        OverlayTexture.NO_OVERLAY
                );

                matrices.popPose();
            }
        }
    }

    @Override
    public void tick(final Minecraft client) {
        final SkyBoxManager skyboxManager = SkyBoxManager.getInstance();

        if (shouldRender) {
            if (!renderingEffect) {
                ticks = 0;
                renderingEffect = true;
                skyboxManager.setEnabled(true);
                skyboxManager.setCurrentSkyBox(new CrimsonSkyBoxCool());
            }
            ticks++;

            if (hasFinishedAnimation()) {
                renderingEffect = false;
                shouldRender = false;
                skyboxManager.setCurrentSkyBox(null);
                skyboxManager.setEnabled(false);
                list.clear();
            }
        } else {
            renderingEffect = false;
            skyboxManager.setCurrentSkyBox(null);
            skyboxManager.setEnabled(false);
            list.clear();
        }
    }

    private boolean hasFinishedAnimation() {
        return ticks > effectLength;
    }

    @Override
    public void renderEffect(float tickDelta) {
        // NO-OP
    }
}
