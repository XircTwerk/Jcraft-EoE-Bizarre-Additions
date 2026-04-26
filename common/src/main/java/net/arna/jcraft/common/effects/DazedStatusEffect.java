package net.arna.jcraft.common.effects;

import net.arna.jcraft.common.tickable.JEnemies;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.UUID;

public class DazedStatusEffect extends MobEffect {
    private static final UUID slowUUID = UUID.fromString("778B48FC-485B-5BA7-58C7-E0D755CE354D");

    public DazedStatusEffect() {
        super(MobEffectCategory.NEUTRAL, 0x444444);
        this.addAttributeModifier(
                Attributes.ATTACK_DAMAGE,
                "FE04CA6A-A3D1-E22B-CB00-EDA6A853F90E",
                -1.0,
                AttributeModifier.Operation.MULTIPLY_BASE
        ).addAttributeModifier(
                Attributes.ATTACK_SPEED,
                "CB402E34-0AAC-383B-B26B-B253430DEEEA",
                -1.0,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ).addAttributeModifier(Attributes.MOVEMENT_SPEED,
                "778B48FC-485B-5BA7-58C7-E0D755CE354D",
                0,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ).addAttributeModifier(
                Attributes.FLYING_SPEED,
                "516B532C-D1D9-C3A0-8970-A2C0A38CC452",
                0,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }

    // Should the status effect be applied and under what condition?
    @Override
    public boolean isDurationEffectTick(final int duration, final int amplifier) {
        return true;
    }

    public static boolean canBeComboBroken(final int amplifier) {
        return switch (amplifier) {
            case (1), (3), (4) -> true;
            default -> false;
        };
    }

    // Stun heavily reduces horizontal speed and prevents mobs from attacking
    // Amplifier = Source ID
    // 0 - Hitstun, not combo breakable
    // 1 - Hitstun, combo breakable
    // 2 - Blocking, not combo breakable
    // 3 - Launch, combo breakable
    // 4 - Winded, small movement penalty, combo breakable
    @Override
    public void applyEffectTick(final LivingEntity entity, final int amplifier) {
        Vec3 eVel = entity.getDeltaMovement();
        double yVel = eVel.y;
        double horizontalMult = 0.4;

        if (amplifier < 2) { // Immobilizing stun
            yVel = Mth.clamp(yVel, -0.5, 0.5);
            horizontalMult = 0.8;
        } else if (amplifier == 3) {
            horizontalMult = 1;
        }

        entity.setDeltaMovement(eVel.x * horizontalMult, yVel, eVel.z * horizontalMult);

        if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
        }

        if (amplifier == 2) {
            return; // Blockstun should not disable targetting
        }
        if (entity instanceof Mob mob) {
            if (JEnemies.contains(mob)) return;
            mob.setTarget(null);
            mob.setAggressive(false);
        }
    }

    @Override
    public double getAttributeModifierValue(final int amplifier, final AttributeModifier modifier) {
        if (Objects.equals(modifier.getId(), slowUUID)) {
            return switch (amplifier) {
                case 3, 1, 0 -> -1;
                case 4 -> -0.25;
                default -> 0;
            };
        }

        return super.getAttributeModifierValue(amplifier, modifier);
    }
}
