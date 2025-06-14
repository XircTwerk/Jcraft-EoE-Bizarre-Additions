package net.arna.jcraft.client.model.entity.stand;

import mod.azure.azurelib.model.GeoModel;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.common.entity.stand.HighwayStarEntity;
import net.minecraft.resources.ResourceLocation;

/**
 * The {@link GeoModel} for {@link HighwayStarEntity}.
 * @see HighwayStarEntity
 */
public class HighwayStarModel extends GeoModel<HighwayStarEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(JCraft.MOD_ID, "geo/highway_star.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(JCraft.MOD_ID, "textures/entity/stands/highway_star/default.png");
    private static final ResourceLocation ANIMATIONS = new ResourceLocation(JCraft.MOD_ID, "animations/highway_star.animation.json");

    @Override
    public ResourceLocation getModelResource(HighwayStarEntity object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(HighwayStarEntity object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(HighwayStarEntity animatable) {
        return ANIMATIONS;
    }
}