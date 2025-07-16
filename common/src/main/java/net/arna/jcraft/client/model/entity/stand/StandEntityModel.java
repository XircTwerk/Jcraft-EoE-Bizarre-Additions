package net.arna.jcraft.client.model.entity.stand;

import mod.azure.azurelib.core.animation.AnimationState;
import mod.azure.azurelib.model.GeoModel;
import net.arna.jcraft.api.stand.StandType;
import net.arna.jcraft.client.util.JClientUtils;
import net.arna.jcraft.api.stand.StandEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.IntStream;

/**
 * The base {@link GeoModel} for stands of any {@link StandType}.
 * @param <E> the entity to model
 * @see net.arna.jcraft.client.renderer.entity.stands.StandEntityRenderer StandEntityRenderer
 */
public class StandEntityModel<E extends StandEntity<?, ?>> extends GeoModel<E> {
    private final StandType type;
    private final ResourceLocation model;
    private final List<ResourceLocation> skins;
    private final ResourceLocation animation;
    private final float torsoPitchOffset, headPitchOffset, velInfluence;
    // public float prevTorsoPitch, prevHeadPitch, prevBasePitch = 0.0f;

    public StandEntityModel(final StandType type) {
        this(type, 0f, 0f);
    }

    public StandEntityModel(final StandType type, final float torsoPitchOffset, final float headPitchOffset) {
        this(type, torsoPitchOffset, headPitchOffset, 90f);
    }

    public StandEntityModel(final StandType type, final float torsoPitchOffset, final float headPitchOffset, final float velInfluence) {
        this.type = type;
        model = type.getId().withPath(path -> "geo/" + path + ".geo.json");
        skins = IntStream.rangeClosed(0, 3)
                .mapToObj(i -> type.getId().withPath(path -> String.format("textures/entity/stands/%s/%s.png",
                        path, i == 0 ? "default" : "skin" + i)))
                .toList();
        animation = type.getId().withPath(path -> "animations/" + path + ".animation.json");

        this.torsoPitchOffset = torsoPitchOffset;
        this.headPitchOffset = headPitchOffset;
        this.velInfluence = velInfluence;
    }

    @Override
    public ResourceLocation getModelResource(final E entity) {
        return model;
    }

    @Override
    public ResourceLocation getTextureResource(final E entity) {
        return skins.get(Mth.clamp(entity.getSkin(), 0, type.getData().getInfo().getSkinCount()));
    }

    @Override
    public ResourceLocation getAnimationResource(final E entity) {
        // Get all animation resources this stand can use
        List<ResourceLocation> allAnimations = getAllAnimationResources(entity);

        // Return the primary animation resource (first in the list)
        return allAnimations.isEmpty() ? animation : allAnimations.get(0);
    }

    /**
     * Gets all animation resources that this stand can use.
     * Override this method in subclasses to provide multiple animation JSONs.
     * The first animation in the list will be used as the primary animation resource.
     *
     * @param entity the entity to get animation resources for
     * @return list of all animation resources this stand can use
     */
    protected List<ResourceLocation> getAllAnimationResources(final E entity) {
        // Check if subclass wants to completely control animation order
        List<ResourceLocation> customOrder = getCustomAnimationOrder(entity);
        if (customOrder != null) {
            return customOrder;
        }

        // Default behavior: default animation first, then additional ones
        List<ResourceLocation> animations = new ArrayList<>();
        animations.add(animation); // Always include the default animation

        // Add any additional animations from subclasses
        List<ResourceLocation> additionalAnimations = getAdditionalAnimationResources(entity);
        if (additionalAnimations != null) {
            animations.addAll(additionalAnimations);
        }

        return animations;
    }

    /**
     * Override this method in subclasses to completely control the animation loading order.
     * If this returns non-null, it will be used instead of the default + additional pattern.
     *
     * @param entity the entity to get custom animation order for
     * @return list of animation resources in desired order, or null to use default behavior
     */
    protected List<ResourceLocation> getCustomAnimationOrder(final E entity) {
        return null;
    }

    /**
     * Override this method in subclasses to provide additional animation resources.
     * This allows stands to use multiple animation JSONs from other stands.
     *
     * @param entity the entity to get additional animation resources for
     * @return list of additional animation resources, or null if none
     */
    protected List<ResourceLocation> getAdditionalAnimationResources(final E entity) {
        return null;
    }

    /**
     * Helper method to create an animation resource for any stand by name.
     *
     * @param standName the name of the stand (e.g., "white_snake", "star_platinum")
     * @return the animation resource for that stand
     */
    protected ResourceLocation createAnimationResource(final String standName) {
        return new ResourceLocation(type.getId().getNamespace(), "animations/" + standName + ".animation.json");
    }

    @Override
    public void setCustomAnimations(final E animatable, final long instanceId, final AnimationState<E> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        if (skipCustomAnimations() || !animatable.hasUser()) {
            return;
        }

        // Note: KingCrimsonEntity overrides this due to some inversions, we should probably change the model later and standardize him.
        JClientUtils.animateGenericHumanoid(this, animatable, animatable.getUser(), animationState.getPartialTick(),
                true, true, torsoPitchOffset, headPitchOffset, velInfluence);
    }

    protected boolean skipCustomAnimations() {
        return false;
    }
}