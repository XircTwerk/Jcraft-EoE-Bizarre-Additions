package net.arna.jcraft.common.attack.moves.highwaystar;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractSimpleAttack;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.common.entity.stand.HighwayStarEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@Getter
public class AnkleBreakerAttack extends AbstractSimpleAttack<AnkleBreakerAttack, HighwayStarEntity> {
    private final int knockdownDuration;

    public AnkleBreakerAttack(int cooldown, int windup, int duration, float moveDistance, float damage, int stun,
                              float hitboxSize, float knockback, float offset, int knockdownDuration) {
        super(cooldown, windup, duration, moveDistance, damage, stun, hitboxSize, knockback, offset);
        this.knockdownDuration = knockdownDuration;
    }

    @Override
    public @NotNull MoveType<AnkleBreakerAttack> getMoveType() {
        return Type.INSTANCE;
    }

    @Override
    protected void processTarget(HighwayStarEntity attacker, LivingEntity target, Vec3 kbVec, DamageSource damageSource) {
        super.processTarget(attacker, target, kbVec, damageSource);

        // Apply knockdown effect
        target.addEffect(new MobEffectInstance(JStatusRegistry.KNOCKDOWN.get(), knockdownDuration, 0, true, false));

        // Low kick that specifically targets legs - extra movement reduction
        target.setDeltaMovement(target.getDeltaMovement().multiply(0.1, 1.0, 0.1));
    }

    @Override
    protected @NonNull AnkleBreakerAttack getThis() {
        return this;
    }

    @Override
    public @NonNull AnkleBreakerAttack copy() {
        return copyExtras(new AnkleBreakerAttack(getCooldown(), getWindup(), getDuration(), getMoveDistance(),
                getDamage(), getStun(), getHitboxSize(), getKnockback(), getOffset(), getKnockdownDuration()));
    }

    public static class Type extends AbstractSimpleAttack.Type<AnkleBreakerAttack> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NotNull App<RecordCodecBuilder.Mu<AnkleBreakerAttack>, AnkleBreakerAttack> buildCodec(RecordCodecBuilder.Instance<AnkleBreakerAttack> instance) {
            return instance.group(extras(), attackExtras(), cooldown(), windup(), duration(), moveDistance(), damage(),
                            stun(), hitboxSize(), knockback(), offset(),
                            Codec.INT.fieldOf("knockdown_duration").forGetter(AnkleBreakerAttack::getKnockdownDuration))
                    .apply(instance, applyAttackExtras(AnkleBreakerAttack::new));
        }
    }
}