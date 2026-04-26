package net.arna.jcraft.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;

public class CooldownCancelCommand {
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("cooldown")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("cancel")
                                .then(Commands.argument("entities", EntityArgument.entities())
                                        .executes(CooldownCancelCommand::run)
                                )
                        )
                        .then(Commands.literal("reset")
                                .then(Commands.argument("entities", EntityArgument.entities())
                                        .executes(CooldownCancelCommand::run)
                                )
                        )
        );

        dispatcher.register(
                Commands.literal("cdc")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("entities", EntityArgument.entities())
                                .executes(CooldownCancelCommand::run)
                        )
        );
    }

    public static int run(final CommandContext<CommandSourceStack> ctx) {
        try {
            final Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "entities");

            for (Entity entity : targets) {
                if (entity instanceof LivingEntity living) JComponentPlatformUtils.getCooldowns(living).clear();
            }

            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
}
