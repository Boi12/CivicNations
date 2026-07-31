package com.austin.civicnations.command;

import com.austin.civicnations.data.NationCurrency;
import com.austin.civicnations.data.NationInvitation;
import com.austin.civicnations.data.NationMember;
import com.austin.civicnations.data.NationRecord;
import com.austin.civicnations.data.NationRole;
import com.austin.civicnations.data.NationSavedData;
import com.austin.civicnations.integration.FTBIntegration;
import com.austin.civicnations.service.CurrencyService;
import com.austin.civicnations.service.NationActionResult;
import com.austin.civicnations.service.NationService;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class NationCommands {
    private NationCommands() {}

    public static void onRegister(RegisterCommandsEvent event) {
        register(event.getDispatcher());
        AdminCommands.register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nation")
                .then(Commands.literal("create")
                        .then(Commands.argument("abbreviation", StringArgumentType.word())
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(NationCommands::create))))
                .then(Commands.literal("disband")
                        .then(Commands.literal("confirm")
                                .executes(NationCommands::disband)))
                .then(Commands.literal("info")
                        .executes(NationCommands::info))
                .then(Commands.literal("list")
                        .executes(NationCommands::list))
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(NationCommands::invite)))
                .then(Commands.literal("invitations")
                        .executes(NationCommands::invitations))
                .then(Commands.literal("accept")
                        .then(Commands.argument("nation", StringArgumentType.greedyString())
                                .suggests((context, builder) -> suggestInvitations(context, builder))
                                .executes(NationCommands::accept)))
                .then(Commands.literal("reject")
                        .then(Commands.argument("nation", StringArgumentType.greedyString())
                                .suggests((context, builder) -> suggestInvitations(context, builder))
                                .executes(NationCommands::reject)))
                .then(Commands.literal("leave")
                        .executes(NationCommands::leave))
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(NationCommands::remove)))
                .then(Commands.literal("transfer")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(NationCommands::transfer)))
                .then(Commands.literal("role")
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("role", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        List.of("treasurer", "official", "citizen"), builder))
                                                .executes(NationCommands::setRole)))))
                .then(Commands.literal("permissions")
                        .executes(NationCommands::permissions))
                .then(Commands.literal("claims")
                        .executes(NationCommands::claims))
                .then(Commands.literal("where")
                        .executes(NationCommands::where))
                .then(Commands.literal("currency")
                        .then(Commands.literal("create")
                                .then(Commands.argument("code", StringArgumentType.word())
                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .executes(NationCommands::createCurrency))))
                        .then(Commands.literal("info")
                                .executes(NationCommands::currencyInfo))
                        .then(Commands.literal("balance")
                                .executes(NationCommands::currencyBalance))
                        .then(Commands.literal("deposit")
                                .executes(NationCommands::currencyDeposit))
                        .then(Commands.literal("withdraw")
                                .then(Commands.argument("denomination", IntegerArgumentType.integer(1))
                                        .executes(context -> currencyWithdraw(context, 1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 640))
                                                .executes(context -> currencyWithdraw(context,
                                                        IntegerArgumentType.getInteger(context, "count"))))))
)
                .then(Commands.literal("treasury")
                        .then(Commands.literal("info")
                                .executes(NationCommands::treasuryInfo))
                        .then(Commands.literal("deposit")
                                .then(Commands.argument("amount", LongArgumentType.longArg(1L))
                                        .executes(NationCommands::treasuryDeposit)))
                        .then(Commands.literal("withdraw")
                                .then(Commands.argument("amount", LongArgumentType.longArg(1L))
                                        .executes(NationCommands::treasuryWithdraw))))
                .then(Commands.literal("cooldown")
                        .executes(NationCommands::cooldown))
        );
    }

    private static int create(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String abbreviation = StringArgumentType.getString(context, "abbreviation");
        String name = StringArgumentType.getString(context, "name");
        ItemStack banner = player.getMainHandItem();
        NationActionResult result = NationService.createNation(player, name, abbreviation, banner);
        return result.success() ? 1 : 0;
    }

    private static int disband(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        NationActionResult result = NationService.disbandNation(context.getSource().getPlayerOrException());
        return result.success() ? 1 : 0;
    }

    private static int invite(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        NationActionResult result = NationService.invitePlayer(
                context.getSource().getPlayerOrException(),
                EntityArgument.getPlayer(context, "player")
        );
        return result.success() ? 1 : 0;
    }

    private static int accept(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        NationActionResult result = NationService.acceptInvitation(
                context.getSource().getPlayerOrException(),
                StringArgumentType.getString(context, "nation")
        );
        return result.success() ? 1 : 0;
    }

    private static int reject(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        NationActionResult result = NationService.rejectInvitation(
                context.getSource().getPlayerOrException(),
                StringArgumentType.getString(context, "nation")
        );
        return result.success() ? 1 : 0;
    }

    private static int leave(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        NationActionResult result = NationService.leaveNation(context.getSource().getPlayerOrException());
        return result.success() ? 1 : 0;
    }

    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        NationActionResult result = NationService.removeMember(
                context.getSource().getPlayerOrException(),
                EntityArgument.getPlayer(context, "player")
        );
        return result.success() ? 1 : 0;
    }

    private static int transfer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        NationActionResult result = NationService.transferLeadership(
                context.getSource().getPlayerOrException(),
                EntityArgument.getPlayer(context, "player")
        );
        return result.success() ? 1 : 0;
    }

    private static int setRole(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer actor = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        String rawRole = StringArgumentType.getString(context, "role");
        Optional<NationRole> role = NationRole.parse(rawRole);
        if (role.isEmpty() || !role.get().canBeAssignedDirectly()) {
            context.getSource().sendFailure(Component.literal(
                    "Role must be treasurer, official, or citizen."
            ));
            return 0;
        }

        NationActionResult result = NationService.setRole(actor, target, role.get());
        return result.success() ? 1 : 0;
    }

    private static int info(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        NationSavedData data = NationSavedData.get(player.server);
        Optional<NationRecord> nationOptional = data.getNationForPlayer(player.getUUID());
        if (nationOptional.isEmpty()) {
            context.getSource().sendFailure(Component.literal("You do not belong to a nation."));
            return 0;
        }

        NationRecord nation = nationOptional.get();
        MinecraftServer server = player.server;
        context.getSource().sendSuccess(() -> Component.literal(
                nation.name() + " [" + nation.abbreviation() + "]"
        ).withStyle(ChatFormatting.GOLD), false);
        context.getSource().sendSuccess(() -> Component.literal(
                nation.serverOwned()
                        ? "Founder: " + playerName(server, nation.founderUuid()) + " | Owner: Server"
                        : "Founder: " + playerName(server, nation.founderUuid())
                        + " | Leader: " + playerName(server, nation.leaderUuid())
        ), false);
        context.getSource().sendSuccess(() -> Component.literal(
                "Citizens: " + nation.memberCount()
                        + " | Claims: " + FTBIntegration.getClaimedChunkCount(nation)
                        + "/" + nation.claimLimit()
        ), false);

        nation.members().stream()
                .sorted(Comparator.comparing((NationMember member) -> member.role().authorityLevel()).reversed()
                        .thenComparing(member -> playerName(server, member.playerId()), String.CASE_INSENSITIVE_ORDER))
                .forEach(member -> context.getSource().sendSuccess(() -> Component.literal(
                        "- " + playerName(server, member.playerId()) + " - " + member.role().displayName()
                ).withStyle(member.playerId().equals(player.getUUID())
                        ? ChatFormatting.AQUA : ChatFormatting.GRAY), false));
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        NationSavedData data = NationSavedData.get(context.getSource().getServer());
        if (data.getNations().isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No nations have been created."), false);
            return 1;
        }

        data.getNations().stream()
                .sorted(Comparator.comparing(NationRecord::name, String.CASE_INSENSITIVE_ORDER))
                .forEach(nation -> context.getSource().sendSuccess(() -> Component.literal(
                        nation.name() + " [" + nation.abbreviation() + "] - "
                                + nation.memberCount() + " citizen(s), "
                                + FTBIntegration.getClaimedChunkCount(nation) + "/" + nation.claimLimit() + " claims"
                ), false));
        return 1;
    }

    private static int invitations(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<NationInvitation> invitations = NationService.getInvitations(player);
        NationSavedData data = NationSavedData.get(player.server);
        if (invitations.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("You have no active nation invitations."), false);
            return 1;
        }

        for (NationInvitation invitation : invitations) {
            data.getNation(invitation.nationId()).ifPresent(nation ->
                    context.getSource().sendSuccess(() -> Component.literal(
                            nation.name() + " [" + nation.abbreviation() + "] - invited by "
                                    + playerName(player.server, invitation.inviterId())
                    ), false)
            );
        }
        return 1;
    }

    private static int permissions(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Optional<NationRecord> nation = NationSavedData.get(player.server).getNationForPlayer(player.getUUID());
        if (nation.isEmpty()) {
            context.getSource().sendFailure(Component.literal("You do not belong to a nation."));
            return 0;
        }

        NationRole role = nation.get().roleOf(player.getUUID());
        context.getSource().sendSuccess(() -> Component.literal(
                "Role: " + role.displayName()
        ).withStyle(ChatFormatting.AQUA), false);
        if (role.permissions().isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No government permissions."), false);
        } else {
            role.permissions().stream()
                    .sorted(Comparator.comparing(Enum::name))
                    .forEach(permission -> context.getSource().sendSuccess(() -> Component.literal(
                            "- " + permission.name().toLowerCase(Locale.ROOT).replace('_', ' ')
                    ), false));
        }
        return 1;
    }

    private static int claims(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Optional<NationRecord> nation = NationSavedData.get(player.server).getNationForPlayer(player.getUUID());
        if (nation.isEmpty()) {
            context.getSource().sendFailure(Component.literal("You do not belong to a nation."));
            return 0;
        }
        int count = FTBIntegration.getClaimedChunkCount(nation.get());
        context.getSource().sendSuccess(() -> Component.literal(
                nation.get().name() + " controls " + count + " of " + nation.get().claimLimit() + " allowed chunks."
        ), false);
        return 1;
    }

    private static int where(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Optional<NationRecord> owner = FTBIntegration.getNationAt(player.serverLevel(), player.blockPosition());
        if (owner.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("This chunk is wilderness."), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal(
                    "This chunk belongs to " + owner.get().name() + " [" + owner.get().abbreviation() + "]."
            ).withStyle(ChatFormatting.GOLD), false);
        }
        return 1;
    }

    private static int createCurrency(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        NationActionResult result = CurrencyService.createCurrency(
                context.getSource().getPlayerOrException(),
                StringArgumentType.getString(context, "name"),
                StringArgumentType.getString(context, "code")
        );
        return result.success() ? 1 : 0;
    }

    private static int currencyInfo(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Optional<NationRecord> nation = NationSavedData.get(player.server)
                .getNationForPlayer(player.getUUID());
        if (nation.isEmpty()) {
            context.getSource().sendFailure(Component.literal("You do not belong to a nation."));
            return 0;
        }
        Optional<NationCurrency> currency = nation.get().currency();
        if (currency.isEmpty()) {
            context.getSource().sendFailure(Component.literal(
                    "Your nation has not created a currency. Use /nation currency create <code> <name>."));
            return 0;
        }
        NationCurrency value = currency.get();
        context.getSource().sendSuccess(() -> Component.literal(
                value.name() + " (" + value.code() + ")"
        ).withStyle(ChatFormatting.GOLD), false);
        context.getSource().sendSuccess(() -> Component.literal(
                "Denominations: " + value.denominations()
                        + " | Total Issued: " + CurrencyService.format(value, value.totalIssued())
        ), false);
        return 1;
    }

    private static int currencyBalance(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Optional<NationRecord> nation = NationSavedData.get(player.server)
                .getNationForPlayer(player.getUUID());
        if (nation.isEmpty() || nation.get().currency().isEmpty()) {
            context.getSource().sendFailure(Component.literal("Your nation has not created a currency."));
            return 0;
        }
        NationCurrency currency = nation.get().currency().get();
        long balance = currency.accountBalance(player.getUUID());
        context.getSource().sendSuccess(() -> Component.literal(
                "Digital balance: " + CurrencyService.format(currency, balance)
        ).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int currencyDeposit(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        NationActionResult result = CurrencyService.depositPhysicalCurrency(
                context.getSource().getPlayerOrException());
        return result.success() ? 1 : 0;
    }

    private static int currencyWithdraw(CommandContext<CommandSourceStack> context, int count)
            throws CommandSyntaxException {
        NationActionResult result = CurrencyService.withdrawPhysicalCurrency(
                context.getSource().getPlayerOrException(),
                IntegerArgumentType.getInteger(context, "denomination"),
                count
        );
        return result.success() ? 1 : 0;
    }


    private static int treasuryInfo(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Optional<NationRecord> nation = NationSavedData.get(player.server)
                .getNationForPlayer(player.getUUID());
        if (nation.isEmpty() || nation.get().currency().isEmpty()) {
            context.getSource().sendFailure(Component.literal("Your nation has not created a currency."));
            return 0;
        }
        NationCurrency currency = nation.get().currency().get();
        context.getSource().sendSuccess(() -> Component.literal(
                "Nation treasury: " + CurrencyService.format(currency, currency.treasuryBalance())
        ).withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int treasuryDeposit(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        NationActionResult result = CurrencyService.depositTreasury(
                context.getSource().getPlayerOrException(),
                LongArgumentType.getLong(context, "amount")
        );
        return result.success() ? 1 : 0;
    }

    private static int treasuryWithdraw(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        NationActionResult result = CurrencyService.withdrawTreasury(
                context.getSource().getPlayerOrException(),
                LongArgumentType.getLong(context, "amount")
        );
        return result.success() ? 1 : 0;
    }

    private static int cooldown(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        long remaining = NationSavedData.get(player.server)
                .getJoinCooldownRemaining(player.getUUID(), System.currentTimeMillis());
        if (remaining <= 0L) {
            context.getSource().sendSuccess(() -> Component.literal("You are not under a nation-joining cooldown."), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal(
                    "Nation joining cooldown: " + NationService.formatDuration(remaining) + " remaining."
            ).withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestInvitations(
            CommandContext<CommandSourceStack> context,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        NationSavedData data = NationSavedData.get(player.server);
        List<String> names = NationService.getInvitations(player).stream()
                .map(invitation -> data.getNation(invitation.nationId()).map(NationRecord::name).orElse(""))
                .filter(name -> !name.isBlank())
                .toList();
        return SharedSuggestionProvider.suggest(names, builder);
    }

    private static String playerName(MinecraftServer server, java.util.UUID playerId) {
        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        return server.getProfileCache()
                .get(playerId)
                .map(GameProfile::getName)
                .orElse(playerId.toString());
    }
}
