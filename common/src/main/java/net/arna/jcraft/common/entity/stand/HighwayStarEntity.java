// HighwayStarEntity.java
package net.arna.jcraft.common.entity.stand;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.NonNull;
import mod.azure.azurelib.core.animation.AnimationState;
import mod.azure.azurelib.core.animation.RawAnimation;
import net.arna.jcraft.api.attack.MoveMap;
import net.arna.jcraft.api.attack.MoveSet;
import net.arna.jcraft.api.attack.MoveSetManager;
import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.attack.enums.MoveInputType;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.attack.StateContainer;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.api.spec.JSpec;
import net.arna.jcraft.api.stand.StandData;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.api.stand.StandInfo;
import net.arna.jcraft.common.attack.moves.highwaystar.*;
import net.arna.jcraft.common.attack.moves.shared.*;
import net.arna.jcraft.common.util.JParticleType;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.common.util.StandAnimationState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Consumer;

import static net.arna.jcraft.JCraft.QUEUE_MOVESTUN_LIMIT;
import static net.arna.jcraft.JCraft.SPEC_QUEUE_MOVESTUN_LIMIT;
import static net.arna.jcraft.api.stand.StandEntity.standUserCombatAI;

public class HighwayStarEntity extends StandEntity<HighwayStarEntity, HighwayStarEntity.State> {
    public static final MoveSet<HighwayStarEntity, State> MOVE_SET = MoveSetManager.create(JStandTypeRegistry.HIGHWAY_STAR,
            HighwayStarEntity::registerMoves, State.class);

    public static final StandData DATA = StandData.builder()
            .idleRotation(220f)
            .info(StandInfo.builder()
                    .name(Component.translatable("entity.jcraft.highway_star"))
                    .proCount(5)
                    .conCount(2)
                    .freeSpace(Component.literal("""
                            PASSIVE: Scent Lock-On - automatically tracks enemies within 15 blocks (Glowing effect)
                            
                            PASSIVE: Speed 1 
                            
                            BNBs:
                            
                            Decent damage utilizing every move:
                            Light>Barrage>Ankle Breaker>Phaser>Leech Grab>Light~Light or Heavy
                            
                            The go-to combo:
                            Light>Barrage>Ankle Breaker>Heavy"""))
                    .build())
            .build();

    // Split Mode State
    private boolean hasFlower = false; // Split mode flag
    private static final double DETECTION_RANGE = 100.0;

    // High-Speed Chase state (integrated)
    private static final int MAX_SPEED_LEVEL = 0;
    private static final int STUN_DURATION = 100; // 5 seconds
    private static final Map<LivingEntity, Boolean> ACTIVE_SPEED_USERS = new HashMap<>();
    private static final Map<LivingEntity, Integer> SPEED_STUN_TIMERS = new HashMap<>();

    // Light Attack Followup
    public static final SimpleAttack<HighwayStarEntity> LIGHT_FOLLOWUP = new SimpleAttack<HighwayStarEntity>(
            0, 9, 14, 0.75f, 4f, 6, 1.25f, 0.8f, -0.1f)
            .withAnim(State.LIGHT_FOLLOWUP)
            .withImpactSound(JSoundRegistry.IMPACT_1)
            .withLaunch()
            .withBlockStun(3)
            .withHitSpark(JParticleType.HIT_SPARK_1)
            .withInfo(
                    Component.literal("Chain Punch"),
                    Component.literal("fast, chainable punches")
            );

    // Crouch + Light - Uppercut
    public static final SimpleAttack<HighwayStarEntity> UPPERCUT = new SimpleAttack<HighwayStarEntity>(
            50, 8, 15, 1f, 5f, 8, 1.5f, 1.2f, 0.1f)
            .withAnim(State.UPPERCUT)
            .withImpactSound(JSoundRegistry.IMPACT_2)
            .withLaunch()
            .withHitSpark(JParticleType.HIT_SPARK_2)
            .withInfo(
                    Component.literal("Uppercut"),
                    Component.literal("strong launch attack")
            );

