package net.arna.jcraft.client.renderer.entity.stands;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.azure.azurelib.cache.object.BakedGeoModel;
import mod.azure.azurelib.renderer.GeoEntityRenderer;
import net.arna.jcraft.client.model.entity.stand.HighwayStarModel;
import net.arna.jcraft.common.entity.stand.HighwayStarEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * The renderer for {@link HighwayStarEntity}.
 * @see HighwayStarModel
 */
public class HighwayStarRenderer extends GeoEntityRenderer<HighwayStarEntity> {
    public HighwayStarRenderer(final EntityRendererProvider.Context context) {
        super(context, new HighwayStarModel());
    }
    @Override
    public void actuallyRender(final PoseStack poseStack, final HighwayStarEntity animatable, final BakedGeoModel model, final RenderType renderType, final MultiBufferSource bufferSource, final VertexConsumer buffer, final boolean isReRender, final float partialTick, final int packedLight, final int packedOverlay, final float red, final float green, final float blue, final float alpha) {
        // Apply stand-specific alpha if you have a method for it
        // final float a = getStandAlpha(animatable, partialTick);
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}