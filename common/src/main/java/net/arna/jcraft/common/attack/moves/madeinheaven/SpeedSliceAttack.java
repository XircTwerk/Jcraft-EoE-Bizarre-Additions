package net.arna.jcraft.common.attack.moves.madeinheaven;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.NonNull;
import net.arna.jcraft.api.AttackData;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.enums.MobilityType;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.component.living.CommonHitPropertyComponent;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.arna.jcraft.common.entity.stand.MadeInHeavenEntity;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.arna.jcraft.api.Attacks.damageLogic;

@Getter
public final class SpeedSliceAttack extends AbstractMove<SpeedSliceAttack, MadeInHeavenEntity> {
    private final float damage, hitboxSize, knockback;

    public SpeedSliceAttack(final int cooldown, final int windup, final int duration, final float moveDistance, final float damage, final float hitboxSize,
                            final float knockback) {
        super(cooldown, windup, duration, moveDistance);
        this.damage = damage;
        this.hitboxSize = hitboxSize;
        this.knockback = knockback;

        ranged = true;
        mobilityType = MobilityType.TELEPORT;
    }

    @Override
    public @NonNull MoveType<SpeedSliceAttack> getMoveType() {
        return Type.INSTANCE;
    }

    @Override
    public @NonNull Set<LivingEntity> perform(final MadeInHeavenEntity attacker, final LivingEntity user) {
        return doSpeedSlice(attacker, user.getEyePosition(), user.getEyePosition().add(user.getLookAngle().scale(8)),
                getDamage(), getKnockback(), getHitboxSize(), 20, 1);
    }

    public static Set<LivingEntity> doSpeedSlice(final MadeInHeavenEntity attacker, final Vec3 start, final Vec3 end,
                                                 final float damage, final float knockback, final float size,
                                                 final int stunTicks, final int stunType) {
        final Level world = attacker.level();
        final LivingEntity user = attacker.getUserOrThrow();
        final HitResult hitResult = world.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user));
        final Vec3 pos1 = user.position();
        final Vec3 pos2 = hitResult.getLocation();
        final Vec3 towardsVec = pos2.subtract(pos1);

        final Vec3 kbVec = towardsVec.normalize();

        final DamageSource playerSource = world.damageSources().mobAttack(user);

        user.teleportToWithTicket(pos2.x, pos2.y, pos2.z);

        final Set<LivingEntity> targets = new HashSet<>();
        double count = Math.round(pos1.distanceTo(pos2));

        for (int i = 0; i < count; i++) {
            final Vec3 curPos = pos1.add(towardsVec.scale(i / count));

            final Vec3 vec1 = curPos.add(-size, -size, -size);
            final Vec3 vec2 = curPos.add(size, size, size);

            JUtils.displayHitbox(world, vec1, vec2);

            final List<LivingEntity> hurt = world.getEntitiesOfClass(LivingEntity.class, new AABB(vec1, vec2),
                    EntitySelector.NO_CREATIVE_OR_SPECTATOR.and(e -> e != attacker && e != user));
            hurt.removeIf(targets::contains);
            targets.addAll(hurt);
        }

        for (final LivingEntity ent : targets) {
            LivingEntity target = JUtils.getUserIfStand(ent);
            damageLogic(world, target, new AttackData(
                    kbVec.scale(knockback).add(0, knockback / 4, 0),
                    stunTicks, stunType, false, damage, true, (int) (4 + damage),
                    playerSource, user, CommonHitPropertyComponent.HitAnimation.MID, attacker.getMoveUsage(),
                    false, false
            ));
        }

        if (attacker.getAccelTime() > 0 && !targets.isEmpty()) {
            attacker.incrementSpeedometer();
        }

        attacker.playSound(JSoundRegistry.MIH_ZOOM.get(), 1f, 1f);

        return targets;
    }

    @Override
    protected @NonNull SpeedSliceAttack getThis() {
        return this;
    }

    @Override
    public @NonNull SpeedSliceAttack copy() {
        return copyExtras(new SpeedSliceAttack(getCooldown(), getWindup(), getDuration(), getMoveDistance(),
                getDamage(), getHitboxSize(), getKnockback()));
    }

    public static class Type extends AbstractMove.Type<SpeedSliceAttack> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<SpeedSliceAttack>, SpeedSliceAttack> buildCodec(RecordCodecBuilder.Instance<SpeedSliceAttack> instance) {
            return baseDefault(instance).and(instance.group(
                    Codec.FLOAT.fieldOf("damage").forGetter(SpeedSliceAttack::getDamage),
                    Codec.FLOAT.fieldOf("hitbox_size").forGetter(SpeedSliceAttack::getHitboxSize),
                    Codec.FLOAT.fieldOf("knockback").forGetter(SpeedSliceAttack::getKnockback)
            )).apply(instance, applyExtras(SpeedSliceAttack::new));
        }
    }
}
