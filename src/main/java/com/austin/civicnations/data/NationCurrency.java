package com.austin.civicnations.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NationCurrency {
    public static final List<Integer> DEFAULT_DENOMINATIONS = List.of(1, 5, 10, 25, 50, 100);
    private static final int MAX_TRANSACTIONS = 250;

    private final UUID currencyId;
    private final String name;
    private final String code;
    private final String symbol;
    private final List<Integer> denominations;
    private long treasuryBalance;
    private long totalIssued;
    private final Map<UUID, Long> accounts;
    private final List<CurrencyTransaction> transactions;

    public NationCurrency(UUID currencyId, String name, String code, String symbol,
                          List<Integer> denominations, long treasuryBalance, long totalIssued,
                          Map<UUID, Long> accounts, List<CurrencyTransaction> transactions) {
        this.currencyId = currencyId;
        this.name = name;
        this.code = code;
        // Symbol is retained only for backward-compatible save loading. New currency
        // displays use the nation currency code instead of a separate symbol.
        this.symbol = symbol == null ? "" : symbol;
        // Civic Nations uses one universal physical denomination set. This also
        // migrates old 20-value currencies to the new 25-value Gold Coin.
        this.denominations = DEFAULT_DENOMINATIONS;
        this.treasuryBalance = Math.max(0L, treasuryBalance);
        this.totalIssued = Math.max(0L, totalIssued);
        this.accounts = new LinkedHashMap<>();
        accounts.forEach((player, balance) -> {
            if (player != null && balance != null && balance > 0L) {
                this.accounts.put(player, balance);
            }
        });
        this.transactions = new ArrayList<>(transactions);
        trimTransactions();
    }

    public static NationCurrency create(String name, String code) {
        return new NationCurrency(
                UUID.randomUUID(),
                name,
                code.toUpperCase(),
                "",
                DEFAULT_DENOMINATIONS,
                0L,
                0L,
                Map.of(),
                List.of()
        );
    }

    /** Backward-compatible overload for older callers; the symbol is ignored. */
    public static NationCurrency create(String name, String code, String symbol) {
        return create(name, code);
    }

    public UUID currencyId() { return currencyId; }
    public String name() { return name; }
    public String code() { return code; }
    public String symbol() { return symbol; }
    public List<Integer> denominations() { return denominations; }
    public long treasuryBalance() { return treasuryBalance; }
    public long totalIssued() { return totalIssued; }

    public Map<UUID, Long> accounts() {
        return Collections.unmodifiableMap(accounts);
    }

    public List<CurrencyTransaction> transactions() {
        return Collections.unmodifiableList(transactions);
    }

    public boolean supportsDenomination(int denomination) {
        return denominations.contains(denomination);
    }

    public long accountBalance(UUID playerId) {
        return accounts.getOrDefault(playerId, 0L);
    }

    public boolean creditAccount(UUID playerId, long amount) {
        if (playerId == null || amount <= 0L) {
            return false;
        }
        long current = accountBalance(playerId);
        if (Long.MAX_VALUE - current < amount) {
            return false;
        }
        accounts.put(playerId, current + amount);
        return true;
    }

    public boolean debitAccount(UUID playerId, long amount) {
        if (playerId == null || amount <= 0L) {
            return false;
        }
        long current = accountBalance(playerId);
        if (current < amount) {
            return false;
        }
        long remaining = current - amount;
        if (remaining == 0L) {
            accounts.remove(playerId);
        } else {
            accounts.put(playerId, remaining);
        }
        return true;
    }

    public boolean mint(long amount) {
        if (amount <= 0L || Long.MAX_VALUE - totalIssued < amount) {
            return false;
        }
        totalIssued += amount;
        return true;
    }

    public boolean depositToTreasury(UUID playerId, long amount) {
        if (amount <= 0L || treasuryBalance > Long.MAX_VALUE - amount) {
            return false;
        }
        if (!debitAccount(playerId, amount)) {
            return false;
        }
        treasuryBalance += amount;
        return true;
    }

    public boolean withdrawFromTreasury(UUID playerId, long amount) {
        if (amount <= 0L || treasuryBalance < amount) {
            return false;
        }
        if (!creditAccount(playerId, amount)) {
            return false;
        }
        treasuryBalance -= amount;
        return true;
    }

    public void addTransaction(long timestamp, String type, String actor, long amount, String note) {
        transactions.add(new CurrencyTransaction(timestamp, type, actor, amount, note));
        trimTransactions();
    }

    private void trimTransactions() {
        while (transactions.size() > MAX_TRANSACTIONS) {
            transactions.remove(0);
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("CurrencyId", currencyId);
        tag.putString("Name", name);
        tag.putString("Code", code);
        tag.putString("Symbol", symbol);
        tag.putLong("TreasuryBalance", treasuryBalance);
        tag.putLong("TotalIssued", totalIssued);

        ListTag denominationList = new ListTag();
        denominations.forEach(value -> denominationList.add(IntTag.valueOf(value)));
        tag.put("Denominations", denominationList);

        ListTag accountList = new ListTag();
        accounts.forEach((playerId, balance) -> {
            CompoundTag account = new CompoundTag();
            account.putUUID("Player", playerId);
            account.putLong("Balance", balance);
            accountList.add(account);
        });
        tag.put("Accounts", accountList);

        ListTag transactionList = new ListTag();
        transactions.forEach(transaction -> transactionList.add(transaction.save()));
        tag.put("Transactions", transactionList);
        return tag;
    }

    public static NationCurrency load(CompoundTag tag) {
        List<Integer> denominations = new ArrayList<>();
        if (tag.contains("Denominations", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Denominations", Tag.TAG_INT);
            for (Tag raw : list) {
                denominations.add(((IntTag) raw).getAsInt());
            }
        }
        if (denominations.isEmpty()) {
            denominations = DEFAULT_DENOMINATIONS;
        }

        Map<UUID, Long> accounts = new LinkedHashMap<>();
        if (tag.contains("Accounts", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Accounts", Tag.TAG_COMPOUND);
            for (Tag raw : list) {
                CompoundTag account = (CompoundTag) raw;
                if (account.hasUUID("Player")) {
                    accounts.put(account.getUUID("Player"), account.getLong("Balance"));
                }
            }
        }

        List<CurrencyTransaction> transactions = new ArrayList<>();
        if (tag.contains("Transactions", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Transactions", Tag.TAG_COMPOUND);
            for (Tag raw : list) {
                transactions.add(CurrencyTransaction.load((CompoundTag) raw));
            }
        }

        return new NationCurrency(
                tag.hasUUID("CurrencyId") ? tag.getUUID("CurrencyId") : UUID.randomUUID(),
                tag.getString("Name"),
                tag.getString("Code"),
                tag.getString("Symbol"),
                denominations,
                tag.getLong("TreasuryBalance"),
                tag.getLong("TotalIssued"),
                accounts,
                transactions
        );
    }
}
