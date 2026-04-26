package net.arna.jcraft.common.network.s2c;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.arna.jcraft.api.registry.JPacketRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class IPSTriggeredPacket {
    public static void send(ServerPlayer victim) {
        NetworkManager.sendToPlayer(victim, JPacketRegistry.S2C_IPS_TRIGGERED, new FriendlyByteBuf(Unpooled.buffer()));
    }
}
