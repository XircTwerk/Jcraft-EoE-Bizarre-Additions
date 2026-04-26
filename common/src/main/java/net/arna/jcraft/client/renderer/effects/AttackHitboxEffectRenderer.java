package net.arna.jcraft.client.renderer.effects;

import com.google.common.collect.EvictingQueue;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.longs.LongLongPair;
import lombok.Synchronized;
import lombok.experimental.UtilityClass;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.*;

@UtilityClass
public class AttackHitboxEffectRenderer {
    // Use an evicting queue to limit the amount of hit boxes rendered at a time to 8.
    // If there are already 8 hit boxes, and we wish to add more, old ones will be removed.
    private static final Queue<Pair<LongLongPair, AABB>> hitboxes = EvictingQueue.create(8);
    private static final List<Pair<LongLongPair, AABB>> highPriorityBoxes = new ArrayList<>(); // These do not get evicted.

    public static void init() {

    }

    public static void addHitboxes(final Iterable<AABB> boxes) {
        for (AABB box : boxes) {
            addHitbox(box);
        }
    }

    public static void addHitboxes(final AABB[] boxes) {
        for (AABB box : boxes) {
            addHitbox(box);
        }
    }

    public static void addHitbox(final AABB box) {
        addHitbox(box, 2500, false);
    }

    @Synchronized
    public static void addHitbox(final AABB box, final long duration, final boolean highPriority) {
        (highPriority ? highPriorityBoxes : hitboxes).add(Pair.of(LongLongPair.of(Util.getEpochMillis(), duration), box));
    }

    @Synchronized
    public static void render(final PoseStack matrices, final Vec3 camPos, final LevelRenderer worldRenderer, final MultiBufferSource consumerProvider) {
        if (!Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes()) {
            return;
        }

        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        matrices.pushPose();

        renderBoxes(consumerProvider, matrices, camPos, hitboxes);
        renderBoxes(consumerProvider, matrices, camPos, highPriorityBoxes);

        matrices.popPose();
    }

    private static void renderBoxes(final MultiBufferSource consumerProvider, final PoseStack matrices, final Vec3 camPos,
                                    final Collection<Pair<LongLongPair, AABB>> boxes) {
        final Iterator<Pair<LongLongPair, AABB>> iterator = boxes.iterator();
        while (iterator.hasNext()) {
            final Pair<LongLongPair, AABB> pair = iterator.next();
            renderBox(consumerProvider, pair.right(), matrices, camPos);
            if (Util.getEpochMillis() - pair.left().leftLong() > pair.left().rightLong()) {
                iterator.remove();
            }
        }
    }

    @SuppressWarnings("DuplicatedCode") // Don't care
    private static void renderBox(final MultiBufferSource consumerProvider, final AABB box, final PoseStack matrices, final Vec3 camPos) {
        // Draw faces
        final Tesselator tess = Tesselator.getInstance();
        final BufferBuilder quadsVc = tess.getBuilder();
        quadsVc.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false); // Don't write to the depth buffer.

        final int c = 0x54FF0000;

        final float minX = (float) (box.minX - camPos.x);
        final float minY = (float) (box.minY - camPos.y);
        final float minZ = (float) (box.minZ - camPos.z);
        final float maxX = (float) (box.maxX - camPos.x);
        final float maxY = (float) (box.maxY - camPos.y);
        final float maxZ = (float) (box.maxZ - camPos.z);

        final Matrix4f m = matrices.last().pose();

        // Up
        quadsVc.vertex(m, minX, maxY, minZ).color(c).endVertex();
        quadsVc.vertex(m, minX, maxY, maxZ).color(c).endVertex();
        quadsVc.vertex(m, maxX, maxY, maxZ).color(c).endVertex();
        quadsVc.vertex(m, maxX, maxY, minZ).color(c).endVertex();

        // Down
        quadsVc.vertex(m, minX, minY, minZ).color(c).endVertex();
        quadsVc.vertex(m, maxX, minY, minZ).color(c).endVertex();
        quadsVc.vertex(m, maxX, minY, maxZ).color(c).endVertex();
        quadsVc.vertex(m, minX, minY, maxZ).color(c).endVertex();

        // North
        quadsVc.vertex(m, minX, minY, minZ).color(c).endVertex();
        quadsVc.vertex(m, minX, maxY, minZ).color(c).endVertex();
        quadsVc.vertex(m, maxX, maxY, minZ).color(c).endVertex();
        quadsVc.vertex(m, maxX, minY, minZ).color(c).endVertex();

        // East
        quadsVc.vertex(m, maxX, minY, minZ).color(c).endVertex();
        quadsVc.vertex(m, maxX, maxY, minZ).color(c).endVertex();
        quadsVc.vertex(m, maxX, maxY, maxZ).color(c).endVertex();
        quadsVc.vertex(m, maxX, minY, maxZ).color(c).endVertex();

        // South
        quadsVc.vertex(m, minX, minY, maxZ).color(c).endVertex();
        quadsVc.vertex(m, maxX, minY, maxZ).color(c).endVertex();
        quadsVc.vertex(m, maxX, maxY, maxZ).color(c).endVertex();
        quadsVc.vertex(m, minX, maxY, maxZ).color(c).endVertex();

        // West
        quadsVc.vertex(m, minX, minY, minZ).color(c).endVertex();
        quadsVc.vertex(m, minX, minY, maxZ).color(c).endVertex();
        quadsVc.vertex(m, minX, maxY, maxZ).color(c).endVertex();
        quadsVc.vertex(m, minX, maxY, minZ).color(c).endVertex();

        tess.end();

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.disableDepthTest();

        // Draw lines
        final VertexConsumer linesVc = Objects.requireNonNull(consumerProvider).getBuffer(RenderType.LINES);
        final AABB newBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        LevelRenderer.renderLineBox(matrices, linesVc, newBox, 1f, 0f, 0f, 1f);
    }
}
