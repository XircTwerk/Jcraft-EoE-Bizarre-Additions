package net.arna.jcraft.common.item;

import net.arna.jcraft.api.registry.JEntityTypeRegistry;
import net.arna.jcraft.common.entity.projectile.ThrownGarlic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GarlicItem extends Item {

    public GarlicItem(Properties properties) {
        super(properties);

        // Register dispenser behavior
        DispenserBlock.registerBehavior(this, new AbstractProjectileDispenseBehavior() {
            @Override
            protected Projectile getProjectile(Level level, Position position, ItemStack stack) {
                ThrownGarlic garlic = new ThrownGarlic(level);
                garlic.setPos(position.x(), position.y(), position.z());
                garlic.setItem(stack);
                return garlic;
            }
        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        // Play throw sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide) {
            // Create and spawn the garlic projectile
            ThrownGarlic thrownGarlic = new ThrownGarlic(level, player);
            thrownGarlic.setItem(itemStack);
            thrownGarlic.shootFromRotation(player, player.getXRot(), player.getYRot(),
                    0.0F, 1.5F, 1.0F);
            level.addFreshEntity(thrownGarlic);
        }

        // Update player stats
        player.awardStat(Stats.ITEM_USED.get(this));

        // Consume the item if not in creative mode
        if (!player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }

        // Add swing animation
        player.swing(hand, true);

        player.getCooldowns().addCooldown(this, 10);

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("item.jcraft.garlic.desc1").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.jcraft.garlic.desc2").withStyle(ChatFormatting.RED));
    }
}