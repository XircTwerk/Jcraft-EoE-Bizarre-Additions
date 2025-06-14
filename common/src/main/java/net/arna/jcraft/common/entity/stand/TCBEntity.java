package net.arna.jcraft.common.entity.stand;

import mod.azure.azurelib.core.animation.AnimationState;
import mod.azure.azurelib.core.animation.RawAnimation;
import net.arna.jcraft.api.attack.MoveMap;
import net.arna.jcraft.api.attack.MoveSet;
import net.arna.jcraft.api.attack.MoveSetManager;
import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.api.stand.*;
import net.arna.jcraft.api.component.living.CommonHitPropertyComponent;
import net.arna.jcraft.common.attack.moves.tcb.AbsoluteDefenseMove;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Consumer;

public class TCBEntity extends StandEntity<TCBEntity, TCBEntity.State> {
    public static final MoveSet<TCBEntity, TCBEntity.State> MOVE_SET = MoveSetManager.create(JStandTypeRegistry.TCB,
            TCBEntity::registerMoves, TCBEntity.State.class);

    private boolean isUsingAbsoluteDefense = false;
    private static final float REFLECTION_RANGE = 10.0f;
    private static final float REFLECTION_DAMAGE_MULTIPLIER = 0.75f;

    public static final StandData DATA = StandData.builder()
            .idleDistance(0f)
            .idleRotation(0f)
            .blockDistance(0f)
            .info(StandInfo.builder()
                    .name(Component.translatable("entity.jcraft.tcb"))
                    .proCount(2)
                    .conCount(4)
                    .build())
            .summonData(SummonData.of(JSoundRegistry.STAND_SUMMON))
            .build();

    public static final AbsoluteDefenseMove ABSOLUTE_DEFENSE = new AbsoluteDefenseMove()
            .withSound(JSoundRegistry.IMPACT_1)
            .withInfo(
                    Component.literal("Countdown"),
                    Component.literal("saves nbt data of all entities in a 64 block radius."));

    public TCBEntity(Level worldIn) {
        super(JStandTypeRegistry.TCB.get(), worldIn);
    this.setDistanceOffset(0.0f);
    this.setRotationOffset(0.0f); // No rotation offset
        auraColors = new Vector3f[]{
                new Vector3f(1.0f, 0.2f, 0.6f),
                new Vector3f(1.0f, 0.2f, 0.6f),
                new Vector3f(1.0f, 0.2f, 0.6f),
                new Vector3f(1.0f, 0.2f, 0.6f)
        };
    }

    private static void registerMoves(MoveMap<TCBEntity, TCBEntity.State> moves) {
        moves.register(MoveClass.ULTIMATE, ABSOLUTE_DEFENSE, State.ABSOLUTE_DEFENSE);
    }

    @Override
    protected State[] getStateValues() {
        return State.values();
    }

    @Override
    public State getBlockState() {
        return null;
    }

    @Override
    protected String getSummonAnimation() {
        return null; // No summon animation, just appears like armor
    }

    @Override
    public @NotNull TCBEntity getThis() {
        return this;
    }

    @Override
    public void tick() {
        super.tick();

        // Always follow the user exactly like armor would
        if (hasUser() && !isRemote()) {
            LivingEntity user = getUserOrThrow();

            // Perfect position sync
            this.setPos(user.position());
            this.setOldPosAndRot();

            // Perfect rotation sync
            this.setYRot(user.getYRot());
            this.setXRot(user.getXRot());
            this.setYHeadRot(user.getYHeadRot());
            this.setYBodyRot(user.yBodyRot);

            // Copy all movement properties
            this.setDeltaMovement(user.getDeltaMovement());
            this.fallDistance = user.fallDistance;
            this.setOnGround(user.onGround());

            // Handle Absolute Defense
            if (isUsingAbsoluteDefense) {
                handleAbsoluteDefense();
            }
        }
    }

    @Override
    public void registerControllers(mod.azure.azurelib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        // Don't register animation controllers - we'll copy player animations in the renderer
    }

