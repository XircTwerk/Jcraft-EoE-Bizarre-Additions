package net.arna.jcraft.common.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

@UtilityClass
public class NbtUtils {

    public void put(final @NonNull CompoundTag compoundTag, final @NonNull String key, final @NonNull Vec3 vec) {
        final ListTag listTag = new ListTag();
        listTag.add(DoubleTag.valueOf(vec.x()));
        listTag.add(DoubleTag.valueOf(vec.y()));
        listTag.add(DoubleTag.valueOf(vec.z()));
        compoundTag.put(key, listTag);
    }

    public Vec3 getVec3(final @NonNull CompoundTag compoundTag, final @NonNull String key) {
        final ListTag listTag = compoundTag.getList(key, Tag.TAG_DOUBLE);
        return new Vec3(listTag.getDouble(0), listTag.getDouble(1), listTag.getDouble(2));
    }

    public void put(final @NonNull CompoundTag compoundTag, final @NonNull String key, final @NonNull Vec3i vec) {
        final ListTag listTag = new ListTag();
        listTag.add(IntTag.valueOf(vec.getX()));
        listTag.add(IntTag.valueOf(vec.getY()));
        listTag.add(IntTag.valueOf(vec.getZ()));
        compoundTag.put(key, listTag);
    }

    public Vec3i getVec3i(final @NonNull CompoundTag compoundTag, final @NonNull String key) {
        final ListTag listTag = compoundTag.getList(key, Tag.TAG_INT);
        return new Vec3i(listTag.getInt(0), listTag.getInt(1), listTag.getInt(2));
    }

}
