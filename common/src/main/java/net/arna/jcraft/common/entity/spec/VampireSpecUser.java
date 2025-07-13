package net.arna.jcraft.common.entity.spec;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import mod.azure.azurelib.animatable.GeoEntity;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.Animation;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.AnimationState;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
import mod.azure.azurelib.util.AzureLibUtil;
import net.arna.jcraft.api.registry.JSpecTypeRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * A mob that uses the Vampire spec
 */
public class VampireSpecUser extends SpecUserMob implements GeoEntity {

    private final ModifierLayer<IAnimation> modifierLayer = new ModifierLayer<>();
    private final AnimatableInstanceCache geoCache = AzureLibUtil.createInstanceCache(this);

    public VampireSpecUser(EntityType<? extends VampireSpecUser> entityType, Level level) {
        super(entityType, level, JSpecTypeRegistry.VAMPIRE.get());
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        // Add spec combat AI goal
        this.goalSelector.addGoal(1, new SpecCombatGoal(this));

        // Add targeting
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<VampireSpecUser> state) {
        if (state.isMoving()) {
            return state.setAndContinue(RawAnimation.begin().then("animation.vampirespecuser.walk", Animation.LoopType.LOOP));
        }
        return state.setAndContinue(RawAnimation.begin().then("animation.vampirespecuser.idle", Animation.LoopType.LOOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }


    public ModifierLayer<IAnimation> jcraft_getModAnimation() {
        return modifierLayer;
    }

    /**
     * AI goal that uses spec combat logic
     */
    public static class SpecCombatGoal extends Goal {
        private final VampireSpecUser mob;
        private LivingEntity target;

        public SpecCombatGoal(VampireSpecUser mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = mob.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }

            this.target = target;
            return mob.canUseSpecs();
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && mob.canUseSpecs();
        }

        @Override
        public void start() {
            // Setup for combat
        }

        @Override
        public void stop() {
            target = null;
        }

        @Override
        public void tick() {
            if (target == null) return;

            // Use the spec combat AI from SpecUserMob
            SpecUserMob.specUserCombatAI(mob, target);
        }
    }
}