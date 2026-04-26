package net.arna.jcraft.datagen.providers.assets;

import lombok.Getter;
import net.arna.jcraft.api.datagen.JCraftPoseProvider;
import net.arna.jcraft.api.pose.ModelType;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.common.entity.stand.*;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;

@Getter
public class JPoseProvider extends JCraftPoseProvider {
    private final String name = "Poses";

    public JPoseProvider(FabricDataOutput dataOutput) {
        super(dataOutput);
    }

    @Override
    protected void registerPoses(PoseRegistrar registrar) {
        registrar.register(JStandTypeRegistry.GOLD_EXPERIENCE_REQUIEM.getId(), ModelType.HUMANOID, GEREntity.POSE.get());
        registrar.register(JStandTypeRegistry.KING_CRIMSON.getId(), ModelType.HUMANOID, KingCrimsonEntity.POSE.get());
        registrar.register(JStandTypeRegistry.KILLER_QUEEN_BITES_THE_DUST.getId(), ModelType.HUMANOID, KQBTDEntity.POSE.get());
        registrar.register(JStandTypeRegistry.KILLER_QUEEN.getId(), ModelType.HUMANOID, KillerQueenEntity.POSE.get());
        registrar.register(JStandTypeRegistry.STAR_PLATINUM.getId(), ModelType.HUMANOID, StarPlatinumEntity.POSE.get());
        registrar.register(JStandTypeRegistry.THE_WORLD.getId(), ModelType.HUMANOID, TheWorldEntity.POSE.get());
        registrar.register(JStandTypeRegistry.THE_WORLD_OVER_HEAVEN.getId(), ModelType.HUMANOID, TheWorldOverHeavenEntity.POSE.get());
    }
}
