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

import java.util.Set;

public final class FlashbangAttack extends AbstractMove<FlashbangAttack, SpeedKingEntity> {

    public FlashbangAttack(final int cooldown, final int windup, final int duration, final float moveDistance) {
        super(cooldown, windup, duration, moveDistance);
        ranged = true;
    }

    @Override
    public @NonNull MoveType<FlashbangAttack> getMoveType() {
        return Type.INSTANCE;
    }

    @Override
    public @NonNull Set<LivingEntity> perform(final SpeedKingEntity attacker, final LivingEntity user) {
        final int sparkCount = 5;
        final float spreadAngle = 30f;

        for (int i = 0; i < sparkCount; i++) {
            float angleOffset = (spreadAngle / (sparkCount - 1)) * i - (spreadAngle / 2);

            final FireSparkProjectile spark = new FireSparkProjectile(attacker.level(), user);

            Vec3 startPos = getOffsetHeightPos(attacker);
            spark.setPos(startPos);

            Vec3 direction = user.getLookAngle();
            Vec3 spreadDirection = direction.yRot((float) Math.toRadians(angleOffset));

            spark.setDeltaMovement(spreadDirection.scale(1.2f));
            spark.hurtMarked = true;

            spark.setFlashbangMode(true);

            attacker.level().addFreshEntity(spark);
        }

        return Set.of();
    }

    @Override
    protected @NonNull FlashbangAttack getThis() {
        return this;
    }

    @Override
    public @NonNull FlashbangAttack copy() {
        return copyExtras(new FlashbangAttack(getCooldown(), getWindup(), getDuration(), getMoveDistance()));
    }

    public static class Type extends AbstractMove.Type<FlashbangAttack> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<FlashbangAttack>, FlashbangAttack> buildCodec(RecordCodecBuilder.Instance<FlashbangAttack> instance) {
            return baseDefault(instance, FlashbangAttack::new);
        }
    }
}