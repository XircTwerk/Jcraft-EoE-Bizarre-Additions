package net.arna.jcraft.common.marker;

import lombok.NonNull;

@FunctionalInterface
public interface MarkerSavePredicate<I, T> {

    MarkerSavePredicate<?, ?> ALL = (id, object) -> true;

    MarkerSavePredicate<?, ?> NONE = (id, object) -> false;

    boolean shouldSave(final @NonNull I id, final @NonNull T object);

    @NonNull
    default MarkerSavePredicate<I,T> and(final @NonNull MarkerSavePredicate<I,T> other) {
        return (id, object) -> shouldSave(id, object) && other.shouldSave(id, object);
    }

    @NonNull
    default MarkerSavePredicate<I,T> or(final @NonNull MarkerSavePredicate<I,T> other) {
        return (id, object) -> shouldSave(id, object) || other.shouldSave(id, object);
    }

    @NonNull
    default MarkerSavePredicate<I,T> negateSave() {
        return (id, object) -> !shouldSave(id, object);
    }

    @SuppressWarnings("unchecked")
    static <J, U> MarkerSavePredicate<J, U> all() {
        return (MarkerSavePredicate<J, U>)ALL;
    }

    @SuppressWarnings("unchecked")
    static <J, U> MarkerSavePredicate<J, U> none() {
        return (MarkerSavePredicate<J, U>)NONE;
    }

}
