package net.arna.jcraft.api.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.JRegistries;
import net.arna.jcraft.common.marker.Extractors;
import net.arna.jcraft.common.util.TriConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Tool to register {@link net.arna.jcraft.common.marker.MarkerType} extractors.
 * @see Extractors
 */
public interface JMarkerExtractorRegistry {

    DeferredRegister<TriConsumer<ResourceLocation,Entity,CompoundTag>> EXTRACTOR_REGISTRY = DeferredRegister.create(JCraft.MOD_ID, JRegistries.EXTRACTOR_REGISTRY_KEY);

    RegistrySupplier<TriConsumer<ResourceLocation,Entity,CompoundTag>> ALL = register("all", Extractors.ALL);

    static RegistrySupplier<TriConsumer<ResourceLocation, Entity, CompoundTag>> register(final String name, final TriConsumer<ResourceLocation,Entity,CompoundTag> extractor) {
        return EXTRACTOR_REGISTRY.register(name, () -> extractor);
    }
}
