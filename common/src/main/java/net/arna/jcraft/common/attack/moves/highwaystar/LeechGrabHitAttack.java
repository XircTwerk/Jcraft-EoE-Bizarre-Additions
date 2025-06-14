package net.arna.jcraft.common.attack.moves.highwaystar;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntCollection;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractMultiHitAttack;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.arna.jcraft.common.entity.stand.HighwayStarEntity;
import net.arna.jcraft.common.util.JParticleType;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.JCraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class LeechGrabHitAttack extends AbstractMultiHitAttack<LeechGrabHitAttack, HighwayStarEntity> {

    public LeechGrabHitAttack(int cooldown, int duration, float moveDistance, float damage, int stun,
                              float hitboxSize, float knockback, float offset, @NonNull IntCollection hitMoments) {
        super(cooldown, duration, moveDistance, damage, stun, hitboxSize, knockback, offset, hitMoments);
    }

    @Override
    public @NotNull MoveType<LeechGrabHitAttack> getMoveType() {
        return Type.INSTANCE;
    }

    @Override
    public @NonNull Set<LivingEntity> perform(HighwayStarEntity attacker, LivingEntity user) {
        // Get targets from the parent multi-hit logic (should only be 1 target)
        Set<LivingEntity> targets = super.perform(attacker, user);

        // Only process if we hit a target
        if (!targets.isEmpty()) {
            // Heal the user for 0.5 hearts per hit (4 hits total = 2 hearts)
            user.heal(1.0f); // 1.0f = 0.5 hearts

            // Play the vampire suck sound effect
            JUtils.serverPlaySound(JSoundRegistry.VAMPIRE_SUCK.get(), (ServerLevel) user.level(), user.position(), 32);

            // Apply exhaustion and visual effects to targets
            for (LivingEntity target : targets) {
                // Create blood/life drain particle effects similar to vampire attack
                float bloodMult = JUtils.getBloodMult(target);

                // Create particle effect at target position
                Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);
                JCraft.createParticle((ServerLevel) target.level(), targetPos.x, targetPos.y, targetPos.z, JParticleType.BACK_STAB);

                if (target instanceof Player targetPlayer) {
                    // Increased exhaustion to match the life drain effect
                    targetPlayer.causeFoodExhaustion(2.0f);
                }
            }
        }

        return targets;
    }

    @Override
    protected @NonNull LeechGrabHitAttack getThis() {
        return this;
    }

    @Override
    public @NonNull LeechGrabHitAttack copy() {
        return copyExtras(new LeechGrabHitAttack(getCooldown(), getDuration(), getMoveDistance(),
                getDamage(), getStun(), getHitboxSize(), getKnockback(), getOffset(), getHitMoments()));
    }

    public static class Type extends AbstractMultiHitAttack.Type<LeechGrabHitAttack> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<LeechGrabHitAttack>, LeechGrabHitAttack>
        buildCodec(RecordCodecBuilder.Instance<LeechGrabHitAttack> instance) {
            return multiHitDefault(instance, LeechGrabHitAttack::new);
        }
    }
}