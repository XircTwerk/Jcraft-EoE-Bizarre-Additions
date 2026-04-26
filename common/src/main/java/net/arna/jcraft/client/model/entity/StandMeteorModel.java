package net.arna.jcraft.client.model.entity;

import lombok.NonNull;
import net.arna.jcraft.common.entity.StandMeteorEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The {@link HierarchicalModel} for {@link StandMeteorEntity}.
 * @see net.arna.jcraft.client.renderer.entity.StandMeteorRenderer StandMeteorRenderer
 */
public final class StandMeteorModel extends HierarchicalModel<StandMeteorEntity> {
    private final ModelPart root;

    public StandMeteorModel(final ModelPart root) {
        this.root = root;
    }

    @Override
    public @NonNull ModelPart root() {
        return root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();
        partDefinition.addOrReplaceChild("body", CubeListBuilder
                .create()
                .texOffs(0, 0)
                .addBox(0, 0, 0, 128.0F, 128.0F, 128.0F)
                , PartPose.offset(-64.0F, -64.0F, -64.0F)
        );
        return LayerDefinition.create(meshDefinition, 32, 32);
    }

    @Override
    public void setupAnim(@NonNull final StandMeteorEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // intentionally empty
    }
}
