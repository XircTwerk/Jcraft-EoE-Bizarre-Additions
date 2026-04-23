package net.arna.jcraft.client.renderer.entity.stands;

import lombok.NonNull;
import mod.azure.azurelib.render.AzLayerRenderer;
import mod.azure.azurelib.render.AzRendererPipeline;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.api.stand.StandType;
import net.arna.jcraft.client.renderer.entity.StandEntityModelRenderer;
import net.arna.jcraft.common.entity.stand.AerosmithEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.UUID;

import static net.arna.jcraft.client.renderer.entity.AbstractEntityRenderer.ANIMATION_STR_TEMPLATE;
import static net.arna.jcraft.client.renderer.entity.AbstractEntityRenderer.MODEL_STR_TEMPLATE;
import static net.arna.jcraft.client.renderer.entity.stands.StandEntityRenderer.*;

/**
 * The {@link StandEntityRenderer} for {@link AerosmithEntity}.
 */
@Environment(EnvType.CLIENT)
public class AerosmithRenderer {
    public static StandEntityRenderer<AerosmithEntity> of(final @NonNull EntityRendererProvider.Context context) {
        final StandType type = JStandTypeRegistry.AEROSMITH.get();
        final var id = type.getId().getPath();
        final var animation = JCraft.id(ANIMATION_STR_TEMPLATE.formatted(id));
        final var model = type.getId().withPath(MODEL_STR_TEMPLATE.formatted(id));
        final var texture = getTextureLocation(type);

        return StandEntityRenderer.of(
                AzEntityRendererConfig
                        .<AerosmithEntity>builder(model, texture)
                        .setAnimatorProvider(() -> new AerosmithAnimator(animation))
                        .setModelRenderer((ctx, layerRenderer) -> new AerosmithModelRenderer(ctx, layerRenderer, context.getItemRenderer()))
                        .setRenderType(renderType())
                        .setPrerenderEntry(preRenderEntry())
                        .build(),
                context,
                model,
                texture
        );
    }

    public static class AerosmithAnimator extends StandAnimator<AerosmithEntity> {
        public AerosmithAnimator(final @NonNull ResourceLocation animation) {
            super(animation, false, false, 0f, 0f, 0f);
        }
    }

    public static class AerosmithModelRenderer extends StandEntityModelRenderer<AerosmithEntity> {
        private final ItemRenderer itemRenderer;

        public AerosmithModelRenderer(
                final @NonNull AzRendererPipeline<UUID, AerosmithEntity> entityRendererPipeline,
                final @NonNull AzLayerRenderer<UUID, AerosmithEntity> layerRenderer,
                final @NonNull ItemRenderer itemRenderer) {
            super(entityRendererPipeline, layerRenderer);
            this.itemRenderer = itemRenderer;
        }

        @Override
        public void render(@NonNull AzRendererPipelineContext<UUID, AerosmithEntity> ctx, boolean isReRender) {
            if (!isReRender) {
                final var entity = ctx.animatable();

                if (entity != null) {
                    final var poseStack = ctx.poseStack();
                    final var item = entity.getHeldItem();
                    if (!item.isEmpty()) {
                        poseStack.pushPose();
                        poseStack.translate(0f, -1f, 0f);
                        itemRenderer.renderStatic(
                                item,
                                ItemDisplayContext.GROUND,
                                ctx.packedLight(),
                                OverlayTexture.NO_OVERLAY,
                                poseStack,
                                ctx.multiBufferSource(),
                                entity.level(),
                                entity.getId()
                        );
                        poseStack.popPose();
                    }
                }
            }

            super.render(ctx, isReRender);
        }
    }
}
