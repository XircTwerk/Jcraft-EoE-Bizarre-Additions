package net.arna.jcraft.client.renderer.entity.projectiles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.arna.jcraft.common.entity.projectile.ItemTossProjectile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

public class ItemTossProjectileRenderer extends EntityRenderer<ItemTossProjectile> {
    protected final ItemRenderer itemRenderer;
    private float scale;

    public ItemTossProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemRenderer = context.getItemRenderer();
        this.scale = 0.75F; // Slightly smaller than full size
        this.shadowRadius = 0.15F;
    }

    @Override
    public void render(ItemTossProjectile entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        if (entity.tickCount >= 2 || !(this.entityRenderDispatcher.camera.getEntity().distanceToSqr(entity) < 12.25D)) {
            poseStack.pushPose();

            // Scale thingy
            poseStack.scale(this.scale, this.scale, this.scale);

            // Simple forward spin while in air
            if (!entity.isInGround()) {
                float spin = (entity.tickCount + partialTicks) * 60.0F;
                poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
            }

            // Render the item as a 2D sprite
            this.itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.GROUND,
                    packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());

            poseStack.popPose();
            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ItemTossProjectile entity) {
        return null; // We're rendering items, not using a texture directly
    }
}