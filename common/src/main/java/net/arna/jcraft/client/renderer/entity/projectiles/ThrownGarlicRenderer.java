package net.arna.jcraft.client.renderer.entity.projectiles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.arna.jcraft.common.entity.projectile.ThrownGarlic;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

public class ThrownGarlicRenderer extends EntityRenderer<ThrownGarlic> {
    private final ItemRenderer itemRenderer;
    private final float scale;

    public ThrownGarlicRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.scale = 0.75F; // Slightly smaller than full size
        this.shadowRadius = 0.15F;
    }

    @Override
    public void render(ThrownGarlic entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        if (entity.tickCount >= 2 || !(this.entityRenderDispatcher.camera.getEntity().distanceToSqr(entity) < 12.25D)) {
            poseStack.pushPose();

            // Scale down slightly
            poseStack.scale(this.scale, this.scale, this.scale);

            // Rotate to face the camera (billboard rendering)
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

            // Counter-rotate on Y axis to keep it upright
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

            // Add spinning animation
            float spin = (entity.tickCount + partialTicks) * 20.0F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(spin));

            // Render the item as a 2D sprite
            this.itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.GROUND,
                    packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());

            poseStack.popPose();
            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownGarlic entity) {
        return TextureAtlas.LOCATION_BLOCKS; // Uses the item texture atlas
    }
}