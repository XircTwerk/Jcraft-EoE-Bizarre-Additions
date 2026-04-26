package net.arna.jcraft.common.marker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public record BlockMarker(BlockPos pos, BlockState state) implements Marker<BlockPos, BlockMarker> {

    public static final Codec<BlockMarker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(BlockMarker::pos),
                    BlockState.CODEC.fieldOf("state").forGetter(BlockMarker::state))
            .apply(instance, BlockMarker::new));

    @NonNull
    @Override
    public BlockPos getId() {
        return pos;
    }

    @NotNull
    @Override
    public Codec<BlockMarker> getCodec() {
        return CODEC;
    }
}