    // Light Attack
    public static final SimpleAttack<HighwayStarEntity> LIGHT = new SimpleAttack<HighwayStarEntity>(
            10, 4, 8, 0.75f, 3.5f, 5, 1.25f, 0.4f, -0.1f)
            .withFollowup(LIGHT_FOLLOWUP)
            .withCrouchingVariant(UPPERCUT)
            .withImpactSound(JSoundRegistry.IMPACT_1)
            .withInfo(
                    Component.literal("Basic Punch"),
                    Component.literal("fast, chainable punches")
            );

    // Heavy Attack - Roundhouse
    public static final KnockdownAttack<HighwayStarEntity> ROUNDHOUSE = new KnockdownAttack<HighwayStarEntity>(
            120, 6, 25, 1f, 7f, 12, 2f, 1.8f, 0f, 40)
            .withAnim(State.ROUNDHOUSE)
            .withImpactSound(JSoundRegistry.TW_KICK_HIT)
            .withHitSpark(JParticleType.HIT_SPARK_3)
            .withInfo(
                    Component.literal("Roundhouse Kick"),
                    Component.literal("knockdown attack, combo finisher")
            );

    // Barrage
    public static final MainBarrageAttack<HighwayStarEntity> BARRAGE = new MainBarrageAttack<HighwayStarEntity>(
            200, 0, 30, 0.75f, 0.8f, 18, 1.75f, 0.3f, 0f, 2, 2.0f)
            .withSound(JSoundRegistry.MIH_BARRAGE)
            .withImpactSound(JSoundRegistry.IMPACT_1)
            .withInfo(
                    Component.literal("Rapid Barrage"),
                    Component.literal("quicker than the copy paste stand barrage")
            );

    // Special 1 - Ankle Breaker
    public static final AnkleBreakerAttack ANKLE_BREAKER = new AnkleBreakerAttack(
            180, 12, 20, 1f, 4f, 20, 1.5f, 0.8f, -0.2f, 40)
            .withAnim(State.ANKLE_BREAKER)
            .withImpactSound(JSoundRegistry.TW_KICK_HIT)
            .withSound(JSoundRegistry.WS_LEGCRUSH)
            .withInfo(
                    Component.literal("Ankle Breaker"),
                    Component.literal("slow low kick that knocks down and does good stun")
            );

    public static final LeechGrabHitAttack LEECH_GRAB_HITS = new LeechGrabHitAttack(
            0, 25, 1f, 4, 8, 1.5f, 0.6f, 0.1f, new IntArrayList(new int[]{5, 8, 12, 16, 20}))
            .withInfo(Component.literal("Leech Hits"), Component.literal("drains health during grab"));

    public static final GrabAttack<HighwayStarEntity, State> GRAB_ATTACK = new GrabAttack<>(
            220, 12, 25, 0.5f, 0f, 35, 2f, 0f, 0.5f, LEECH_GRAB_HITS,
            StateContainer.of(State.GRAB_HIT), 30, 1.0)
            .withAnim(State.GRAB_ATTACK)
            .withSound(JSoundRegistry.VAMPIRE_SUCK)
            .withImpactSound(JSoundRegistry.VAMPIRE_GRAB_HIT)
            .withInfo(Component.literal("Leech Grab"), Component.literal("grabs enemy and drains their health and saturation over time"));

    // Special 3 - Phaser
    public static final PhaserAttack PHASER = new PhaserAttack(
            150, 5, 10, 2f, 0f, 25, 2f, 0f, 0f)
            .withAnim(State.PHASER)
            .withSound(JSoundRegistry.MIH_SPEEDSLICE)
            .withInfo(
                    Component.literal("Phaser"),
                    Component.literal("Fast 3-block teleport where you're looking. Does no damage, but inflicts good stun.")
            );

