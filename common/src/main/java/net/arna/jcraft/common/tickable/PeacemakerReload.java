package net.arna.jcraft.common.tickable;

import lombok.experimental.UtilityClass;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.common.item.Peacemaker;
import net.arna.jcraft.common.util.DimensionData;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PeacemakerReload {
    private final List<DimensionData> toReload = new ArrayList<>();
    private final List<DimensionData> toAdd = new ArrayList<>();

    public void enqueue(DimensionData dimensionData) {
        toAdd.add(dimensionData);
    }

    public void remove(DimensionData dimensionData) {
        toReload.remove(dimensionData);
    }

    public void tick(MinecraftServer server) {
        // First, add any new entries that were queued during the last tick
        if (!toAdd.isEmpty()) {
            toReload.addAll(toAdd);
            toAdd.clear();
        }

        // Now process the reload list
        List<DimensionData> newToReload = new ArrayList<>();

        for (DimensionData toReloadData : toReload) {
            LivingEntity user = toReloadData.getUser();
            if (user != null && user.isAlive()) {
                final int timer = toReloadData.getTimer();
                if (timer > 0) {
                    toReloadData.decreaseTimer();
                    newToReload.add(toReloadData);
                } else {
                    ServerLevel world = server.getLevel(toReloadData.getWorldKey());
                    if (world == null) {
                        JCraft.LOGGER.warn("World that toReloadData belongs to no longer exists! Key: " + toReloadData.getWorldKey() + " user: " + user);
                        continue;
                    }

                    ItemStack main = user.getMainHandItem();
                    if (main.getItem() == JItemRegistry.PEACEMAKER.get()) {
                        Peacemaker.finishReload(main, world, user);
                    }
                }
            }
        }

        toReload.clear();
        toReload.addAll(newToReload);
    }
}