package net.arna.jcraft.api.attack.moves;

import com.mojang.datafixers.Products;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.util.Function10;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.NonNull;
import net.arna.jcraft.api.attack.IAttacker;
import net.arna.jcraft.common.attack.core.data.AttackMoveExtras;
import net.arna.jcraft.common.attack.core.data.BaseMoveExtras;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Set;

/**
 * A simple attack that uses ray-cast to hitscan.
 *
 * @param <T>
 * @param <A>
 */
@SuppressWarnings("UnusedReturnValue")
@Getter
public abstract class AbstractHitscanAttack<T extends AbstractHitscanAttack<T, A>, A extends IAttacker<? extends A, ?>> extends AbstractSimpleAttack<T, A> {
    private float range;
    private float hardness;
    private float breakChance;

    protected AbstractHitscanAttack(final int cooldown, final int windup, final int duration, final float moveDistance, final float damage,
                                    final int stun, final float knockback,
                                    final float range, final float hardness, final float breakChance) {
        super(cooldown, windup, duration, moveDistance, damage, stun, 0f, knockback, 0f);

        withRange(range);
        withHardness(hardness);
        withBreakChance(breakChance);
    }

    public T withRange(final float range) {
        this.range = range;
        return getThis();
    }

    public T withHardness(final float hardness) {
        this.hardness = hardness;
        return getThis();
    }

    public T withBreakChance(final float breakChance) {
        this.breakChance = breakChance;
        return getThis();
    }

    @Override
    public @NonNull Set<LivingEntity> perform(final A attacker, final LivingEntity user) {
        if (user == null) {
            return Set.of();
        }
        final HitResult goal = user.pick(getRange(), 0f, false);
        if (goal.getType() == HitResult.Type.ENTITY) {
            final Entity hitEntity = ((EntityHitResult)goal).getEntity();
            if (hitEntity instanceof LivingEntity living) {
                return Set.of(living);
            }
        }
        else if (goal.getType() == HitResult.Type.BLOCK && getBreakChance() > 0f) {
            final BlockPos pos = ((BlockHitResult)goal).getBlockPos();
            final BlockState state = user.level().getBlockState(pos);
            if (getHardness() >= state.getBlock().defaultDestroyTime() && user.getRandom().nextDouble() >= getBreakChance()) {
                user.level().destroyBlock(pos, true, user);
            }
        }
        return Set.of();
    }

    protected abstract static class Type<M extends AbstractHitscanAttack<? extends M, ?>> extends AbstractSimpleAttack.Type<M> {
        protected RecordCodecBuilder<M, Float> range() {
            return Codec.FLOAT.fieldOf("range").forGetter(AbstractHitscanAttack::getRange);
        }
        protected RecordCodecBuilder<M, Float> hardness() {
            return Codec.FLOAT.fieldOf("hardness").forGetter(AbstractHitscanAttack::getHardness);
        }
        protected RecordCodecBuilder<M, Float> breakChance() {
            return Codec.FLOAT.fieldOf("breakChance").forGetter(AbstractHitscanAttack::getBreakChance);
        }

        protected Products.P12<RecordCodecBuilder.Mu<M>, BaseMoveExtras, AttackMoveExtras, Integer, Integer, Integer, Float,
                Float, Integer, Float, Float, Float, Float>
        hitscanDefault(RecordCodecBuilder.Instance<M> instance) {
            return instance.group(extras(), attackExtras(), cooldown(), windup(), duration(), moveDistance(), damage(), stun(),
                    knockback(), range(), hardness(), breakChance());
        }

        protected App<RecordCodecBuilder.Mu<M>, M> hitscanDefault(RecordCodecBuilder.Instance<M> instance, Function10<Integer, Integer, Integer, Float,
                                        Float, Integer, Float, Float, Float, Float, M> function) {
            return hitscanDefault(instance).apply(instance, applyAttackExtras(function));
        }
    }
}
