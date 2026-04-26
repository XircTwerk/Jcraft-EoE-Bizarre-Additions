package net.arna.jcraft.datagen.providers.data;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.function.BiConsumer;

public abstract class JAttackerDataProvider<D> extends FabricCodecDataProvider<D> {
    private final Map<ResourceLocation, D> dataMap;

    public JAttackerDataProvider(final FabricDataOutput dataOutput, PackOutput.Target outputType,
                                 final String directoryName, final Codec<D> codec, final Map<ResourceLocation, D> dataMap) {
        super(dataOutput, outputType, directoryName, codec);
        this.dataMap = dataMap;
    }

    @Override
    protected final void configure(final BiConsumer<ResourceLocation, D> provider) {
        dataMap.forEach(provider);
    }
}
