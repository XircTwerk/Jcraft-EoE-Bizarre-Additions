package net.arna.jcraft.common.attack.moves.goldexperience;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import lombok.Setter;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.common.entity.GEButterflyEntity;
import net.arna.jcraft.common.entity.GEFrogEntity;
import net.arna.jcraft.common.entity.GESnakeEntity;
import net.arna.jcraft.common.entity.stand.GoldExperienceEntity;
import net.arna.jcraft.api.registry.JEntityTypeRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class LifeGiverAttack extends AbstractMove<LifeGiverAttack, GoldExperienceEntity> {
    @Setter
    private LifeGiverType typeToSummon;

    public LifeGiverAttack(final int cooldown, final int windup, final int duration, final float moveDistance) {
        super(cooldown, windup, duration, moveDistance);
        ranged = true;
    }

    @Override
    public @NotNull MoveType<LifeGiverAttack> getMoveType() {
        return Type.INSTANCE;
    }

    @Override
    public @NonNull Set<LivingEntity> perform(final GoldExperienceEntity attacker, final LivingEntity user) {
        ItemStack item = user.getOffhandItem(); // Get offhand, or if unavailable main hand stack
        if (item.isEmpty()) {
            item = user.getMainHandItem();
        }
        if (item.isEmpty()) {
            return Set.of();
        }

        LivingEntity animal = null;
        final ItemStack animalItem = item.copy();
        animalItem.setCount(1);
        switch (typeToSummon) {
            case SNAKE -> {
                if (item.getMaxStackSize() <= 1) {
                    return Set.of();
                }

                final GESnakeEntity snake = new GESnakeEntity(JEntityTypeRegistry.GE_SNAKE.get(), attacker.level());
                if (user instanceof Player playerEntity) {
                    snake.tame(playerEntity);
                } else {
                    snake.setTame(true);
                    snake.setOwnerUUID(user.getUUID());
                }
                animal = snake;
            }
            case FROG -> {
                if (item.getMaxStackSize() <= 1) {
                    return Set.of();
                }

                final GEFrogEntity frog = new GEFrogEntity(JEntityTypeRegistry.GE_FROG.get(), attacker.level());
                frog.setMaster(user);
                animal = frog;
            }
            case BUTTERFLY -> {
                final GEButterflyEntity butterfly = new GEButterflyEntity(JEntityTypeRegistry.GE_BUTTERFLY.get(), attacker.level());
                butterfly.setMaster(user);
                animal = butterfly;
            }
            default -> JCraft.LOGGER.error("Attempted to create Life Giver entity with invalid LifeGiverType: {}", this);
        }

        if (animal == null) {
            JCraft.LOGGER.error("Failed to create animal of type {} from item {}", typeToSummon, animalItem);
            return Set.of();
        }
        if (!(user instanceof Player player && player.getAbilities().instabuild)) {
            item.shrink(1);
        }
        animal.moveTo(attacker.getX(), attacker.getY() + 0.5f, attacker.getZ(), attacker.getYRot(), attacker.getXRot());
        animal.setItemInHand(InteractionHand.MAIN_HAND, animalItem);
        attacker.level().addFreshEntity(animal);

        return Set.of();
    }

    @Override
    protected @NonNull LifeGiverAttack getThis() {
        return this;
    }

    @Override
    public @NonNull LifeGiverAttack copy() {
        return copyExtras(new LifeGiverAttack(getCooldown(), getWindup(), getDuration(), getMoveDistance()));
    }

    public enum LifeGiverType {
        SNAKE,
        FROG,
        BUTTERFLY
    }

    public static class Type extends AbstractMove.Type<LifeGiverAttack> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NotNull App<RecordCodecBuilder.Mu<LifeGiverAttack>, LifeGiverAttack> buildCodec(RecordCodecBuilder.Instance<LifeGiverAttack> instance) {
            return baseDefault(instance, LifeGiverAttack::new);
        }
    }
}
