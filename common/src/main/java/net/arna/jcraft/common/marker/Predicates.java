package net.arna.jcraft.common.marker;

import lombok.NonNull;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Collection;
import java.util.function.BiPredicate;

public interface Predicates {

    static BiPredicate<ResourceLocation, Entity> fromSet(final @NonNull Collection<ResourceLocation> ids) {
        return (id, entity) -> ids.contains(id);
    }

    MarkerLoadPredicate<EntityMarker> DEFAULT_LOAD = (entityMarker, serverLevel) -> {
        final Entity entity = serverLevel.getEntity(entityMarker.id());
        return entity != null && entity.isAlive() && (!(entity instanceof ServerPlayer player) || (!(player.isSpectator() || player.isCreative())));
    };

}
