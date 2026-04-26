package net.arna.jcraft.common.marker;

import com.mojang.datafixers.util.Pair;
import lombok.NonNull;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record BlockMarkerType(MarkerSavePredicate<BlockPos, BlockState> savePredicate, MarkerLoadPredicate<BlockMarker> loadPredicate)
        implements MarkerSavePredicate<BlockPos, BlockState>, MarkerLoadPredicate<BlockMarker>, MarkerType<BlockPos, BlockState, BlockMarker> {

    @Override
    public boolean shouldSave(final @NonNull BlockPos id, final @NonNull BlockState object) {
        return savePredicate.shouldSave(id, object);
    }

    @Override
    public boolean shouldLoad(final @NonNull BlockMarker marker, final @NonNull ServerLevel level) {
        return loadPredicate.shouldLoad(marker, level);
    }

    @NotNull
    @Override
    public BlockMarker save(final @NonNull BlockPos id, final @NonNull BlockState object) {
        return new BlockMarker(id, object);
    }

    @NotNull
    @Override
    public Optional<Pair<BlockPos, BlockState>> load(final @NonNull BlockMarker marker, final @NonNull ServerLevel level) {
        level.setBlock(marker.pos(), marker.state(), Block.UPDATE_CLIENTS);
        return Optional.of(Pair.of(marker.pos(), marker.state()));
    }
}
