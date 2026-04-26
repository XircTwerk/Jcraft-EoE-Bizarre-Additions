package net.arna.jcraft.datagen.providers.data;

import lombok.Getter;
import net.arna.jcraft.api.spec.SpecData;
import net.arna.jcraft.datagen.Util;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class JSpecDataProvider extends JAttackerDataProvider<SpecData> {
    private final String name = "Spec Data";

    public JSpecDataProvider(FabricDataOutput dataOutput) {
        super(dataOutput, PackOutput.Target.DATA_PACK, "specs", SpecData.CODEC,
                processDataMap(Util.getSpecDataMap()));
    }

    private static Map<ResourceLocation, SpecData> processDataMap(Map<ResourceLocation, SpecData> dataMap) {
        return dataMap.entrySet().stream()
                .map(JSpecDataProvider::processEntry)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map.Entry<ResourceLocation, SpecData> processEntry(Map.Entry<ResourceLocation, SpecData> entry) {
        ResourceLocation key = entry.getKey();
        SpecData data = entry.getValue();

        String baseKey = data.getNameKey();

        data = data.withDescription(Component.translatable(baseKey + ".info.desc"))
                .withDetails(Component.translatable(baseKey + ".info.details"));

        return Map.entry(key, data);
    }
}
