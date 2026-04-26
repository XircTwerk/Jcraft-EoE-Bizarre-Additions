package net.arna.jcraft.common.marker;

import com.mojang.datafixers.util.Pair;
import lombok.NonNull;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public interface MarkerType<I, T, M extends Marker<I, M>> {

    @NonNull M save(final @NonNull I id, final @NonNull T object);

    @NonNull Optional<Pair<I,T>> load(final @NonNull M marker, final @NonNull ServerLevel level);

}
