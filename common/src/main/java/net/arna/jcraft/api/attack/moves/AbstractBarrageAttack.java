package net.arna.jcraft.api.attack.moves;

import com.mojang.datafixers.Products;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.util.Function10;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.NonNull;
import net.arna.jcraft.api.attack.IAttacker;
import net.arna.jcraft.api.attack.enums.StunType;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.attack.core.data.AttackMoveExtras;
import net.arna.jcraft.common.attack.core.data.BaseMoveExtras;
import net.arna.jcraft.common.gravity.api.GravityChangerAPI;
import net.arna.jcraft.common.gravity.util.RotationUtil;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * A simple attack that performs at a set interval.
 *
 * @param <T>
 * @param <A>
 */
@Getter
public abstract class AbstractBarrageAttack<T extends AbstractBarrageAttack<T, A>, A extends IAttacker<? extends A, ?>> extends AbstractSimpleAttack<T, A> {
    private final int interval;
    protected boolean inflictsSlowness = true;

    @Getter
    private boolean stunWindsUp = true;
    private final int originalStun;

    protected AbstractBarrageAttack(final int cooldown, final int windup, final int duration, final float moveDistance,
                                    final float damage, final int stun, final float hitboxSize, final float knockback,
                                    final float offset, final int interval) {
        super(cooldown, windup, duration, moveDistance, damage, stun, hitboxSize, knockback, offset);
        barrage = true;
        this.interval = interval;
        withBlockStun(3);
        withStunType(StunType.WINDED);
        withHitSpark(null);

        originalStun = stun;
    }

    public @NonNull T withoutSlowness() {
        this.inflictsSlowness = false;
        return getThis();
    }

    @Override
    public boolean shouldPerform(final A attacker, final int moveStun) {
        // If move stun is 22 ticks, windup is 6 and interval is 4, the first hit will occur at tick 6 (when move stun is 22 - 6 = 16),
        // the second at tick 10 (when move stun is 22 - 10 = 12), then at tick 14, etc.
        // For hit 2:
        // move stun = 22, windup = 6, stand move stun = 8 (move stun - windup - 2 * interval)
        // (22 - 6 - 8) % 4 =
        // (16 - 8) % 4 =
        // 8 % 4 = 0

        // This calculation is different from how it used to be done as that was
        // stand move stun % interval == 0
        // Which means that if your move stun is 22, windup is 6 and interval is 6,
        // the first blow will not be landed after 6 ticks (when stand move stun is 22 - 6 = 16),
        // but rather after 10 ticks (when stand move stun is 22 - 10 = 12).
        return attacker.hasUser() && hasWindupPassed(attacker, moveStun) && (getDuration() - getWindup() - moveStun) % interval == 0;
    }

    public T withNoStunWindup() {
        stunWindsUp = false;
        return getThis();
    }

