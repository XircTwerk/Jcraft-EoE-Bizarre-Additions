package net.arna.jcraft.common.attack.moves.horus;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.enums.MobilityType;
import net.arna.jcraft.api.attack.moves.AbstractChargeAttack;
import net.arna.jcraft.api.attack.moves.AbstractSimpleAttack;
import net.arna.jcraft.common.entity.stand.HorusEntity;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.gravity.api.GravityChangerAPI;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public final class HorusDivekickAttack extends AbstractChargeAttack<HorusDivekickAttack, HorusEntity, HorusEntity.State> {
    private static final MobEffectInstance LEVITATE = new MobEffectInstance(MobEffects.SLOW_FALLING, 9, 4, true, false);
    private Vec3 lookDir = Vec3.ZERO;

    public HorusDivekickAttack(final int cooldown, final int windup, final int duration, final float moveDistance,
                               final float damage, final int stun, final float hitboxSize, final float knockback, final float offset) {
        super(cooldown, windup, duration, moveDistance, damage, stun, hitboxSize, knockback, offset, HorusEntity.State.DIVEKICK_HIT);
        withMobilityType(MobilityType.FLIGHT);
    }

    @Override
    public @NotNull MoveType<HorusDivekickAttack> getMoveType() {
        return Type.INSTANCE;
    }

    @Override
    public void onInitiate(final HorusEntity attacker) {
        super.onInitiate(attacker);
        final LivingEntity user = attacker.getUserOrThrow();
        if (attacker.isFree()) attacker.setFree(false);

        lookDir = user.getLookAngle().scale(0.95);

        int duration = 20 + (int)user.getXRot();
        if (duration < getWindup()) duration = getWindup();
        withDuration(duration);

        user.addEffect(new MobEffectInstance(LEVITATE));
    }

    @Override
    protected void endCharge(final HorusEntity attacker) {
        super.endCharge(attacker);
        final Vec3 newPos = advanceChargePos(attacker, getMoveDistance(), getWindupPoint());
        attacker.setFreePos(new Vector3f((float) newPos.x, (float) newPos.y, (float) newPos.z));
    }

    @Override
    protected Vec3 advanceChargePos(final StandEntity<?, ?> attacker, final float moveDistance, final int windupPoint) {
        return attacker.position().add(lookDir);
    }

    @Override
    protected void tickChargeAttack(final StandEntity<HorusEntity, HorusEntity.State> attacker, final boolean shouldPerform, final float moveDistance, final int windupPoint) {
        super.tickChargeAttack(attacker, shouldPerform, moveDistance, windupPoint);
        if (attacker.getMoveStun() < windupPoint) {
            if (attacker.getBlockStateOn().canOcclude()) {
                endCharge((HorusEntity) attacker);
            } else {
                final LivingEntity user = attacker.getUserOrThrow();
                GravityChangerAPI.setWorldVelocity(user, lookDir);
                JUtils.syncVelocityUpdate(user);
                user.resetFallDistance();
            }
        }
    }

    @Override
    protected @NonNull HorusDivekickAttack getThis() {
        return this;
    }

    @Override
    public @NonNull HorusDivekickAttack copy() {
        return copyExtras(new HorusDivekickAttack(getCooldown(), getWindup(), getDuration(),
                getMoveDistance(), getDamage(), getStun(), getHitboxSize(), getKnockback(), getOffset()));
    }

    public static class Type extends AbstractSimpleAttack.Type<HorusDivekickAttack> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NotNull App<RecordCodecBuilder.Mu<HorusDivekickAttack>, HorusDivekickAttack> buildCodec(RecordCodecBuilder.Instance<HorusDivekickAttack> instance) {
            return attackDefault(instance, HorusDivekickAttack::new);
        }
    }
}
