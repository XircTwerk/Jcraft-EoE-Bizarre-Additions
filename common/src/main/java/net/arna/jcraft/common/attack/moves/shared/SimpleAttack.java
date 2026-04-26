package net.arna.jcraft.common.attack.moves.shared;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.attack.IAttacker;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractSimpleAttack;
import org.jetbrains.annotations.NotNull;

public final class SimpleAttack<A extends IAttacker<? extends A, ?>> extends AbstractSimpleAttack<SimpleAttack<A>, A> {
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
    public SimpleAttack(final int cooldown, final int windup, final int duration, final float moveDistance,
                        final float damage, final int stun,final float hitboxSize, final float knockback, final float offset) {
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
    public static <A extends IAttacker<? extends A, ?>> SimpleAttack<A> lightAttack(final int windup, final int duration,
                                                                                    final float moveDistance, final float damage, final int stun,
                                                                                    final float knockback, final float offset) {
        return new SimpleAttack<A>(duration + stun, windup, duration, moveDistance, damage, stun, 1.5f, knockback, offset).noLoopPrevention();
    }

    @Override
    public @NotNull MoveType<SimpleAttack<A>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    @Override
    protected @NonNull SimpleAttack<A> getThis() {
        return this;
    }

    @Override
    public @NonNull SimpleAttack<A> copy() {
        return copyExtras(new SimpleAttack<>(getCooldown(), getWindup(), getDuration(), getMoveDistance(), getDamage(),
                getStun(), getHitboxSize(), getKnockback(), getOffset()));
    }

    public static class Type extends AbstractSimpleAttack.Type<SimpleAttack<?>> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NotNull App<RecordCodecBuilder.Mu<SimpleAttack<?>>, SimpleAttack<?>> buildCodec(RecordCodecBuilder.Instance<SimpleAttack<?>> instance) {
            return attackDefault(instance, SimpleAttack::new);
        }
    }
}