    public void activateAbsoluteDefense() {
        if (!isUsingAbsoluteDefense && hasUser()) {
            isUsingAbsoluteDefense = true;
            setState(State.ABSOLUTE_DEFENSE);

            // Stop user movement
            LivingEntity user = getUserOrThrow();
            user.setDeltaMovement(Vec3.ZERO);

            // Visual effects
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        getX(), getY() + 1, getZ(),
                        10, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }

    public void deactivateAbsoluteDefense() {
        if (isUsingAbsoluteDefense) {
            isUsingAbsoluteDefense = false;
            setState(State.valueOf(null));
        }
    }

    private void handleAbsoluteDefense() {
        if (!hasUser()) return;

        LivingEntity user = getUserOrThrow();

        // Keep user immobile
        user.setDeltaMovement(Vec3.ZERO);

        // Particle effects
        if (tickCount % 5 == 0 && level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    getX(), getY() + 1, getZ(),
                    5, 0.3, 0.3, 0.3, 0.05);
        }
    }

    @Override
    public boolean handleDamage(Vec3 kbVec, int stunTicks, int stunLevel, boolean overrideStun,
                                float damage, boolean lift, int blockstun, DamageSource source,
                                Entity attacker, CommonHitPropertyComponent.HitAnimation hitAnimation,
                                boolean canBackstab, boolean unblockable) {

        if (isUsingAbsoluteDefense) {
            // Immune to all damage except void
            if (!source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
                // Reflect damage as energy spiral
                reflectDamage(damage, attacker);
                return false; // Don't process damage
            }
        }

        return super.handleDamage(kbVec, stunTicks, stunLevel, overrideStun, damage, lift,
                blockstun, source, attacker, hitAnimation, canBackstab, unblockable);
    }

    private void reflectDamage(float originalDamage, Entity attacker) {
        if (level().isClientSide()) return;

        float reflectedDamage = originalDamage * REFLECTION_DAMAGE_MULTIPLIER;

        // Create energy spiral effect
        if (level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 20; i++) {
                double angle = (i / 20.0) * Math.PI * 2;
                double radius = i * 0.5;
                double x = getX() + Math.cos(angle) * radius;
                double z = getZ() + Math.sin(angle) * radius;
                double y = getY() + (i * 0.2);

                serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
                        x, y, z,
                        1, 0, 0, 0, 0);
            }
        }

        // Damage all entities in range
        AABB damageBox = new AABB(position().add(-REFLECTION_RANGE, -REFLECTION_RANGE, -REFLECTION_RANGE),
                position().add(REFLECTION_RANGE, REFLECTION_RANGE, REFLECTION_RANGE));

        List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, damageBox,
                entity -> entity != this && entity != getUser() && entity.isAlive());

        DamageSource reflectionSource = level().damageSources().magic();

        for (LivingEntity target : targets) {
            double distance = target.distanceTo(this);
            if (distance <= REFLECTION_RANGE) {
                float finalDamage = reflectedDamage * (1.0f - (float)(distance / REFLECTION_RANGE));
                Vec3 knockback = target.position().subtract(position()).normalize().scale(0.5);

                damageLogic(level(), target, knockback, 10, 1, false, finalDamage, false, 5,
                        reflectionSource, this, CommonHitPropertyComponent.HitAnimation.valueOf(null), false, true);
            }
        }
    }

    public boolean isUsingAbsoluteDefense() {
        return isUsingAbsoluteDefense;
    }

    @Override
    public boolean canAttack() {
        // Can only use Absolute Defense
        return super.canAttack() && !isUsingAbsoluteDefense;
    }

    @Override
    public boolean shouldOffsetHeight() {
        return false; // Always stay at exact user position
    }

    @Override
    public void idleOverride() {
        // Always stay exactly on the user
        setDistanceOffset(0.0f);
        setRotationOffset(0.0f);
    }

    public enum State implements net.arna.jcraft.common.util.StandAnimationState<TCBEntity> {
        ABSOLUTE_DEFENSE(builder -> builder.setAnimation(RawAnimation.begin().thenLoop("idle")));

        private final Consumer<AnimationState<TCBEntity>> animator;

        State(Consumer<AnimationState<TCBEntity>> animator) {
            this.animator = animator;
        }

        @Override
        public void playAnimation(TCBEntity attacker, AnimationState<TCBEntity> builder) {
            animator.accept(builder);
        }
    }
}