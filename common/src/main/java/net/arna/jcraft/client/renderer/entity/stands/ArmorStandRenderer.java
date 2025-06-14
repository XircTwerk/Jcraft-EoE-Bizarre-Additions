package net.arna.jcraft.client.renderer.entity.stands;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import mod.azure.azurelib.cache.object.BakedGeoModel;
import mod.azure.azurelib.cache.object.GeoBone;
import mod.azure.azurelib.util.RenderUtils;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.client.model.entity.stand.StandEntityModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Renderer for armor-type stands that perfectly mirrors player movement
 */
public class ArmorStandRenderer<T extends StandEntity<?, ?>> extends StandEntityRenderer<T> {

    // Bone name constants matching the model
    private static final String HEAD_BONE = "bipedHead";
    private static final String BODY_BONE = "bipedBody";
    private static final String RIGHT_ARM_BONE = "bipedRightArm";
    private static final String LEFT_ARM_BONE = "bipedLeftArm";
    private static final String RIGHT_LEG_BONE = "bipedRightLeg";
    private static final String LEFT_LEG_BONE = "bipedLeftLeg";

    // Caches for performance
    private final Map<String, BoneTransform> transformCache = new HashMap<>();
    private final Map<String, Vec3> bonePositionOffsets = new HashMap<>();
    private final Map<String, ModelPart> modelPartCache = new HashMap<>();

    // State tracking
    private PlayerModel<AbstractClientPlayer> lastPlayerModel;
    private float lastSyncTick = -1;
    private boolean requiresFullSync = true;

    protected ArmorStandRenderer(EntityRendererProvider.Context context, StandEntityModel<T> model) {
        super(context, model);
        initializeBoneOffsets();
    }

    private void initializeBoneOffsets() {
        // Define position offsets to properly center bones on player model
        bonePositionOffsets.put(HEAD_BONE, Vec3.ZERO);
        bonePositionOffsets.put(BODY_BONE, Vec3.ZERO);
        bonePositionOffsets.put(RIGHT_ARM_BONE, new Vec3(-5.0, 2.0, 0.0));
        bonePositionOffsets.put(LEFT_ARM_BONE, new Vec3(5.0, 2.0, 0.0));
        bonePositionOffsets.put(RIGHT_LEG_BONE, new Vec3(-1.9, 12.0, 0.0));
        bonePositionOffsets.put(LEFT_LEG_BONE, new Vec3(1.9, 12.0, 0.0));
    }

