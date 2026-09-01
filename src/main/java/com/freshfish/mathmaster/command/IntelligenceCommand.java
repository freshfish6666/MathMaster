package com.freshfish.mathmaster.command;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

@EventBusSubscriber(modid = MathMaster.MODID)
public final class IntelligenceCommand {
    private static final SimpleCommandExceptionType ERROR_SET_POINTS_INVALID =
            new SimpleCommandExceptionType(Component.translatable("commands.mathmaster.set.points.invalid"));

    private IntelligenceCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("mathmaster")
                        .requires(source -> source.hasPermission(2))
                        .then(buildAddCommand())
                        .then(buildSetCommand())
                        .then(buildQueryCommand())
        );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildAddCommand() {
        return Commands.literal("add")
                .then(
                        Commands.argument("targets", EntityArgument.players())
                                .then(
                                        Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(context -> addIntelligence(
                                                        context,
                                                        Type.POINTS
                                                ))
                                                .then(
                                                        Commands.literal("points")
                                                                .executes(context -> addIntelligence(
                                                                        context,
                                                                        Type.POINTS
                                                                ))
                                                )
                                                .then(
                                                        Commands.literal("levels")
                                                                .executes(context -> addIntelligence(
                                                                        context,
                                                                        Type.LEVELS
                                                                ))
                                                )
                                )
                );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildSetCommand() {
        return Commands.literal("set")
                .then(
                        Commands.argument("targets", EntityArgument.players())
                                .then(
                                        Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(context -> setIntelligence(
                                                        context,
                                                        Type.POINTS
                                                ))
                                                .then(
                                                        Commands.literal("points")
                                                                .executes(context -> setIntelligence(
                                                                        context,
                                                                        Type.POINTS
                                                                ))
                                                )
                                                .then(
                                                        Commands.literal("levels")
                                                                .executes(context -> setIntelligence(
                                                                        context,
                                                                        Type.LEVELS
                                                                ))
                                                )
                                )
                );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildQueryCommand() {
        return Commands.literal("query")
                .then(
                        Commands.argument("targets", EntityArgument.player())
                                .then(
                                        Commands.literal("points")
                                                .executes(context -> queryIntelligence(
                                                        context,
                                                        Type.POINTS
                                                ))
                                )
                                .then(
                                        Commands.literal("levels")
                                                .executes(context -> queryIntelligence(
                                                        context,
                                                        Type.LEVELS
                                                ))
                                )
                );
    }

    private static int addIntelligence(CommandContext<CommandSourceStack> context, Type type) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Collection<? extends ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        for (ServerPlayer player : targets) {
            type.add.accept(player, amount);
        }

        if (targets.size() == 1) {
            source.sendSuccess(
                    () -> Component.translatable(
                            "commands.mathmaster.add." + type.name + ".success.single",
                            amount,
                            targets.iterator().next().getDisplayName()
                    ),
                    true
            );
        } else {
            source.sendSuccess(
                    () -> Component.translatable(
                            "commands.mathmaster.add." + type.name + ".success.multiple",
                            amount,
                            targets.size()
                    ),
                    true
            );
        }

        return targets.size();
    }

    private static int setIntelligence(CommandContext<CommandSourceStack> context, Type type) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Collection<? extends ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        int changed = 0;
        for (ServerPlayer player : targets) {
            if (type.set.test(player, amount)) {
                changed++;
            }
        }

        if (changed == 0) {
            throw ERROR_SET_POINTS_INVALID.create();
        }

        if (targets.size() == 1) {
            source.sendSuccess(
                    () -> Component.translatable(
                            "commands.mathmaster.set." + type.name + ".success.single",
                            amount,
                            targets.iterator().next().getDisplayName()
                    ),
                    true
            );
        } else {
            source.sendSuccess(
                    () -> Component.translatable(
                            "commands.mathmaster.set." + type.name + ".success.multiple",
                            amount,
                            targets.size()
                    ),
                    true
            );
        }

        return targets.size();
    }

    private static int queryIntelligence(CommandContext<CommandSourceStack> context, Type type) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = EntityArgument.getPlayer(context, "targets");
        int value = type.query.applyAsInt(player);

        source.sendSuccess(
                () -> Component.translatable(
                        "commands.mathmaster.query." + type.name,
                        player.getDisplayName(),
                        value
                ),
                false
        );

        return value;
    }

    private enum Type {
        POINTS(
                "points",
                IntelligenceManager::addExperience,
                IntelligenceManager::setExperience,
                player -> IntelligenceManager.get(player).getExperience()
        ),
        LEVELS(
                "levels",
                IntelligenceManager::addIq,
                (player, amount) -> {
                    IntelligenceManager.setIq(player, amount);
                    return true;
                },
                player -> IntelligenceManager.get(player).getIq()
        );

        private final String name;
        private final BiConsumer<ServerPlayer, Integer> add;
        private final BiPredicate<ServerPlayer, Integer> set;
        private final ToIntFunction<ServerPlayer> query;

        Type(
                String name,
                BiConsumer<ServerPlayer, Integer> add,
                BiPredicate<ServerPlayer, Integer> set,
                ToIntFunction<ServerPlayer> query
        ) {
            this.name = name;
            this.add = add;
            this.set = set;
            this.query = query;
        }
    }
}
