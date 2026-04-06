package net.arna.jcraft.common.events;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.architectury.event.EventResult;
import lombok.NonNull;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface JBlockEvents<T> {
    /**
     * Invoked directly before a block is being set, unless the block is not being changed or it happens during chunk generation.
     */
    Event<BeforeSet> BEFORE_SET = EventFactory.createEventResult();

    void add(T instance);

    interface BeforeSet {
        EventResult setBlock(final @NonNull BlockPos blockPos, final @NonNull BlockState oldBlockState, final @NonNull BlockState newBlockState, final @NonNull Level level);
    }
}
