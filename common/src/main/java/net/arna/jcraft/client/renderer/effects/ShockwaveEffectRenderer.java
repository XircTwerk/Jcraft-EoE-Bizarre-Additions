package net.arna.jcraft.client.renderer.effects;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.component.world.CommonShockwaveHandlerComponent;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static net.arna.jcraft.api.component.world.CommonShockwaveHandlerComponent.Shockwave;

public class ShockwaveEffectRenderer {
    private static final Map<Shockwave.Type, List<ResourceLocation>> TEXTURES = new EnumMap<>(Shockwave.Type.class);

    static {
        for (Shockwave.Type type : Shockwave.Type.values()) {
            List<ResourceLocation> list = new ArrayList<>();

            // Generate ResourceLocation for each index, prefixed with type.name
            for (int i = 0; i < Shockwave.MAX_AGE; i++) {
                ResourceLocation id = JCraft.id("textures/effect/shockwave/" + type.getName() + "_" + i + ".png");
                list.add(id);
            }

            TEXTURES.put(type, list);
        }
    }
    private static final List<Shockwave> toRender = new ArrayList<>();

    public static void render(final PoseStack stack, final Vec3 camPos, final ClientLevel world, final MultiBufferSource consumerProvider) {

        final CommonShockwaveHandlerComponent shockwaveHandler = JComponentPlatformUtils.getShockwaveHandler(world);

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);

        // java.util.ConcurrentModificationException prevention
        toRender.clear();
        toRender.addAll(shockwaveHandler.getShockwaves());
        for (final Shockwave shockwave : toRender) {
            stack.pushPose();

            // Calculate matrix
            stack.translate(shockwave.getX() - camPos.x, shockwave.getY() - camPos.y, shockwave.getZ() - camPos.z);
            stack.mulPose(Axis.YP.rotationDegrees(-shockwave.getYaw()));
            stack.mulPose(Axis.XP.rotationDegrees(shockwave.getPitch()));
            final Matrix4f mat = stack.last().pose();

            // Calculate light
            final int blockLight = world.getBrightness(LightLayer.BLOCK, shockwave.getBlockPos());
            final int skyLight = world.getBrightness(LightLayer.SKY, shockwave.getBlockPos());
            final int light = LightTexture.pack(blockLight, skyLight);

            // Set texture
            RenderSystem.setShaderTexture(0,
                    TEXTURES.get(shockwave.getType())
                            .get(shockwave.getFrame())
            );

            // Setup buffer
            final Tesselator tess = Tesselator.getInstance();
            final BufferBuilder buff = tess.getBuilder();
            buff.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);

            // Fill buffer
            float min = -0.5f * shockwave.getScale();
            float max = 0.5f * shockwave.getScale();
            buff.vertex(mat, min, min, 0).color(255, 255, 255, 255).uv(0, 0).uv2(light).endVertex();
            buff.vertex(mat, max, min, 0).color(255, 255, 255, 255).uv(0, 1).uv2(light).endVertex();
            buff.vertex(mat, max, max, 0).color(255, 255, 255, 255).uv(1, 1).uv2(light).endVertex();
            buff.vertex(mat, min, max, 0).color(255, 255, 255, 255).uv(1, 0).uv2(light).endVertex();

            // Finish up
            tess.end();
            stack.popPose();
        }
    }
}
