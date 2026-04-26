package net.arna.jcraft.common.marker;

import com.mojang.serialization.Codec;
import lombok.NonNull;

public interface Marker<I, M extends Marker<I, ?>> {

    @NonNull I getId();

    @NonNull Codec<M> getCodec();

}
