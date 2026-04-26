package net.arna.jcraft.common.ai;

import lombok.Getter;
import lombok.NonNull;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.world.entity.LivingEntity;

public class CombatInstantContext {
    @Getter
    private CombatEntityContext attackerCtx, targetCtx;
    @Getter
    private double distanceBetween;

    public void update(@NonNull LivingEntity attacker, @NonNull LivingEntity target) {
        attackerCtx = CombatEntityContext.from(attacker);
        targetCtx = CombatEntityContext.from(target);

        distanceBetween = Math.sqrt(
                JUtils.min(
                        JUtils.nullSafeDistanceSqr(attacker, target),
                        JUtils.nullSafeDistanceSqr(attacker, targetCtx.stand()),
                        JUtils.nullSafeDistanceSqr(attackerCtx.stand(), target),
                        JUtils.nullSafeDistanceSqr(attackerCtx.stand(), targetCtx.stand())
                )
        );
    }

    public CombatInstantContext() {
    }
}
