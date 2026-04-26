package net.arna.jcraft.common.marker;

import lombok.NonNull;
import net.arna.jcraft.common.util.TriConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.function.BiPredicate;

/**
 * A record for checking, extracting and setting values based on identifiers
 * @param predicate a predicate to check if the value of the identifier should be saved
 * @param extractor a function to get the value of the identifier from the entity
 * @param injector a consumer to save the value of the identifier in the entity
 */
public record EntityDataHandler(@NonNull BiPredicate<ResourceLocation, Entity> predicate, @NonNull TriConsumer<ResourceLocation, Entity, CompoundTag> extractor, @NonNull TriConsumer<ResourceLocation, Entity, CompoundTag> injector) {
    // nothing to add
}
