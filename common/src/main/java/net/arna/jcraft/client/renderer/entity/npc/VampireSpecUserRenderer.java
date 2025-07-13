package net.arna.jcraft.client.renderer.entity.npc;

import mod.azure.azurelib.renderer.GeoEntityRenderer;
import net.arna.jcraft.client.model.entity.npc.VampireSpecUserModel;
import net.arna.jcraft.common.entity.spec.VampireSpecUser;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class VampireSpecUserRenderer extends GeoEntityRenderer<VampireSpecUser> {

    public VampireSpecUserRenderer(final EntityRendererProvider.Context renderManager) {
        super(renderManager, new VampireSpecUserModel());
    }
}