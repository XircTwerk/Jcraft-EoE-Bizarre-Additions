package net.arna.jcraft.client.renderer.entity.layer;

import lombok.NonNull;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.client.renderer.entity.projectiles.RapierRenderer;
import net.arna.jcraft.common.attack.moves.shared.TossChargeMove;
import net.arna.jcraft.common.attack.moves.shared.TossMove;
import net.arna.jcraft.common.entity.stand.SilverChariotEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderType;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public class SCRapierLayer extends AbstractRenderLayer<SilverChariotEntity> {

    @Override
    public void render(final @NonNull AzRendererPipelineContext<UUID, SilverChariotEntity> pc) {
        final SilverChariotEntity sc = pc.animatable();

        final AbstractMove<?,?> currentMove = sc.getCurrentMove();
        if (!sc.hasRapier() || currentMove instanceof TossMove || currentMove instanceof TossChargeMove) {
            return;
        }

        final int skin = sc.getSkin();

        final SilverChariotEntity.Mode mode = sc.getMode();

        final RenderType cameo = RenderType.armorCutoutNoCull(
                mode == SilverChariotEntity.Mode.POSSESSED ? RapierRenderer.POSSESSED_SKINS.get(skin) :
                        mode == SilverChariotEntity.Mode.ARMORLESS ? RapierRenderer.ARMOR_OFF_TEXTURE :
                                RapierRenderer.SKINS.get(skin)
        );

        pc.setRenderType(cameo);
        pc.setVertexConsumer(pc.multiBufferSource().getBuffer(cameo));
        pc.rendererPipeline().reRender(pc);
    }

}
