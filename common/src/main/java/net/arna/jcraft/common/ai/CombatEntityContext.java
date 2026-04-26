package net.arna.jcraft.common.ai;

import lombok.NonNull;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.api.spec.JSpec;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

// Was a record until a field was made mutable. Converted to a class to not waste excess memory.
public class CombatEntityContext {
    private final @NonNull LivingEntity entity;
    public LivingEntity entity() { return entity; }

    private @Nullable StandEntity<?, ?> stand;
    public StandEntity<?, ?> stand() { return stand; }

    private @Nullable AbstractMove<?, ?> standAttack;
    public AbstractMove<?, ?> standAttack() { return standAttack; }

    private final @Nullable JSpec<?, ?> spec;
    public JSpec<?, ?> spec() { return spec; }

    private final @Nullable AbstractMove<?, ?> specAttack;
    public AbstractMove<?, ?> specAttack() { return specAttack; }

    private final int moveStun;
    public int moveStun() { return moveStun; }

    private final boolean blocking;
    public boolean blocking() { return blocking; }

    private final @Nullable MobEffectInstance stun;
    public MobEffectInstance stun() { return stun; }

    public CombatEntityContext(@NonNull LivingEntity entity,
                               @Nullable StandEntity<?, ?> stand,
                               @Nullable AbstractMove<?, ?> standAttack,
                               @Nullable JSpec<?, ?> spec,
                               @Nullable AbstractMove<?, ?> specAttack,
                               int moveStun,
                               boolean blocking,
                               @Nullable MobEffectInstance stun) {

        this.entity = entity;
        this.stand = stand;
        this.standAttack = standAttack;
        this.spec = spec;
        this.specAttack = specAttack;
        this.moveStun = moveStun;
        this.blocking = blocking;
        this.stun = stun;
    }

    public void reassignStand(@Nullable StandEntity<?, ?> stand) {
        this.stand = stand;
        this.standAttack = stand == null ? null : stand.getCurrentMove();
    }

    public int disadvantage() {
        if (stun == null) return moveStun;
        return Math.max(moveStun, stun.getDuration());
    }

    public static CombatEntityContext from(LivingEntity entity) {
        final StandEntity<?, ?> stand = JUtils.getStand(entity);
        final JSpec<?, ?> spec = JUtils.getSpec(entity);

        return new CombatEntityContext(
                entity,
                stand,
                stand == null ? null : stand.getCurrentMove(),
                spec,
                spec == null ? null : spec.getCurrentMove(),
                Math.max(
                        stand == null ? 0 : stand.getMoveStun(),
                        spec == null ? 0 : spec.getMoveStun()
                ),
                stand != null && stand.blocking,
                entity.getEffect(JStatusRegistry.DAZED.get())
        );
    }

    public boolean noWindupsPassed() {
        if (standAttack != null) if (standAttack.hasWindupPassed(stand)) return false;
        if (specAttack != null) if (specAttack.hasWindupPassed(spec)) return false;

        return true;
    }
}
