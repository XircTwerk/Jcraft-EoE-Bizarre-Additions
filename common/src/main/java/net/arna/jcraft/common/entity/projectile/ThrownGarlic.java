package net.arna.jcraft.common.entity.projectile;

import net.arna.jcraft.api.registry.JEntityTypeRegistry;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.api.registry.JSpecTypeRegistry;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrownGarlic extends ThrowableItemProjectile {

    public ThrownGarlic(EntityType<? extends ThrownGarlic> entityType, Level level) {
        super(entityType, level);
    }

    public ThrownGarlic(Level level, LivingEntity shooter) {
        super(JEntityTypeRegistry.THROWN_GARLIC.get(), shooter, level);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
    }

    public ThrownGarlic(Level level) {
        super(JEntityTypeRegistry.THROWN_GARLIC.get(), level);
    }

    @Override
    protected Item getDefaultItem() {
        return JItemRegistry.GARLIC.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();

        if (entity instanceof LivingEntity livingEntity) {
            // Check if the entity has vampire spec
            if (hasVampireSpec(livingEntity)) {
                // Deal 4 damage (2 hearts) to vampires
                entity.hurt(this.damageSources().thrown(this, this.getOwner()), 4.0F);

                // Play hurt sound
                livingEntity.playSound(SoundEvents.PLAYER_HURT_ON_FIRE, 1.0F, 1.0F);

                // Add some smoke particles for effect
                for (int i = 0; i < 8; i++) {
                    this.level().addParticle(ParticleTypes.SMOKE,
                            entity.getX() + this.random.nextGaussian() * 0.3,
                            entity.getY() + entity.getBbHeight() * 0.5,
                            entity.getZ() + this.random.nextGaussian() * 0.3,
                            0.0D, 0.1D, 0.0D);
                }
            } else if (entity instanceof Player player) {
                FoodData foodData = player.getFoodData();
                foodData.eat(2, 0.3F); // Restore 1 hunger bar


                // Play eating sound
                player.playSound(SoundEvents.PLAYER_BURP, 0.5F, 1.0F);
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        // Create item break particles
        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, (byte)3);
            this.discard();
        }

        // Play impact sound
        this.playSound(SoundEvents.GRASS_BREAK, 0.8F, 1.0F);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            // Create break particles using the garlic item texture
            ParticleOptions particleOptions = new ItemParticleOption(ParticleTypes.ITEM, this.getItem());

            for (int i = 0; i < 8; ++i) {
                this.level().addParticle(particleOptions,
                        this.getX(), this.getY(), this.getZ(),
                        this.random.nextGaussian() * 0.15D,
                        this.random.nextDouble() * 0.2D,
                        this.random.nextGaussian() * 0.15D);
            }
        }
    }

    private boolean hasVampireSpec(LivingEntity entity) {
        // Check if entity has spec component and if it's vampire type
        try {
            var specData = JComponentPlatformUtils.getSpecData(entity);
            if (specData != null && specData.getSpec() != null) {
                return specData.getSpec().getType().equals(JSpecTypeRegistry.VAMPIRE.get());
            }
        } catch (Exception e) {
            // Handle case where entity doesn't have spec component
        }
        return false;
    }
    @Override
    protected float getGravity() {
        return 0.03F;
    }
}