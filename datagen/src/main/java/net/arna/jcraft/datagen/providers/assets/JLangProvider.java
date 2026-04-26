package net.arna.jcraft.datagen.providers.assets;

import com.google.gson.JsonObject;
import net.arna.jcraft.api.JRegistries;
import net.arna.jcraft.api.attack.MoveMap;
import net.arna.jcraft.api.attack.MoveSetManager;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.spec.SpecData;
import net.arna.jcraft.api.stand.StandData;
import net.arna.jcraft.api.stand.StandInfo;
import net.arna.jcraft.datagen.Util;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class JLangProvider implements DataProvider {
    public JLangProvider(FabricDataOutput ignoredDataOutput) {}

    @Override
    public @NotNull String getName() {
        return "Moveset and Data Language";
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput output) {
        Map<String, String> newLang = new LinkedHashMap<>(Util.lang);
        BiConsumer<String, String> builder = (key, value) -> {
            if (value != null && !value.isBlank() && newLang.put(key, value) != null) {
                throw new IllegalArgumentException("Duplicate or invalid key found in language file: " + key);
            }
        };

        generateTranslations(builder);

        // Write output
        JsonObject obj = new JsonObject();
        newLang.forEach(obj::addProperty);

        return DataProvider.saveStable(output, obj, Util.langFile);
    }

    private void generateTranslations(BiConsumer<String, String> builder) {
        generateMoveSetTranslations(builder);
        generateStandDataTranslations(builder);
        generateSpecDataTranslations(builder);
    }

    private void generateMoveSetTranslations(BiConsumer<String, String> builder) {
        Set<ResourceLocation> ids = new HashSet<>(JRegistries.STAND_TYPE_REGISTRY.getIds());
        ids.addAll(JRegistries.SPEC_TYPE_REGISTRY.getIds());

        Set<String> keys = new HashSet<>();

        for (ResourceLocation type : ids) {
            MoveSetManager.get(type).forEach((id, moveSet) -> {
                String msBaseKey = String.format("move.%s.%s", type.toLanguageKey(), moveSet.getName());
                MoveMap<?, ?> moveMap = moveSet.save();
                for (AbstractMove<?, ?> move : moveMap.asMovesList()) {
                    String key = Util.generateKey(move.getName());
                    if (key == null) {
                        continue; // Skip if the key cannot be generated, likely already translated.
                    }

                    String baseKey = String.join(".", msBaseKey, key);
                    if (move.isFollowup()) baseKey += ".followup";
                    else if (move.isCrouchingVariant()) baseKey += ".cr";
                    else if (move.isAerialVariant()) baseKey += ".air";

                    if (!keys.add(baseKey)) throw new IllegalArgumentException(String.format(
                            "Move %s from moveset %s of type %s resolves to a duplicate key: %s",
                            toString(move.getName()), moveSet.getName(), type, baseKey));

                    addIfLiteral(move.getName(), baseKey + ".name", builder);
                    addIfLiteral(move.getDescription(), baseKey + ".description", builder);
                }
            });
        }
    }

    private void generateStandDataTranslations(BiConsumer<String, String> builder) {
        for (Map.Entry<ResourceLocation, StandData> entry : Util.getStandDataMap().entrySet()) {
            String[] parts = entry.getKey().getPath().split("_");
            String variant = JRegistries.STAND_TYPE_REGISTRY.contains(entry.getKey()) ? null : parts[parts.length - 1];

            StandData data = entry.getValue();
            StandInfo info = data.getInfo();
            String baseKey = info.getNameKey() + (variant == null ? "" : "." + variant) + ".info";

            addIfLiteral(info.getFreeSpace(), baseKey + ".freeSpace", builder);
            for (int i = 0; i < info.getSkinNames().size(); i++) {
                Component skin = info.getSkinNames().get(i);
                addIfLiteral(skin, baseKey + ".skin" + (i + 1), builder);
            }
        }
    }

    private void generateSpecDataTranslations(BiConsumer<String, String> builder) {
        for (SpecData data : Util.getSpecDataMap().values()) {
            String baseKey = data.getNameKey() + ".info";

            addIfLiteral(data.getDescription(), baseKey + ".desc", builder);
            addIfLiteral(data.getDetails(), baseKey + ".details", builder);
        }
    }

    private static void addIfLiteral(Component component, String key, BiConsumer<String, String> builder) {
        if (component.getContents() instanceof LiteralContents literal) {
            builder.accept(key, literal.text());
        }
    }

    private String toString(Component comp) {
        ComponentContents contents = comp.getContents();
        if (contents instanceof LiteralContents literal) {
            return literal.text();
        } else if (contents instanceof TranslatableContents translatable) {
            return Util.translate(translatable);
        }

        return null; // Return null for unsupported contents
    }
}
