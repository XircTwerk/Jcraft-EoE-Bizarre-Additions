package net.arna.jcraft.client.model.entity.stand;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.common.entity.stand.AbstractStarPlatinumEntity;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.common.entity.stand.TCBEntity;
import net.arna.jcraft.common.entity.stand.TheHandEntity;
import net.minecraft.resources.ResourceLocation;

/**
 * The {@link StandEntityModel} for {@link net.arna.jcraft.common.entity.stand.StarPlatinumEntity StarPlatinumEntity}
 * and {@link net.arna.jcraft.common.entity.stand.SPTWEntity SPTWEntity}.
 * @see net.arna.jcraft.client.renderer.entity.stands.StarPlatinumRenderer StarPlatinumRenderer
 * @see net.arna.jcraft.client.renderer.entity.stands.SPTWRenderer SPTWRenderer
 */
public class TCBModel extends StandEntityModel<TCBEntity> {
    public TCBModel() {
        super(JStandTypeRegistry.TCB.get());
    }
}