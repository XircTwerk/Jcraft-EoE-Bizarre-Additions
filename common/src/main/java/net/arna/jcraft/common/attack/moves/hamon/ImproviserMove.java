package net.arna.jcraft.common.attack.moves.hamon;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.common.spec.HamonSpec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public class ImproviserMove extends AbstractMove<ImproviserMove, HamonSpec> {

    @Getter
    private final float chargePerTick;
    private boolean active;

    public ImproviserMove(final int cooldown, final int windup, final int duration, final float chargePerTick) {
        super(cooldown, windup, duration, 0f);
        this.chargePerTick = chargePerTick;
    }

    @Override
    public @NonNull Set<LivingEntity> perform(final HamonSpec attacker, final LivingEntity user) {
        active = !active;
        return Set.of();
    }

    @Override
    public void tick(final HamonSpec attacker) {
        if (active && attacker.hasUser() && attacker.getCharge() >= chargePerTick) {
            final LivingEntity living = attacker.getUserOrThrow();
            if (!(living instanceof ServerPlayer player) || !player.isCreative()) {
                attacker.drainCharge(chargePerTick);
            }
            living.addEffect(new MobEffectInstance(JStatusRegistry.WATER_WALKING.get(), 1, 0, false, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1, 0, false, false, true));
        }
        else if (attacker.getCharge() < chargePerTick) {
            active = false;
        }
    }

    @Override
    protected @NonNull ImproviserMove getThis() {
        return this;
    }

    @Override
    public @NonNull ImproviserMove copy() {
        return copyExtras(new ImproviserMove(getCooldown(), getWindup(), getDuration(), getChargePerTick()));
    }

    @Override
    public @NonNull MoveType<ImproviserMove> getMoveType() {
        return Type.INSTANCE.cast();
    }

    public static class Type extends AbstractMove.Type<ImproviserMove> {

        public static final Type INSTANCE = new Type();

        protected RecordCodecBuilder<ImproviserMove, Float> chargePerTick() {
            return Codec.FLOAT.fieldOf("chargePerTick").forGetter(ImproviserMove::getChargePerTick);
        }

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<ImproviserMove>, ImproviserMove> buildCodec(final RecordCodecBuilder.Instance<ImproviserMove> instance) {
            return instance.group(cooldown(), windup(), duration(), chargePerTick()).apply(instance, ImproviserMove::new);
        }

    }

}
