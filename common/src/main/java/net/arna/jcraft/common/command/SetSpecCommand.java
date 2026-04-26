package net.arna.jcraft.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.spec.SpecType;
import net.arna.jcraft.common.argumenttype.SpecArgumentType;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;

public class SetSpecCommand {
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spec")
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("entities", EntityArgument.entities())
                                .then(Commands.argument("spec", SpecArgumentType.spec())
                                        .executes(SetSpecCommand::run)
                                )
                        )
                )
        );
    }

    public static int run(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            final SpecType specType = context.getArgument("spec", SpecType.class);
            final Collection<? extends Entity> targets = EntityArgument.getEntities(context, "entities");

            if (targets.isEmpty()) {
                return 0;
            }
            for (Entity entity : targets) {
                if (entity instanceof LivingEntity living) JComponentPlatformUtils.getSpecData(living).setType(specType);
            }
        } catch (Exception e) {
            JCraft.LOGGER.error("Failed to set spec", e);
            return 0;
        }

        return 1;
    }
}
