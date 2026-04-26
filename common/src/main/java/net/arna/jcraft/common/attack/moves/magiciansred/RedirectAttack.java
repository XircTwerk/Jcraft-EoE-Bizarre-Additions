package net.arna.jcraft.common.attack.moves.magiciansred;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.MoveSelectionResult;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.common.entity.projectile.AnkhProjectile;
import net.arna.jcraft.common.entity.stand.MagiciansRedEntity;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.api.attack.enums.MobilityType;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Set;

public final class RedirectAttack extends AbstractMove<RedirectAttack, MagiciansRedEntity> {
    public RedirectAttack(final int cooldown, final int windup, final int duration, final float moveDistance) {
        super(cooldown, windup, duration, moveDistance);
        mobilityType = MobilityType.TELEPORT; // this is a LIE, it just tells the AI to use it at a range of >3m
    }

    @Override
    public @NonNull MoveType<RedirectAttack> getMoveType() {
        return Type.INSTANCE;
    }

    @Override
    public @NonNull Set<LivingEntity> perform(final MagiciansRedEntity attacker, final LivingEntity user) {
        final List<AnkhProjectile> ankhs = attacker.level().getEntitiesOfClass(AnkhProjectile.class,
                attacker.getBoundingBox().inflate(32), EntitySelector.ENTITY_STILL_ALIVE);

        final Vec3 eyePos = getOffsetHeightPos(attacker);
        if (!ankhs.isEmpty()) {
            final Vec3 pos = JUtils.raycastAll(user, eyePos, eyePos.add(user.getLookAngle().scale(24)), ClipContext.Fluid.NONE).getLocation();

            for (AnkhProjectile ankh : ankhs) {
                if (ankh.getOwner() != user) {
                    continue;
                }
                ankh.setVariation(false);
                ankh.setDeltaMovement(pos.subtract(ankh.position()).normalize().scale(0.6));
                ankh.hurtMarked = true;
            }
        }

        return Set.of();
    }

    @Override
    public MoveSelectionResult specificMoveSelectionCriterion(MagiciansRedEntity attacker, LivingEntity mob,
                                                                                  LivingEntity target, int stunTicks, int enemyMoveStun,
                                                                                  double distance, StandEntity<?, ?> enemyStand,
                                                                                  AbstractMove<?, ?> enemyAttack) {
        return attacker.getRandom().nextFloat() > 0.05 ? MoveSelectionResult.STOP : MoveSelectionResult.PASS;
    }

    @Override
    protected @NonNull RedirectAttack getThis() {
        return this;
    }

    @Override
    public @NonNull RedirectAttack copy() {
        return copyExtras(new RedirectAttack(getCooldown(), getWindup(), getDuration(), getMoveDistance()));
    }

    public static class Type extends AbstractMove.Type<RedirectAttack> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<RedirectAttack>, RedirectAttack> buildCodec(RecordCodecBuilder.Instance<RedirectAttack> instance) {
            return baseDefault(instance, RedirectAttack::new);
        }
    }
}