    @Override
    public void preRender(PoseStack poseStack, T stand, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {

        // Check if we need to sync with player
        if (shouldSyncWithPlayer(stand, partialTick)) {
            performPlayerSync(model, stand, partialTick);
        }

        // Apply any stand-specific modifications
        applyStandSpecificTransforms(model, stand, partialTick);

        super.preRender(poseStack, stand, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private boolean shouldSyncWithPlayer(T stand, float partialTick) {
        return stand.hasUser() &&
                stand.getUser() instanceof Player &&
                (requiresFullSync || lastSyncTick != partialTick);
    }

    private void performPlayerSync(BakedGeoModel model, T stand, float partialTick) {
        if (!(stand.getUser() instanceof AbstractClientPlayer player)) return;

        PlayerRenderer playerRenderer = getPlayerRenderer(player);
        if (playerRenderer == null) return;

        PlayerModel<AbstractClientPlayer> playerModel = playerRenderer.getModel();

        // Setup player model animation state
        preparePlayerModel(playerModel, player, partialTick);

        // Cache model parts for efficiency
        cacheModelParts(playerModel);

        // Sync all bones with perfect centering
        syncAllBones(model, playerModel, player, partialTick);

        // Apply player state-based adjustments
        applyPlayerStateAdjustments(model, player, partialTick);

        // Update tracking variables
        lastPlayerModel = playerModel;
        lastSyncTick = partialTick;
        requiresFullSync = false;
    }

    private void preparePlayerModel(PlayerModel<AbstractClientPlayer> playerModel,
                                    AbstractClientPlayer player, float partialTick) {
        float limbSwing = player.walkAnimation.position(partialTick);
        float limbSwingAmount = player.walkAnimation.speed(partialTick);
        float ageInTicks = player.tickCount + partialTick;
        float netHeadYaw = player.getYHeadRot() - player.yBodyRot;
        float headPitch = player.getXRot();

        // Setup animations
        playerModel.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // Prepare for rendering
        playerModel.prepareMobModel(player, limbSwing, limbSwingAmount, partialTick);
    }

    private void cacheModelParts(PlayerModel<AbstractClientPlayer> playerModel) {
        modelPartCache.clear();
        modelPartCache.put(HEAD_BONE, playerModel.head);
        modelPartCache.put(BODY_BONE, playerModel.body);
        modelPartCache.put(RIGHT_ARM_BONE, playerModel.rightArm);
        modelPartCache.put(LEFT_ARM_BONE, playerModel.leftArm);
        modelPartCache.put(RIGHT_LEG_BONE, playerModel.rightLeg);
        modelPartCache.put(LEFT_LEG_BONE, playerModel.leftLeg);
    }

    private void syncAllBones(BakedGeoModel model, PlayerModel<AbstractClientPlayer> playerModel,
                              AbstractClientPlayer player, float partialTick) {
        // Sync each bone with centering
        syncAndCenterBone(model, HEAD_BONE, playerModel.head);
        syncAndCenterBone(model, BODY_BONE, playerModel.body);
        syncAndCenterBone(model, RIGHT_ARM_BONE, playerModel.rightArm);
        syncAndCenterBone(model, LEFT_ARM_BONE, playerModel.leftArm);
        syncAndCenterBone(model, RIGHT_LEG_BONE, playerModel.rightLeg);
        syncAndCenterBone(model, LEFT_LEG_BONE, playerModel.leftLeg);

        // Apply arm swing for held items
        applyArmSwing(model, player, partialTick);
    }

    private void syncAndCenterBone(BakedGeoModel model, String boneName, ModelPart modelPart) {
        Optional<GeoBone> boneOpt = model.getBone(boneName);
        if (boneOpt.isEmpty() || modelPart == null) return;

        GeoBone bone = boneOpt.get();

        // Copy rotations
        bone.setRotX(modelPart.xRot);
        bone.setRotY(modelPart.yRot);
        bone.setRotZ(modelPart.zRot);

        // Calculate centered position
        Vector3f centerPos = calculateCenteredPosition(modelPart, boneName);
        bone.setPosX(centerPos.x);
        bone.setPosY(centerPos.y);
        bone.setPosZ(centerPos.z);

        // Store transform
        storeTransform(boneName, bone);
    }

    private Vector3f calculateCenteredPosition(ModelPart modelPart, String boneName) {
        // Get base position from model part
        float baseX = modelPart.x;
        float baseY = modelPart.y;
        float baseZ = modelPart.z;

        // Apply bone-specific offset
        Vec3 offset = bonePositionOffsets.getOrDefault(boneName, Vec3.ZERO);

        // Calculate center with offset
        return new Vector3f(
                baseX + (float)offset.x,
                baseY + (float)offset.y,
                baseZ + (float)offset.z
        );
    }

    private void applyArmSwing(BakedGeoModel model, AbstractClientPlayer player, float partialTick) {
        // Handle item usage animations
        if (player.isUsingItem()) {
            ItemStack useItem = player.getUseItem();
            InteractionHand usedHand = player.getUsedItemHand();
            HumanoidArm activeArm = usedHand == InteractionHand.MAIN_HAND ?
                    player.getMainArm() : player.getMainArm().getOpposite();

            applyItemUseAnimation(model, useItem, activeArm, player.getTicksUsingItem(), partialTick);
        }

        // Handle attack swing
        if (player.swingTime > 0) {
            applyAttackSwing(model, player, partialTick);
        }
    }

    private void applyItemUseAnimation(BakedGeoModel model, ItemStack item, HumanoidArm arm,
                                       int useTicks, float partialTick) {
        String armBone = arm == HumanoidArm.RIGHT ? RIGHT_ARM_BONE : LEFT_ARM_BONE;
        Optional<GeoBone> boneOpt = model.getBone(armBone);

        boneOpt.ifPresent(bone -> {
            float useProgress = Math.min(useTicks / 20.0f, 1.0f);

            switch (item.getUseAnimation()) {
                case EAT, DRINK -> {
                    bone.setRotX(-1.2f - 0.2f * Mth.sin(useProgress * Mth.PI));
                    bone.setRotY(arm == HumanoidArm.RIGHT ? -0.3f : 0.3f);
                }
                case BLOCK -> {
                    bone.setRotX(-0.9f);
                    bone.setRotY(arm == HumanoidArm.RIGHT ? -0.4f : 0.4f);
                }
                case BOW -> {
                    bone.setRotX(-1.5708f + useProgress * 0.3f);
                    bone.setRotY(arm == HumanoidArm.RIGHT ? 0.1f : -0.1f);
                }
                case SPEAR -> {
                    bone.setRotX(-2.4f + 0.2f * useProgress);
                    bone.setRotY(arm == HumanoidArm.RIGHT ? 0.3f : -0.3f);
                }
            }
        });
    }

    private void applyAttackSwing(BakedGeoModel model, AbstractClientPlayer player, float partialTick) {
        HumanoidArm swingArm = player.getMainArm();
        String armBone = swingArm == HumanoidArm.RIGHT ? RIGHT_ARM_BONE : LEFT_ARM_BONE;

        float swingProgress = player.getAttackAnim(partialTick);
        float swingAngle = Mth.sin(swingProgress * Mth.PI);

        model.getBone(armBone).ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - swingAngle * 1.2f);
            bone.setRotZ(swingAngle * 0.3f);
        });
    }

