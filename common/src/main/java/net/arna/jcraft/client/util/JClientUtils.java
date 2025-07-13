package net.arna.jcraft.client.util;

import mod.azure.azurelib.core.animatable.model.CoreGeoBone;
import mod.azure.azurelib.core.animation.AnimationProcessor;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.client.model.entity.stand.StandEntityModel;
import net.arna.jcraft.api.component.living.CommonHitPropertyComponent;
import net.arna.jcraft.common.entity.stand.*;
import net.arna.jcraft.common.util.DimensionData;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.arna.jcraft.common.util.JUtils.DEG_TO_RAD;
import static net.arna.jcraft.common.util.JUtils.deltaPos;

public class JClientUtils {

    // Timestop tracking
    public static List<DimensionData> activeTimestops = new ArrayList<>();

    // Mustn't directly remove the DimensionData due to the possibility of a ConcurrentModificationException
    // Setting the timer to 0 will make the next tick remove it
    public static void removeTimestop(final int timestopperId) {
        for (DimensionData timestop : activeTimestops) {
            final Entity timestopper = timestop.user;
            if (timestopper.getId() != timestopperId) {
                continue;
            }
            timestop.timer = 0;
            return;
        }
    }

    public static boolean isInTSRange(final Entity entity) {
        if (entity == null) {
            return false;
        }

        return isInTSRange(entity.position());
    }

