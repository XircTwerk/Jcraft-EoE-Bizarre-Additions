package net.arna.jcraft.common.network.c2s;

import dev.architectury.networking.NetworkManager;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.common.config.JServerConfig;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class CooldownCancelPacket {
    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        final ServerPlayer player = (ServerPlayer) context.getPlayer();

        if (
                player.isCreative() ||
                (JServerConfig.SURVIVAL_CDC.getValue() && !player.hasEffect(JStatusRegistry.DAZED.get()))
        ) {
            JComponentPlatformUtils.getCooldowns(player).cooldownCancel();
        }
    }
}
