package net.arna.jcraft.common.attack.moves.shared;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.attack.IAttacker;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractSimpleAttack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.jetbrains.annotations.NotNull;

public final class MovementSlowingSimpleAttack<A extends IAttacker<? extends A, ?>> extends AbstractSimpleAttack<MovementSlowingSimpleAttack<A>, A> {
    /**
     * Creates a new simple attack with a single hitbox.
     *
     * @param cooldown     The cooldown for this attack in ticks.
     * @param windup       The windup of this attack in ticks. How long until the blow is landed.
     * @param duration     The duration after which a new attack can be initiated in ticks.
     * @param moveDistance The distance at which the hitbox is placed.
     * @param damage       The damage this attack deals.
     * @param hitboxSize   The size of the hitbox in blocks.
     * @param knockback    The strength of the knock-back.
     * @param offset       The amount the hitbox is offset by.
     */
    public MovementSlowingSimpleAttack(final int cooldown, final int windup, final int duration, final float moveDistance,
                                       final float damage, final int stun, final float hitboxSize, final float knockback, final float offset) {
        super(cooldown, windup, duration, moveDistance, damage, stun, hitboxSize, knockback, offset);
    }

    /**
     * For light attacks
     *
     * @param windup       The windup of this attack in ticks. How long until the blow is landed.
     * @param duration     The duration after which a new attack can be initiated in ticks.
     * @param moveDistance The distance at which the hitbox is placed.
     * @param damage       The damage this attack deals.
     * @param offset       The amount the hitbox is offset by.
     */
    public static <A extends IAttacker<? extends A, ?>> MovementSlowingSimpleAttack<A> lightAttack(final int windup, final int duration,
                                                                                                   final float moveDistance, final float damage, final int stun,
                                                                                                   final float knockback, final float offset) {
        return new MovementSlowingSimpleAttack<A>(duration, windup, duration, moveDistance, damage, stun, 1.5f, knockback, offset).noLoopPrevention();
    }

    @Override
    public void activeTick(A attacker, int moveStun) {
        super.activeTick(attacker, moveStun);

        if (attacker.hasUser() && !attacker.isRemote()) {
            attacker.getUserOrThrow().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 2, true, false));
        }
    }

    @Override
    public @NotNull MoveType<MovementSlowingSimpleAttack<A>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    @Override
    protected @NonNull MovementSlowingSimpleAttack<A> getThis() {
        return this;
    }

    @Override
    public @NonNull MovementSlowingSimpleAttack<A> copy() {
        return copyExtras(new MovementSlowingSimpleAttack<>(getCooldown(), getWindup(), getDuration(), getMoveDistance(), getDamage(),
                getStun(), getHitboxSize(), getKnockback(), getOffset()));
    }

    public static class Type extends AbstractSimpleAttack.Type<MovementSlowingSimpleAttack<?>> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NotNull App<RecordCodecBuilder.Mu<MovementSlowingSimpleAttack<?>>, MovementSlowingSimpleAttack<?>> buildCodec(RecordCodecBuilder.Instance<MovementSlowingSimpleAttack<?>> instance) {
            return attackDefault(instance, MovementSlowingSimpleAttack::new);
        }
    }
}
