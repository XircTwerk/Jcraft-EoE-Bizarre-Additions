package net.arna.jcraft.common.attack.moves.theworld.overheaven;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractSimpleAttack;
import net.arna.jcraft.common.entity.projectile.KnifeProjectile;
import net.arna.jcraft.common.entity.stand.TheWorldOverHeavenEntity;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public final class AerialDivineFinisherAttack extends AbstractSimpleAttack<AerialDivineFinisherAttack, TheWorldOverHeavenEntity> {
    public AerialDivineFinisherAttack(final int cooldown, final int windup, final int duration, final float moveDistance, final float damage, final int stun,
                                      final float hitboxSize, final float knockback, final float offset) {
        super(cooldown, windup, duration, moveDistance, damage, stun, hitboxSize, knockback, offset);
        ranged = true;
    }

    @Override
    public @NonNull MoveType<AerialDivineFinisherAttack> getMoveType() {
        return Type.INSTANCE;
    }

    @Override
    public void onInitiate(TheWorldOverHeavenEntity attacker) {
        super.onInitiate(attacker);
        attacker.getUserOrThrow().addEffect(new MobEffectInstance(
                MobEffects.LEVITATION, 10, 2, true, false
        ));
    }

    @Override
    public @NonNull Set<LivingEntity> perform(final TheWorldOverHeavenEntity attacker, final LivingEntity user) {
        final Set<LivingEntity> targets = super.perform(attacker, user);

        final Vec3 heightOffset = getOffsetHeightPos(attacker);

        final RandomSource random = attacker.getRandom();
        for (int i = 0; i < 8; i++) {
            final KnifeProjectile knife = new KnifeProjectile(attacker.level(), user);
            knife.setLightning(true);
            knife.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            knife.explosive = true;
            knife.shootFromRotation(user, user.getXRot(), user.getYRot(), 0, 2.0f - (i / 8.0f), 1);
            knife.setPos(heightOffset.add(
                    random.triangle(0, 0.5),
                    random.triangle(0, 0.5),
                    random.triangle(0, 0.5)));
            attacker.level().addFreshEntity(knife);
        }

        return targets;
    }

    @Override
    protected @NonNull AerialDivineFinisherAttack getThis() {
        return this;
    }

    @Override
    public @NonNull AerialDivineFinisherAttack copy() {
        return copyExtras(new AerialDivineFinisherAttack(getCooldown(), getWindup(), getDuration(), getMoveDistance(),
                getDamage(), getStun(), getHitboxSize(), getKnockback(), getOffset()));
    }

    public static class Type extends AbstractSimpleAttack.Type<AerialDivineFinisherAttack> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<AerialDivineFinisherAttack>, AerialDivineFinisherAttack> buildCodec(RecordCodecBuilder.Instance<AerialDivineFinisherAttack> instance) {
            return attackDefault(instance, AerialDivineFinisherAttack::new);
        }
    }
}
