package net.arna.jcraft.common.entity.stand;

import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.NonNull;
import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.attack.MoveMap;
import net.arna.jcraft.api.attack.MoveSet;
import net.arna.jcraft.api.attack.MoveSetManager;
import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.attack.enums.StunType;
import net.arna.jcraft.api.component.living.CommonMiscComponent;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.api.stand.StandData;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.api.stand.StandInfo;
import net.arna.jcraft.api.stand.SummonData;
import net.arna.jcraft.common.attack.moves.aerosmith.*;
import net.arna.jcraft.common.util.JParticleType;
import net.arna.jcraft.common.util.StandAnimationState;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class AerosmithEntity extends StandEntity<AerosmithEntity, AerosmithEntity.State> {
    public static final MoveSet<AerosmithEntity, AerosmithEntity.State> MOVE_SET = MoveSetManager.create(JStandTypeRegistry.AEROSMITH,
            AerosmithEntity::registerDefaultMoves, AerosmithEntity.State.class);

    public static final EntityDataAccessor<Float> OVERHEAT = SynchedEntityData.defineId(AerosmithEntity.class, EntityDataSerializers.FLOAT);
    public static final float OVERHEAT_MAX = 15f;

    public float oldPitch = 0.0f, oldYaw = 0.0f, oldRoll = 0.0f;
    public float pitch = 0.0f, yaw = 0.0f, roll = 0.0f;

    // TODO Arna balance this
    public static final MuzzleHitscanAttack BULLET = new MuzzleHitscanAttack(
            1, 1, 2, 0f, 1f, 0, 0f, 30f, 10f, 1/6f, 0.01f)
            .withSound(JSoundRegistry.AS_SHOOT)
            .withHitSpark(JParticleType.HIT_SPARK_1)
            .withShootSpark(JParticleType.LEMON)
            .withStunType(StunType.WINDED);
    // TODO Arna description

    // TODO Arna balance this
    public static final ItemDropAttack ITEM_DROP = new ItemDropAttack(
            200, 1, 100, 0f, 30f);
    // TODO Arna description

    // TODO Arna balance this
    public static final BombDropAttack<AerosmithEntity> BOMB_DROP = new BombDropAttack<AerosmithEntity>(
            200, 1, 100, 0f, 30f)
            .withCrouchingVariant(ITEM_DROP);
    // TODO Arna description

    // TODO Arna balance this
    public static final PatrolMove<AerosmithEntity> PATROL = new PatrolMove<>(
            200, 1, 100, 0f, 30f, 10f, 0.5f);
    // TODO Arna description

    public static final AerosmithChargeAttack CHARGE = new AerosmithChargeAttack(
            300, 50, 1.0f, 15, 1.66f, 0.1f, 0.0f,
            IntSet.of(10, 15, 20, 25, 30, 35, 40, 45, 50))
            .withStaticY()
            .withStunType(StunType.LAUNCH)
            .withSound(JSoundRegistry.AS_SUMMON)
            .withImpactSound(JSoundRegistry.IMPACT_1)
            .withInfo(
                    Component.literal("Dive Charge"),
                    Component.literal("Non-remote: a straight charge, rising at the end. Carries enemies with Aerosmith.")
            );

    public static final StandData DATA = StandData.builder()
            .info(StandInfo.builder()
                    .name(Component.translatable("entity.jcraft.aerosmith"))
                    .skinName(Component.literal("Manga"))
                    .skinName(Component.literal("Vento Aureo"))
                    .skinName(Component.literal("Interceptor"))
                    .build())
            .summonData(SummonData.of(JSoundRegistry.AS_SUMMON).withAnimDuration(48))
            .build();

    private CommonMiscComponent miscComponent;
    private int overheatTick;

    public AerosmithEntity(final Level world) {
        super(JStandTypeRegistry.AEROSMITH.get(), world);
        // setYDistanceOffset(10f); // TODO for patrol mode
        setYDistanceOffset(1.2f);
        setNoGravity(true);
    }

    @NonNull
    public ItemStack getHeldItem() { return getItemBySlot(EquipmentSlot.FEET); }
    public void setHeldItem(final @NonNull ItemStack stack) { setItemSlot(EquipmentSlot.FEET, stack); }

    @Override
    public boolean remoteControllable() {
        return false;
    }

    @Override
    public @NonNull AerosmithEntity getThis() {
        return this;
    }

    private static void registerDefaultMoves(final @NonNull MoveMap<AerosmithEntity, AerosmithEntity.State> moves) {
        moves.registerImmediate(MoveClass.LIGHT, BULLET, State.LIGHT);
        moves.registerImmediate(MoveClass.HEAVY, BOMB_DROP, State.ACTIVE);
        moves.registerImmediate(MoveClass.UTILITY, PATROL, State.ACTIVE);
        moves.registerImmediate(MoveClass.BARRAGE, CHARGE, State.CHARGE);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(getCurrentMove() instanceof MuzzleHitscanAttack) && ++overheatTick % 5 == 0) {
            addOverheat(-0.4f);
        }
    }

    @Override
    protected void pushEntities() {
        if (getState() == State.CHARGE) // slice through enemies while charging
            return;
        super.pushEntities();
    }

    @Override
    public void setUser(@Nullable final LivingEntity user) {
        super.setUser(user);
        if (user == null) {
            return;
        }
        miscComponent = JComponentPlatformUtils.getMiscData(getUser());
        if (miscComponent == null) {
            return;
        }
        setOverheat(miscComponent.getAerosmithOverheat());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        float startValue = miscComponent == null ? 0f : miscComponent.getAerosmithOverheat();
        entityData.define(OVERHEAT, startValue);
    }

    public float getOverheat() {
        return entityData.get(OVERHEAT);
    }

    public void setOverheat(final float overheat) {
        entityData.set(OVERHEAT, overheat);
        if (miscComponent == null) {
            return;
        }
        miscComponent.setAerosmithOverheat(overheat);
    }

    public void addOverheat(float amount) {
        setOverheat(Mth.clamp(entityData.get(OVERHEAT) + amount, 0f, OVERHEAT_MAX));
    }

    @Override
    public void setRemote(final boolean r) {
        super.setRemote(r);
        setAlphaOverride(r ? 1f : -1f);
    }

    @Override
    public void desummon() {
        if (!level().isClientSide() && !getHeldItem().isEmpty()) {
            Containers.dropItemStack(level(), getX(), getY(), getZ(), getHeldItem());
        }
        super.desummon();
    }

    public Vec3 getPureLookVector() {
        return calculateViewVector(getXRot(), getYRot());
    }

    public enum State implements StandAnimationState<AerosmithEntity> {
        IDLE(AzCommand.create(JCraft.BASE_CONTROLLER, "idle", AzPlayBehaviors.LOOP)),
        ACTIVE(AzCommand.create(JCraft.BASE_CONTROLLER, "idle", AzPlayBehaviors.LOOP)),
        CHARGE(AzCommand.create(JCraft.BASE_CONTROLLER, "charge", AzPlayBehaviors.HOLD_ON_LAST_FRAME)),
        LIGHT(AzCommand.create(JCraft.BASE_CONTROLLER, "burst", AzPlayBehaviors.HOLD_ON_LAST_FRAME)),
        BLOCK(AzCommand.create(JCraft.BASE_CONTROLLER, "block", AzPlayBehaviors.LOOP))
        ;

        private final AzCommand animator;

        State(final @NonNull AzCommand animator) {
            this.animator = animator;
        }

        @Override
        public void playAnimation(final @NonNull AerosmithEntity attacker) {
            animator.sendForEntity(attacker);
        }
    }

    @Override
    protected State[] getStateValues() {
        return State.values();
    }

    @Override
    public State getBlockState() {
        return State.BLOCK;
    }
}
