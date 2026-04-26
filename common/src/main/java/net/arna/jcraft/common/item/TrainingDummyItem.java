package net.arna.jcraft.common.item;

import net.arna.jcraft.common.entity.TrainingDummyEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class TrainingDummyItem extends Item {

    public TrainingDummyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos blockPos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockState blockState = level.getBlockState(blockPos);

        // Get the position where the dummy should be placed
        BlockPos spawnPos;
        if (!blockState.getCollisionShape(level, blockPos).isEmpty()) {
            spawnPos = blockPos.relative(direction);
        } else {
            spawnPos = blockPos;
        }

        // Check if there's already a training dummy at this position
        AABB checkArea = new AABB(spawnPos).inflate(1.0);
        List<TrainingDummyEntity> existingDummies = level.getEntitiesOfClass(TrainingDummyEntity.class, checkArea);

        if (!existingDummies.isEmpty()) {
            // There's already a training dummy here, don't place another one
            return InteractionResult.FAIL;
        }

        // Create and spawn the training dummy
        TrainingDummyEntity trainingDummy = new TrainingDummyEntity(level,
                spawnPos.getX() + 0.5,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5);

        // Set the rotation to face the player
        if (context.getPlayer() != null) {
            trainingDummy.setYRot(context.getPlayer().getYRot() + 180.0f);
        }

        // Try to spawn the entity
        if (level.addFreshEntity(trainingDummy)) {
            // Play placement sound
            level.playSound(null, spawnPos, SoundEvents.ARMOR_STAND_PLACE,
                    SoundSource.BLOCKS, 0.75F, 0.8F);

            // Consume the item if not in creative mode
            if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                context.getItemInHand().shrink(1);
            }

            return InteractionResult.CONSUME;
        }

        return InteractionResult.FAIL;
    }
}