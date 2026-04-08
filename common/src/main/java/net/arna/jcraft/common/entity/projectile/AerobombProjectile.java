package net.arna.jcraft.common.entity.projectile;

import net.arna.jcraft.api.registry.JEntityTypeRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class AerobombProjectile extends AbstractArrow {

    public AerobombProjectile(final Level level) {
        super(JEntityTypeRegistry.AEROBOMB.get(), level);
    }

    @Override
    protected boolean tryPickup(final Player player) {
        return false;
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }


}
