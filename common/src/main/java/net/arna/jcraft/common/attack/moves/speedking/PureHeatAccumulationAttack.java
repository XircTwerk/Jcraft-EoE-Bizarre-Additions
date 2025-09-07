package net.arna.jcraft.common.attack.moves.speedking;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.common.entity.stand.SpeedKingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PureHeatAccumulationAttack extends AbstractMove<PureHeatAccumulationAttack, SpeedKingEntity> {
    private static final Map<String, Long> HEATED_BLOCKS = new HashMap<>();
    private static final Map<String, Long> ACTIVE_GEYSER = new HashMap<>();

    public PureHeatAccumulationAttack(final int cooldown, final int windup, final int duration, final float moveDistance) {
        super(cooldown, windup, duration, moveDistance);
    }

    @Override
    public @NonNull MoveType<PureHeatAccumulationAttack> getMoveType() {
        return Type.INSTANCE;
    }

    @Override
    public @NonNull Set<LivingEntity> perform(final SpeedKingEntity attacker, final LivingEntity user) {
        Vec3 userPos = user.position();
        BlockPos centerPos = new BlockPos((int) userPos.x, (int) userPos.y, (int) userPos.z);
        int radius = 5;

        // Get all entities in 5 block radius and apply boiling effect
        List<LivingEntity> entitiesList = attacker.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(userPos.add(-radius, -2, -radius), userPos.add(radius, 2, radius)),
                entity -> entity != user && entity != attacker);

        for (LivingEntity entity : entitiesList) {
            entity.addEffect(new MobEffectInstance(JStatusRegistry.BOILING.get(), 200, 0, false, true));
        }

        // Heat all blocks in the radius to make them like magma blocks
        makeBlocksHot(attacker.level(), centerPos, radius);
        
        createGeyser(attacker.level(), centerPos);

        // Destroy plants and water
        destroyStuff(attacker.level(), centerPos, radius);

        // Convert List to Set for return
        return new HashSet<>(entitiesList);
    }

    private void makeBlocksHot(Level level, BlockPos center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        String key = level.dimension().location() + "_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
                        HEATED_BLOCKS.put(key, level.getGameTime() + 100);
                    }
                }
            }
        }
    }

    private void createGeyser(Level level, BlockPos center) {
        if (!(level instanceof ServerLevel)) return;

        // Create only ONE geyser entry at the center position
        String key = level.dimension().location() + "_" + center.getX() + "_" + center.getY() + "_" + center.getZ();
        ACTIVE_GEYSER.put(key, level.getGameTime() + 100);
    }

    private void destroyStuff(Level level, BlockPos center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    if (state.is(BlockTags.FLOWERS) || state.is(BlockTags.CROPS) ||
                            state.is(Blocks.GRASS) || state.is(Blocks.WATER)) {
                        level.destroyBlock(pos, false);
                    }
                }
            }
        }
    }

    public static void tickGeyser(ServerLevel level) {
        ACTIVE_GEYSER.entrySet().removeIf(entry -> {
            long expireTime = entry.getValue();
            long startTime = expireTime - 100;
            long currentTime = level.getGameTime();

            if (currentTime >= expireTime) {
                return true;
            }

            String[] parts = entry.getKey().split("_");
            if (parts.length >= 4) {
                try {
                    int centerX = Integer.parseInt(parts[1]);
                    int centerY = Integer.parseInt(parts[2]);
                    int centerZ = Integer.parseInt(parts[3]);

                    int ticksActive = (int)(currentTime - startTime);
                    double maxHeight = Math.min(6.0, ticksActive / 3.0); // 1 block higher every 3 ticks

                    // Generate particles only on the circle outline (perimeter)
                    int radius = 5; // Changed to 5-block radius
                    int numPoints = 20; // Reduced from 32 to 20 points for fewer particles

                    for (int i = 0; i < numPoints; i++) {
                        double angle = (2 * Math.PI * i) / numPoints;
                        double x = radius * Math.cos(angle);
                        double z = radius * Math.sin(angle);

                        // Spawn particles from ground up to max height at each outline point
                        for (double h = 0; h <= maxHeight; h += 0.5) { // Increased spacing to 0.5 for fewer particles
                            if (level.random.nextFloat() < 0.25f) { // Reduced chance to 25% for fewer particles
                                level.sendParticles(ParticleTypes.FLAME,
                                        centerX + x + 0.5, centerY + h, centerZ + z + 0.5,
                                        1, 0, 0, 0, 0.05);
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    protected @NonNull PureHeatAccumulationAttack getThis() {
        return this;
    }

    @Override
    public @NonNull PureHeatAccumulationAttack copy() {
        return copyExtras(new PureHeatAccumulationAttack(getCooldown(), getWindup(), getDuration(), getMoveDistance()));
    }

    public static class Type extends AbstractMove.Type<PureHeatAccumulationAttack> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<PureHeatAccumulationAttack>, PureHeatAccumulationAttack> buildCodec(RecordCodecBuilder.Instance<PureHeatAccumulationAttack> instance) {
            return baseDefault(instance, PureHeatAccumulationAttack::new);
        }
    }
}