package net.arna.jcraft.common.attack.moves.speedking;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.common.entity.damage.JDamageSources;
import net.arna.jcraft.common.entity.stand.SpeedKingEntity;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.arna.jcraft.api.Attacks.damageLogic;

@Getter
public final class FireGrabHitAttack extends AbstractMove<FireGrabHitAttack, SpeedKingEntity> {
    private final int stun;
    private final float damage;

    public FireGrabHitAttack(final int cooldown, final int windup, final int duration, final float moveDistance,
                             final float damage, final int stun) {
        super(cooldown, windup, duration, moveDistance);
        this.damage = damage;
        this.stun = stun;
    }

    @Override
    public @NonNull MoveType<FireGrabHitAttack> getMoveType() {
        return Type.INSTANCE;
    }

    @Override
    public @NonNull Set<LivingEntity> perform(final SpeedKingEntity attacker, final LivingEntity user) {
        // Find the entity that is currently being grabbed by this user
        List<LivingEntity> potentialTargets = attacker.level().getEntitiesOfClass(LivingEntity.class,
                attacker.getBoundingBox().inflate(3.0),
                entity -> entity != user);

        Set<LivingEntity> hitTargets = new HashSet<>();

        // Only hit the target that is being grabbed by this attacker
        for (LivingEntity target : potentialTargets) {
            // Check if this entity is currently being grabbed by this attacker
            Entity grabAttacker = JComponentPlatformUtils.getGrab(target).getAttacker();
            if (grabAttacker == attacker.getBaseEntity()) {
                final ServerLevel world = (ServerLevel) attacker.level();
                final DamageSource damageSource = JDamageSources.stand(attacker);

                // Strong knockback, also knocks up
                Vec3 forwardDirection = attacker.getLookAngle().normalize();
                Vec3 knockbackVector = forwardDirection.add(0, 0.5, 0);

                damageLogic(world, target, knockbackVector, stun, 3, true,
                        damage, false, 4, damageSource, user, null);

                applyHeatEffects(target);

                hitTargets.add(target);
                break; // Only hit one target - the grabbed one
            }
        }

        return hitTargets;
    }

    private void applyHeatEffects(LivingEntity target) {
        target.addEffect(new MobEffectInstance(JStatusRegistry.BOILING.get(), 200, 0, false, true)); // 10 seconds
    }

    @Override
    protected @NonNull FireGrabHitAttack getThis() {
        return this;
    }

    @Override
    public @NonNull FireGrabHitAttack copy() {
        return copyExtras(new FireGrabHitAttack(getCooldown(), getWindup(), getDuration(), getMoveDistance(),
                getDamage(), getStun()));
    }

    public static class Type extends AbstractMove.Type<FireGrabHitAttack> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<FireGrabHitAttack>, FireGrabHitAttack> buildCodec(RecordCodecBuilder.Instance<FireGrabHitAttack> instance) {
            return baseDefault(instance)
                    .and(Codec.FLOAT.fieldOf("damage").forGetter(FireGrabHitAttack::getDamage))
                    .and(Codec.INT.fieldOf("stun").forGetter(FireGrabHitAttack::getStun))
                    .apply(instance, applyExtras(FireGrabHitAttack::new));
        }
    }
}