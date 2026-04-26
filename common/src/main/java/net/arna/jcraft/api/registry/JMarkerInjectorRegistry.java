package net.arna.jcraft.api.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.JRegistries;
import net.arna.jcraft.common.marker.Injectors;
import net.arna.jcraft.common.util.TriConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Tool to register {@link net.arna.jcraft.common.marker.MarkerType} injectors.
 * @see Injectors
 */
public interface JMarkerInjectorRegistry {

    DeferredRegister<TriConsumer<ResourceLocation,Entity,CompoundTag>> INJECTOR_REGISTRY = DeferredRegister.create(JCraft.MOD_ID, JRegistries.INJECTOR_REGISTRY_KEY);

    RegistrySupplier<TriConsumer<ResourceLocation,Entity,CompoundTag>> ALL = register("all", Injectors.ALL);

    static RegistrySupplier<TriConsumer<ResourceLocation, Entity, CompoundTag>> register(final String name, final TriConsumer<ResourceLocation,Entity,CompoundTag> injector) {
        return INJECTOR_REGISTRY.register(name, () -> injector);
    }
}
