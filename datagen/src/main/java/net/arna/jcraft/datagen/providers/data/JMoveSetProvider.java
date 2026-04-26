package net.arna.jcraft.datagen.providers.data;

import com.mojang.serialization.Codec;
import lombok.Getter;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.JRegistries;
import net.arna.jcraft.api.attack.MoveSet;
import net.arna.jcraft.api.attack.MoveSetManager;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.spec.SpecType;
import net.arna.jcraft.api.stand.StandType;
import net.arna.jcraft.api.attack.IAttacker;
import net.arna.jcraft.api.attack.MoveMap;
import net.arna.jcraft.datagen.Util;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class JMoveSetProvider<A extends IAttacker<A, S>, S extends Enum<S>>
        extends FabricCodecDataProvider<MoveMap.Entry<A, S>> {
    @Getter // implements abstract method
    private final String name;
    private final ResourceLocation type;

    public JMoveSetProvider(FabricDataOutput dataOutput, ResourceLocation type) {
        super(dataOutput, PackOutput.Target.DATA_PACK, String.format("movesets/%s/%s", getKind(type), type.getPath()), getCodec(type));
        // Turn the type name into camel case
        name = Arrays.stream(type.getPath().split("_"))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                .collect(Collectors.joining(" ")) + " Moveset(s)";
        this.type = type;
    }

    private static <A extends IAttacker<A, S>, S extends Enum<S>> Codec<MoveMap.Entry<A, S>> getCodec(ResourceLocation type) {
        ensureClassLoaded(type);
        return MoveMap.Entry.codecFor(Optional.ofNullable(MoveSetManager.<A, S>get(type, "default"))
                .orElseThrow(() -> new IllegalArgumentException("No default moveset found for " + type))
                .getStateClass());
    }

    private static void ensureClassLoaded(ResourceLocation type) {
        // Ensure the corresponding class is loaded so the move sets have been created.
        StandType standType = JRegistries.STAND_TYPE_REGISTRY.get(type);
        if (standType != null) {
            Util.getEntityClass(standType.getEntityType());
        } else {
            SpecType specType = JRegistries.SPEC_TYPE_REGISTRY.get(type);
            if (specType != null) {
                Util.getSpecClass(specType);
            }
        }
    }

    @Override
    protected void configure(BiConsumer<ResourceLocation, MoveMap.Entry<A, S>> provider) {
        // Generate a JSON file for each entry in each move set.
        Map<String, MoveSet<A, S>> moveSets = MoveSetManager.get(type);
        moveSets.forEach((name, moveSet) ->
                moveSet.save().getEntries().entries().forEach(e ->
                        provider.accept(JCraft.id(String.format("%s/%s/%s", name, e.getKey().getName(), getMoveName(e.getValue()))),
                                processMove(name, e.getValue()))));
    }

    private MoveMap.Entry<A, S> processMove(String moveSetName, MoveMap.Entry<A, S> entry) {
        AbstractMove<?, ? super A> move = entry.getMove();
        Component name = move.getName();

        String key = "move." + String.join(".", type.toLanguageKey(), moveSetName, Util.generateKey(name));
        if (move.isFollowup()) key += ".followup";
        else if (move.isCrouchingVariant()) key += ".cr";
        else if (move.isAerialVariant()) key += ".air";

        move.withInfo(name.getContents() instanceof TranslatableContents ? name :
                Component.translatable(key + ".name"),
                move.getDescription().getContents() instanceof TranslatableContents ? move.getDescription() :
                Component.translatable(key + ".description"));

        if (entry.getFollowup() != null)
            processMove(moveSetName, entry.getFollowup());
        if (entry.getCrouchingVariant() != null)
            processMove(moveSetName, entry.getCrouchingVariant());
        if (entry.getAerialVariant() != null)
            processMove(moveSetName, entry.getAerialVariant());

        return entry;
    }

    private static String getMoveName(MoveMap.Entry<?, ?> entry) {
        String name = entry.getMove().getName().getString();
        return name.toLowerCase(Locale.ROOT)
                .replace(" ", "_")
                .replaceAll("[^a-z0-9/._-]", "");
    }

    private static String getKind(ResourceLocation type) {
        return JRegistries.SPEC_TYPE_REGISTRY.contains(type) ? "spec" : "stand";
    }
}
