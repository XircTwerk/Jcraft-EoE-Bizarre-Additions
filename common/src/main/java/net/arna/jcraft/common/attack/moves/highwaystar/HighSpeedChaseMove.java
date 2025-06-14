package net.arna.jcraft.common.attack.moves.highwaystar;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.common.entity.stand.HighwayStarEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HighSpeedChaseMove extends AbstractMove<HighSpeedChaseMove, HighwayStarEntity> {
    private static final int MAX_SPEED_LEVEL = 1; // Speed 2 (amplifier 1)
    private static final int STUN_DURATION = 100; // 5 seconds

    private static final Map<LivingEntity, Boolean> ACTIVE_USERS = new HashMap<>();
    private static final Map<LivingEntity, Integer> STUN_TIMERS = new HashMap<>();

    public HighSpeedChaseMove(int cooldown, int windup, int duration, float moveDistance) {
        super(cooldown, windup, duration, moveDistance);
    }

    @Override
    public @NotNull MoveType<HighSpeedChaseMove> getMoveType() {
        return Type.INSTANCE;
    }

    @Override
    public @NonNull Set<LivingEntity> perform(HighwayStarEntity attacker, LivingEntity user) {
        Boolean isActive = ACTIVE_USERS.get(user);

        if (isActive == null || !isActive) {
            // Activate
            activateForUser(user);
        } else {
            // Deactivate
            ACTIVE_USERS.put(user, false);
            STUN_TIMERS.remove(user);
            user.removeEffect(MobEffects.MOVEMENT_SPEED);
            user.sendSystemMessage(Component.literal("High-Speed Chase DISABLED"));
        }

        return Set.of();
    }

    // Auto-activate method for when stand is summoned
    public static void autoActivate(LivingEntity user) {
        if (!ACTIVE_USERS.getOrDefault(user, false)) {
            activateForUser(user);
        }
    }

    private static void activateForUser(LivingEntity user) {
        ACTIVE_USERS.put(user, true);
        // Apply speed immediately
        user.removeEffect(MobEffects.MOVEMENT_SPEED);
        user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, MAX_SPEED_LEVEL, false, false, true));
        user.sendSystemMessage(Component.literal("High-Speed Chase ACTIVE - Speed 2"));
    }

    public static boolean isActive(LivingEntity user) {
        return ACTIVE_USERS.getOrDefault(user, false);
    }

    public static void handleSpeedMomentum(HighwayStarEntity attacker, LivingEntity user) {
        if (!ACTIVE_USERS.getOrDefault(user, false)) return;

        // Check if stand is active
        if (!attacker.hasUser() || !attacker.isAlive()) {
            ACTIVE_USERS.put(user, false);
            STUN_TIMERS.remove(user);
            user.removeEffect(MobEffects.MOVEMENT_SPEED);
            return;
        }

        // Check stun timer
        Integer stunTimer = STUN_TIMERS.get(user);
        if (stunTimer != null && stunTimer > 0) {
            STUN_TIMERS.put(user, stunTimer - 1);
            user.removeEffect(MobEffects.MOVEMENT_SPEED);

            // Show countdown every second
            if (stunTimer % 20 == 0) {
                user.sendSystemMessage(Component.literal("Speed returns in " + (stunTimer / 20) + "s"));
            }

            if (stunTimer == 1) {
                STUN_TIMERS.remove(user);
                user.sendSystemMessage(Component.literal("Speed restored!"));
                // Reapply speed
                user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, MAX_SPEED_LEVEL, false, false, true));
            }
            return;
        }

        // Ensure speed effect stays active (refresh every second)
        if (user.tickCount % 20 == 0) {
            user.removeEffect(MobEffects.MOVEMENT_SPEED);
            user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, MAX_SPEED_LEVEL, false, false, true));
        }
    }

    public static void handleSpeedStunVulnerability(LivingEntity user, LivingEntity attacker) {
        if (!ACTIVE_USERS.getOrDefault(user, false)) return;

        // When hit, lose speed for 5 seconds
        STUN_TIMERS.put(user, STUN_DURATION);
        user.removeEffect(MobEffects.MOVEMENT_SPEED);
        user.sendSystemMessage(Component.literal("Speed lost for 5 seconds!"));
    }

    public static void deactivate(LivingEntity user) {
        ACTIVE_USERS.put(user, false);
        STUN_TIMERS.remove(user);
        user.removeEffect(MobEffects.MOVEMENT_SPEED);
    }

    public static void cleanupInactivePlayers() {
        ACTIVE_USERS.entrySet().removeIf(entry -> !entry.getKey().isAlive());
        STUN_TIMERS.entrySet().removeIf(entry -> !entry.getKey().isAlive());
    }

    @Override
    protected @NonNull HighSpeedChaseMove getThis() {
        return this;
    }

    @Override
    public @NonNull HighSpeedChaseMove copy() {
        return copyExtras(new HighSpeedChaseMove(getCooldown(), getWindup(), getDuration(), getMoveDistance()));
    }

    public static class Type extends AbstractMove.Type<HighSpeedChaseMove> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NotNull App<RecordCodecBuilder.Mu<HighSpeedChaseMove>, HighSpeedChaseMove> buildCodec(RecordCodecBuilder.Instance<HighSpeedChaseMove> instance) {
            return baseDefault(instance, HighSpeedChaseMove::new);
        }
    }
}