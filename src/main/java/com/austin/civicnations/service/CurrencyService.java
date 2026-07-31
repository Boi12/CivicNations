package com.austin.civicnations.service;

import com.austin.civicnations.block.CoinMintBlock;
import com.austin.civicnations.data.NationCurrency;
import com.austin.civicnations.data.NationPermission;
import com.austin.civicnations.data.NationRecord;
import com.austin.civicnations.data.NationSavedData;
import com.austin.civicnations.integration.FTBIntegration;
import com.austin.civicnations.util.CoinDenomination;
import com.austin.civicnations.util.CurrencyItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class CurrencyService {
    private static final Pattern CURRENCY_NAME = Pattern.compile("[A-Za-z0-9 '\\-]{3,24}");
    private static final Pattern CURRENCY_CODE = Pattern.compile("[A-Za-z0-9]{2,5}");
    private static final List<Integer> COIN_MINT_RUN_SIZES = List.of(1, 4, 8, 16, 32, 64);

    private CurrencyService() {}

    public static NationActionResult createCurrency(ServerPlayer actor, String name,
                                                     String code) {
        NationSavedData data = NationSavedData.get(actor.server);
        Optional<NationRecord> nationOptional = data.getNationForPlayer(actor.getUUID());
        if (nationOptional.isEmpty()) {
            return send(actor, failure("You do not belong to a nation."));
        }
        NationRecord nation = nationOptional.get();
        boolean serverAdmin = nation.serverOwned() && actor.hasPermissions(2);
        if (!serverAdmin && !nation.leaderUuid().equals(actor.getUUID())) {
            return send(actor, failure("Only the Leader can establish a national currency."));
        }
        return createCurrencyForNation(actor, nation, name, code, false);
    }

    /** Backward-compatible overload; separate currency symbols are no longer used. */
    public static NationActionResult createCurrency(ServerPlayer actor, String name,
                                                     String code, String ignoredSymbol) {
        return createCurrency(actor, name, code);
    }

    public static NationActionResult adminCreateCurrency(ServerPlayer operator, NationRecord nation,
                                                          String name, String code) {
        return createCurrencyForNation(operator, nation, name, code, true);
    }

    /** Backward-compatible overload; separate currency symbols are no longer used. */
    public static NationActionResult adminCreateCurrency(ServerPlayer operator, NationRecord nation,
                                                          String name, String code,
                                                          String ignoredSymbol) {
        return adminCreateCurrency(operator, nation, name, code);
    }

    private static NationActionResult createCurrencyForNation(ServerPlayer actor, NationRecord nation,
                                                                String name, String code,
                                                                boolean adminAction) {
        String cleanName = name == null ? "" : name.trim();
        String cleanCode = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        if (!CURRENCY_NAME.matcher(cleanName).matches()) {
            return send(actor, failure("Currency names must be 3-24 characters."));
        }
        if (!CURRENCY_CODE.matcher(cleanCode).matches()) {
            return send(actor, failure("Currency codes must be 2-5 letters or numbers."));
        }
        if (nation.currency().isPresent()) {
            return send(actor, failure(nation.name() + " already has a national currency."));
        }

        NationCurrency currency = NationCurrency.create(cleanName, cleanCode);
        if (!nation.createCurrency(currency)) {
            return send(actor, failure("The currency could not be created."));
        }
        String actorName = adminAction ? "Server operator " + actor.getGameProfile().getName()
                : actor.getGameProfile().getName();
        currency.addTransaction(System.currentTimeMillis(), "created", actorName, 0L,
                "Established the national currency.");
        NationSavedData data = NationSavedData.get(actor.server);
        data.addHistory(nation.nationId(), actorName + " established " + cleanName
                + " (" + cleanCode + ") as the national currency.");
        data.markChanged();
        return send(actor, success("Created " + cleanName + " (" + cleanCode + ") for "
                + nation.name() + "."));
    }

    public static NationActionResult mintAtCoinMint(ServerPlayer actor, BlockPos mintPos,
                                                      int denomination, int count) {
        if (!CoinMintBlock.isComplete(actor.serverLevel(), mintPos)) {
            return send(actor, failure("The Coin Mint is incomplete or no longer present."));
        }
        if (actor.distanceToSqr(mintPos.getX() + 1.0D, mintPos.getY() + 0.5D,
                mintPos.getZ() + 0.5D) > 64.0D) {
            return send(actor, failure("You are too far away from the Coin Mint."));
        }

        NationSavedData data = NationSavedData.get(actor.server);
        Optional<NationRecord> nationOptional = data.getNationForPlayer(actor.getUUID());
        if (nationOptional.isEmpty()) {
            return send(actor, failure("You do not belong to a nation."));
        }
        NationRecord nation = nationOptional.get();
        boolean serverAdmin = nation.serverOwned() && actor.hasPermissions(2);
        if (!serverAdmin && !nation.hasPermission(actor.getUUID(), NationPermission.MINT_CURRENCY)) {
            return send(actor, failure("Your role cannot operate the Coin Mint."));
        }

        BlockState mintState = actor.serverLevel().getBlockState(mintPos);
        BlockPos rightPos = CoinMintBlock.otherPos(mintState, mintPos);
        Optional<NationRecord> territoryOwner = FTBIntegration.getNationAt(actor.serverLevel(), mintPos);
        Optional<NationRecord> rightTerritoryOwner = FTBIntegration.getNationAt(actor.serverLevel(), rightPos);
        if (territoryOwner.isEmpty() || rightTerritoryOwner.isEmpty()
                || !territoryOwner.get().nationId().equals(nation.nationId())
                || !rightTerritoryOwner.get().nationId().equals(nation.nationId())) {
            return send(actor, failure(
                    "Both halves of the Coin Mint must be inside territory claimed by your nation."));
        }
        NationCurrency currency = nation.currency().orElse(null);
        if (currency == null) {
            return send(actor, failure("Your nation has not created a currency."));
        }
        Optional<CoinDenomination> coinOptional = CoinDenomination.fromValue(denomination);
        if (coinOptional.isEmpty() || !currency.supportsDenomination(denomination)) {
            return send(actor, failure("Valid denominations: " + currency.denominations()));
        }
        if (!COIN_MINT_RUN_SIZES.contains(count)) {
            return send(actor, failure("Valid Coin Mint run sizes: " + COIN_MINT_RUN_SIZES));
        }
        CoinDenomination coin = coinOptional.get();
        if (countCoinBlanks(actor, coin) < count) {
            return send(actor, failure("You need " + count + " " + coin.blankName()
                    + (count == 1 ? "" : "s") + " for this minting run."));
        }

        NationActionResult result = mintToPlayer(actor, nation, actor, denomination, count,
                false, mintPos);
        if (result.success()) {
            consumeCoinBlanks(actor, coin, count);
            actor.getInventory().setChanged();
        }
        return result;
    }

    public static NationActionResult adminGiveCurrency(ServerPlayer operator, NationRecord nation,
                                                        ServerPlayer target, int denomination, int count) {
        return mintToPlayer(operator, nation, target, denomination, count, true, null);
    }

    private static NationActionResult mintToPlayer(ServerPlayer actor, NationRecord nation,
                                                    ServerPlayer target, int denomination,
                                                    int count, boolean adminAction,
                                                    BlockPos mintPos) {
        NationCurrency currency = nation.currency().orElse(null);
        if (currency == null) {
            return send(actor, failure(nation.name() + " has not created a currency yet."));
        }
        if (!currency.supportsDenomination(denomination)) {
            return send(actor, failure("Valid denominations: " + currency.denominations()));
        }
        if (count < 1 || count > 640) {
            return send(actor, failure("Count must be between 1 and 640."));
        }

        long amount;
        try {
            amount = Math.multiplyExact((long) denomination, (long) count);
        } catch (ArithmeticException exception) {
            return send(actor, failure("That issue amount is too large."));
        }
        if (!currency.mint(amount)) {
            return send(actor, failure("The currency supply limit was exceeded."));
        }

        giveCurrencyStacks(target, nation, denomination, count);
        String actorName = adminAction ? "Server operator " + actor.getGameProfile().getName()
                : actor.getGameProfile().getName();
        String targetName = target.getGameProfile().getName();
        String transactionType;
        String transactionNote;
        String historyText;
        String successText;
        if (adminAction) {
            transactionType = "testing override";
            transactionNote = "OP givecurrency testing override issued to " + targetName + ".";
            historyText = actorName + " issued " + format(currency, amount) + " to "
                    + targetName + " using the OP givecurrency testing override.";
            successText = "Testing override issued " + count + " x "
                    + coinName(denomination, currency.code()) + " to " + targetName + ".";
        } else {
            transactionType = "minted";
            String positionText = mintPos == null ? "unknown position"
                    : mintPos.getX() + ", " + mintPos.getY() + ", " + mintPos.getZ();
            transactionNote = "Produced at Coin Mint " + positionText + ".";
            historyText = actorName + " minted " + format(currency, amount)
                    + " at Coin Mint " + positionText + ".";
            successText = "Minted " + count + " x "
                    + coinName(denomination, currency.code()) + ".";
        }
        currency.addTransaction(System.currentTimeMillis(), transactionType, actorName, amount,
                transactionNote);
        NationSavedData data = NationSavedData.get(actor.server);
        data.addHistory(nation.nationId(), historyText);
        data.markChanged();
        return send(actor, success(successText));
    }

    public static NationActionResult depositPhysicalCurrency(ServerPlayer player) {
        NationSavedData data = NationSavedData.get(player.server);
        Optional<NationRecord> nationOptional = data.getNationForPlayer(player.getUUID());
        if (nationOptional.isEmpty()) {
            return send(player, failure("You do not belong to a nation."));
        }
        NationRecord nation = nationOptional.get();
        NationCurrency currency = nation.currency().orElse(null);
        if (currency == null) {
            return send(player, failure("Your nation has not created a currency."));
        }

        long amount = 0L;
        for (ItemStack stack : player.getInventory().items) {
            if (!CurrencyItems.matches(stack, currency.currencyId())) {
                continue;
            }
            long stackValue;
            try {
                stackValue = Math.multiplyExact((long) CurrencyItems.denomination(stack),
                        (long) stack.getCount());
                amount = Math.addExact(amount, stackValue);
            } catch (ArithmeticException exception) {
                return send(player, failure("The held currency total is too large to deposit."));
            }
        }
        if (amount <= 0L) {
            return send(player, failure("You are not carrying any " + currency.code() + " currency."));
        }
        if (Long.MAX_VALUE - currency.accountBalance(player.getUUID()) < amount) {
            return send(player, failure("Your digital account cannot hold that amount."));
        }

        for (ItemStack stack : player.getInventory().items) {
            if (CurrencyItems.matches(stack, currency.currencyId())) {
                stack.setCount(0);
            }
        }
        currency.creditAccount(player.getUUID(), amount);
        player.getInventory().setChanged();
        currency.addTransaction(System.currentTimeMillis(), "account deposit",
                player.getGameProfile().getName(), amount, "Physical currency deposited.");
        data.markChanged();
        return send(player, success("Deposited " + format(currency, amount)
                + " into your digital account."));
    }

    public static NationActionResult withdrawPhysicalCurrency(ServerPlayer player,
                                                               int denomination, int count) {
        NationSavedData data = NationSavedData.get(player.server);
        Optional<NationRecord> nationOptional = data.getNationForPlayer(player.getUUID());
        if (nationOptional.isEmpty()) {
            return send(player, failure("You do not belong to a nation."));
        }
        NationRecord nation = nationOptional.get();
        NationCurrency currency = nation.currency().orElse(null);
        if (currency == null) {
            return send(player, failure("Your nation has not created a currency."));
        }
        if (!currency.supportsDenomination(denomination)) {
            return send(player, failure("Valid denominations: " + currency.denominations()));
        }
        if (count < 1 || count > 640) {
            return send(player, failure("Count must be between 1 and 640."));
        }
        long amount;
        try {
            amount = Math.multiplyExact((long) denomination, (long) count);
        } catch (ArithmeticException exception) {
            return send(player, failure("That withdrawal is too large."));
        }
        if (!currency.debitAccount(player.getUUID(), amount)) {
            return send(player, failure("Your digital account does not contain "
                    + format(currency, amount) + "."));
        }
        giveCurrencyStacks(player, nation, denomination, count);
        currency.addTransaction(System.currentTimeMillis(), "account withdrawal",
                player.getGameProfile().getName(), amount, "Physical currency withdrawn.");
        data.markChanged();
        return send(player, success("Withdrew " + count + " x "
                + coinName(denomination, currency.code()) + "."));
    }

    public static NationActionResult depositTreasury(ServerPlayer player, long amount) {
        NationSavedData data = NationSavedData.get(player.server);
        Optional<NationRecord> nationOptional = data.getNationForPlayer(player.getUUID());
        if (nationOptional.isEmpty()) {
            return send(player, failure("You do not belong to a nation."));
        }
        NationRecord nation = nationOptional.get();
        NationCurrency currency = nation.currency().orElse(null);
        if (currency == null) {
            return send(player, failure("Your nation has not created a currency."));
        }
        if (!currency.depositToTreasury(player.getUUID(), amount)) {
            return send(player, failure("Your digital account does not contain "
                    + format(currency, amount) + "."));
        }
        currency.addTransaction(System.currentTimeMillis(), "treasury deposit",
                player.getGameProfile().getName(), amount, "Deposited into the nation treasury.");
        data.addHistory(nation.nationId(), player.getGameProfile().getName() + " deposited "
                + format(currency, amount) + " into the treasury.");
        data.markChanged();
        return send(player, success("Deposited " + format(currency, amount)
                + " into the nation treasury."));
    }

    public static NationActionResult withdrawTreasury(ServerPlayer player, long amount) {
        NationSavedData data = NationSavedData.get(player.server);
        Optional<NationRecord> nationOptional = data.getNationForPlayer(player.getUUID());
        if (nationOptional.isEmpty()) {
            return send(player, failure("You do not belong to a nation."));
        }
        NationRecord nation = nationOptional.get();
        boolean serverAdmin = nation.serverOwned() && player.hasPermissions(2);
        if (!serverAdmin && !nation.hasPermission(player.getUUID(), NationPermission.SPEND_TREASURY)) {
            return send(player, failure("Your role cannot withdraw treasury funds."));
        }
        NationCurrency currency = nation.currency().orElse(null);
        if (currency == null) {
            return send(player, failure("Your nation has not created a currency."));
        }
        if (!currency.withdrawFromTreasury(player.getUUID(), amount)) {
            return send(player, failure("The treasury does not contain " + format(currency, amount) + "."));
        }
        currency.addTransaction(System.currentTimeMillis(), "treasury withdrawal",
                player.getGameProfile().getName(), amount, "Withdrawn to a digital account.");
        data.addHistory(nation.nationId(), player.getGameProfile().getName() + " withdrew "
                + format(currency, amount) + " from the treasury.");
        data.markChanged();
        return send(player, success("Withdrew " + format(currency, amount)
                + " from the nation treasury."));
    }


    private static int countCoinBlanks(ServerPlayer player, CoinDenomination denomination) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (denomination.matchesBlank(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void consumeCoinBlanks(ServerPlayer player, CoinDenomination denomination,
                                          int amount) {
        int remaining = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (remaining <= 0) {
                break;
            }
            if (!denomination.matchesBlank(stack)) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
        }
    }

    private static void giveCurrencyStacks(ServerPlayer target, NationRecord nation,
                                           int denomination, int count) {
        int remaining = count;
        while (remaining > 0) {
            int stackCount = Math.min(64, remaining);
            ItemStack stack = CurrencyItems.create(nation, denomination, stackCount);
            if (!target.addItem(stack)) {
                target.drop(stack, false);
            }
            remaining -= stackCount;
        }
    }

    public static String format(NationCurrency currency, long amount) {
        return amount + " " + currency.code();
    }

    private static String coinName(int denomination, String currencyCode) {
        return CoinDenomination.fromValue(denomination)
                .map(type -> type.issuedName(currencyCode))
                .orElse(denomination + " " + currencyCode + " Coin");
    }

    private static NationActionResult send(ServerPlayer player, NationActionResult result) {
        Component message = result.message().copy().withStyle(
                result.success() ? ChatFormatting.GREEN : ChatFormatting.RED);
        player.sendSystemMessage(message);
        if (!result.success()) {
            player.displayClientMessage(message, true);
        }
        return result;
    }

    private static NationActionResult success(String message) {
        return NationActionResult.success(Component.literal(message));
    }

    private static NationActionResult failure(String message) {
        return NationActionResult.failure(Component.literal(message));
    }
}