    // Ultimate - Room Trap
    public static final RoomTrapAttack ROOM_TRAP = new RoomTrapAttack(
            600, 25, 40, 2f, 0f, 10, 2f, 0f, 0f)
            .withAnim(State.ROOM_TRAP)
            .withSound(JSoundRegistry.TIME_ERASE)
            .withInfo(
                    Component.literal("Room Trap"),
                    Component.literal("traps enemy in isolated room for 6 seconds with blindness and mining fatigue")
            );

    public HighwayStarEntity(Level worldIn) {
        super(JStandTypeRegistry.HIGHWAY_STAR.get(), worldIn);

        auraColors = new Vector3f[]{
                new Vector3f(0.9f, 0.1f, 0.1f),  // Red
                new Vector3f(0.2f, 0.2f, 0.8f),  // Blue
                new Vector3f(0.8f, 0.8f, 0.1f),  // Yellow
                new Vector3f(0.6f, 0.1f, 0.8f)   // Purple
        };
    }

    private static void registerMoves(MoveMap<HighwayStarEntity, State> moves) {
        moves.registerImmediate(MoveClass.LIGHT, LIGHT, State.LIGHT);
        moves.register(MoveClass.HEAVY, ROUNDHOUSE, State.ROUNDHOUSE);
        moves.register(MoveClass.BARRAGE, BARRAGE, State.BARRAGE);
        moves.register(MoveClass.SPECIAL1, ANKLE_BREAKER, State.ANKLE_BREAKER);
        moves.register(MoveClass.SPECIAL2, GRAB_ATTACK, State.GRAB_ATTACK);
        moves.register(MoveClass.SPECIAL3, PHASER, State.PHASER);
        moves.register(MoveClass.ULTIMATE, ROOM_TRAP, State.ROOM_TRAP);
    }

    @Override
    public boolean initMove(MoveClass moveClass) {
        // Handle split mode toggle for utility
        if (moveClass == MoveClass.UTILITY && hasUser()) {
            if (getCurrentMove() != null || getMoveStun() > 0) {
                return false;
            }
            toggleSplitMode();
            return true;
        }

        // Handle light attack followups
        if (moveClass == MoveClass.LIGHT && tryFollowUp(moveClass, MoveClass.LIGHT)) {
            return true;
        }

        return super.initMove(moveClass);
    }

    public void toggleSplitMode() {
        if (!hasUser()) return;

        LivingEntity user = getUserOrThrow();
        hasFlower = !hasFlower; // Toggle split mode using hasFlower flag

        if (hasFlower) {
            setRemote(true);
            Vec3 userPos = user.position();
            Vec3 lookDir = user.getLookAngle();
            Vec3 splitPos = userPos.add(lookDir.scale(3.0));
            // Ensure we don't phase into ground
            teleportTo(splitPos.x, splitPos.y + 0.5, splitPos.z);
        } else {
            setRemote(false);
            setTarget(null);
            teleportTo(user.getX(), user.getY(), user.getZ());
            setState(State.IDLE);
        }
    }

