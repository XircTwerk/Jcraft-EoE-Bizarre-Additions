package net.arna.jcraft.common.util;

import net.minecraft.world.entity.LivingEntity;

public interface IComboCounter {
    LivingEntity jcraft$getLastAttacked();

    void jcraft$setLastAttacked(LivingEntity l);

    int jcraft$getComboCount();

    //boolean jcraft$wasStunned();

    void jcraft$setComboCount(int i);

    void jcraft$incrementComboCount();
}
