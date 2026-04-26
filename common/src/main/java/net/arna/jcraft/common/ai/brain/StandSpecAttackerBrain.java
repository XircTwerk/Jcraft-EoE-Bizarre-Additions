package net.arna.jcraft.common.ai.brain;

import net.arna.jcraft.api.spec.JSpec;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.ai.AttackerBrainInfo;
import net.arna.jcraft.common.ai.CombatInstantContext;
import net.arna.jcraft.common.ai.IJAttackerBrain;
import net.minecraft.world.entity.Mob;

public interface StandSpecAttackerBrain extends IJAttackerBrain {
    static void tick(Mob mob, AttackerBrainInfo info) {
        final CombatInstantContext combatCtx = IJAttackerBrain.target(mob, info);
        if (combatCtx == null) return;

        final int aiLevel = info.getAiLevel();
        info.setReactionTime(IJAttackerBrain.reactionTimeFor(aiLevel, mob.getRandom()));

        StandEntity<?, ?> stand = StandAttackerBrain.handleDesiredSummoning(mob, info, combatCtx);
        plan(aiLevel, info, combatCtx);
        stand = StandAttackerBrain.handleDesiredSummoning(mob, info, combatCtx);
        if (stand != null) stand.executePlan(aiLevel, info, combatCtx);

        final JSpec<?, ?> spec = combatCtx.getAttackerCtx().spec();
        if (spec != null) spec.executePlan(aiLevel, info, combatCtx);
    }

    static void plan(final int aiLevel, final AttackerBrainInfo info, final CombatInstantContext combatCtx) {
        IJAttackerBrain.planDefense(info);
        IJAttackerBrain.pacing(info, combatCtx);

        final StandEntity<?, ?> stand = combatCtx.getAttackerCtx().stand();
        if (stand != null) {
            stand.plan(aiLevel, info, combatCtx);
        } else {
            final JSpec<?, ?> spec = combatCtx.getAttackerCtx().spec();
            if (spec == null) return;
            spec.plan(aiLevel, info, combatCtx);
        }
    }
}