package net.arna.jcraft.api.component.entity;

import net.arna.jcraft.api.component.JComponent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public interface CommonTimeStopComponent extends JComponent {
    int getTicks();

    //TODO: specialized sync packet
    void setTicks(final int ticks);

    void addTotalVelocity(final Vec3 vel);

    void tick(final CallbackInfo ci);
}
