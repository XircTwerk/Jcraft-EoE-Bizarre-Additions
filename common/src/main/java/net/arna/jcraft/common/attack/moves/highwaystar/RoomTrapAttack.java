package net.arna.jcraft.common.attack.moves.highwaystar;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.enums.BlockableType;
import net.arna.jcraft.api.attack.moves.AbstractSimpleAttack;
import net.arna.jcraft.common.entity.stand.HighwayStarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RoomTrapAttack extends AbstractSimpleAttack<RoomTrapAttack, HighwayStarEntity> {
    private static final int TRAP_DURATION = 120; // 6 seconds
    private static final int ROOM_SIZE = 4; // 9x9x9 room (4 blocks in each direction from center)
    private static final Map<LivingEntity, List<BlockRestoreData>> TRAPPED_ENTITIES = new HashMap<>();
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);

    private static class BlockRestoreData {
        final BlockPos pos;
        final BlockState originalState;

        BlockRestoreData(BlockPos pos, BlockState state) {
            this.pos = pos;
            this.originalState = state;
        }
    }

    public RoomTrapAttack(int cooldown, int windup, int duration, float moveDistance, float damage, int stun,
                          float hitboxSize, float knockback, float offset) {
        super(cooldown, windup, duration, moveDistance, damage, stun, hitboxSize, knockback, offset);
        withBlockableType(BlockableType.NON_BLOCKABLE);
    }

    @Override
    public @NotNull MoveType<RoomTrapAttack> getMoveType() {
        return Type.INSTANCE;
    }

    @Override
    public @NonNull Set<LivingEntity> perform(HighwayStarEntity attacker, LivingEntity user) {
        // Get targets using the proper hitbox centered on Highway Star
        Set<LivingEntity> targets = super.perform(attacker, user);

        // Only trap the first target
        if (!targets.isEmpty()) {
            LivingEntity target = targets.stream().findFirst().get();

            // Remove any existing room for this target
            if (TRAPPED_ENTITIES.containsKey(target)) {
                removeRoom(target);
            }

            // Create room trap at target's position
            List<BlockRestoreData> roomBlocks = createRoom(target);
            if (!roomBlocks.isEmpty()) {
                TRAPPED_ENTITIES.put(target, roomBlocks);

                // Apply blindness effect
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, TRAP_DURATION, 0));

                // Schedule room removal
                scheduleRoomRemoval(target, TRAP_DURATION);
            }

            // Return only the single target
            return Set.of(target);
        }

        return Set.of();
    }

    private List<BlockRestoreData> createRoom(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return new ArrayList<>();
        }

        BlockPos center = target.blockPosition();
        List<BlockRestoreData> roomBlocks = new ArrayList<>();

        // Create a 9x9x9 room around the target
        for (int x = -ROOM_SIZE; x <= ROOM_SIZE; x++) {
            for (int y = -2; y <= 6; y++) { // Floor at -2, ceiling at 6 (8 blocks high)
                for (int z = -ROOM_SIZE; z <= ROOM_SIZE; z++) {
                    BlockPos pos = center.offset(x, y, z);

                    // Create walls, floor, and ceiling - but leave interior empty
                    boolean isWall = (x == -ROOM_SIZE || x == ROOM_SIZE || z == -ROOM_SIZE || z == ROOM_SIZE);
                    boolean isFloorOrCeiling = (y == -2 || y == 6);

                    if (isWall || isFloorOrCeiling) {
                        // Store original block state
                        BlockState originalState = serverLevel.getBlockState(pos);
                        roomBlocks.add(new BlockRestoreData(pos, originalState));

                        // Place stone brick
                        serverLevel.setBlock(pos, Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    }
                }
            }
        }

        // Teleport target to center of room to prevent them from being stuck in walls
        target.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);

        return roomBlocks;
    }

    private void scheduleRoomRemoval(LivingEntity target, int delayTicks) {
        SCHEDULER.schedule(() -> {
            if (target.level() instanceof ServerLevel) {
                // Run on main thread
                target.level().getServer().execute(() -> removeRoom(target));
            }
        }, delayTicks * 50L, TimeUnit.MILLISECONDS);
    }

    private void removeRoom(LivingEntity target) {
        List<BlockRestoreData> roomBlocks = TRAPPED_ENTITIES.remove(target);
        if (roomBlocks != null && target.level() instanceof ServerLevel serverLevel) {
            for (BlockRestoreData data : roomBlocks) {
                // Restore original blocks
                serverLevel.setBlock(data.pos, data.originalState, 3);
            }
        }
    }

    // Clean up method to be called when the world unloads
    public static void cleanupAllRooms() {
        for (LivingEntity target : new ArrayList<>(TRAPPED_ENTITIES.keySet())) {
            List<BlockRestoreData> roomBlocks = TRAPPED_ENTITIES.remove(target);
            if (roomBlocks != null && target.level() instanceof ServerLevel serverLevel) {
                for (BlockRestoreData data : roomBlocks) {
                    serverLevel.setBlock(data.pos, data.originalState, 3);
                }
            }
        }
        TRAPPED_ENTITIES.clear();
    }

    @Override
    protected @NonNull RoomTrapAttack getThis() {
        return this;
    }

    @Override
    public @NonNull RoomTrapAttack copy() {
        return copyExtras(new RoomTrapAttack(getCooldown(), getWindup(), getDuration(), getMoveDistance(),
                getDamage(), getStun(), getHitboxSize(), getKnockback(), getOffset()));
    }

    public static class Type extends AbstractSimpleAttack.Type<RoomTrapAttack> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NotNull App<RecordCodecBuilder.Mu<RoomTrapAttack>, RoomTrapAttack> buildCodec(RecordCodecBuilder.Instance<RoomTrapAttack> instance) {
            return attackDefault(instance, RoomTrapAttack::new);
        }
    }
}