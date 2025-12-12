package net.arna.jcraft.common.item;

import dev.architectury.registry.registries.RegistrySupplier;
import lombok.NonNull;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CosplayItem<T extends ArmorItem> {

    public static final Map<ArmorMaterial, String> SUFFIXES = new LinkedHashMap<>();

    public static final Map<ArmorMaterial, Boolean> ALLOWS_VAMPIRE_PROTECTION = new HashMap<>();

    static {
        SUFFIXES.put(ArmorMaterials.LEATHER, "_leather");
        SUFFIXES.put(ArmorMaterials.GOLD, "_gold");
        SUFFIXES.put(ArmorMaterials.CHAIN, "_chainmail");
        SUFFIXES.put(ArmorMaterials.IRON, "_iron");
        SUFFIXES.put(ArmorMaterials.DIAMOND, "_diamond");
        SUFFIXES.put(ArmorMaterials.TURTLE, "_turtle");
        SUFFIXES.put(ArmorMaterials.NETHERITE, "");
        ALLOWS_VAMPIRE_PROTECTION.put(ArmorMaterials.LEATHER, true);
        ALLOWS_VAMPIRE_PROTECTION.put(ArmorMaterials.GOLD, true);
        ALLOWS_VAMPIRE_PROTECTION.put(ArmorMaterials.CHAIN, true);
        ALLOWS_VAMPIRE_PROTECTION.put(ArmorMaterials.IRON, true);
        ALLOWS_VAMPIRE_PROTECTION.put(ArmorMaterials.DIAMOND, false);
        ALLOWS_VAMPIRE_PROTECTION.put(ArmorMaterials.TURTLE, false);
        ALLOWS_VAMPIRE_PROTECTION.put(ArmorMaterials.NETHERITE, false);
    }

    protected final String modId;
    protected final String name;
    protected final ArmorItem.Type slot;
    protected final boolean vampireProtection;
    protected final CosplayItemConstructor<T> ctor;

    protected final Map<ArmorMaterial, RegistrySupplier<T>> items = new HashMap<>();

    public CosplayItem(final @NonNull String modId, final @NonNull String name, final @NonNull ArmorItem.Type slot, final boolean vampireProtection, final @NonNull CosplayItemConstructor<T> ctor) {
        this.modId = modId;
        this.name = name;
        this.slot = slot;
        this.vampireProtection = vampireProtection;
        this.ctor = ctor;
    }

    public CosplayItem(final @NonNull String modId, final @NonNull String name, final @NonNull ArmorItem.Type slot, final @NonNull CosplayItemConstructor<T> ctor) {
        this(modId, name, slot, false, ctor);
    }

    public CosplayItem<T> register(final @NonNull CosplayItemRegistrator<T> registrator) {
        for (final var entry : SUFFIXES.entrySet()) {
            // don't register e.g. netherite Red Hat
            if (vampireProtection && !ALLOWS_VAMPIRE_PROTECTION.get(entry.getKey())) {
                continue;
            }
            // only register turtle for helmets
            if (entry.getKey() == ArmorMaterials.TURTLE && slot != ArmorItem.Type.HELMET) {
                continue;
            }
            final Item.Properties properties;
            if (entry.getKey() == ArmorMaterials.NETHERITE) {
                properties = new Item.Properties().fireResistant();
            }
            else {
                properties = new Item.Properties();
            }
            items.put(entry.getKey(), registrator.register(name + entry.getValue(), () -> ctor.create(entry.getKey(), slot, properties)));
        }
        return this;
    }

    @FunctionalInterface
    public interface CosplayItemConstructor<S extends ArmorItem> {
        S create(final @NonNull ArmorMaterial materialIn, final @NonNull ArmorItem.Type slot, final @NonNull Item.Properties builder);
    }

    @FunctionalInterface
    public interface CosplayItemRegistrator<S extends ArmorItem> {
        RegistrySupplier<S> register(String id, Supplier<? extends S> supplier);
    }
}
