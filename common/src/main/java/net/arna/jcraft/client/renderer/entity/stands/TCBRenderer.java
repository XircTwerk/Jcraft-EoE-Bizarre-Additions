package net.arna.jcraft.client.renderer.entity.stands;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.azure.azurelib.cache.object.BakedGeoModel;
import mod.azure.azurelib.cache.object.GeoBone;
import net.arna.jcraft.client.model.entity.stand.TCBModel;
import net.arna.jcraft.common.entity.stand.TCBEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class TCBRenderer extends ArmorStandRenderer<TCBEntity> {

    public TCBRenderer(final EntityRendererProvider.Context context) {
        super(context, new TCBModel());
    }

    @Override
    public void actuallyRender(final PoseStack poseStack, final TCBEntity animatable,
                               final BakedGeoModel model, final RenderType renderType,
                               final MultiBufferSource bufferSource, final VertexConsumer buffer,
                               final boolean isReRender, final float partialTick, final int packedLight,
                               final int packedOverlay, final float red, final float green,
                               final float blue, final float alpha) {

        final float a = StandEntityRenderer.getAlpha(animatable, partialTick);

        // Handle Absolute Defense pose
        if (animatable.isUsingAbsoluteDefense()) {
            applyKneelingPose(model);
        }

        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, a);
    }

    private void applyKneelingPose(BakedGeoModel model) {
        // Kneeling pose for Absolute Defense
        GeoBone rightLeg = model.getBone("bipedRightLeg").orElse(null);
        GeoBone leftLeg = model.getBone("bipedLeftLeg").orElse(null);
        GeoBone body = model.getBone("bipedBody").orElse(null);

        if (rightLeg != null) {
            rightLeg.setRotX(1.5708f); // 90 degrees
            rightLeg.setPosY(rightLeg.getPosY() - 6);
        }

        if (leftLeg != null) {
            leftLeg.setRotX(1.5708f); // 90 degrees
            leftLeg.setPosY(leftLeg.getPosY() - 6);
        }

        if (body != null) {
            body.setPosY(body.getPosY() - 8); // Lower the body
            body.setRotX(0.3f); // Slight forward lean
        }
    }
}