    // High-Speed Chase integrated methods
    private void activateHighSpeedChase(LivingEntity user) {
        if (!ACTIVE_SPEED_USERS.getOrDefault(user, false)) {
            ACTIVE_SPEED_USERS.put(user, true);
            user.removeEffect(MobEffects.MOVEMENT_SPEED);
            user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, MAX_SPEED_LEVEL, false, false, true));
        }
    }

    private void deactivateHighSpeedChase(LivingEntity user) {
        ACTIVE_SPEED_USERS.put(user, false);
        SPEED_STUN_TIMERS.remove(user);
        user.removeEffect(MobEffects.MOVEMENT_SPEED);
    }

    private void handleHighSpeedMomentum(LivingEntity user) {
        if (!ACTIVE_SPEED_USERS.getOrDefault(user, false)) return;

        // Check if stand is active
        if (!hasUser() || !isAlive()) {
            ACTIVE_SPEED_USERS.put(user, false);
            SPEED_STUN_TIMERS.remove(user);
            user.removeEffect(MobEffects.MOVEMENT_SPEED);
            return;
        }

        // Check stun timer
        Integer stunTimer = SPEED_STUN_TIMERS.get(user);
        if (stunTimer != null && stunTimer > 0) {
            SPEED_STUN_TIMERS.put(user, stunTimer - 1);
            user.removeEffect(MobEffects.MOVEMENT_SPEED);

            if (stunTimer == 1) {
                SPEED_STUN_TIMERS.remove(user);
                // Reapply speed
                user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, MAX_SPEED_LEVEL, false, false, true));
            }
            return;
        }

        // Ensure speed effect stays active (refresh every second)
        if (user.tickCount % 20 == 0) {
            user.removeEffect(MobEffects.MOVEMENT_SPEED);
            user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, MAX_SPEED_LEVEL, false, false, true));
        }
    }

    @Override
    public void onUserMoveInput(AbstractMove<?, ? super HighwayStarEntity> currentMove, MoveInputType type, boolean pressed, boolean moveInitiated) {
        if (!pressed) return;

        MoveClass moveClass = type.getMoveClass(standby);
        if (moveClass == null) return;

        // Handle Highway Star's own moves (SPECIAL1, SPECIAL2, SPECIAL3, ULTIMATE, UTILITY)
        if (moveClass == MoveClass.SPECIAL1 || moveClass == MoveClass.SPECIAL2 ||
                moveClass == MoveClass.SPECIAL3 || moveClass == MoveClass.ULTIMATE ||
                moveClass == MoveClass.UTILITY) {

            // If in split mode, only allow UTILITY (to toggle back)
            if (isSplitMode() && moveClass != MoveClass.UTILITY) {
                return; // Can't use stand moves while in automatic mode except toggle
            }

            if (canAttack()) {
                initMove(moveClass);
            } else if (getMoveStun() > 0 && getMoveStun() < QUEUE_MOVESTUN_LIMIT) {
                queueMove(type);
            }
            return;
        }

        // For spec moves (LIGHT, HEAVY, BARRAGE), allow them while in split mode
        if (hasUser() && getUser() instanceof Player player) {
            JSpec<?, ?> spec = JUtils.getSpec(player);
            if (spec != null) {
                // Check if spec can attack
                if (spec.canAttack()) {
                    if (spec.initMove(moveClass)) {
                        // Move was successfully initiated
                    } else if (spec.moveStun > 0 && spec.moveStun < SPEC_QUEUE_MOVESTUN_LIMIT) {
                        // Queue the move for the spec
                        spec.queuedMove = type;
                    }
                }
            }
        }
    }

    private static void cleanupInactivePlayers() {
        ACTIVE_SPEED_USERS.entrySet().removeIf(entry -> !entry.getKey().isAlive());
        SPEED_STUN_TIMERS.entrySet().removeIf(entry -> !entry.getKey().isAlive());
    }

    public boolean isSplitMode() {
        return hasFlower;
    }

    @Override
    public boolean allowMoveHandling() {
        return !isSplitMode();
    }

    @Override
    public boolean remoteControllable() {
        return false;
    }

    // NEVER BLOCK - Highway Star is pure aggression
    protected boolean doAutoBlocking(Mob mob, AbstractMove<?,?> enemyAttack, boolean enemyHasStand, double distance, int enemyMoveStun) {
        return false; // NEVER BLOCK
    }

    @Override
    protected boolean doEnvironmentalBlocking(Mob mob, boolean wantToBlock) {
        return false; // NEVER BLOCK
    }

    @Override
    public void tick() {
        super.tick();

        // Handle Scent Lock-On Passive (only when not in split mode)
        if (hasUser() && !isSplitMode() && tickCount % 20 == 0) {
            applyScentTracking(getUserOrThrow());
        }

        // Handle High-Speed Chase momentum (only when not in split mode)
        if (hasUser() && !isSplitMode()) {
            handleHighSpeedMomentum(getUserOrThrow());
        }

        // Split mode AI
        if (hasFlower) {
            if (getMoveStun() > 0) {
                if (navigation.isInProgress()) {
                    navigation.stop();
                }
            } else {
                handleSplitModeAI();
            }
        }

        // Cleanup and initialization
        if (tickCount % 100 == 0) {
            cleanupInactivePlayers();
        }
        if (tickCount == 1 && hasUser()) {
            activateHighSpeedChase(getUserOrThrow());
        }
    }

    private void updateSplitMovementAnimation() {
        if ((getState() == State.IDLE || blocking) && JUtils.canAct(this) && getCurrentMove() == null) {
            double f = getMoveControl().getSpeedModifier();
            double s = getMoveControl().strafeRight;
            boolean onGround = onGround();

            if (s > 0) {
                setStateNoReset(onGround ? State.RIGHT : State.RIGHT);
            } else if (s < 0) {
                setStateNoReset(onGround ? State.LEFT : State.LEFT);
            } else if (f > 0) {
                setStateNoReset(onGround ? State.FORWARD : State.FORWARD);
            } else if (f < 0) {
                setStateNoReset(onGround ? State.BACKWARD : State.BACKWARD);
            }
        }
    }

    private void handleSplitModeAI() {
        if (!hasUser()) {
            return;
        }
        LivingEntity user = getUser();

        if (!level().isClientSide()) {
            boolean isRemote = isRemote();

            if (!remoteControllable()) {
                if (getAlphaOverride() != 1.0f) {
                    setAlphaOverride(1.0f);
                }

                LivingEntity target = getTarget();
                if ((target != null && !target.isAlive()) || (target instanceof Player player && (player.isCreative() || player.isSpectator()))) {
                    target = null;
                }

                if (target == null) {
                    List<LivingEntity> potentialTargets = level().getEntitiesOfClass(
                            LivingEntity.class,
                            getBoundingBox().inflate(DETECTION_RANGE),
                            EntitySelector.NO_CREATIVE_OR_SPECTATOR.and(EntitySelector.LIVING_ENTITY_STILL_ALIVE));
                    potentialTargets.remove(this);


                    Comparator<Entity> distanceComparator = (entity1, entity2) -> {
                        double distance1 = this.distanceToSqr(entity1);
                        double distance2 = this.distanceToSqr(entity2);
                        return Double.compare(distance1, distance2);
                    };
                    potentialTargets.sort(distanceComparator);

                    for (LivingEntity potentialTarget : potentialTargets) {
                        if (!hasLineOfSight(potentialTarget)) {
                            continue;
                        }
                        if (potentialTarget instanceof StandEntity<?, ?> standEntity && standEntity.hasUser()) {
                            setTarget(standEntity.getUserOrThrow());
                            break;
                        }
                        if (potentialTarget == user) {
                            // Skip targeting the user
                            continue;
                        }
                        setTarget(potentialTarget);
                        break;
                    }
                } else {
                    // Apply glowing effect to tracked target
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false, true));

                    double speed = getAttributeValue(Attributes.MOVEMENT_SPEED);
                    if (Objects.requireNonNull(user).hasEffect(JStatusRegistry.DAZED.get())) {
                        speed = user.getSpeed();
                    }
                    if (tickCount % 4 == 0) // Pathfinding is expensive
                    {
                        navigation.moveTo(target, speed);
                    }

                    standUserCombatAI(this, target, this);
                }

                if (!isRemote) {
                    setRemote(true);
                }
            }

            if (isRemote) {
                updateSplitMovementAnimation();
            }
        }
    }

    private void applyScentTracking(LivingEntity user) {
        final double TRACKING_RANGE = 15.0;
        final int GLOWING_DURATION = 40;

        for (LivingEntity entity : user.level().getEntitiesOfClass(LivingEntity.class,
                user.getBoundingBox().inflate(TRACKING_RANGE))) {
            if (entity != user && entity.isAlive() && user.distanceTo(entity) <= TRACKING_RANGE) {
                entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOWING_DURATION, 0, false, false, true));
            }
        }
    }

    @Override
    public void desummon() {
        if (hasUser()) {
            deactivateHighSpeedChase(getUserOrThrow());
        }
        super.desummon();
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (hasUser()) {
            deactivateHighSpeedChase(getUserOrThrow());
        }
        super.remove(reason);
    }

    @Override
    public void cancelMove() {
        super.cancelMove();
        setState(State.IDLE);
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);

        if (result && hasUser() && source.getEntity() instanceof LivingEntity) {
            // When hit, lose speed for 5 seconds
            LivingEntity user = getUserOrThrow();
            if (ACTIVE_SPEED_USERS.getOrDefault(user, false)) {
                SPEED_STUN_TIMERS.put(user, STUN_DURATION);
                user.removeEffect(MobEffects.MOVEMENT_SPEED);
            }
        }

        return result;
    }

    public State getIdleState() {
        return State.IDLE;
    }

    @Override
    @NonNull
    public HighwayStarEntity getThis() {
        return this;
    }

    // Animation states
    public enum State implements StandAnimationState<HighwayStarEntity> {
        IDLE(builder -> builder.setAnimation(RawAnimation.begin().thenLoop("idle"))),
        LIGHT(builder -> builder.setAnimation(RawAnimation.begin().thenPlayAndHold("light"))),
        LIGHT_FOLLOWUP(builder -> builder.setAnimation(RawAnimation.begin().thenPlayAndHold("light_followup"))),
        UPPERCUT(builder -> builder.setAnimation(RawAnimation.begin().thenPlayAndHold("uppercut"))),
        BLOCK(builder -> builder.setAnimation(RawAnimation.begin().thenLoop("block"))),
        ROUNDHOUSE(builder -> builder.setAnimation(RawAnimation.begin().thenPlayAndHold("heavy"))),
        BARRAGE(builder -> builder.setAnimation(RawAnimation.begin().thenLoop("barrage"))),
        ANKLE_BREAKER(builder -> builder.setAnimation(RawAnimation.begin().thenPlayAndHold("ankle breaker"))),
        GRAB_ATTACK(builder -> builder.setAnimation(RawAnimation.begin().thenPlayAndHold("animation.grab"))),
        GRAB_HIT(builder -> builder.setAnimation(RawAnimation.begin().thenPlayAndHold("animation.grabhit"))),
        PHASER(builder -> builder.setAnimation(RawAnimation.begin().thenPlayAndHold("animation.idle"))),
        ROOM_TRAP(builder -> builder.setAnimation(RawAnimation.begin().thenPlayAndHold("animation.roomplant"))),

        // Movement animations for split mode
        FORWARD(builder -> builder.setAnimation(RawAnimation.begin().thenLoop("animation.forw"))),
        BACKWARD(builder -> builder.setAnimation(RawAnimation.begin().thenLoop("animation.back"))),
        LEFT(builder -> builder.setAnimation(RawAnimation.begin().thenLoop("animation.left"))),
        RIGHT(builder -> builder.setAnimation(RawAnimation.begin().thenLoop("animation.right")));

        private final Consumer<AnimationState<HighwayStarEntity>> animator;

        State(Consumer<AnimationState<HighwayStarEntity>> animator) {
            this.animator = animator;
        }

        @Override
        public void playAnimation(HighwayStarEntity attacker, AnimationState<HighwayStarEntity> builder) {
            animator.accept(builder);
        }
    }

    @Override
    protected State[] getStateValues() {
        return State.values();
    }

    @Nullable
    @Override
    protected String getSummonAnimation() {
        return "summon";
    }

    @Override
    public State getBlockState() {
        return State.BLOCK;
    }
}