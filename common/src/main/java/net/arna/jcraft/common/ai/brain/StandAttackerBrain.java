package net.arna.jcraft.common.ai.brain;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.ai.AttackerBrainInfo;
import net.arna.jcraft.common.ai.CombatInstantContext;
import net.arna.jcraft.common.ai.IJAttackerBrain;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.world.entity.Mob;

public interface StandAttackerBrain extends IJAttackerBrain {
    static StandEntity<?, ?> handleDesiredSummoning(Mob mob, AttackerBrainInfo info, CombatInstantContext combatCtx) {
        boolean isStandOnDesired = info.getDesiredStandOffTime() <= 0;
        if (info.getState() == AttackerBrainInfo.State.DEFENSE) isStandOnDesired = true;

        StandEntity<?, ?> stand = JUtils.getStand(mob);
        if (isStandOnDesired) {
            if (stand == null) stand = JCraft.summon(mob);
            combatCtx.getAttackerCtx().reassignStand(stand);
        } else {
            if (stand != null) stand.desummon();
            combatCtx.getAttackerCtx().reassignStand(stand);
            if (stand == null || stand.isRemoved()) return null;
        }

        return stand;
    }

    static void tick(Mob mob, AttackerBrainInfo info) {
        final CombatInstantContext combatCtx = IJAttackerBrain.target(mob, info);
        if (combatCtx == null) return;

        final int aiLevel = info.getAiLevel();
        info.setReactionTime(IJAttackerBrain.reactionTimeFor(aiLevel, mob.getRandom()));

        StandEntity<?, ?> stand = handleDesiredSummoning(mob, info, combatCtx);
        plan(aiLevel, info, combatCtx);
        stand = handleDesiredSummoning(mob, info, combatCtx);
        if (stand != null) stand.executePlan(aiLevel, info, combatCtx);
    }

    static void plan(final int aiLevel, final AttackerBrainInfo info, final CombatInstantContext combatCtx) {
        IJAttackerBrain.planDefense(info);
        IJAttackerBrain.pacing(info, combatCtx);

        final StandEntity<?, ?> stand = combatCtx.getAttackerCtx().stand();
        if (stand == null) return;
        stand.plan(aiLevel, info, combatCtx);
    }
}