    private void applyPlayerStateAdjustments(BakedGeoModel model, AbstractClientPlayer player,
                                             float partialTick) {
        // Crouch adjustments
        if (player.isCrouching()) {
            applyCrouchingAdjustments(model);
        }

        // Swimming adjustments
        if (player.isSwimming()) {
            applySwimmingAdjustments(model, partialTick);
        }

        // Sleeping adjustments
        if (player.isSleeping()) {
            applySleepingAdjustments(model, player);
        }

        // Elytra flying
        if (player.isFallFlying()) {
            applyElytraFlyingAdjustments(model, player, partialTick);
        }

        // Spin attack
        if (player.isAutoSpinAttack()) {
            applySpinAttackAdjustments(model, player, partialTick);
        }

        // Death animation
        if (player.deathTime > 0) {
            applyDeathAnimationAdjustments(model, player, partialTick);
        }
    }

    private void applyCrouchingAdjustments(BakedGeoModel model) {
        model.getBone(BODY_BONE).ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + 0.5f);
            bone.setPosY(bone.getPosY() - 3.0f);
        });

        model.getBone(HEAD_BONE).ifPresent(bone -> {
            bone.setPosY(bone.getPosY() - 3.0f);
        });
    }

    private void applySwimmingAdjustments(BakedGeoModel model, float partialTick) {
        model.getBone(BODY_BONE).ifPresent(bone -> {
            bone.setRotX(-1.5708f); // 90 degrees
        });

        // Swimming arm movements
        float swimProgress = partialTick * 0.6f;
        model.getBone(RIGHT_ARM_BONE).ifPresent(bone -> {
            bone.setRotX(-2.0f + 0.4f * Mth.sin(swimProgress));
            bone.setRotY(0.2f);
        });
        model.getBone(LEFT_ARM_BONE).ifPresent(bone -> {
            bone.setRotX(-2.0f + 0.4f * Mth.sin(swimProgress + Mth.PI));
            bone.setRotY(-0.2f);
        });
    }

    private void applySleepingAdjustments(BakedGeoModel model, AbstractClientPlayer player) {
        model.getBone(BODY_BONE).ifPresent(bone -> {
            bone.setRotZ(1.5708f); // 90 degrees on side
        });

        // Adjust based on bed direction
        if (player.getBedOrientation() != null) {
            float bedRotation = getDirectionRotation(player.getBedOrientation());
            model.getBone(BODY_BONE).ifPresent(bone -> {
                bone.setRotY(bone.getRotY() + bedRotation);
            });
        }
    }

    private void applyElytraFlyingAdjustments(BakedGeoModel model, AbstractClientPlayer player,
                                              float partialTick) {
        Vec3 lookAngle = player.getLookAngle();
        float pitch = (float)Math.asin(lookAngle.y) * 180.0f / Mth.PI;

        model.getBone(BODY_BONE).ifPresent(bone -> {
            bone.setRotX(Mth.lerp(partialTick, bone.getRotX(), pitch * Mth.DEG_TO_RAD));
        });

        // Arms back for gliding
        model.getBone(RIGHT_ARM_BONE).ifPresent(bone -> {
            bone.setRotX(0.5f);
            bone.setRotZ(-0.4f);
        });
        model.getBone(LEFT_ARM_BONE).ifPresent(bone -> {
            bone.setRotX(0.5f);
            bone.setRotZ(0.4f);
        });
    }

    private void applySpinAttackAdjustments(BakedGeoModel model, AbstractClientPlayer player,
                                            float partialTick) {
        float spinRotation = (player.tickCount + partialTick) * -75.0f;

        model.getBone(BODY_BONE).ifPresent(bone -> {
            bone.setRotY(spinRotation * Mth.DEG_TO_RAD);
        });

        // Arms out for spinning
        model.getBone(RIGHT_ARM_BONE).ifPresent(bone -> {
            bone.setRotZ(-1.5708f);
        });
        model.getBone(LEFT_ARM_BONE).ifPresent(bone -> {
            bone.setRotZ(1.5708f);
        });
    }

    private void applyDeathAnimationAdjustments(BakedGeoModel model, AbstractClientPlayer player,
                                                float partialTick) {
        float deathProgress = (player.deathTime + partialTick - 1.0f) / 20.0f * 1.6f;
        float rotation = Math.min(Mth.sqrt(deathProgress), 1.0f) * 90.0f;

        model.getBone(BODY_BONE).ifPresent(bone -> {
            bone.setRotZ(rotation * Mth.DEG_TO_RAD);
        });
    }

    private void applyStandSpecificTransforms(BakedGeoModel model, T stand, float partialTick) {
        if (stand.blocking) {
            applyDefensivePose(model);
        }

        // Apply scale based on stand power
        float powerScale = calculatePowerScale(stand);
        if (powerScale != 1.0f) {
            applyPowerScaling(model, powerScale);
        }
    }

    private void applyDefensivePose(BakedGeoModel model) {
        // Arms crossed defensive pose
        model.getBone(RIGHT_ARM_BONE).ifPresent(bone -> {
            bone.setRotX(-0.8f);
            bone.setRotY(-0.8f);
            bone.setRotZ(-0.2f);
        });
        model.getBone(LEFT_ARM_BONE).ifPresent(bone -> {
            bone.setRotX(-0.8f);
            bone.setRotY(0.8f);
            bone.setRotZ(0.2f);
        });
    }

    private void applyPowerScaling(BakedGeoModel model, float scale) {
        model.topLevelBones().forEach(bone -> {
            bone.setScaleX(scale);
            bone.setScaleY(scale);
            bone.setScaleZ(scale);
        });
    }

    @Override
    public void actuallyRender(PoseStack poseStack, T animatable, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {

        // Apply smooth interpolation
        applySmoothInterpolation(model, partialTick);

        // Calculate final alpha
        float finalAlpha = getAlpha(animatable, partialTick) * alpha;

        if (finalAlpha <= 0.01f) return;

        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, finalAlpha);
    }

    private void applySmoothInterpolation(BakedGeoModel model, float partialTick) {
        float lerpFactor = 0.6f; // Smoothing factor

        transformCache.forEach((boneName, prevTransform) -> {
            model.getBone(boneName).ifPresent(bone -> {
                // Smooth rotation interpolation
                bone.setRotX(Mth.rotLerp(lerpFactor, prevTransform.rotX, bone.getRotX()));
                bone.setRotY(Mth.rotLerp(lerpFactor, prevTransform.rotY, bone.getRotY()));
                bone.setRotZ(Mth.rotLerp(lerpFactor, prevTransform.rotZ, bone.getRotZ()));

                // Smooth position interpolation
                bone.setPosX(Mth.lerp(lerpFactor, prevTransform.posX, bone.getPosX()));
                bone.setPosY(Mth.lerp(lerpFactor, prevTransform.posY, bone.getPosY()));
                bone.setPosZ(Mth.lerp(lerpFactor, prevTransform.posZ, bone.getPosZ()));
            });
        });
    }

    private PlayerRenderer getPlayerRenderer(AbstractClientPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer) {
            return renderer;
        }
        return null;
    }

    private void storeTransform(String boneName, GeoBone bone) {
        transformCache.put(boneName, new BoneTransform(
                bone.getRotX(), bone.getRotY(), bone.getRotZ(),
                bone.getPosX(), bone.getPosY(), bone.getPosZ()
        ));
    }

    private float calculatePowerScale(T stand) {
        // Calculate scale based on stand's power level
        return 1.0f; // Override in subclasses
    }

    private float getDirectionRotation(net.minecraft.core.Direction direction) {
        return switch (direction) {
            case NORTH -> 180.0f;
            case SOUTH -> 0.0f;
            case WEST -> 90.0f;
            case EAST -> -90.0f;
            default -> 0.0f;
        };
    }

    public void markRequiresFullSync() {
        this.requiresFullSync = true;
    }

    private static class BoneTransform {
        final float rotX, rotY, rotZ;
        final float posX, posY, posZ;

        BoneTransform(float rotX, float rotY, float rotZ, float posX, float posY, float posZ) {
            this.rotX = rotX;
            this.rotY = rotY;
            this.rotZ = rotZ;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
        }
    }
}