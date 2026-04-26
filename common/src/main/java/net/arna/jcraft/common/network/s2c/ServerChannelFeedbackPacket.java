package net.arna.jcraft.common.network.s2c;

import dev.architectury.networking.NetworkManager;
import lombok.NonNull;
import net.arna.jcraft.api.registry.JPacketRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;


public class ServerChannelFeedbackPacket {
    public static void send(@NonNull final ServerPlayer serverPlayerEntity, @NonNull final FriendlyByteBuf buf) {
        // JCraft.LOGGER.info("Sending CF Packet of hash {} to {}", buf.hashCode(), serverPlayerEntity);
        NetworkManager.sendToPlayer(serverPlayerEntity, JPacketRegistry.S2C_SERVER_CHANNEL_FEEDBACK, buf);
    }

    public static void send(@NonNull final Iterable<ServerPlayer> serverPlayers, @NonNull final FriendlyByteBuf buf) {
        // JCraft.LOGGER.info("Sending CF Packet of hash {} to {}", buf.hashCode(), serverPlayers);
        NetworkManager.sendToPlayers(serverPlayers, JPacketRegistry.S2C_SERVER_CHANNEL_FEEDBACK, buf);
    }
}
