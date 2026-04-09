package net.arna.jcraft.client.rendering.skybox;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.arna.jcraft.JCraft;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Objects;

public class CrimsonSkyBoxCool implements JSkyBox {
    public Rotation rotation = Rotation.DEFAULT;
    public float alpha = 10;
    protected Textures.Texture texture = new Textures.Texture(JCraft.id("textures/environment/time_erase/space.png"));
    public Textures textures;

    public CrimsonSkyBoxCool() {
        this.textures = new Textures(
                texture.withUV(1.0F / 3.0F, 1.0F / 2.0F, 2.0F / 3.0F, 1),
                texture.withUV(2.0F / 3.0F, 0, 1, 1.0F / 2.0F),
                texture.withUV(2.0F / 3.0F, 1.0F / 2.0F, 1, 1),
                texture.withUV(0, 1.0F / 2.0F, 1.0F / 3.0F, 1),
                texture.withUV(1.0F / 3.0F, 0, 2.0F / 3.0F, 1.0F / 2.0F),
                texture.withUV(0, 0, 1.0F / 3.0F, 1.0F / 2.0F)
        );
    }

    @Override
    public float getAlpha() {
        return alpha;
    }

    private boolean isShouldRotate() {
        return true;
    }

    @Override
    public void render(final PoseStack matrices, final Matrix4f matrix4f, final float tickDelta, final Camera camera, final boolean thickFog) {
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);

        final ClientLevel world = Objects.requireNonNull(Minecraft.getInstance().level);

        Vector3f rotationStatic = this.rotation.staticRot();

        matrices.pushPose();
        final double timeRotation = isShouldRotate() ? 360.0D * Mth.positiveModulo(world.dayTime() / (24000.D / this.rotation.rotationSpeed()) + 0.75D, 1) : 0D;
        this.applyTimeRotation(matrices, (float) timeRotation);
        matrices.mulPose(Axis.XP.rotationDegrees(rotationStatic.x()));
        matrices.mulPose(Axis.YP.rotationDegrees(rotationStatic.y()));
        matrices.mulPose(Axis.ZP.rotationDegrees(rotationStatic.z()));
        this.renderSkybox(matrices, tickDelta);
        matrices.mulPose(Axis.ZP.rotationDegrees(rotationStatic.z()));
        matrices.mulPose(Axis.YP.rotationDegrees(rotationStatic.y()));
        matrices.mulPose(Axis.XP.rotationDegrees(rotationStatic.x()));
        matrices.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderSkybox(PoseStack matrices, float tickDelta) {
        final Tesselator tesselator = Tesselator.getInstance();
        final BufferBuilder bufferBuilder = tesselator.getBuilder();

        for (int i = 0; i < 6; ++i) {
            final Textures.Texture tex = this.textures.byId(i);
            matrices.pushPose();

            RenderSystem.setShaderTexture(0, tex.textureId());

            if (i == 1) {
                matrices.mulPose(Axis.XP.rotationDegrees(90.0F));
            } else if (i == 2) {
                matrices.mulPose(Axis.XP.rotationDegrees(-90.0F));
                matrices.mulPose(Axis.YP.rotationDegrees(180.0F));
            } else if (i == 3) {
                matrices.mulPose(Axis.XP.rotationDegrees(180.0F));
            } else if (i == 4) {
                matrices.mulPose(Axis.ZP.rotationDegrees(90.0F));
                matrices.mulPose(Axis.YP.rotationDegrees(-90.0F));
            } else if (i == 5) {
                matrices.mulPose(Axis.ZP.rotationDegrees(-90.0F));
                matrices.mulPose(Axis.YP.rotationDegrees(90.0F));
            }

            final Matrix4f matrix4f = matrices.last().pose();
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, -100.0F).uv(tex.minU(), tex.minV()).color(1f, 1f, 1f, alpha).endVertex();
            bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, 100.0F).uv(tex.minU(), tex.maxV()).color(1f, 1f, 1f, alpha).endVertex();
            bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, 100.0F).uv(tex.maxU(), tex.maxV()).color(1f, 1f, 1f, alpha).endVertex();
            bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, -100.0F).uv(tex.maxU(), tex.minV()).color(1f, 1f, 1f, alpha).endVertex();
            tesselator.end();
            matrices.popPose();
        }
    }

    private void applyTimeRotation(PoseStack matrices, float timeRotation) {
        Vector3f timeRotationAxis = this.rotation.axisRot();
        matrices.mulPose(Axis.XP.rotationDegrees(timeRotationAxis.x()));
        matrices.mulPose(Axis.YP.rotationDegrees(timeRotationAxis.y()));
        matrices.mulPose(Axis.ZP.rotationDegrees(timeRotationAxis.z()));
        matrices.mulPose(Axis.YP.rotationDegrees(timeRotation));
        matrices.mulPose(Axis.ZN.rotationDegrees(timeRotationAxis.z()));
        matrices.mulPose(Axis.YN.rotationDegrees(timeRotationAxis.y()));
        matrices.mulPose(Axis.XN.rotationDegrees(timeRotationAxis.x()));
    }
}
