package net.arna.jcraft.forge.loot;

import com.mojang.serialization.Codec;
import net.arna.jcraft.JCraft;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class JForgeLootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, JCraft.MOD_ID);

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> FORGE_LOOT_ADAPTER =
            LOOT_MODIFIER_SERIALIZERS.register("loot_adapter", JForgeLootAdapter.CODEC);

    public static void register(final IEventBus eventBus) {
        LOOT_MODIFIER_SERIALIZERS.register(eventBus);
    }
}
