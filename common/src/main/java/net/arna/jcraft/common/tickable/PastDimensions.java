package net.arna.jcraft.common.tickable;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.common.util.DimensionData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PastDimensions {
    protected static final List<DimensionData> dimensions = new ArrayList<>();

    public static void enqueue(DimensionData dimensionData) {
        dimensions.add(dimensionData);
    }

    public static void remove(DimensionData dimensionData) {
        dimensions.remove(dimensionData);
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer serverPlayer : JCraft.auWorld.players()) {
            boolean contained = false;
            for (DimensionData dimensionData : dimensions) {
                if (dimensionData.getUser() == serverPlayer) {
                    contained = true;
                    break;
                }
            }
            if (!contained) safeReturn(serverPlayer);
        }

        List<DimensionData> newDimensions = new ArrayList<>();

        for (DimensionData dimensionData : dimensions) {
            Entity user = dimensionData.getUser();
            if (user == null || !user.isAlive()) {
                continue;
            }

            ServerLevel original = server.getLevel(dimensionData.getWorldKey());
            if (user.level() == original) {
                continue;
            }

            dimensionData.decreaseTimer();
            if (dimensionData.getTimer() > 1) {
                newDimensions.add(dimensionData);
                continue;
            }

            Vec3 dimPos = user.position().add(0d, (JCraft.auWorld.getHeight() - original.getHeight()) / 2, 0d); //dimValues.pos;
            if (user instanceof ServerPlayer player) {
                player.teleportTo(original, dimPos.x, dimPos.y, dimPos.z, player.getYRot(), player.getXRot());
            } else {
                JCraft.teleportToWorld(user, original, dimPos.x, dimPos.y, dimPos.z);
            }
        }

        if (JCraft.preloadLockTicks <= 0 && newDimensions.isEmpty()) // Nobody left in AU
        {
            JCraft.clearPreloadedChunks();
        }

        dimensions.clear();
        dimensions.addAll(newDimensions);
    }

    /**
     * Returns a player to their respawn point.
     * Used when PastDimensions does not have a return DimensionData specified for a player in the jcraft:audim
     */
    public static void safeReturn(ServerPlayer serverPlayer) {
        //todo: implement NBT saving for PastDimensions!
        JCraft.LOGGER.warn("PastDimensions.safeReturn called on " + serverPlayer);
        BlockPos spawnPos = serverPlayer.getRespawnPosition(); // Prioritize spawn point
        // Use current position if all else fails
        if (spawnPos == null) spawnPos = serverPlayer.blockPosition();
        PastDimensions.enqueue(new DimensionData(serverPlayer, new Vec3(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ()), serverPlayer.getRespawnDimension()));
    }

    public static boolean tryExit(LivingEntity user, Set<? extends Entity> targets) {
        boolean isStored = false;

        for (DimensionData dimV : dimensions) {
            // Bring others out of the AU
            if (targets.contains(dimV.getUser())) {
                dimV.setTimer(1);
                continue;
            }

            if (dimV.getUser() != user) {
                continue;
            }
            isStored = true;
            dimV.setTimer(1);
        }

        return isStored;
    }
}
