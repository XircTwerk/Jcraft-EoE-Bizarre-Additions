package net.arna.jcraft.datagen.providers.data;

import lombok.Getter;
import net.arna.jcraft.api.JRegistries;
import net.arna.jcraft.api.stand.StandData;
import net.arna.jcraft.api.stand.StandInfo;
import net.arna.jcraft.datagen.Util;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
public class JStandDataProvider extends JAttackerDataProvider<StandData> {
    private final String name = "Stand Data";

    public JStandDataProvider(FabricDataOutput dataOutput) {
        super(dataOutput, PackOutput.Target.DATA_PACK, "stands", StandData.CODEC,
                processDataMap(Util.getStandDataMap()));
    }

    private static Map<ResourceLocation, StandData> processDataMap(Map<ResourceLocation, StandData> dataMap) {
        return dataMap.entrySet().stream()
                .map(JStandDataProvider::processEntry)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map.Entry<ResourceLocation, StandData> processEntry(Map.Entry<ResourceLocation, StandData> entry) {
        ResourceLocation key = entry.getKey();
        StandData data = entry.getValue();
        StandInfo info = data.getInfo();

        String[] parts = key.getPath().split("_");
        String variant = JRegistries.STAND_TYPE_REGISTRY.contains(key) ? null : parts[parts.length - 1];
        String baseKey = info.getNameKey() + (variant == null ? "" : "." + variant) + ".info";

        data = data.withInfo(info.toBuilder()
                .freeSpace(Component.translatable(baseKey + ".freeSpace"))
                .clearSkinNames()
                .skinNames(IntStream.range(0, info.getSkinCount() - 1)
                        .mapToObj(i -> Component.translatable(baseKey + ".skin" + (i + 1)))
                        .collect(Collectors.toList()))
                .build());

        return Map.entry(key, data);
    }
}