    public static boolean isInTSRange(final Vec3 pos) {
        for (final DimensionData timeStop : activeTimestops) {
            if (timeStop != null && timeStop.pos.distanceToSqr(pos.x(), pos.y(), pos.z()) <= 65536) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInTSRange(final BlockPos pos) {
        for (final DimensionData timeStop : activeTimestops) {
            if (timeStop != null && timeStop.pos.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= 65536) {
                return true;
            }
        }
        return false;
    }

    public static int getTicksIfInTSRange(final BlockPos pos) {
        for (final DimensionData timeStop : activeTimestops) {
            if (timeStop != null && timeStop.pos.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= 65536) {
                return timeStop.timer;
            }
        }
        return 0;
    }

    // Torso/Head rotation for stands
    public static void animateGenericHumanoid(final StandEntityModel<?> model, final StandEntity<?, ?> entity, final LivingEntity player, final float partialTick) {
        animateGenericHumanoid(model, entity, player, partialTick, false, false);
    }

    public static void animateGenericHumanoid(final StandEntityModel<?> model, final StandEntity<?, ?> entity, final LivingEntity player, final float partialTick, final boolean flipBody, final boolean flipHead) {
        animateGenericHumanoid(model, entity, player, partialTick, flipBody, flipHead, 0, 0, 90f);
    }

    public static void animateGenericHumanoid(final StandEntityModel<?> model, final StandEntity<?, ?> entity, final LivingEntity player, final float partialTick, final boolean flipBody, final boolean flipHead, final float tPO, final float hPO) {
        animateGenericHumanoid(model, entity, player, partialTick, flipBody, flipHead, tPO, hPO, 90f);
    }

    // basically, unless the animation specifies every rotation, said rotations will persist through each model.
    public static void animateGenericHumanoid(final StandEntityModel<?> model, final StandEntity<?, ?> entity, final LivingEntity player, final float partialTick, final boolean flipBody, final boolean flipHead, final float tPO, final float hPO, float velInfluence) {
        float overVel = 0;

        final AnimationProcessor<?> animationProcessor = model.getAnimationProcessor();

        if (entity.getMoveStun() < 1) {
            Vec3 playerVel = (entity.isRemote() && !entity.remoteControllable()) ? entity.getDeltaMovement() : deltaPos(player);
            overVel = Mth.clamp((float) playerVel.horizontalDistance() - 0.05f, -1f, 1f);

            // If going backwards
            if (playerVel.normalize().add(entity.getLookAngle()).horizontalDistanceSqr() < playerVel.normalize().horizontalDistanceSqr())
                velInfluence *= -1;

            // Tilt torso relative to speed
            CoreGeoBone torso = animationProcessor.getBone("torso");
            if (torso != null) {
                //model.prevTorsoPitch = torso.getRotX();
                float pitch = (180f + overVel * velInfluence) * 3.1415f / 180f;
                if (flipBody) {
                    pitch += 3.1415f;
                    pitch = -pitch;
                }
                torso.setRotX(pitch + tPO);
            }
        }

        if (entity.isBlocking() || entity.isIdle()) {
            // Look up/down, same as the stand user
            final CoreGeoBone head = animationProcessor.getBone("head");
            if (head != null) {
                //model.prevHeadPitch = head.getRotX();
                float headPitch = (player.getXRot() - overVel * velInfluence) * 3.1415f / 180f;
                if (flipHead) headPitch = -headPitch;
                head.setRotX(headPitch + hPO);
            }
        } else if (entity.getMoveStun() > 0) { // if doing something
            if (entity.shouldOffsetHeight()) {
                // Turn entire stand up/down
                final CoreGeoBone base = animationProcessor.getBone("base");
                if (base != null) {
                    //model.prevBasePitch = base.getRotX();
                    float torsoPitch = (player.getXRot() * 0.9f) * 3.1415f / 180f;
                    base.setRotX(base.getRotX() - torsoPitch);
                }
            }
        }
    }

    public static boolean shouldForceRender(Entity entity) {
        if (entity instanceof final D4CEntity d4c && d4c.getState() == D4CEntity.State.FLAG ||
                entity instanceof final KingCrimsonEntity kc && kc.getTETime() > 0 && kc.getUser() == Minecraft.getInstance().player) {
            return true;
        }
        return entity instanceof final CreamEntity cream && cream.isHalfBall();
    }

    public static boolean shouldNotRender(Entity entity) {
        // Don't block rendering for leashed mobs
        if (entity instanceof Mob mob && mob.isLeashed()) {
            return false;
        }

        final Entity passenger = entity.getFirstPassenger();
        return passenger instanceof final KingCrimsonEntity kc && kc.getTETime() > 0 ||
                passenger instanceof final D4CEntity d4c && d4c.getState() == D4CEntity.State.FLAG ||
                passenger instanceof final CreamEntity cream && cream.isHalfBall() ||
                passenger instanceof final MetallicaEntity metallica && metallica.isInvisible();
    }

    public static void resetPartAngles(final ModelPart part) {
        final PartPose defaultTransform = part.getInitialPose();
        part.xRot = defaultTransform.xRot;
        part.yRot = defaultTransform.yRot;
        part.zRot = defaultTransform.zRot;
    }

    public static void animateHit(final CommonHitPropertyComponent.HitAnimation hitAnimation, long endHitAnimTime, final Vec3 randomRotation, final ModelPart head, final @Nullable ModelPart hat, final ModelPart body, final ModelPart rightArm, final ModelPart leftArm, final ModelPart rightLeg, final ModelPart leftLeg) {
        if (endHitAnimTime > 20L) {
            endHitAnimTime = 20L;
        }
        float angDegrees = endHitAnimTime * DEG_TO_RAD;

        if (endHitAnimTime <= 1) {
            leftLeg.resetPose();
            rightLeg.resetPose();
            resetPartAngles(body);
        } else {
            body.yRot = (float) (randomRotation.x * angDegrees * 0.35);
            body.zRot = (float) (randomRotation.z * angDegrees * 0.35);
        }

        if (endHitAnimTime == 0) // If dead
        {
            return;
        }

        switch (hitAnimation) {
            case HIGH -> {
                angDegrees *= 1.5F;

                head.xRot += angDegrees;

                body.xRot -= angDegrees;

                leftLeg.z -= endHitAnimTime * 0.25F;
                rightLeg.z -= endHitAnimTime * 0.25F;

                rightArm.zRot += angDegrees;
                leftArm.zRot -= angDegrees;
            }
            case MID -> {
                angDegrees *= 1.5F;

                head.xRot += angDegrees;

                body.xRot += angDegrees;

                leftLeg.z += endHitAnimTime * 0.25F;
                rightLeg.z += endHitAnimTime * 0.25F;
                leftLeg.y -= endHitAnimTime * 0.175F;
                rightLeg.y -= endHitAnimTime * 0.175F;

                rightLeg.xRot -= angDegrees;
                leftLeg.xRot -= angDegrees;
            }
            case LOW -> {
                angDegrees *= 1.5F;

                head.xRot += angDegrees;

                body.xRot += angDegrees;

                leftLeg.z += endHitAnimTime * 0.175F;
                rightLeg.z += endHitAnimTime * 0.175F;
                leftLeg.y -= endHitAnimTime * 0.0875F;
                rightLeg.y -= endHitAnimTime * 0.0875F;

                rightLeg.xRot += angDegrees;
                leftLeg.xRot += angDegrees;

                rightArm.zRot += angDegrees;
                leftArm.zRot -= angDegrees;
            }
            case CRUSH -> {
                body.xRot += angDegrees;

                leftLeg.z += endHitAnimTime * 0.25F;
                rightLeg.z += endHitAnimTime * 0.25F;
                leftLeg.y -= endHitAnimTime * 0.175F;
                rightLeg.y -= endHitAnimTime * 0.175F;

                angDegrees *= 1.75F;

                head.xRot += Mth.sin(endHitAnimTime * 0.1F);

                rightArm.zRot += angDegrees;
                leftArm.zRot -= angDegrees;
                rightLeg.xRot -= angDegrees;
                leftLeg.xRot -= angDegrees;
            }
            case LAUNCH -> {
                //angDegrees *= 4.0F;

                head.xRot += angDegrees;

                body.xRot += angDegrees;

                leftLeg.z += endHitAnimTime * 0.125F;
                rightLeg.z += endHitAnimTime * 0.125F;
                leftLeg.y -= endHitAnimTime * 0.125F;
                rightLeg.y -= endHitAnimTime * 0.125F;

                rightArm.zRot += angDegrees;
                leftArm.zRot -= angDegrees;
                rightLeg.xRot -= angDegrees;
                leftLeg.xRot -= angDegrees;
            }
            case ROLL -> {

            }
        }
    }

    public static boolean shouldRenderStands() {
        return JUtils.shouldRenderStandsFor(Minecraft.getInstance().player);
    }
}
