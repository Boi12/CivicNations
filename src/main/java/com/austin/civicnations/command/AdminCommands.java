package com.austin.civicnations.command;

import com.austin.civicnations.data.NationRecord;
import com.austin.civicnations.data.NationRole;
import com.austin.civicnations.data.NationSavedData;
import com.austin.civicnations.integration.FTBIntegration;
import com.austin.civicnations.service.CurrencyService;
import com.austin.civicnations.service.NationActionResult;
import com.austin.civicnations.service.NationService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class AdminCommands {
    private AdminCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("civicnations")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("repair")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(AdminCommands::repair)))
                .then(Commands.literal("clearcooldown")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(AdminCommands::clearCooldown)))
                .then(Commands.literal("admin")
                        .then(Commands.literal("createservernation")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .then(Commands.argument("abbreviation", StringArgumentType.word())
                                                .executes(AdminCommands::createServerNation))))
                        .then(Commands.literal("addmember")
                                .then(Commands.argument("nation", StringArgumentType.string())
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("role", StringArgumentType.word())
                                                        .executes(AdminCommands::addMember)))))
                        .then(Commands.literal("setrole")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("role", StringArgumentType.word())
                                                .executes(AdminCommands::setRole))))
                        .then(Commands.literal("removemember")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(AdminCommands::removeMember)))
                        .then(Commands.literal("disbandservernation")
                                .then(Commands.argument("nation", StringArgumentType.string())
                                        .executes(AdminCommands::disbandServerNation)))
                        .then(Commands.literal("createcurrency")
                                .then(Commands.argument("nation", StringArgumentType.string())
                                        .then(Commands.argument("code", StringArgumentType.word())
                                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                                        .executes(AdminCommands::createCurrency)))))
                        .then(Commands.literal("givecurrency")
                                .then(Commands.argument("nation", StringArgumentType.string())
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("denomination", IntegerArgumentType.integer(1))
                                                        .executes(context -> giveCurrency(context, 1))
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 640))
                                                                .executes(context -> giveCurrency(context,
                                                                        IntegerArgumentType.getInteger(context, "count"))))))))
                )
        );
    }

    private static int repair(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        FTBIntegration.repairPlayerTeam(player);
        context.getSource().sendSuccess(() -> Component.literal(
                "Reconciliation attempted for " + player.getGameProfile().getName() + "."
        ), true);
        return 1;
    }

    private static int clearCooldown(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        NationSavedData.get(context.getSource().getServer()).setJoinCooldown(player.getUUID(), 0L);
        context.getSource().sendSuccess(() -> Component.literal(
                "Cleared nation cooldown for " + player.getGameProfile().getName() + "."
        ), true);
        return 1;
    }

    private static int createServerNation(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer operator = context.getSource().getPlayerOrException();
        NationActionResult result = NationService.createServerNation(
                operator,
                StringArgumentType.getString(context, "name"),
                StringArgumentType.getString(context, "abbreviation")
        );
        return result.success() ? 1 : 0;
    }

    private static int addMember(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer operator = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        Optional<NationRecord> nation = NationSavedData.get(context.getSource().getServer())
                .findNationByName(StringArgumentType.getString(context, "nation"));
        if (nation.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No nation with that name exists."));
            return 0;
        }
        Optional<NationRole> role = NationRole.parse(StringArgumentType.getString(context, "role"));
        if (role.isEmpty()) {
            context.getSource().sendFailure(Component.literal(
                    "Role must be citizen, official, or treasurer."));
            return 0;
        }
        NationActionResult result = NationService.adminAddMember(operator, nation.get(), target, role.get());
        return result.success() ? 1 : 0;
    }

    private static int setRole(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer operator = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        Optional<NationRole> role = NationRole.parse(StringArgumentType.getString(context, "role"));
        if (role.isEmpty()) {
            context.getSource().sendFailure(Component.literal(
                    "Role must be citizen, official, or treasurer."));
            return 0;
        }
        NationActionResult result = NationService.adminSetRole(operator, target, role.get());
        return result.success() ? 1 : 0;
    }

    private static int removeMember(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer operator = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        NationActionResult result = NationService.adminRemoveMember(operator, target);
        return result.success() ? 1 : 0;
    }

    private static int createCurrency(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer operator = context.getSource().getPlayerOrException();
        Optional<NationRecord> nation = NationSavedData.get(context.getSource().getServer())
                .findNationByName(StringArgumentType.getString(context, "nation"));
        if (nation.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No nation with that name exists."));
            return 0;
        }
        NationActionResult result = CurrencyService.adminCreateCurrency(
                operator,
                nation.get(),
                StringArgumentType.getString(context, "name"),
                StringArgumentType.getString(context, "code")
        );
        return result.success() ? 1 : 0;
    }

    private static int giveCurrency(CommandContext<CommandSourceStack> context, int count)
            throws CommandSyntaxException {
        ServerPlayer operator = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        Optional<NationRecord> nation = NationSavedData.get(context.getSource().getServer())
                .findNationByName(StringArgumentType.getString(context, "nation"));
        if (nation.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No nation with that name exists."));
            return 0;
        }
        NationActionResult result = CurrencyService.adminGiveCurrency(
                operator,
                nation.get(),
                target,
                IntegerArgumentType.getInteger(context, "denomination"),
                count
        );
        return result.success() ? 1 : 0;
    }

    private static int disbandServerNation(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer operator = context.getSource().getPlayerOrException();
        Optional<NationRecord> nation = NationSavedData.get(context.getSource().getServer())
                .findNationByName(StringArgumentType.getString(context, "nation"));
        if (nation.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No nation with that name exists."));
            return 0;
        }
        NationActionResult result = NationService.adminDisbandServerNation(operator, nation.get());
        return result.success() ? 1 : 0;
    }
}
