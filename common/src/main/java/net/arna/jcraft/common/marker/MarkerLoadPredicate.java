package net.arna.jcraft.common.marker;

import lombok.NonNull;
import net.minecraft.server.level.ServerLevel;

@FunctionalInterface
public interface MarkerLoadPredicate<M extends Marker<?,?>> {

    boolean shouldLoad(final @NonNull M marker, final @NonNull ServerLevel level);

    @NonNull
    default MarkerLoadPredicate<M> and(final @NonNull MarkerLoadPredicate<M> other) {
        return (marker, level) -> shouldLoad(marker, level) && other.shouldLoad(marker, level);
    }

    @NonNull
    default MarkerLoadPredicate<M> or(final @NonNull MarkerLoadPredicate<M> other) {
        return (marker, level) -> shouldLoad(marker, level) || other.shouldLoad(marker, level);
    }

    @NonNull
    default MarkerLoadPredicate<M> negateLoad() {
        return (marker, level) -> !shouldLoad(marker, level);
    }

}
