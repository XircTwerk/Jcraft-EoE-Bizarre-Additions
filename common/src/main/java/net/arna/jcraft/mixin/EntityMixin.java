package net.arna.jcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.common.effects.AbstractFluidWalkingEffect;
import net.arna.jcraft.common.entity.stand.KingCrimsonEntity;
import net.arna.jcraft.common.events.EntityTickEvent;
import net.arna.jcraft.common.events.JBlockEvents;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.mixin_logic.EntityAddon;
import net.arna.jcraft.mixin_logic.EntityMixinLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(value = Entity.class)
public abstract class EntityMixin implements EntityAddon {

    @Unique
    private boolean fromSpawner = false;

    /**
     * Stand positioning mixin function
     *
     * @param passenger stand entity
     */
    @Inject(method = "positionRider(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$MoveFunction;)V", at = @At("HEAD"), cancellable = true)
    private void jcraft$updatePassengerPosition(Entity passenger, Entity.MoveFunction positionUpdater, CallbackInfo info) {
        EntityMixinLogic.jcraft$updatePassengerPosition((Entity)(Object)this, passenger, positionUpdater, info);
    }

    /**
     * Disables sprinting particles during time erase
     */
    @SuppressWarnings("ConstantValue")
    @Inject(method = "canSpawnSprintParticle", at = @At("HEAD"), cancellable = true)
    private void jcraft$shouldSpawnSprintingParticles(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof LivingEntity living && JUtils.getStand(living) instanceof KingCrimsonEntity kc && kc.getTETime() > 0) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void preTick(CallbackInfo ci) {
        EntityTickEvent.ENTITY_PRE.invoker().tick((Entity) (Object) this);
    }

    @Override
    public boolean jcraft$setFromSpawner() {
        return fromSpawner = true;
    }

    @Override
    public boolean jcraft$isFromSpawner() {
        return fromSpawner;
    }

    @WrapOperation(method = "checkSupportingBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;findSupportingBlock(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/Optional;"))
    private Optional<BlockPos> jcraft$walkOnLiquid(final Level level, final Entity entity, final AABB aabb, final Operation<Optional<BlockPos>> original) {
        Optional<BlockPos> supportPos = original.call(level, entity, aabb);
        if (entity instanceof LivingEntity living && supportPos.isEmpty()) {
            final FluidState state = level.getFluidState(living.blockPosition().below());
            AbstractFluidWalkingEffect[] walkingEffects = new AbstractFluidWalkingEffect[] {
                    JStatusRegistry.WATER_WALKING.get()
            };
            for (AbstractFluidWalkingEffect walkingEffect : walkingEffects) {
                if (living.hasEffect(walkingEffect) && !state.isEmpty() && walkingEffect.supports(state.getType())) {
                    supportPos = Optional.of(living.blockPosition().below());
                    break;
                }
            }
        }
        return supportPos;
    }
}
