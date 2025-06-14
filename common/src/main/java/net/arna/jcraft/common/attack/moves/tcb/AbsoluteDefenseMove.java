package net.arna.jcraft.common.attack.moves.tcb;

import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.common.attack.moves.mandom.RewindMove;
import net.arna.jcraft.common.entity.stand.TCBEntity;
import net.arna.jcraft.api.registry.JMoveTypeRegistry;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class AbsoluteDefenseMove extends AbstractMove<AbsoluteDefenseMove, TCBEntity> {

    public AbsoluteDefenseMove() {
        super(0, 10, Integer.MAX_VALUE, 0f);
        this.counter = true; // Acts as a counter since it reflects damage
        this.manualCooldown = false;
        this.copyOnUse = false;
    }

    @Override
    public void onInitiate(final TCBEntity stand) {
        super.onInitiate(stand);

        if (stand.hasUser()) {
            LivingEntity user = stand.getUserOrThrow();

            // Stop all user momentum
            user.setDeltaMovement(0, 0, 0);

            // Remove any active effects that would move the user
            user.removeAllEffects();
        }
    }

    public void postPerform(TCBEntity stand, LivingEntity user, int remainingTicks) {
        // Keep the user immobile
        user.setDeltaMovement(0, 0, 0);

        // Check if we should deactivate (when user releases the key)
        if (!stand.isHolding()) {
            stand.deactivateAbsoluteDefense();
            stand.cancelMove();
        }
    }

    @Override
    public void onCancel(TCBEntity stand) {
        super.onCancel(stand);
        stand.deactivateAbsoluteDefense();
    }

    @Override
    public float getMoveDistance() {
        return 0f;
    }

    @Override
    public int getArmor() {
        return Integer.MAX_VALUE; // Can't be interrupted
    }

    @Override
    public @Nullable TCBEntity.State getAnimation() {
        return TCBEntity.State.ABSOLUTE_DEFENSE;
    }

    public boolean canAttack() {
        return true; // Can't use other moves during this
    }

    @Override
    public boolean isCharge() {
        return false;
    }

    @Override
    public @NonNull MoveType<AbsoluteDefenseMove> getMoveType() {
        return Type.INSTANCE;
    }


    @Override
    protected @NonNull AbsoluteDefenseMove getThis() {
        return this;
    }

    @Override
    public @NonNull AbsoluteDefenseMove copy() {
        return copyExtras(new AbsoluteDefenseMove());
    }

    @Override
    public boolean onInitMove(TCBEntity stand, MoveClass moveClass) {
        // Block all other moves during Absolute Defense
        return true; // Consume the event
    }

    @Override
    public @NonNull Set<LivingEntity> perform(TCBEntity stand, LivingEntity user) {
        // Activate Absolute Defense
        stand.activateAbsoluteDefense();
        return Set.of(); // No direct targets
    }

    @Override
    public void activeTick(TCBEntity stand, int moveStun) {
        super.activeTick(stand, moveStun);

        if (stand.hasUser()) {
            LivingEntity user = stand.getUserOrThrow();

            // Keep the user immobile
            user.setDeltaMovement(0, 0, 0);

            // Check if we should deactivate (when user releases the key)
            if (!stand.isHolding() || !stand.isUsingAbsoluteDefense()) {
                stand.deactivateAbsoluteDefense();
                stand.cancelMove();
            }
        }
    }

    @Override
    public boolean preventsMoves() {
        return true;
    }

    public static class Type extends AbstractMove.Type<AbsoluteDefenseMove> {
        public static final AbsoluteDefenseMove.Type INSTANCE = new AbsoluteDefenseMove.Type();
        @Override
        protected @NonNull com.mojang.datafixers.kinds.App<com.mojang.serialization.codecs.RecordCodecBuilder.Mu<AbsoluteDefenseMove>, AbsoluteDefenseMove> buildCodec(com.mojang.serialization.codecs.RecordCodecBuilder.Instance<AbsoluteDefenseMove> instance) {
            return baseDefault(instance, (cooldown, windup, duration, moveDistance) -> new AbsoluteDefenseMove());
        }
    }
}