package net.arna.jcraft.client.model.entity.stand;

import net.arna.jcraft.common.entity.stand.AbstractStarPlatinumEntity;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * The {@link StandEntityModel} for {@link AbstractStarPlatinumEntity}.
 * This uses base model animations for standby mode.
 */
public class StarPlatinumModel extends StandEntityModel<AbstractStarPlatinumEntity<?, ?>> {

    public StarPlatinumModel(boolean someCondition) {
        super(JStandTypeRegistry.STAR_PLATINUM.get(), 0f, 0f);
    }

    @Override
    public ResourceLocation getAnimationResource(final AbstractStarPlatinumEntity<?, ?> entity) {
        // Check if the entity is in standby mode and should use base model animations
        if (shouldUseBaseModelAnimations(entity)) {
            return createAnimationResource("base_model");
        } else {
            return createAnimationResource("star_platinum");
        }
    }

    /**
     * Determines if the current entity state should use base model animations
     */
    private boolean shouldUseBaseModelAnimations(AbstractStarPlatinumEntity<?, ?> entity) {
        if (entity.getState() == null) return false;

        String stateName = entity.getState().toString();

        // Use base model animations for these specific states
        return stateName.equals("STANDBY_IDLE") ||
                stateName.equals("ITEM_TOSS_CHARGE") ||
                stateName.equals("ITEM_TOSS");
    }
}