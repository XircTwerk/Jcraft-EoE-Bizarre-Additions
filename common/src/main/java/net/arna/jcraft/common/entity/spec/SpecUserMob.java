package net.arna.jcraft.common.entity.spec;

import net.arna.jcraft.api.JRegistries;
import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.api.spec.JSpec;
import net.arna.jcraft.api.spec.SpecType;
import net.arna.jcraft.api.registry.JSpecTypeRegistry;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.common.util.SpecAnimationState;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class SpecUserMob extends Mob implements JSpecHolder {

    private static final EntityDataAccessor<String> ANIMATION_ID =
            SynchedEntityData.defineId(SpecUserMob.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> ANIMATION_SPEED =
            SynchedEntityData.defineId(SpecUserMob.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> SPEC_TYPE_ID =
            SynchedEntityData.defineId(SpecUserMob.class, EntityDataSerializers.STRING);

    private JSpec<?, ?> spec;
    private SpecAnimationState<?> currentState;

    protected SpecUserMob(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    // Constructor with initial spec type
    protected SpecUserMob(EntityType<? extends Mob> entityType, Level level, SpecType specType) {
        super(entityType, level);
        setSpecType(specType);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIMATION_ID, "");
        this.entityData.define(ANIMATION_SPEED, 1.0f);
        this.entityData.define(SPEC_TYPE_ID, "");
    }

    @Override
    public void tick() {
        super.tick();

        if (spec != null) {
            spec.tickSpec();
        }
    }

    @Override
    public void setSpec(JSpec<?, ?> spec) {
        this.spec = spec;
        if (spec != null) {
            setSpecTypeId(spec.getType().getId().toString());
        } else {
            setSpecTypeId("");
        }
    }

    @Override
    public JSpec<?, ?> getSpec() {
        return spec;
    }

    @Override
    public void setAnimation(String animationID, float animationSpeed) {
        if (!level().isClientSide) {
            this.entityData.set(ANIMATION_ID, animationID);
            this.entityData.set(ANIMATION_SPEED, animationSpeed);
        }
    }

    @Override
    public SpecAnimationState<?> getState() {
        return currentState;
    }

    // Sets the spec type and creates the appropriate spec instance
    public void setSpecType(SpecType specType) {
        if (specType == null || specType == JSpecTypeRegistry.NONE.get()) {
            this.spec = null;
            setSpecTypeId("");
            return;
        }

        this.spec = specType.createSpec(this);
        setSpecTypeId(specType.getId().toString());
    }

    // Gets the current spec type
    public SpecType getSpecType() {
        String specTypeId = getSpecTypeId();
        if (specTypeId.isEmpty()) {
            return null;
        }

        try {
            ResourceLocation id = new ResourceLocation(specTypeId);
            return JRegistries.SPEC_TYPE_REGISTRY.get(id);
        } catch (Exception e) {
            return null;
        }
    }

    private String getSpecTypeId() {
        return this.entityData.get(SPEC_TYPE_ID);
    }

    private void setSpecTypeId(String specTypeId) {
        this.entityData.set(SPEC_TYPE_ID, specTypeId);
    }

    // Gets the current animation ID for client-side rendering
    public String getAnimationId() {
        return this.entityData.get(ANIMATION_ID);
    }

    // Gets the current animation speed for client-side rendering
    public float getAnimationSpeed() {
        return this.entityData.get(ANIMATION_SPEED);
    }

    // Checks if this mob currently has an active animation
    public boolean hasActiveAnimation() {
        return !getAnimationId().isEmpty();
    }

    // Clears the current animation
    public void clearAnimation() {
        setAnimation("", 1.0f);
    }

    // Attempts to use a spec move based on the move class
    public boolean useSpecMove(MoveClass moveClass) {
        if (spec == null || !spec.canAttack()) {
            return false;
        }

        return spec.initMove(moveClass);
    }

    // Cancels the current spec move if any
    public void cancelSpecMove() {
        if (spec != null) {
            spec.cancelMove();
        }
    }

    // Gets the current move stun (how long until the mob can act again)
    public int getMoveStun() {
        return spec != null ? spec.getMoveStun() : 0;
    }

    // Checks if the mob is currently performing a spec move
    public boolean isPerformingSpecMove() {
        return spec != null && spec.getCurrentMove() != null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);

        SpecType specType = getSpecType();
        if (specType != null) {
            compound.putString("SpecType", specType.getId().toString());
        }

        if (hasActiveAnimation()) {
            compound.putString("AnimationId", getAnimationId());
            compound.putFloat("AnimationSpeed", getAnimationSpeed());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);

        if (compound.contains("SpecType")) {
            try {
                ResourceLocation specTypeId = new ResourceLocation(compound.getString("SpecType"));
                SpecType specType = JRegistries.SPEC_TYPE_REGISTRY.get(specTypeId);
                if (specType != null) {
                    setSpecType(specType);
                }
            } catch (Exception e) {
                // Invalid spec type, ignore
            }
        }

        if (compound.contains("AnimationId")) {
            setAnimation(
                    compound.getString("AnimationId"),
                    compound.getFloat("AnimationSpeed")
            );
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if (level().isClientSide) {
            if (key.equals(SPEC_TYPE_ID)) {
                SpecType specType = getSpecType();
                if (specType != null && (spec == null || !spec.getType().equals(specType))) {
                    setSpecType(specType);
                }
            } else if (key.equals(ANIMATION_ID) || key.equals(ANIMATION_SPEED)) {
                onAnimationChanged();
            }
        }
    }

    // Called when animation data changes on the client
    protected void onAnimationChanged() {
        // Override in subclasses to handle animation updates
    }

    // Handles movement, spec control, system mechanic control for Spec User mobs while they have a target
    public static void specUserCombatAI(SpecUserMob mob, LivingEntity target) {
        JSpec<?, ?> spec = mob.getSpec();
        if (spec == null || mob == target || !JUtils.canDamage(spec.getDamageSource(), target)) {
            return;
        }

        final JumpControl mobJumpControl = mob.getJumpControl();
        final MoveControl mobMoveControl = mob.getMoveControl();

        mob.lookAt(target, 30, 30);
        mob.getLookControl().setLookAt(target);

        JSpec<?, ?> targetSpec = null;
        AbstractMove<?, ?> enemyAttack = null;
        double distance = target.distanceTo(mob);
        int enemyMoveStun = 0;
        int blockPlusTicks = 0;

        // Get enemy spec attack
        if (target instanceof Player player) {
            targetSpec = JComponentPlatformUtils.getSpecData(player).getSpec();
            if (targetSpec != null && targetSpec != spec) {
                enemyMoveStun = targetSpec.getMoveStun();
                enemyAttack = targetSpec.getCurrentMove();
            }
        } else if (target instanceof SpecUserMob specMob) {
            targetSpec = specMob.getSpec();
            if (targetSpec != null && targetSpec != spec) {
                enemyMoveStun = targetSpec.getMoveStun();
                enemyAttack = targetSpec.getCurrentMove();
            }
        }

        // Check for stand attacks as well
        if (enemyAttack == null) {
            StandEntity<?, ?> enemyStand = JUtils.getStand(target);
            if (enemyStand != null) {
                enemyMoveStun = enemyStand.getMoveStun();
                enemyAttack = enemyStand.getCurrentMove();
                distance = enemyStand.distanceTo(mob);

                if (enemyStand.blocking) {
                    blockPlusTicks = enemyMoveStun;
                }
            }
        }

        final MobEffectInstance mobStun = mob.getEffect(JStatusRegistry.DAZED.get());
        // If stunned, and about to get hit by another move, try to combo break occasionally
        if (mobStun != null) {
            if (enemyAttack != null && enemyMoveStun > enemyAttack.getWindup() && mob.getRandom().nextFloat() < 0.1f) {
                // Specs can't combo break like stands, but could potentially use a counter move
                // This would need to be implemented based on your spec system
            }
        }

        // Movement towards/away from target
        PathNavigation entityNavigation = mob.getNavigation();

        final MobEffectInstance stun = target.getEffect(JStatusRegistry.DAZED.get());
        // Overestimating stun up to 1/4 of a second for longer combos and frametraps
        int stunTicks = stun != null ? stun.getDuration() + mob.getRandom().nextInt(5) : 0;
        stunTicks += blockPlusTicks;
        if (JComponentPlatformUtils.getTimeStopData(target).isPresent()) {
            stunTicks += JComponentPlatformUtils.getTimeStopData(target).get().getTicks();
        }

        // Only select attacks when not in move stun
        if (spec.getMoveStun() <= 1) {
            // Basic AI - let each spec type override this behavior if needed
            basicSpecAttackAI(mob, target, spec, distance, stunTicks, enemyAttack, enemyMoveStun);
        }

        // Movement logic
        doSpecMovement(mob, mobJumpControl, mobMoveControl, entityNavigation, distance, target, stunTicks);
    }

    // Basic AI that can be overridden by specific spec types
    private static void basicSpecAttackAI(SpecUserMob mob, LivingEntity target, JSpec<?, ?> spec,
                                          double distance, int stunTicks, AbstractMove<?, ?> enemyAttack, int enemyMoveStun) {

        // Don't attack if enemy is performing a move with armor
        if (enemyAttack != null && enemyMoveStun > enemyAttack.getWindup()) {
            return;
        }

        // Simple attack logic - try random moves based on cooldown availability
        MoveClass[] possibleMoves = {MoveClass.HEAVY, MoveClass.BARRAGE, MoveClass.SPECIAL1, MoveClass.SPECIAL2};

        for (MoveClass moveClass : possibleMoves) {
            if (mob.getRandom().nextFloat() < 0.3f) { // 30% chance to try each move
                if (spec.handleMove(moveClass)) {
                    break; // Successfully used a move, stop trying others
                }
            }
        }
    }

    private static void doSpecMovement(SpecUserMob mob, JumpControl jumpControl, MoveControl moveControl,
                                       PathNavigation navigation, double distance, LivingEntity target, int stunTicks) {

        // Movement logic based on distance and situation
        if (distance > 16.0) {
            // Too far, move closer
            navigation.moveTo(target, 1.0);
        } else if (distance < 2.0 && stunTicks <= 0) {
            // Too close and enemy isn't stunned, back away slightly
            navigation.stop();
        } else if (distance > 4.0) {
            // Good distance for most attacks, move closer slowly
            navigation.moveTo(target, 0.8);
        } else {
            // Good distance, stop moving
            navigation.stop();
        }
    }

    // Utility method to check if this mob can currently use specs
    public boolean canUseSpecs() {
        return spec != null && !isDeadOrDying();
    }

    // Gets the armor points from the current spec move
    public int getSpecArmorPoints() {
        return spec != null ? spec.getArmorPoints() : 0;
    }

    // Switches the mob's moveset (useful for AI or special conditions)
    public void switchMoveSet(String moveSetName) {
        if (spec != null) {
            spec.switchMoveSet(moveSetName);
        }
    }
}