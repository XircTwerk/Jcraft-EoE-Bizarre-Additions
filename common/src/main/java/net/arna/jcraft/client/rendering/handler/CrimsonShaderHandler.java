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
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public class CrimsonShaderHandler extends StandShaderHandler {
    public static final CrimsonShaderHandler INSTANCE = new CrimsonShaderHandler();
    public long effectLength = 0;
    public List<BlockInfo> list = new ArrayList<>();

    // Store the camera state when effect starts
    private Vec3 initialCameraPosition = Vec3.ZERO;
    private BlockPos initialCameraBlockPos = BlockPos.ZERO;
    private Quaternionf initialCameraRotation = new Quaternionf();

    @Override
    public void onWorldRendered(final @NonNull PoseStack matrices, final @NonNull Camera camera, final float tickDelta, final long nanoTime) {
        if (renderingEffect) {
            final Level world = camera.getEntity().level();
            if (list.isEmpty()) {
                // Collect blocks relative to initial camera position
                list = JUtils.collectBlockInfo(world, initialCameraBlockPos, 8);
            }
            final BlockRenderDispatcher manager = Minecraft.getInstance().getBlockRenderer();
            final MultiBufferSource.BufferSource consumer = Minecraft.getInstance().renderBuffers().bufferSource();

            // Save current matrix state
            matrices.pushPose();

            // Apply initial camera rotation to lock the orientation
            matrices.mulPose(initialCameraRotation);

            // Get current camera position for relative rendering
            Vec3 currentCameraPos = camera.getPosition();

            for (final BlockInfo info : list) {
                matrices.pushPose();

                // This keeps blocks in the same position relative to initial view
                Vec3 blockWorldPos = new Vec3(info.pos().getX(), info.pos().getY(), info.pos().getZ());
                Vec3 relativePos = blockWorldPos.subtract(initialCameraPosition);

                matrices.translate(relativePos.x, relativePos.y, relativePos.z);

                manager.getModelRenderer().tesselateBlock(
                        world, manager.getBlockModel(info.state()),
                        info.state(), info.pos(), matrices,
                        consumer.getBuffer(ItemBlockRenderTypes.getChunkRenderType(info.state())),
                        true, RandomSource.create(), info.state().getSeed(info.pos()),
                        OverlayTexture.NO_OVERLAY
                );

                matrices.popPose();
            }

            matrices.popPose();
        }
    }

    @Override
    public void tick(final Minecraft client) {
        final SkyBoxManager skyboxManager = SkyBoxManager.getInstance();

        if (shouldRender) {
            if (!renderingEffect) {
                ticks = 0;
                renderingEffect = true;

                // Capture camera state at effect start
                Camera camera = client.gameRenderer.getMainCamera();
                initialCameraPosition = camera.getPosition();
                initialCameraBlockPos = camera.getBlockPosition();
                initialCameraRotation = camera.rotation();

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
                initialCameraPosition = Vec3.ZERO;
                initialCameraBlockPos = BlockPos.ZERO;
                initialCameraRotation = new Quaternionf();
            }
        } else {
            renderingEffect = false;
            skyboxManager.setCurrentSkyBox(null);
            skyboxManager.setEnabled(false);
            list.clear();
            initialCameraPosition = Vec3.ZERO;
            initialCameraBlockPos = BlockPos.ZERO;
            initialCameraRotation = new Quaternionf();
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