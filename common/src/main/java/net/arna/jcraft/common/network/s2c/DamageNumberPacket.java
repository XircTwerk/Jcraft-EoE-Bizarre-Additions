package net.arna.jcraft.common.network.s2c;

import net.minecraft.network.FriendlyByteBuf;

public record DamageNumberPacket(int entityId, float damageAmount) {

    public DamageNumberPacket(final FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readFloat());
    }

    public void write(final FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeFloat(this.damageAmount);
    }
}