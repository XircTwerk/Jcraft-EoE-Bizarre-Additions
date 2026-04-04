package net.arna.jcraft.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.arna.jcraft.mixin_logic.PostChainAddon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Map;

@Mixin(PostChain.class)
public class PostChainMixin implements PostChainAddon {

    @Shadow
    @Final
    private List<RenderTarget> fullSizedTargets;

    @Shadow
    private int screenWidth;

    @Shadow
    private int screenHeight;

    @Shadow
    @Final
    private Map<String, RenderTarget> customRenderTargets;

    @Override
    public void jcraft$addTempTarget(String name, boolean useDepth) {
        RenderTarget renderTarget = new TextureTarget(screenWidth, screenHeight, useDepth, Minecraft.ON_OSX);
        renderTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        customRenderTargets.put(name, renderTarget);
        fullSizedTargets.add(renderTarget);
    }
}
