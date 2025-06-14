package net.arna.jcraft.common.attack.moves.highwaystar;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractSimpleAttack;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.common.entity.stand.HighwayStarEntity;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class PhaserAttack extends AbstractSimpleAttack<PhaserAttack, HighwayStarEntity> {
    private static final double TELEPORT_DISTANCE = 3.0;
    private static final double PATH_HITBOX_SIZE = 0.9;

    // Instance fields replacing MoveContext variables
    private boolean hasTeleported = false;
    private Vec3 startPos = Vec3.ZERO;
    private Vec3 endPos = Vec3.ZERO;
    private boolean hasHitTarget = false;

    public PhaserAttack(int cooldown, int windup, int duration, float moveDistance, float damage, int stun,
                        float hitboxSize, float knockback, float offset) {
        super(cooldown, windup, duration, moveDistance, damage, stun, hitboxSize, knockback, offset);
        ranged = true;
    }

    @Override
    public @NotNull MoveType<PhaserAttack> getMoveType() {
        return Type.INSTANCE;
    }

    @Override
    public void onInitiate(HighwayStarEntity attacker) {
        super.onInitiate(attacker);
        // Reset state variables
        hasTeleported = false;
        startPos = Vec3.ZERO;
        endPos = Vec3.ZERO;
        hasHitTarget = false;
    }

    @Override
    public @NonNull Set<LivingEntity> perform(HighwayStarEntity attacker, LivingEntity user) {
        Level world = attacker.level();

        // Teleport once after windup
        if (!hasTeleported && attacker.getMoveStun() <= getDuration() - getWindup()) {
            // Determine who should be teleported based on split mode
            LivingEntity teleportEntity = attacker.isSplitMode() ? attacker : user;

            Vec3 startPosition = teleportEntity.position();
            Vec3 eyePos = teleportEntity.getEyePosition();
            Vec3 lookDirection = teleportEntity.getLookAngle();

            // Store start position for hit detection
            this.startPos = startPosition;

            // Calculate end position based on where entity is looking
            Vec3 targetPos = eyePos.add(lookDirection.scale(TELEPORT_DISTANCE));

            // Raycast from eye position to check for obstacles
            HitResult hitResult = world.clip(new ClipContext(
                    eyePos,
                    targetPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    teleportEntity));

            Vec3 teleportPos;
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                // Hit a wall - teleport just before it
                teleportPos = hitResult.getLocation().subtract(lookDirection.scale(0.5));
            } else {
                // No obstacle - teleport to target position at ground level
                teleportPos = new Vec3(targetPos.x, startPosition.y, targetPos.z);
            }

            // Handle teleportation based on split mode
            if (attacker.isSplitMode()) {
                // In split mode: only teleport the stand
                attacker.teleportTo(teleportPos.x, teleportPos.y, teleportPos.z);
            } else {
                // Not in split mode: teleport both user and stand
                user.teleportTo(teleportPos.x, teleportPos.y, teleportPos.z);
                attacker.teleportTo(teleportPos.x, teleportPos.y, teleportPos.z);
            }

            this.endPos = new Vec3(teleportPos.x, teleportPos.y, teleportPos.z);
            this.hasTeleported = true;
        }

        // Check for hits along the teleport path after teleporting
        if (hasTeleported) {
            Vec3 pathStart = this.startPos;
            Vec3 pathEnd = this.endPos;

            if (pathStart == null || pathEnd == null || pathStart.equals(Vec3.ZERO) || pathEnd.equals(Vec3.ZERO)) {
                return Set.of();
            }

            Set<LivingEntity> targets = new HashSet<>();
            Vec3 pathVector = pathEnd.subtract(pathStart);
            double distance = pathStart.distanceTo(pathEnd);
            int steps = Math.max(1, (int) Math.ceil(distance * 2));

            DamageSource damageSource = world.damageSources().mobAttack(attacker.isSplitMode() ? attacker : user);
            Vec3 kbVec = pathVector.normalize();

            // Check for hits along the path
            for (int i = 0; i <= steps; i++) {
                double progress = i / (double) steps;
                Vec3 checkPos = pathStart.add(pathVector.scale(progress));

                // Create hitbox at body level (not translated up)
                AABB hitbox = new AABB(
                        checkPos.x - PATH_HITBOX_SIZE,
                        checkPos.y,
                        checkPos.z - PATH_HITBOX_SIZE,
                        checkPos.x + PATH_HITBOX_SIZE,
                        checkPos.y + 2.0,
                        checkPos.z + PATH_HITBOX_SIZE
                );

                // Debug visualization
                if (!world.isClientSide()) {
                    JUtils.displayHitbox(world, hitbox);
                }

                // Find entities
                for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class, hitbox)) {
                    if (entity != user && entity != attacker && !targets.contains(entity)) {
                        targets.add(entity);
                        processTarget(attacker, entity, kbVec, damageSource);
                        this.hasHitTarget = true;
                    }
                }
            }

            return targets;
        }

        return Set.of();
    }

    @Override
    protected void performHook(HighwayStarEntity attacker, Set<LivingEntity> targets, Set<AABB> boxes,
                               DamageSource damageSource, Vec3 forwardPos, Vec3 rotationVector) {
        super.performHook(attacker, targets, boxes, damageSource, forwardPos, rotationVector);

        // Check if move is ending and apply self-stun if missed
        if (attacker.getMoveStun() <= 1 && !hasHitTarget && !attacker.level().isClientSide()) {
            // Apply stun to the entity that was teleported
            LivingEntity stunTarget = attacker.isSplitMode() ? attacker : attacker.getUser();
            if (stunTarget != null) {
                stunTarget.addEffect(new MobEffectInstance(JStatusRegistry.DAZED.get(), 30, 0, true, false));
                if (attacker.isSplitMode()) {
                    attacker.setMoveStun(30);
                }
            }
        }
    }

    public static void cleanupStand(HighwayStarEntity stand) {
        // Cleanup if needed
    }

    @Override
    protected @NonNull PhaserAttack getThis() {
        return this;
    }

    @Override
    public @NonNull PhaserAttack copy() {
        return copyExtras(new PhaserAttack(getCooldown(), getWindup(), getDuration(), getMoveDistance(),
                getDamage(), getStun(), getHitboxSize(), getKnockback(), getOffset()));
    }

    public static class Type extends AbstractSimpleAttack.Type<PhaserAttack> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NotNull App<RecordCodecBuilder.Mu<PhaserAttack>, PhaserAttack> buildCodec(RecordCodecBuilder.Instance<PhaserAttack> instance) {
            return attackDefault(instance, PhaserAttack::new);
        }
    }
}