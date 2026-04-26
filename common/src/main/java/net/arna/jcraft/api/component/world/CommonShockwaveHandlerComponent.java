package net.arna.jcraft.api.component.world;

import lombok.Data;
import lombok.Getter;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public interface CommonShockwaveHandlerComponent {
    void addShockwave(final double x, final double y, final double z, final float pitch, final float yaw, final float scale, final Shockwave.Type type);

    default void addShockwave(final double x, final double y, final double z, final float pitch, final float yaw) {
        addShockwave(x, y, z, pitch, yaw, 1.0f, Shockwave.Type.DEFAULT);
    }

    default void addShockwave(final Vec3 pos, final float pitch, final float yaw, final float scale) {
        addShockwave(pos.x, pos.y, pos.z, pitch, yaw, scale, Shockwave.Type.DEFAULT);
    }

    default void addShockwave(final Vec3 pos, final float pitch, final float yaw) {
        addShockwave(pos.x, pos.y, pos.z, pitch, yaw);
    }

    default void addShockwave(final Vec3 pos, final Vec3 rotation, final float scale, Shockwave.Type type) {
        Vec2 polarRot = JUtils.rotationVectorToPolar(rotation);
        addShockwave(pos.x, pos.y, pos.z, polarRot.x, polarRot.y, scale, type);
    }

    default void addShockwave(final Vec3 pos, final Vec3 rotation) {
        Vec2 polarRot = JUtils.rotationVectorToPolar(rotation);
        addShockwave(pos.x, pos.y, pos.z, polarRot.x, polarRot.y);
    }

    default void addShockwave(final Vec3 pos, final Vec3 rotation, final float scale) {
        Vec2 polarRot = JUtils.rotationVectorToPolar(rotation);
        addShockwave(pos.x, pos.y, pos.z, polarRot.x, polarRot.y, scale, Shockwave.Type.DEFAULT);
    }

    List<Shockwave> getShockwaves();

    @Data
    class Shockwave {
        @Getter
        public enum Type {
            DEFAULT(0, ""),
            PUSHBLOCK(1, "pb");

            Type(int id, String name) {
                this.id = id;
                this.name = name;
            }

            private final int id;
            private final String name;

            public static Type of(int id) {
                if (id > values().length) throw new IllegalArgumentException("Index out of bounds");
                return values()[id];
            }
        }

        public static final int MAX_AGE = 6;
        public final double x, y, z;
        public final BlockPos blockPos;
        public final float pitch, yaw, scale;
        private final Shockwave.Type type;
        private int age;

        public Shockwave(double x, double y, double z, float pitch, float yaw, float scale, int age, Shockwave.Type type) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockPos = BlockPos.containing(x, y, z);
            this.pitch = pitch;
            this.yaw = yaw;
            this.scale = scale;
            this.age = age;
            this.type = type;
        }

        public Shockwave(double x, double y, double z, float pitch, float yaw, float scale, Shockwave.Type type) {
            this(x, y, z, pitch, yaw, scale, 0, type);
        }

        public void tick() {
            age++;
        }

        // Currently just the age, but this might change.
        public int getFrame() {
            return Math.min(age, MAX_AGE - 1);
        }
    }
}
