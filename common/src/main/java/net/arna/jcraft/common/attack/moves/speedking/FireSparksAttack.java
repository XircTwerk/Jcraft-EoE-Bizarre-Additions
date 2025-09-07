package net.arna.jcraft.common.attack.moves.speedking;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.common.entity.projectile.FireSparkProjectile;
import net.arna.jcraft.common.entity.stand.SpeedKingEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

public final class FireSparksAttack extends AbstractMove<FireSparksAttack, SpeedKingEntity> {

    public FireSparksAttack(final int cooldown, final int windup, final int duration, final float moveDistance) {
        super(cooldown, windup, duration, moveDistance);
        ranged = true;
    }

    @Override
    public @NonNull MoveType<FireSparksAttack> getMoveType() {
        return Type.INSTANCE;
    }

    @Override
    public @NonNull Set<LivingEntity> perform(final SpeedKingEntity attacker, final LivingEntity user) {
        final int sparkCount = 36;
        final int radius = 9;

        Vec3 userPos = user.position();

        // Apply boiling effect to ALL entities within the 9-block radius
        List<LivingEntity> entitiesInCircle = attacker.level().getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(userPos.add(-radius, -3, -radius), userPos.add(radius, 3, radius)),
                entity -> entity != user && entity != attacker && entity.distanceTo(user) <= radius);

        for (LivingEntity entity : entitiesInCircle) {
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.arna.jcraft.api.registry.JStatusRegistry.BOILING.get(), 200, 0, false, true));
        }

        // Shoot projectiles outward in a circle but keep them close
        for (int i = 0; i < sparkCount; i++) {
            // Calculate angle for full 360 degree circle
            double angle = (2 * Math.PI * i) / sparkCount;

            final FireSparkProjectile spark = new FireSparkProjectile(attacker.level(), user);

            Vec3 startPos = getOffsetHeightPos(attacker);
            spark.setPos(startPos);

            // Calculate direction outward from center (horizontal only)
            Vec3 direction = new Vec3(Math.cos(angle), 0, Math.sin(angle)).normalize();

            spark.setDeltaMovement(direction.scale(0.8f)); // Much slower speed to keep them close
            spark.hurtMarked = true;

            // Enable bouncing mode for Fire Sparks attack
            spark.setBouncingMode(true);
            spark.setBaseDamage(3.0f);
            spark.setBounceCount(0);
            spark.setMaxBounces(3);

            // Spawn 1 particle per projectile for visibility
            if (attacker.level().isClientSide) {
                attacker.level().addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                        startPos.x, startPos.y, startPos.z, 0, 0, 0);
            }

            attacker.level().addFreshEntity(spark);
        }

        return new java.util.HashSet<>(entitiesInCircle);
    }

    @Override
    protected @NonNull FireSparksAttack getThis() {
        return this;
    }

    @Override
    public @NonNull FireSparksAttack copy() {
        return copyExtras(new FireSparksAttack(getCooldown(), getWindup(), getDuration(), getMoveDistance()));
    }

    public static class Type extends AbstractMove.Type<FireSparksAttack> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<FireSparksAttack>, FireSparksAttack> buildCodec(RecordCodecBuilder.Instance<FireSparksAttack> instance) {
            return baseDefault(instance, FireSparksAttack::new);
        }
    }
}