    @Override
    public void activeTick(final A attacker, final int moveStun) {
        super.activeTick(attacker, moveStun);

        if (stunWindsUp) {
            final float twoThirdsDuration = 0.75f * getDuration();

            if (moveStun > twoThirdsDuration) {
                int newStun = (int) Mth.sqrt(originalStun / (moveStun - twoThirdsDuration)) + originalStun / interval;
                withStun(newStun);
            } else {
                withStun(originalStun);
            }
        }

        // Consider replacing the isRemote() with isFree()?
        if (attacker.hasUser() && inflictsSlowness && !attacker.isRemote()) {
            attacker.getUserOrThrow().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 2, true, false));
        }
    }

    @Override
    protected Set<LivingEntity> validateTargets(final A attacker, final Set<LivingEntity> targets) {
        if (!(attacker instanceof StandEntity<?, ?> stand)) {
            return targets;
        }

        // Barrage clashing logic.
        for (final LivingEntity target : targets) {
            final StandEntity<?, ?> targetStand = JUtils.getStand(target);
            Vec3 forwardPos = stand.getLookAngle();
            forwardPos = new Vec3(stand.getX() + forwardPos.x, stand.getY() + forwardPos.y, stand.getZ() + forwardPos.z);
            if (targetStand == null ||
                    targetStand == attacker ||
                    targetStand.getCurrentMove() == null ||
                    !targetStand.getCurrentMove().isBarrage() ||
                    targetStand.distanceToSqr(forwardPos) > 4) {
                continue;
            }
            onClash(attacker.getUserOrThrow());
            onClash(target);

            // Override stun with high priority 0.5s stun, also stops all current sounds for cleaner audio cue
            if (target instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundStopSoundPacket(null, SoundSource.PLAYERS));
            }
            if (attacker.getUserOrThrow() instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundStopSoundPacket(null, SoundSource.PLAYERS));
            }

            // Cancels both barrages
            stand.cancelMove();
            targetStand.cancelMove();
            final Vec3 midPos = attacker.getBaseEntity().position().scale(.5)
                    .add(targetStand.position().scale(.5));
            attacker.getEntityWorld().playSound(null, midPos.x, midPos.y, midPos.z, JSoundRegistry.IMPACT_1.get(), SoundSource.NEUTRAL, 1, 0.5f);

            return Set.of();
        }

        return targets;
    }

    protected void onClash(final LivingEntity entity) {
        entity.removeEffect(JStatusRegistry.DAZED.get());
        entity.addEffect(new MobEffectInstance(JStatusRegistry.DAZED.get(), 10, 3, true, false));
    }

    @Override
    public int getBlow(final A attacker) {
        int tick = getDuration() - attacker.getMoveStun();
        return tick <= getWindup() ? 0 : (tick - getWindup()) / getInterval();
    }

    @Override
    protected void createShockwaves(A attacker, LivingEntity user) {
        final LivingEntity attackerEntity = attacker.getBaseEntity();
        final RandomSource random = attackerEntity.getRandom();
        Vec3 shockwavePos = attackerEntity.position().add(
                random.nextGaussian() / 3.0,
                random.nextGaussian() / 3.0,
                random.nextGaussian() / 3.0
        );
        final Vec3 rotVec = user.getLookAngle();
        shockwavePos = shockwavePos.add(rotVec);
        shockwavePos = shockwavePos.add(RotationUtil.vecPlayerToWorld(new Vec3(0, attackerEntity.getBbHeight() / 1.8 - getOffset(), 0), GravityChangerAPI.getGravityDirection(user)));
        JComponentPlatformUtils.getShockwaveHandler(attacker.getEntityWorld())
                .addShockwave(shockwavePos, user.getLookAngle(), getDamage() / 1.5f);
    }

    @Override
    protected @NonNull T copyExtras(final @NonNull T base) {
        AbstractBarrageAttack<T, A> cast = super.copyExtras(base);
        cast.inflictsSlowness = inflictsSlowness;
        return base;
    }

    protected abstract static class Type<M extends AbstractBarrageAttack<? extends M, ?>> extends AbstractSimpleAttack.Type<M> {
        protected RecordCodecBuilder<M, Integer> interval() {
            return Codec.INT.fieldOf("interval").forGetter(AbstractBarrageAttack::getInterval);
        }

        protected RecordCodecBuilder<M, Boolean> inflictsSlowness() {
            return Codec.BOOL.optionalFieldOf("inflicts_slowness", true).forGetter(AbstractBarrageAttack::isInflictsSlowness);
        }

        protected Products.P13<RecordCodecBuilder.Mu<M>, BaseMoveExtras, AttackMoveExtras, Integer, Integer, Integer,
                Float, Float, Integer, Float, Float, Float, Integer, Boolean> barrageDefault(final RecordCodecBuilder.Instance<M> instance) {
            return instance.group(extras(), attackExtras(), cooldown(), windup(), duration(), moveDistance(), damage(),
                    stun(), hitboxSize(), knockback(), offset(), interval(), inflictsSlowness());
        }

        protected App<RecordCodecBuilder.Mu<M>, M> barrageDefault(final RecordCodecBuilder.Instance<M> instance, Function10<
                        Integer, Integer, Integer, Float, Float, Integer, Float, Float, Float, Integer, M> function) {
            return barrageDefault(instance).apply(instance, applyAttackExtras((cooldown, windup, duration,
                                                                               moveDistance, damage, stun,
                                                                               hitboxSize, knockback, offset,
                                                                               interval, inflictsSlowness) -> {
                M move = function.apply(cooldown, windup, duration, moveDistance, damage, stun, hitboxSize, knockback, offset, interval);
                if (!inflictsSlowness) move.withoutSlowness();
                return move;
            }));
        }
    }
}
